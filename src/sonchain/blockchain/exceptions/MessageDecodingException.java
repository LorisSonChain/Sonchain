package sonchain.blockchain.exceptions;

/**
 * 消息的解码异常
 * @author GAIA
 *
 */
public class MessageDecodingException extends RuntimeException {
    public MessageDecodingException(String message) {
        super(message);
    }

    public MessageDecodingException(String message, Throwable cause) {
        super(message, cause);
    }
}
