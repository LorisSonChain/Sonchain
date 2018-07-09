package sonchain.blockchain.exceptions;

/**
 * 消息的编码异常
 * @author GAIA
 *
 */
public class MessageEncodingException extends RuntimeException {
    public MessageEncodingException(String message) {
        super(message);
    }

    public MessageEncodingException(String message, Throwable cause) {
        super(message, cause);
    }
}
