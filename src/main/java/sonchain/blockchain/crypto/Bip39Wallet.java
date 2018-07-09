package sonchain.blockchain.crypto;

/**
 *
 */
public class Bip39Wallet {

	/**
	 * Init
	 * 
	 * @param filename
	 * @param mnemonic
	 */
	public Bip39Wallet(String filename, String mnemonic) {
		m_filename = filename;
		m_mnemonic = mnemonic;
	}

	private final String m_filename;

	private final String m_mnemonic;

	/**
	 * 
	 * @return
	 */
	public String getFilename() {
		return m_filename;
	}

	/**
	 * 
	 * @return
	 */
	public String getMnemonic() {
		return m_mnemonic;
	}

	/**
	 */
	@Override
	public String toString() {
		return "Bip39Wallet{" + "filename='" + m_filename + '\'' + ", mnemonic='" + m_mnemonic + '\'' + '}';
	}
}
