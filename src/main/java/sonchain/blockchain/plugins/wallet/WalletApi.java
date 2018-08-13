package sonchain.blockchain.plugins.wallet;

import java.util.Map;
import java.util.Set;

public interface WalletApi {

	/**
     * Get the private key corresponding to a public key.  The
     * private key must already be in the wallet.
	 * @param pubKey
	 * @return
	 */
	String getPrivateKey(String pubKey);
	/**
	 * Checks whether the wallet is locked (is unable to use its private keys).
	 * This state can be changed by calling \c lock() or \c unlock().
	 * @return true if the wallet is locked
	 * @return
	 */
	boolean isLocked();
	/**
	 * Locks the wallet immediately
	 */
	void lock();
	/**
	 * Unlocks the wallet. 
	 * The wallet remain unlocked until the \c lock is called or the program exits.
     * @param password the password previously set with \c set_password()
	 */
	void unlock(String password);
    /** Checks the password of the wallet
     *
     * Validates the password on a wallet even if the wallet is already unlocked,
     * throws if bad password given.
     * @param password the password previously set with \c set_password()
	 */
	void checkPassword(String password);
	/**
	 * Sets a new password on the wallet.
	 * The wallet must be either 'new' or 'unlocked' to execute this command.
	 * @param password
	 */
	void setPassword(String password);
	/**
	 * Dumps all private keys owned by the wallet.
	 * The keys are printed in WIF format.  You can import these keys into another wallet using \c import_key()
	 * @return
	 */
	Map<String, String> listKeys();
	/**
	 *  Dumps all public keys owned by the wallet.
	 *  @returns a vector containing the public keys
	 * @return
	 */
	Set<String> listPubKeys();
	/**
	 * Imports a WIF Private Key into the wallet to be used to sign transactions by an account.
	 * @param wifKey
	 * @return
	 */
	boolean importKey(String wifKey);
	/**
	 * Removes a key from the wallet.
	 * @param key the Public Key to remove
	 * @return
	 */
	boolean removeKey(String key);
	/**
	 * Creates a key within the wallet to be used to sign transactions by an account.
	 * @param keyType the key type to create. May be empty to allow wallet to pick appropriate/"best" key type
	 * @return
	 */
	String createKey(String keyType);

    /** Returns a signature given the digest and public_key, if this wallet can sign via that public key
     */
    byte[] trySignDigest(byte[] digest, String publicKey );
	
}
