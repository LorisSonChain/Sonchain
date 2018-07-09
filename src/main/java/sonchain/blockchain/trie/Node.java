package sonchain.blockchain.trie;

import sonchain.blockchain.util.Value;

public class Node {

    /* RLP encoded value of the Trie-node */
    private Value m_value;
    private boolean m_dirty = false;

    public Node(Value val) {
        this(val, false);
    }

    public Node(Value val, boolean dirty) {
    	m_value = val;
    	m_dirty = dirty;
    }

    public Node copy() {
        return new Node(m_value, m_dirty);
    }

    public boolean isDirty() {
        return m_dirty;
    }

    public void setDirty(boolean dirty) {
        this.m_dirty = dirty;
    }

    public Value getValue() {
        return m_value;
    }

    @Override
    public String toString() {
        return "[" + m_dirty + ", " + m_value + "]";
    }
}