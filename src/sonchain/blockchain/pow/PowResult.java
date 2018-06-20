package sonchain.blockchain.pow;

import java.math.BigInteger;

/**
 * 工作量证明计算结果
 * @author GAIA
 *
 */
public class PowResult {
    /**
     * hash值
     */
    public byte[] m_hash; 

    /**
     * 计数器
     */
    public BigInteger m_nonce = BigInteger.ZERO;
    
    /**
     * 初始化
     */
    public PowResult()
    {    
    }
    
    /**
     * 初始化
     * @param hash
     * @param nonce
     */
    public PowResult(byte[] hash, BigInteger nonce)
    {
    	m_hash = hash;
    	m_nonce = nonce;
    }
}
