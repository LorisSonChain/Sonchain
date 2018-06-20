package sonchain.blockchain.db;

import java.io.Serializable;
import java.math.BigInteger;

/**
 * 区块信息定义
 * @author GAIA
 *
 */
public class BlockInfo implements Serializable {
    private byte[] m_hash = null;
    private boolean m_mainChain = true;

    public byte[] getHash() {
        return m_hash;
    }

    public void setHash(byte[] hash) {
        this.m_hash = hash;
    }

    public boolean isMainChain() {
        return m_mainChain;
    }

    public void setMainChain(boolean mainChain) {
        this.m_mainChain = mainChain;
    }
}