package sonchain.blockchain.core.genesis;

import java.util.HashMap;
import java.util.Map;

/**
 *
 */
public class AllocatedAccount {
	private String m_balance = "";
	private String m_code = "";
	private String m_nonce = "";
	private Map<String, String> m_storage = new HashMap<String, String>();

	/**
	 * Balance
	 * @return
	 */
	public String getBalance() {
		return m_balance;
	}
	
	/**
	 * Balance
	 * @param m_balance
	 */
	public void setBalance(String balance) {
		m_balance = balance;
	}
	
	/**
	 * @return
	 */
	public String getCode() {
		return m_code;
	}
	
	/**
	 * @param m_code
	 */
	public void setCode(String code) {
		m_code = code;
	}
	
	/**
	 * @return
	 */
	public String getNonce() {
		return m_nonce;
	}
	
	/**
	 * @param m_nonce
	 */
	public void setMonce(String nonce) {
		m_nonce = nonce;
	}
	
	public Map<String, String> GetStorage() {
		return m_storage;
	}
	
	public void SetStorage(Map<String, String> storage) {
		m_storage = storage;
	}
}
