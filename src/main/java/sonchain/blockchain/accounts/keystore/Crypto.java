package sonchain.blockchain.accounts.keystore;

/**
 * Crypto
 *
 */
public class Crypto {
	public String m_cipher = "";
	public String m_ciphertext = "";
	public Cipherparams m_cipherparams = null;
	public String m_kdf = "scrypt";
	public ScryptKdfParams m_kdfParams = null;
	public String m_mac = "";
}
