package sonchain.blockchain.core.genesis;

import java.util.HashMap;
import java.util.Map;

/**
 * 已经分配的账户定义
 * @author GAIA
 *
 */
public class AllocatedAccount {
	private String m_balance = "";
	private String m_code = "";
	private String m_nonce = "";
	private Map<String, String> m_storage = new HashMap<String, String>();

	/**
	 * 获取余额
	 * @return
	 */
	public String getBalance() {
		return m_balance;
	}
	
	/**
	 * 设置余额
	 * @param m_balance
	 */
	public void setBalance(String balance) {
		m_balance = balance;
	}
	
	/**
	 * 获取代码
	 * @return
	 */
	public String getCode() {
		return m_code;
	}
	
	/**
	 * 设置代码
	 * @param m_code
	 */
	public void setCode(String code) {
		m_code = code;
	}
	
	/**
	 * 获取一个64位随机数
	 * @return
	 */
	public String getNonce() {
		return m_nonce;
	}
	
	/**
	 * 设置一个64位随机数
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
