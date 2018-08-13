package sonchain.blockchain.core;

import org.apache.log4j.Logger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import sonchain.blockchain.crypto.ECKey;
import sonchain.blockchain.crypto.ECKey.ECDSASignature;
import sonchain.blockchain.crypto.ECKey.MissingPrivateKeyException;

public class SignedTransaction extends Transaction  implements IJson{
	public static final Logger m_logger = Logger.getLogger(SignedTransaction.class);

    private ECDSASignature m_signature = null;    
	public void setSignature(ECDSASignature m_signature) {
		this.m_signature = m_signature;
	}

	public ECDSASignature getSignature() {
        return m_signature;
    }

	/**
	 * Constructor
	 */
	public SignedTransaction()
	{
		
	}

	/**
	 * Constructor
	 * 
	 * @param nonce
	 * @param receiveAddress
	 * @param value
	 * @param data
	 */
	public SignedTransaction(byte[] nonce, byte[] receiveAddress, byte[] value, byte[] data) {
		super(nonce, receiveAddress, value, data);	
	}

	/**
	 * Constructor
	 * 
	 * @param nonce
	 * @param receiveAddress
	 * @param value
	 * @param data
	 * @param privateNote
	 */
	public SignedTransaction(byte[] nonce, byte[] receiveAddress, byte[] value, byte[] data, String privateNote) {
		super(nonce, receiveAddress, value, data, privateNote);		
	}

	/**
	 * Constructor
	 * 
	 * @param nonce
	 * @param receiveAddress
	 * @param senderAddress
	 * @param value
	 * @param data
	 * @param privateNote
	 */
	public SignedTransaction(byte[] nonce, byte[] receiveAddress, byte[] senderAddress, byte[] value, byte[] data, String privateNote) {
		super(nonce, receiveAddress, senderAddress, value, data, privateNote);	
	}
    
    public void sign(ECKey key) throws MissingPrivateKeyException {
        m_signature = key.sign(super.getEncoded());
        m_logger.debug("getRawHash sign signature:" + m_signature.toHex());
    }
    
    @Override
    public void toJson(ObjectNode transactionNode){    
    	super.toJson(transactionNode);
    	if(m_signature != null){
    		transactionNode.put("signature", m_signature.toBase64());
    	}
    	else{
    		transactionNode.put("signature", "");    		
    	}
    }

    @Override
	public synchronized void jsonParse(JsonNode node) {
		try {
			super.jsonParse(node);
			JsonNode subNode = node.get("signature");
			if(subNode != null){
				String signature = subNode.asText();		
				m_signature = ECDSASignature.decodeFromBase64(signature);
			}
		}catch (Exception e) {
			 m_logger.error(e);
			 throw new RuntimeException("Error on parsing Json", e);
		}
	}
	
    @Override
    public String toString() {
        return toStringWithSuffix("\n");
    }
    
    private String toStringWithSuffix(final String suffix) {
    	String transactionStr = super.toString();
        StringBuilder toStringBuff = new StringBuilder();
        toStringBuff.append("  SignedTransaction[transaction=").append(transactionStr).append(suffix);
        toStringBuff.append("  signature=").append(m_signature.toBase64()).append(suffix);
        toStringBuff.append("   ]");
        return toStringBuff.toString();
    }
}
