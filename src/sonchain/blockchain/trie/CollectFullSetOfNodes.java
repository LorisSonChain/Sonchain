package sonchain.blockchain.trie;

import java.util.HashSet;
import java.util.Set;

import sonchain.blockchain.db.ByteArrayWrapper;

public class CollectFullSetOfNodes implements TrieImpl.ScanAction {
    private Set<ByteArrayWrapper> m_nodes = new HashSet<>();

    public Set<ByteArrayWrapper> getCollectedHashes() {
        return m_nodes;
    }

    @Override
    public void doOnNode(byte[] hash, TrieImpl.Node node) {
    	m_nodes.add(new ByteArrayWrapper(hash));
    }

    @Override
    public void doOnValue(byte[] nodeHash, TrieImpl.Node node, byte[] key, byte[] value) {}
}
