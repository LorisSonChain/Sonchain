package sonchain.blockchain.core;

import java.io.IOException;

import org.apache.log4j.Logger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class PackedTransaction implements IJson{
	public static final Logger m_logger = Logger.getLogger(PackedTransaction.class);
	public PackedTransaction(){
	}	
	
	public PackedTransaction(SignedTransaction transaction, CompressionType compressionType){
		m_transaction = transaction;
		m_compressionType = compressionType;
	}
	
	private CompressionType m_compressionType = CompressionType.None;
	public CompressionType getCompressionType() {
		return m_compressionType;
	}
	
	public void setCompressionType(CompressionType compressionType) {
		m_compressionType = compressionType;
	}
	
	private SignedTransaction m_transaction = null;	
	public SignedTransaction getTransaction() {
		return m_transaction;
	}
	public void setTransaction(SignedTransaction transaction) {
		m_transaction = transaction;
	}
	
	public byte[] getRawTransaction(){
		if(m_transaction != null){
			m_transaction.getEncoded();
		}
		return null;
	}
	
	public byte[] getPackedTransaction(){
		byte[] bytes = getRawTransaction();
		return bytes;
	}	

    @Override
    public void toJson(ObjectNode transactionNode){ 
    	if(m_transaction != null){
    		m_transaction.toJson(transactionNode);
    	}
    	transactionNode.put("compression", m_compressionType.toString());
    }

    @Override
	public synchronized void jsonParse(JsonNode node) {
		try {
	    	if(m_transaction != null){
	    		m_transaction.jsonParse(node);
	    	}
	    	String compression = node.get("compression").asText();
	    	m_compressionType = CompressionType.fromString(compression);
		}catch (Exception e) {
			 m_logger.error(e);
			 throw new RuntimeException("Error on parsing Json", e);
		}
	}
	
    @Override
	public String toJson(){
		try{
			ObjectMapper mapper = new ObjectMapper();
			ObjectNode node = mapper.createObjectNode();
			toJson(node);
			String jsonStr =  mapper.writeValueAsString (node);
			m_logger.debug(" PackedTransaction Json String is :" + jsonStr);
			return jsonStr;
		}
		catch(Exception ex){
			m_logger.error(" PackedTransaction toJson error:" + ex.getMessage());
			return "";
		}
	}

    @Override
	public synchronized void jsonParse(String json) {
		try{
			ObjectMapper mapper = new ObjectMapper();
			JsonNode node = mapper.readTree(json); 
			jsonParse(node);
		}
		catch(IOException ex){
			m_logger.error(" PackedTransaction jsonParse error:" + ex.getMessage());
		}
	}
	
    @Override
    public String toString() {
        return toStringWithSuffix("\n");
    }
    
    private String toStringWithSuffix(final String suffix) {
        StringBuilder toStringBuff = new StringBuilder();
        toStringBuff.append("  PackedTransaction[transaction=").append(m_transaction.toString()).append(suffix);
        toStringBuff.append("  compressionType=").append(m_compressionType.toString()).append(suffix);
        toStringBuff.append("   ]");
        return toStringBuff.toString();
    }
}