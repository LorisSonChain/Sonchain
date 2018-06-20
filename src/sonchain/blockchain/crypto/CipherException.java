package sonchain.blockchain.crypto;

/**
 * 加密异常类
 */
public class CipherException extends Exception {

	/**
	 * 初始化
	 * @param message
	 */
    public CipherException(String message) {
        super(message);
    }

    /**
     * 初始化
     * @param cause
     */
    public CipherException(Throwable cause) {
        super(cause);
    }

    /**
     * 初始化
     * @param message
     * @param cause
     */
    public CipherException(String message, Throwable cause) {
        super(message, cause);
    }
}
