package sonchain.blockchain.trie;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import org.apache.commons.lang3.concurrent.ConcurrentUtils;
import org.apache.log4j.Logger;
import org.bouncycastle.util.encoders.Hex;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import sonchain.blockchain.crypto.HashUtil;
import sonchain.blockchain.datasource.Source;
import sonchain.blockchain.datasource.inmem.HashMapDB;
import sonchain.blockchain.util.ByteUtil;
import sonchain.blockchain.util.FastByteComparisons;
import sonchain.blockchain.util.RLP;

/**
 * TrieImpl
 *
 */
public class TrieImpl implements Trie<byte[]> {
	public static final Logger m_logger = Logger.getLogger(TrieImpl.class);
    private final static Object NULL_NODE = new Object();
    private final static int MIN_BRANCHES_CONCURRENTLY = 3;
    private static ExecutorService m_executor;
    
    public static ExecutorService getExecutor() {
        if (m_executor == null) {
        	m_logger.debug(" getExecutor start");
        	m_executor = Executors.newFixedThreadPool(4,
                    new ThreadFactoryBuilder().setNameFormat("trie-calc-thread-%d").build());
        	m_logger.debug(" getExecutor end");
        }
        return m_executor;
    }

    public enum NodeType {
        BranchNode,
        KVNodeValue,
        KVNodeNode
    }

    public final class Node {

        public Node() {
        	m_children = new Object[17];
            m_dirty = true;
        }
        public Node(TrieKey key, Object valueOrNode) {
            this(new Object[]{key, valueOrNode});
            m_dirty = true;
        }
        public Node(byte[] hashOrRlp) {
            if (hashOrRlp.length == 32) {
                this.m_hash = hashOrRlp;
            } else {
                this.m_rlp = hashOrRlp;
            }
        }
        private Node(RLP.LList parsedRlp) {
            this.m_parsedRlp = parsedRlp;
        }

        private Node(Object[] children) {
            this.m_children = children;
        }
        private byte[] m_hash = null;
        private byte[] m_rlp = null;
        private RLP.LList m_parsedRlp = null;
        private boolean m_dirty = false;
        private Object[] m_children = null;
        
        public byte[] encode() {
        	m_logger.debug("encode start.");
            return encode(1, true);
        }
        
        public boolean branchNodeCanCompact() {
        	m_logger.debug("branchNodeCanCompact start.");
        	parse();
            int cnt = 0;
            for (int i = 0; i < 16; i++) {
                cnt += branchNodeGetChild(i) == null ? 0 : 1;
                if (cnt > 1) return false;
            }
            return cnt == 0 || branchNodeGetValue() == null;
        }
        
        public int branchNodeCompactIdx() {
        	m_logger.debug("branchNodeCompactIdx start.");
        	parse();
            int cnt = 0;
            int idx = -1;
            for (int i = 0; i < 16; i++) {
                if (branchNodeGetChild(i) != null) {
                    cnt++;
                    idx = i;
                    if (cnt > 1) return -1;
                }
            }
            return cnt > 0 ? idx : (branchNodeGetValue() == null ? -1 : 16);
        }
        
        public Node branchNodeGetChild(int hex) {
        	m_logger.debug("branchNodeGetChild start.");
            parse();
            Object n = m_children[hex];
            if (n == null && m_parsedRlp != null) {
                if (m_parsedRlp.isList(hex)) {
                    n = new Node(m_parsedRlp.getList(hex));
                } else {
                    byte[] bytes = m_parsedRlp.getBytes(hex);
                    if (bytes.length == 0) {
                        n = NULL_NODE;
                    } else {
                        n = new Node(bytes);
                    }
                }
                m_children[hex] = n;
            }
            return n == NULL_NODE ? null : (Node) n;
        }
        
        public Node branchNodeSetChild(int hex, Node node) {
        	m_logger.debug("branchNodeSetChild start.");
        	parse();
            m_children[hex] = node == null ? NULL_NODE : node;
            m_dirty = true;
            return this;
        }
        
        public byte[] branchNodeGetValue() {
        	m_logger.debug("branchNodeGetValue start.");
        	parse();
            Object n = m_children[16];
            if (n == null && m_parsedRlp != null) {
                byte[] bytes = m_parsedRlp.getBytes(16);
                if (bytes.length == 0) {
                    n = NULL_NODE;
                } else {
                    n = bytes;
                }
                m_children[16] = n;
            }
            return n == NULL_NODE ? null : (byte[]) n;
        }
        
        public Node branchNodeSetValue(byte[] val) {
        	m_logger.debug("branchNodeSetValue start.");
        	parse();
            m_children[16] = val == null ? NULL_NODE : val;
            m_dirty = true;
            return this;
        }

        private String dumpContent(boolean recursion, boolean compact) {
        	m_logger.debug("dumpContent start.");
            if (recursion && m_hash != null) {
            	return hash2str(m_hash, compact);
            }
            String ret;
            if (getType() == NodeType.BranchNode) {
                ret = "[";
                for (int i = 0; i < 16; i++) {
                    Node child = branchNodeGetChild(i);
                    ret += i == 0 ? "" : ",";
                    ret += child == null ? "" : child.dumpContent(true, compact);
                }
                byte[] value = branchNodeGetValue();
                ret += value == null ? "" : ", " + val2str(value, compact);
                ret += "]";
            } else if (getType() == NodeType.KVNodeNode) {
                ret = "[<" + kvNodeGetKey() + ">, " + kvNodeGetChildNode().dumpContent(true, compact) + "]";
            } else {
                ret = "[<" + kvNodeGetKey() + ">, " + val2str(kvNodeGetValue(), compact) + "]";
            }
        	m_logger.debug("dumpContent end.");
            return ret;
        }
        
        public String dumpStruct(String indent, String prefix) {
        	m_logger.debug("dumpStruct start:" + indent + "prefix:" + prefix);
            String ret = indent + prefix + getType() + (m_dirty ? " *" : "") +
                    (m_hash == null ? "" : "(hash: " + Hex.toHexString(m_hash).substring(0, 6) + ")");
            if (getType() == NodeType.BranchNode) {
                byte[] value = branchNodeGetValue();
                ret += (value == null ? "" : " [T] = " + Hex.toHexString(value)) + "\n";
                for (int i = 0; i < 16; i++) {
                    Node child = branchNodeGetChild(i);
                    if (child != null) {
                        ret += child.dumpStruct(indent + "  ", "[" + i + "] ");
                    }
                }

            } else if (getType() == NodeType.KVNodeNode) {
                ret += " [" + kvNodeGetKey() + "]\n";
                ret += kvNodeGetChildNode().dumpStruct(indent + "  ", "");
            } else {
                ret += " [" + kvNodeGetKey() + "] = " + Hex.toHexString(kvNodeGetValue()) + "\n";
            }
        	m_logger.debug("dumpStruct end:" + indent + "prefix:" + prefix);
            return ret;
        }

        public List<String> dumpTrieNode(boolean compact) {
        	m_logger.debug("dumpTrieNode start");
            List<String> ret = new ArrayList<>();
            if (m_hash != null) {
                ret.add(hash2str(m_hash, compact) + " ==> " + dumpContent(false, compact));
            }

            if (getType() == NodeType.BranchNode) {
                for (int i = 0; i < 16; i++) {
                    Node child = branchNodeGetChild(i);
                    if (child != null) ret.addAll(child.dumpTrieNode(compact));
                }
            } else if (getType() == NodeType.KVNodeNode) {
                ret.addAll(kvNodeGetChildNode().dumpTrieNode(compact));
            }
        	m_logger.debug("dumpTrieNode end");
            return ret;
        }
        
        private byte[] encode(final int depth, boolean forceHash) {
        	m_logger.debug("encode start depth:" + depth + " forceHash: " + forceHash);
            if (!m_dirty) {
                return m_hash != null ? RLP.encodeElement(m_hash) : m_rlp;
            } else {
                NodeType type = getType();
                byte[] ret;
                if (type == NodeType.BranchNode) {
                    if (depth == 1 && m_async) {
                        // parallelize encode() on the first trie level only and if there are at least
                        // MIN_BRANCHES_CONCURRENTLY branches are modified
                        final Object[] encoded = new Object[17];
                        int encodeCnt = 0;
                        for (int i = 0; i < 16; i++) {
                            final Node child = branchNodeGetChild(i);
                            if (child == null) {
                                encoded[i] = RLP.EMPTY_ELEMENT_RLP;
                            } else if (!child.m_dirty) {
                                encoded[i] = child.encode(depth + 1, false);
                            } else {
                                encodeCnt++;
                            }
                        }
                        for (int i = 0; i < 16; i++) {
                            if (encoded[i] == null) {
                                final Node child = branchNodeGetChild(i);
                                if (encodeCnt >= MIN_BRANCHES_CONCURRENTLY) {
                                    encoded[i] = getExecutor().submit(new Callable<byte[]>() {
                                        @Override
                                        public byte[] call() throws Exception {
                                        	m_logger.debug("encoded call result:" + depth);
                                            return child.encode(depth + 1, false);
                                        }
                                    });
                                } else {
                                    encoded[i] = child.encode(depth + 1, false);
                                }
                            }
                        }
                        byte[] value = branchNodeGetValue();
                        encoded[16] = ConcurrentUtils.constantFuture(RLP.encodeElement(value));
                        try {
                            ret = encodeRlpListFutures(encoded);
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    } else {
                        byte[][] encoded = new byte[17][];
                        for (int i = 0; i < 16; i++) {
                            Node child = branchNodeGetChild(i);
                            encoded[i] = child == null ? RLP.EMPTY_ELEMENT_RLP : child.encode(depth + 1, false);
                        }
                        byte[] value = branchNodeGetValue();
                        encoded[16] = RLP.encodeElement(value);
                        ret = RLP.encodeList(encoded);
                    }
                } else if (type == NodeType.KVNodeNode) {
                    ret = RLP.encodeList(RLP.encodeElement(kvNodeGetKey().toPacked()), kvNodeGetChildNode().encode(depth + 1, false));
                } else {
                    byte[] value = kvNodeGetValue();
                    ret = RLP.encodeList(RLP.encodeElement(kvNodeGetKey().toPacked()),
                    		RLP.encodeElement(value == null ? ByteUtil.EMPTY_BYTE_ARRAY : value));
                }
                if (m_hash != null) {
                    deleteHash(m_hash);
                }
                m_dirty = false;
                if (ret.length < 32 && !forceHash) {
                	m_rlp = ret;
                	m_logger.debug("encode end result:" + Hex.toHexString(ret));
                    return ret;
                } else {
                	m_hash = HashUtil.sha3(ret);
                    addHash(m_hash, ret);
                	m_logger.debug("encode end result:" + Hex.toHexString(m_hash));
                    return RLP.encodeElement(m_hash);
                }
            }
        }
        
        @SafeVarargs
        private final byte[] encodeRlpListFutures(Object... list) throws ExecutionException, InterruptedException {
            byte[][] vals = new byte[list.length][];
            for (int i = 0; i < list.length; i++) {
                if (list[i] instanceof Future) {
                    vals[i] = ((Future<byte[]>) list[i]).get();
                } else {
                    vals[i] = (byte[]) list[i];
                }
            }
            return RLP.encodeList(vals);
        }

        public void dispose() {
            if (m_hash != null) {
                deleteHash(m_hash);
            }
        }

        public NodeType getType() {
        	parse();
            return m_children.length == 17 ? NodeType.BranchNode :
                    (m_children[1] instanceof Node ? NodeType.KVNodeNode : NodeType.KVNodeValue);
        }
        
        public Node invalidate() {
        	m_dirty = true;
            return this;
        }
        
        public TrieKey kvNodeGetKey() {
        	parse();
            return (TrieKey) m_children[0];
        }

        public Node kvNodeGetChildNode() {
        	parse();
            return (Node) m_children[1];
        }

        public byte[] kvNodeGetValue() {
        	parse();
            return (byte[]) m_children[1];
        }
        
        public Node kvNodeSetValue(byte[] value) {
        	parse();
            m_children[1] = value;
            m_dirty = true;
            return this;
        }

        public Object kvNodeGetValueOrNode() {
        	parse();
            return m_children[1];
        }

        public Node kvNodeSetValueOrNode(Object valueOrNode) {
        	parse();
            m_children[1] = valueOrNode;
            m_dirty = true;
            return this;
        }

        @Override
        public String toString() {
            return getType() + (m_dirty ? " *" : "") + (m_hash == null ? "" : "(hash: " + Hex.toHexString(m_hash) + " )");
        }
        
        /**
         * 解析
         */
        private void parse() {
            if (m_children != null)
            {
            	return;
            }
            resolve();
            RLP.LList list = m_parsedRlp == null ? RLP.decodeLazyList(m_rlp) : m_parsedRlp;
            if (list.size() == 2) {
            	m_children = new Object[2];
                TrieKey key = TrieKey.fromPacked(list.getBytes(0));
                m_children[0] = key;
                if (key.isTerminal()) {
                	m_children[1] = list.getBytes(1);
                } else {
                	m_children[1] = list.isList(1) ? new Node(list.getList(1)) : new Node(list.getBytes(1));
                }
            } else {
            	m_children = new Object[17];
                m_parsedRlp = list;
            }
        }

        /**
         * 
         */
        private void resolve() {
            if (!resolveCheck()) {
                throw new RuntimeException("Invalid Trie state, can't resolve hash " + Hex.toHexString(m_hash));
            }
        }

        /**
         * 
         * @return
         */
        public boolean resolveCheck() {
            if (m_rlp != null || m_parsedRlp != null || m_hash == null) 
            {
            	return true;
            }
            m_rlp = getHash(m_hash);
            return m_rlp != null;
        }
        
    }

    public interface ScanAction {
        void doOnNode(byte[] hash, Node node);
        void doOnValue(byte[] nodeHash, Node node, byte[] key, byte[] value);
    }

    private boolean m_async = true;
    private Source<byte[], byte[]> m_cache = null;
    private Node m_root = null;

    public TrieImpl() {
        this((byte[]) null);
    }

    public TrieImpl(byte[] root) {
        this(new HashMapDB<byte[]>(), root);
    }

    public TrieImpl(Source<byte[], byte[]> cache) {
        this(cache, null);
    }
    
    public TrieImpl(Source<byte[], byte[]> cache, byte[] root) {
    	m_cache = cache;
        setRoot(root);
    }
    
    private void addHash(byte[] hash, byte[] ret) {
    	m_cache.put(hash, ret);
    }
    
    @Override
    public void clear() {
        throw new RuntimeException("Not implemented yet");
    }

    @Override
    public void delete(byte[] key) {
        TrieKey k = TrieKey.fromNormal(key);
        if (m_root != null) {
        	m_root = delete(m_root, k);
        }
    }

    private Node delete(Node n, TrieKey k) {
        NodeType type = n.getType();
        Node newKvNode;
        if (type == NodeType.BranchNode) {
            if (k.isEmpty())  {
                n.branchNodeSetValue(null);
            } else {
                int idx = k.getHex(0);
                Node child = n.branchNodeGetChild(idx);
                if (child == null) return n; // no key found

                Node newNode = delete(child, k.shift(1));
                n.branchNodeSetChild(idx, newNode);
                if (newNode != null) return n; // newNode != null thus number of children didn't decrease
            }

            // child node or value was deleted and the branch node may need to be compacted
            int compactIdx = n.branchNodeCompactIdx();
            if (compactIdx < 0) {
            	return n; // no compaction is required
            }
            // only value or a single child left - compact branch node to kvNode
            n.dispose();
            if (compactIdx == 16) { // only value left
                return new Node(TrieKey.empty(true), n.branchNodeGetValue());
            } else { // only single child left
                newKvNode = new Node(TrieKey.singleHex(compactIdx), n.branchNodeGetChild(compactIdx));
            }
        } else { // n - kvNode
            TrieKey k1 = k.matchAndShift(n.kvNodeGetKey());
            if (k1 == null) {
                // no key found
                return n;
            } else if (type == NodeType.KVNodeValue) {
                if (k1.isEmpty()) {
                    // delete this kvNode
                    n.dispose();
                    return null;
                } else {
                    // else no key found
                    return n;
                }
            } else {
                Node newChild = delete(n.kvNodeGetChildNode(), k1);
                if (newChild == null) {
                	throw new RuntimeException("Shouldn't happen");
                }
                newKvNode = n.kvNodeSetValueOrNode(newChild);
            }
        }

        // if we get here a new kvNode was created, now need to check
        // if it should be compacted with child kvNode
        Node newChild = newKvNode.kvNodeGetChildNode();
        if (newChild.getType() != NodeType.BranchNode) {
            // two kvNodes should be compacted into a single one
            TrieKey newKey = newKvNode.kvNodeGetKey().concat(newChild.kvNodeGetKey());
            Node newNode = new Node(newKey, newChild.kvNodeGetValueOrNode());
            newChild.dispose();
            return newNode;
        } else {
            // no compaction needed
            return newKvNode;
        }
    }
    
    private void deleteHash(byte[] hash) {
    	m_cache.delete(hash);
    }
    
    public String DumpStructure() {
        return m_root == null ? "<empty>" : m_root.dumpStruct("", "");
    }
    
    public String DumpTrie() {
        return DumpTrie(true);
    }
    
    public String DumpTrie(boolean compact) {
        if (m_root == null) {
        	return "<empty>";
        }
        encode();
        StringBuilder ret = new StringBuilder();
        List<String> strings = m_root.dumpTrieNode(compact);
        ret.append("Root: " + hash2str(getRootHash(), compact) + "\n");
        for (String s : strings) {
            ret.append(s).append('\n');
        }
        return ret.toString();
    }

    private void encode() {
        if (m_root != null) {
        	m_root.encode();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()){
        	return false;
        }
        TrieImpl trieImpl1 = (TrieImpl) o;
        return FastByteComparisons.equal(getRootHash(), trieImpl1.getRootHash());
    }

    @Override
    public boolean flush() {
        if (m_root != null && m_root.m_dirty) {
            // persist all dirty nodes to underlying Source
            encode();
            // release all Trie Node instances for GC
            m_root = new Node(m_root.m_hash);
            return true;
        } else {
            return false;
        }
    }
    
    public byte[] get(byte[] key) {
        if (!hasRoot()) {
        	return null; // treating unknown root hash as empty trie
        }
        TrieKey k = TrieKey.fromNormal(key);
        return get(m_root, k);
    }

    private byte[] get(Node n, TrieKey k) {
        if (n == null) return null;

        NodeType type = n.getType();
        if (type == NodeType.BranchNode) {
            if (k.isEmpty()) {
            	return n.branchNodeGetValue();
            }
            Node childNode = n.branchNodeGetChild(k.getHex(0));
            return get(childNode, k.shift(1));
        } else {
            TrieKey k1 = k.matchAndShift(n.kvNodeGetKey());
            if (k1 == null) return null;
            if (type == NodeType.KVNodeValue) {
                return k1.isEmpty() ? n.kvNodeGetValue() : null;
            } else {
                return get(n.kvNodeGetChildNode(), k1);
            }
        }
    }

    public Source<byte[], byte[]> getCache() {
        return m_cache;
    }

    private byte[] getHash(byte[] hash) {
        return m_cache.get(hash);
    }

    @Override
    public byte[] getRootHash() {
        encode();
        return m_root != null ? m_root.m_hash : HashUtil.EMPTY_TRIE_HASH;
    }

    private static String hash2str(byte[] hash, boolean shortHash) {
        String ret = Hex.toHexString(hash);
        return "0x" + (shortHash ? ret.substring(0,8) : ret);
    }

    private boolean hasRoot() {
        return m_root != null && m_root.resolveCheck();
    }

    private Node insert(Node n, TrieKey k, Object nodeOrValue) {
        NodeType type = n.getType();
        if (type == NodeType.BranchNode) {
            if (k.isEmpty()) {
            	return n.branchNodeSetValue((byte[]) nodeOrValue);
            }
            Node childNode = n.branchNodeGetChild(k.getHex(0));
            if (childNode != null) {
                return n.branchNodeSetChild(k.getHex(0), insert(childNode, k.shift(1), nodeOrValue));
            } else {
                TrieKey childKey = k.shift(1);
                Node newChildNode;
                if (!childKey.isEmpty()) {
                    newChildNode = new Node(childKey, nodeOrValue);
                } else {
                    newChildNode = nodeOrValue instanceof Node ?
                            (Node) nodeOrValue : new Node(childKey, nodeOrValue);
                }
                return n.branchNodeSetChild(k.getHex(0), newChildNode);
            }
        } else {
            TrieKey commonPrefix = k.getCommonPrefix(n.kvNodeGetKey());
            if (commonPrefix.isEmpty()) {
                Node newBranchNode = new Node();
                insert(newBranchNode, n.kvNodeGetKey(), n.kvNodeGetValueOrNode());
                insert(newBranchNode, k, nodeOrValue);
                n.dispose();
                return newBranchNode;
            } else if (commonPrefix.equals(k)) {
                return n.kvNodeSetValueOrNode(nodeOrValue);
            } else if (commonPrefix.equals(n.kvNodeGetKey())) {
                insert(n.kvNodeGetChildNode(), k.shift(commonPrefix.getLength()), nodeOrValue);
                return n.invalidate();
            } else {
                Node newBranchNode = new Node();
                Node newKvNode = new Node(commonPrefix, newBranchNode);
                // TODO can be optimized
                insert(newKvNode, n.kvNodeGetKey(), n.kvNodeGetValueOrNode());
                insert(newKvNode, k, nodeOrValue);
                n.dispose();
                return newKvNode;
            }
        }
    }

    public void put(byte[] key, byte[] value) {
        TrieKey k = TrieKey.fromNormal(key);
        if (m_root == null) {
            if (value != null && value.length > 0) {
            	m_root = new Node(k, value);
            }
        } else {
            if (value == null || value.length == 0) {
            	m_root = delete(m_root, k);
            } else {
            	m_root = insert(m_root, k, value);
            }
        }
    }

    public void scanTree(ScanAction scanAction) {
        scanTree(m_root, TrieKey.empty(false), scanAction);
    }

    public void scanTree(Node node, TrieKey k, ScanAction scanAction) {
        if (node == null) return;
        if (node.m_hash != null) {
            scanAction.doOnNode(node.m_hash, node);
        }
        if (node.getType() == NodeType.BranchNode) {
            if (node.branchNodeGetValue() != null)
                scanAction.doOnValue(node.m_hash, node, k.toNormal(), node.branchNodeGetValue());
            for (int i = 0; i < 16; i++) {
            	scanTree(node.branchNodeGetChild(i), k.concat(TrieKey.singleHex(i)), scanAction);
            }
        } else if (node.getType() == NodeType.KVNodeNode) {
        	scanTree(node.kvNodeGetChildNode(), k.concat(node.kvNodeGetKey()), scanAction);
        } else {
            scanAction.doOnValue(node.m_hash, node, k.concat(node.kvNodeGetKey()).toNormal(), node.kvNodeGetValue());
        }
    }

    public void setAsync(boolean async) {
    	m_async = async;
    }

    public void setRoot(byte[] root) {
        if (root != null && !FastByteComparisons.equal(root, HashUtil.EMPTY_TRIE_HASH)) {
        	m_root = new Node(root);
        } else {
        	m_root = null;
        }
    }

    private static String val2str(byte[] val, boolean shortHash) {
        String ret = Hex.toHexString(val);
        if (val.length > 16) {
            ret = ret.substring(0,10) + "... len " + val.length;
        }
        return "\"" + ret + "\"";
    }
}
