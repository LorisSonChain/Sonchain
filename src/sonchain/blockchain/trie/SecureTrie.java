package sonchain.blockchain.trie;

import sonchain.blockchain.crypto.HashUtil;
import sonchain.blockchain.datasource.Source;
import sonchain.blockchain.util.ByteUtil;

public class SecureTrie extends TrieImpl {

    public SecureTrie(byte[] root) {
        super(root);
    }

    public SecureTrie(Source<byte[], byte[]> cache) {
        super(cache, null);
    }

    public SecureTrie(Source<byte[], byte[]> cache, byte[] root) {
        super(cache, root);
    }

    @Override
    public void delete(byte[] key) {
        put(key, ByteUtil.EMPTY_BYTE_ARRAY);
    }

    @Override
    public byte[] get(byte[] key) {
        return super.get(HashUtil.sha3(key));
    }

    @Override
    public void put(byte[] key, byte[] value) {
        super.put(HashUtil.sha3(key), value);
    }
}
