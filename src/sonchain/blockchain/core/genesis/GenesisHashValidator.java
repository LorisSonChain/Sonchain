package sonchain.blockchain.core.genesis;

/**
 * 创世块验证器
 * @author GAIA
 *
 */
public class GenesisHashValidator {
	
    private String m_hash = "";
    private long m_number = 0;
    
	public String getHash() {
		return m_hash;
	}
	public void setHash(String m_hash) {
		this.m_hash = m_hash;
	}
    
    public long getNumber() {
		return m_number;
	}
    
	public void setNumber(long m_number) {
		this.m_number = m_number;
	}
}
