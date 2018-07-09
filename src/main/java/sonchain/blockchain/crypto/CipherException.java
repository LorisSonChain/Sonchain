package sonchain.blockchain.crypto;

/**
 */
public class CipherException extends Exception {

	/**
	 * Init
	 * @param message
	 */
    public CipherException(String message) {
        super(message);
    }

    /**
     * Init
     * @param cause
     */
    public CipherException(Throwable cause) {
        super(cause);
    }

    /**
     * Init
     * @param message
     * @param cause
     */
    public CipherException(String message, Throwable cause) {
        super(message, cause);
    }
}
