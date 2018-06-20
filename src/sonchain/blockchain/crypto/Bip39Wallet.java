package sonchain.blockchain.crypto;

/**
 * Bip39钱包
 * 
 * @author GAIA
 *
 */
public class Bip39Wallet {

	/**
	 * 初始化
	 * 
	 * @param filename
	 * @param mnemonic
	 */
	public Bip39Wallet(String filename, String mnemonic) {
		m_filename = filename;
		m_mnemonic = mnemonic;
	}

	/**
	 * 钱包路径
	 */
	private final String m_filename;

	/**
	 * 钱包BIP-39助记词
	 */
	private final String m_mnemonic;

	/**
	 * 获取文件名称
	 * 
	 * @return
	 */
	public String getFilename() {
		return m_filename;
	}

	/**
	 * 获取助记词
	 * 
	 * @return
	 */
	public String getMnemonic() {
		return m_mnemonic;
	}

	/**
	 * 转成字符串
	 */
	@Override
	public String toString() {
		return "Bip39Wallet{" + "filename='" + m_filename + '\'' + ", mnemonic='" + m_mnemonic + '\'' + '}';
	}
}
