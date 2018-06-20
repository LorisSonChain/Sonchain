package sonchain.blockchain.trie;

public class CountAllNodes implements TrieImpl.ScanAction {

    private int m_counted = 0;

    public int getCounted() {
        return m_counted;
    }

    @Override
    public void doOnNode(byte[] hash, TrieImpl.Node node) {
        ++m_counted;
    }

    @Override
    public void doOnValue(byte[] nodeHash, TrieImpl.Node node, byte[] key, byte[] value) {}
}
