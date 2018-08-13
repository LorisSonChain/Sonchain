package sonchain.blockchain.core;

import java.io.IOException;

import org.apache.log4j.Logger;
import org.bouncycastle.util.encoders.Hex;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import sonchain.blockchain.util.ByteUtil;
import sonchain.blockchain.util.Numeric;

public class TransactionHeader implements IJson{
	public static final Logger m_logger = Logger.getLogger(TransactionHeader.class);
	public static final int HASH_LENGTH = 32;
	
	private long m_expiration = 0;	
	public long getExpiration() {
		return m_expiration;
	}
	public void setExpiration(long expiration) {
		m_expiration = expiration;
	}

	private long m_refBlockHeight = 0;		
	public long getRefBlockHeight() {
		return m_refBlockHeight;
	}
	public void setRefBlockHeight(long refBlockHeight) {
		m_refBlockHeight = refBlockHeight;
	}
	
	private byte[] m_hash = ByteUtil.ZERO_BYTE_ARRAY;	
	public byte[] getHash() {
		return m_hash;
	}
	public void setHash(byte[] hash) {
		m_hash = hash;
	}
	
	private long m_timeStamp = 0;	
	public long getTimeStamp() {
		return m_timeStamp;
	}
	
	public void setTimeStamp(long timeStamp) {
		m_timeStamp = timeStamp;
	}
	
	private byte[] m_value = ByteUtil.ZERO_BYTE_ARRAY;	
	public byte[] getValue() {
        return m_value == null ? ByteUtil.ZERO_BYTE_ARRAY : m_value;
	}
	public void setValue(byte[] value) {
		m_value = value;
	}
	
	private int m_version = 0;	
	public int getVersion() {
		return m_version;
	}
	public void setVersion(int version) {
		m_version = version;
	}
	
	public TransactionHeader(){
		
	}
	
	public TransactionHeader(long refBlockHeight, byte[] hash, long timeStamp, long expiration, byte[] value, int version ){
		m_refBlockHeight = refBlockHeight;
		m_hash = hash;
		m_timeStamp = timeStamp;
		m_expiration = expiration;

		if (ByteUtil.isSingleZero(value)) {
			m_value = ByteUtil.EMPTY_BYTE_ARRAY;
		} else {
			m_value = value;
		}
		m_version = version;
	}
	
	//TODO
	public boolean validate(){
        if (m_value != null  && m_value.length > HASH_LENGTH)
        {
			m_logger.error("Value is not valid!");
			return false;
        }
		return true;
	}
	
	public byte[] getEncodedRaw() {
    	try
    	{
			ObjectMapper mapper = new ObjectMapper();
		    ObjectNode transactionNode = mapper.createObjectNode();
			
	    	String value = Hex.toHexString(m_value);
	    	
	    	m_logger.debug("getEncoded refBlockHeight\t\t\t: " + m_refBlockHeight);
	    	m_logger.debug("getEncoded timeStamp\t\t\t: " + m_timeStamp);
	    	m_logger.debug("getEncoded version\t\t\t: " + m_version);
	    	m_logger.debug("getEncoded value\t\t\t: " + value);    
	    	m_logger.debug("getEncoded expiration\t\t\t: " + m_expiration);
	    	
	    	transactionNode.put("refBlockHeight", m_refBlockHeight);
	    	transactionNode.put("timeStamp", m_timeStamp);
	    	transactionNode.put("version", m_version);
	    	transactionNode.put("value", value);
	    	transactionNode.put("expiration", m_expiration);
	    	String content = mapper.writeValueAsString (transactionNode);
	    	m_logger.debug(" Json Content:" + content);
		    return content.getBytes();
    	}catch(JsonProcessingException ex){
    		m_logger.error("getEncoded error:" + ex.getMessage());
    		return null;
    	}	 
	}
	
	public byte[] getEncoded() {
		return null;
	}
	
	@Override
	public void toJson(ObjectNode headerNode) { 	
    	String value = Hex.toHexString(m_value);
    	
    	m_logger.debug("toJson refBlockHeight\t\t\t: " + m_refBlockHeight);
    	m_logger.debug("toJson timeStamp\t\t\t: " + m_timeStamp);
    	m_logger.debug("toJson version\t\t\t: " + m_version);
    	m_logger.debug("toJson value\t\t\t: " + value);    
    	m_logger.debug("toJson expiration\t\t\t: " + m_expiration);
    	
    	headerNode.put("refBlockHeight", m_refBlockHeight);
    	headerNode.put("timeStamp", m_timeStamp);
    	headerNode.put("version", m_version);
    	headerNode.put("value", value);
    	headerNode.put("expiration", m_expiration);
		
	}
	@Override
	public void jsonParse(JsonNode transactionNode) {
		try {
			m_refBlockHeight = transactionNode.get("refBlockHeight").asLong();	
			m_timeStamp = transactionNode.get("timeStamp").asLong();
			String value = transactionNode.get("value").asText();
			m_version = transactionNode.get("version").asInt();	
	        m_value = Numeric.hexStringToByteArray(value);
	        m_expiration = transactionNode.get("expiration").asLong();	
			
	        m_logger.debug("jsonParse refBlockHeight\t\t\t: " + m_refBlockHeight);
	        m_logger.debug("jsonParse timeStamp\t\t\t: " + m_timeStamp);
	        m_logger.debug("jsonParse version\t\t\t: " + m_version);
	        m_logger.debug("jsonParse value\t\t\t: " + value);
	        m_logger.debug("jsonParse expiration\t\t\t: " + m_expiration);
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
			m_logger.debug(" TransactionHeader Json String is :" + jsonStr);
			return jsonStr;
		}
		catch(Exception ex){
			m_logger.error(" TransactionHeader toJson error:" + ex.getMessage());
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
			m_logger.error(" TransactionHeader jsonParse error:" + ex.getMessage());
		}
	}
	
    @Override
    public String toString() {
        return toStringWithSuffix("\n");
    }
    
    private String toStringWithSuffix(final String suffix) {
        StringBuilder toStringBuff = new StringBuilder();
        toStringBuff.append("  TransactionHeader[refBlockHeight=").append(m_refBlockHeight).append(suffix);
        toStringBuff.append("  timeStamp=").append(m_timeStamp).append(suffix);
        toStringBuff.append("  version=").append(m_version).append(suffix);
        toStringBuff.append("  value=").append(Hex.toHexString(m_value)).append(suffix);
        toStringBuff.append("  expiration=").append(m_expiration).append(suffix);
        toStringBuff.append("  ]");
        return toStringBuff.toString();
    }
}
