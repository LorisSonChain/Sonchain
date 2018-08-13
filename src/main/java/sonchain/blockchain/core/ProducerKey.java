package sonchain.blockchain.core;

import java.io.IOException;

import org.apache.log4j.Logger;
import org.bouncycastle.util.encoders.Hex;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import sonchain.blockchain.util.ByteUtil;
import sonchain.blockchain.util.Numeric;
import sonchain.blockchain.util.Utils;

/**
 * The Producer Key
 * @author GAIA
 *
 */
public class ProducerKey implements IJson {
	public static final Logger m_logger = Logger.getLogger(ProducerKey.class);
	
	private String m_producerName = "";
	private String m_address = "";
	private String m_blockProducerSigningKey = "";
	
	public ProducerKey(){
	}
	
	public String getProducerName() {
		return m_producerName;
	}
	public void setProducerName(String name) {
		m_producerName = name;
	}
	
	public String getAddress() {
		return m_address;
	}
	public void setAddress(String address) {
		m_address = address;
	}
	
	public String getBlockProducerSigningKey() {
		return m_blockProducerSigningKey;
	}
	public void setBlockProducerSigningKey(String blockProducerSigningKey) {
		m_blockProducerSigningKey = blockProducerSigningKey;
	}
	
	public ProducerKey(String m_producerName, String m_address, String m_blockProducerSigningKey) {
		super();
		this.m_producerName = m_producerName;
		this.m_address = m_address;
		this.m_blockProducerSigningKey = m_blockProducerSigningKey;
	}

	@Override
	public void toJson(ObjectNode producerNode){
        m_logger.debug("toJson producername\t\t\t: " + m_producerName);
        m_logger.debug("toJson address\t\t\t: " + m_address);
        m_logger.debug("toJson blockProducerSigningKey\t\t\t: " + m_blockProducerSigningKey);
        producerNode.put("producername", m_producerName);
        producerNode.put("address", m_address);
        producerNode.put("blockProducerSigningKey", m_blockProducerSigningKey);
	}

	@Override
	public synchronized void jsonParse(JsonNode producerNode) {
		try {
			m_producerName = producerNode.get("producername").asText();
			m_address = producerNode.get("address").asText();
			m_blockProducerSigningKey = producerNode.get("blockProducerSigningKey").asText();
	        m_logger.debug("jsonParse producername\t\t\t: " + m_producerName);
	        m_logger.debug("jsonParse address\t\t\t: " + m_address);
	        m_logger.debug("jsonParse blockProducerSigningKey\t\t\t: " + m_blockProducerSigningKey);
		} catch (Exception e) {
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
			m_logger.debug(" ProducerKey Json String is :" + jsonStr);
			return jsonStr;
		}
		catch(Exception ex){
			m_logger.error(" ProducerKey toJson error:" + ex.getMessage());
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
			m_logger.error(" ProducerKey jsonParse error:" + ex.getMessage());
		}
	}
	
    @Override
    public String toString() {
        return toStringWithSuffix("\n");
    }
    
    private String toStringWithSuffix(final String suffix) {
        StringBuilder toStringBuff = new StringBuilder();
        toStringBuff.append("  producername=").append(m_producerName).append(suffix);
        toStringBuff.append("  address=").append(m_address).append(suffix);
        toStringBuff.append("  blockProducerSigningKey=").append(m_blockProducerSigningKey).append(suffix);
        return toStringBuff.toString();
    }
}
