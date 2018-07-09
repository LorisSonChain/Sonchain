package sonchain.blockchain.trie;

import org.bouncycastle.util.encoders.Hex;

import sonchain.blockchain.util.ByteUtil;

public final class TrieKey {
    public static final int ODD_OFFSET_FLAG = 0x1;
    public static final int TERMINATOR_FLAG = 0x2;
    private byte[] m_key = null;
    private int m_off = 0;
    private boolean m_terminal = false;

    public TrieKey(byte[] key, int off, boolean terminal) {
    	m_terminal = terminal;
    	m_off = off;
    	m_key = key;
    }

    private TrieKey(byte[] key) {
        this(key, 0, true);
    }

    public TrieKey concat(TrieKey k) {
        if (isTerminal()) {
        	throw new RuntimeException("Can' append to terminal key: " + this + " + " + k);
        }
        int len = getLength();
        int kLen = k.getLength();
        int newLen = len + kLen;
        byte[] newKeyBytes = new byte[(newLen + 1) >> 1];
        TrieKey ret = new TrieKey(newKeyBytes, newLen & 1, k.isTerminal());
        for (int i = 0; i < len; i++) {
            ret.setHex(i, getHex(i));
        }
        for (int i = 0; i < kLen; i++) {
            ret.setHex(len + i, k.getHex(i));
        }
        return ret;
    }

    public static TrieKey empty(boolean terminal) {
        return new TrieKey(ByteUtil.EMPTY_BYTE_ARRAY, 0, terminal);
    }
    
    @Override
    public boolean equals(Object obj) {
        TrieKey k = (TrieKey) obj;
        int len = getLength();

        if (len != k.getLength()){
        	return false;
        }
        // TODO can be optimized
        for (int i = 0; i < len; i++) {
            if (getHex(i) != k.getHex(i)) {
            	return false;
            }
        }
        return isTerminal() == k.isTerminal();
    }

    public static TrieKey fromNormal(byte[] key) {
        return new TrieKey(key);
    }

    public static TrieKey fromPacked(byte[] key) {
        return new TrieKey(key, ((key[0] >> 4) & ODD_OFFSET_FLAG) != 0 ? 1 : 2, 
        		((key[0] >> 4) & TERMINATOR_FLAG) != 0);
    }

    public TrieKey getCommonPrefix(TrieKey k) {
        // TODO can be optimized
        int prefixLen = 0;
        int thisLenght = getLength();
        int kLength = k.getLength();
        while (prefixLen < thisLenght && prefixLen < kLength && getHex(prefixLen) == k.getHex(prefixLen)){
            prefixLen++;
        }
        byte[] prefixKey = new byte[(prefixLen + 1) >> 1];
        TrieKey ret = new TrieKey(prefixKey, (prefixLen & 1) == 0 ? 0 : 1,
                prefixLen == getLength() && prefixLen == k.getLength() && m_terminal && k.isTerminal());
        for (int i = 0; i < prefixLen; i++) {
            ret.setHex(i, k.getHex(i));
        }
        return ret;
    }

    public int getHex(int idx) {
        byte b = m_key[(m_off + idx) >> 1];
        return (((m_off + idx) & 1) == 0 ? (b >> 4) : b) & 0xF;
    }

    public int getLength() {
        return (m_key.length << 1) - m_off;
    }

    public boolean isEmpty() {
        return getLength() == 0;
    }

    public boolean isTerminal() {
        return m_terminal;
    }

    public TrieKey matchAndShift(TrieKey k) {
        int len = getLength();
        int kLen = k.getLength();
        if (len < kLen) {
        	return null;
        }

        if ((m_off & 1) == (k.m_off & 1)) {
            // optimization to compare whole keys bytes
            if ((m_off & 1) == 1) {
                if (getHex(0) != k.getHex(0)) {
                	return null;
                }
            }
            int idx1 = (m_off + 1) >> 1;
            int idx2 = (k.m_off + 1) >> 1;
            int l = kLen >> 1;
            for (int i = 0; i < l; i++, idx1++, idx2++) {
                if (m_key[idx1] != k.m_key[idx2]){
                	return null;
                }
            }
        } else {
            for (int i = 0; i < kLen; i++) {
                if (getHex(i) != k.getHex(i)) {
                	return null;
                }
            }
        }
        return shift(kLen);
    }

    private void setHex(int idx, int hex) {
        int byteIdx = (m_off + idx) >> 1;
        if (((m_off + idx) & 1) == 0) {
        	m_key[byteIdx] &= 0x0F;
        	m_key[byteIdx] |= hex << 4;
        } else {
        	m_key[byteIdx] &= 0xF0;
        	m_key[byteIdx] |= hex;
        }
    }
    
    public TrieKey shift(int hexCnt) {
        return new TrieKey(this.m_key, m_off + hexCnt, m_terminal);
    }

    public static TrieKey singleHex(int hex) {
        TrieKey ret = new TrieKey(new byte[1], 1, false);
        ret.setHex(0, hex);
        return ret;
    }

    public byte[] toNormal() {
        if ((m_off & 1) != 0){
        	throw new RuntimeException("Can't convert a key with odd number of hexes to normal: " + this);
        }
        int arrLen = m_key.length - m_off / 2;
        byte[] ret = new byte[arrLen];
        System.arraycopy(m_key, m_key.length - arrLen, ret, 0, arrLen);
        return ret;
    }

    public byte[] toPacked() {
        int flags = ((m_off & 1) != 0 ? ODD_OFFSET_FLAG : 0) | (m_terminal ? TERMINATOR_FLAG : 0);
        byte[] ret = new byte[getLength() / 2 + 1];
        int toCopy = (flags & ODD_OFFSET_FLAG) != 0 ? ret.length : ret.length - 1;
        System.arraycopy(m_key, m_key.length - toCopy, ret, ret.length - toCopy, toCopy);
        ret[0] &= 0x0F;
        ret[0] |= flags << 4;
        return ret;
    }

    @Override
    public String toString() {
        return Hex.toHexString(m_key).substring(m_off) + (isTerminal() ? "T" : "");
    }
}