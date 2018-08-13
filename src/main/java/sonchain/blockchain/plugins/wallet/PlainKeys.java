package sonchain.blockchain.plugins.wallet;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.bouncycastle.util.encoders.Hex;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import sonchain.blockchain.core.IJson;
import sonchain.blockchain.util.Numeric;

public class PlainKeys implements IJson{
	public static final Logger m_logger = Logger.getLogger(PlainKeys.class);
	
	private byte[] m_checksum = null;
	private Map<String, String> m_keys = new HashMap<String, String>();
	
	public byte[] getChecksum() {
		return m_checksum;
	}
	
	public void setChecksum(byte[] checksum) {
		m_checksum = checksum;
	}
	
	public Map<String, String> getKeys() {
		return m_keys;
	}
	
	public void setKeys(Map<String, String> keys) {
		m_keys = keys;
	}

    @Override
	public String toJson(){
		try{
			ObjectMapper mapper = new ObjectMapper();
			ObjectNode plainKeysNode = mapper.createObjectNode();
			toJson(plainKeysNode);
			String jsonStr =  mapper.writeValueAsString (plainKeysNode);
			m_logger.debug(" PlainKeys Json String is :" + jsonStr);
			return jsonStr;
		}
		catch(Exception ex){
			m_logger.error(" PlainKeys toJson error:" + ex.getMessage());
			return "";
		}
	}

    @Override
	public synchronized void jsonParse(String json) {
		try{
			ObjectMapper mapper = new ObjectMapper();
			JsonNode plainKeysNode = mapper.readTree(json); 
			jsonParse(plainKeysNode);
		}
		catch(IOException ex){
			m_logger.error(" PlainKeys jsonParse error:" + ex.getMessage());
		}
	}

    @Override
	public void toJson(ObjectNode plainKeysNode){
    	String checksum = Hex.toHexString(m_checksum);
    	plainKeysNode.put("checksum", checksum);
        m_logger.debug("toJson checksum\t\t\t: " + checksum);
		if(m_keys != null){
			ArrayNode keysNode = plainKeysNode.arrayNode();
			for(Map.Entry<String, String> entry : m_keys.entrySet()){
				ObjectNode keyNode = plainKeysNode.objectNode();
				keyNode.put("publickey", entry.getKey());
				keyNode.put("privatekey", entry.getValue());
		        m_logger.debug("toJson publickey\t\t\t: " + entry.getKey());
		        m_logger.debug("toJson privatekey\t\t\t: " + entry.getValue());
				keysNode.add(keyNode);
			}
			plainKeysNode.set("keys", keysNode);
		}
	}

    @Override
	public synchronized void jsonParse(JsonNode plainKeysNode) {
		try {
		
			String checksum = plainKeysNode.get("checksum").asText();
	        m_logger.debug("jsonParse checksum\t\t\t: " + checksum);
			m_checksum = Numeric.hexStringToByteArray(checksum);				
			JsonNode keysNode = plainKeysNode.get("keys");
			for(JsonNode keyNode:keysNode){
				String publickey = keyNode.get("publickey").asText();
				String privatekey = keyNode.get("privatekey").asText();
		        m_logger.debug("jsonParse publickey\t\t\t: " + publickey);
		        m_logger.debug("jsonParse privatekey\t\t\t: " + privatekey);
		        m_keys.put(publickey, privatekey);
			}
			
		} catch (Exception e) {
	        m_logger.error(e);
			throw new RuntimeException("Error on parsing Json", e);
		}
	}
}
