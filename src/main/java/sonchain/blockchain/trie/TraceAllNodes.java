package sonchain.blockchain.trie;

import org.bouncycastle.util.encoders.Hex;

public class TraceAllNodes implements TrieImpl.ScanAction {

    private StringBuilder m_output = new StringBuilder();

    public String getOutput() {
        return m_output.toString();
    }

    @Override
    public void doOnNode(byte[] hash, TrieImpl.Node node) {
    	m_output.append(Hex.toHexString(hash)).append(" ==> ").append(node.toString()).append("\n");
    }

    @Override
    public void doOnValue(byte[] nodeHash, TrieImpl.Node node, byte[] key, byte[] value) {}
}