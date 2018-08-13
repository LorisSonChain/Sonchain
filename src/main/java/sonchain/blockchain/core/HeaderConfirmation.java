package sonchain.blockchain.core;

import java.io.IOException;

import org.apache.log4j.Logger;
import org.bouncycastle.util.encoders.Hex;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import sonchain.blockchain.util.ByteUtil;
import sonchain.blockchain.util.Numeric;

public class HeaderConfirmation implements IJson {
	public static final Logger m_logger = Logger.getLogger(HeaderConfirmation.class);

	public HeaderConfirmation()
	{		
	}
	
	private byte[] m_blockHash = null;
	private String m_producer = "";
	//生产者签名
	private String m_producerSignature = null;
	
	public byte[] getBlockHash() {
		return m_blockHash;
	}
	public void setBlockHash(byte[] blockHash) {
		m_blockHash = blockHash;
	}
	
	public String getProducer() {
		return m_producer;
	}
	public void setProducer(String producer) {
		m_producer = producer;
	}
	
	public String getProducerSignature() {
		return m_producerSignature;
	}
	public void setProducerSignature(String producerSignature) {
		m_producerSignature = producerSignature;
	}	
	
	public HeaderConfirmation(byte[] m_blockHash, String m_producer, String m_producerSignature) {
		this.m_blockHash = m_blockHash;
		this.m_producer = m_producer;
		this.m_producerSignature = m_producerSignature;
	}
	@Override
	public void toJson(ObjectNode node) {
		String blockHash = Hex.toHexString(m_blockHash);
		node.put("blockHash", blockHash);
		node.put("producerSignature", m_producerSignature);
		node.put("producer", m_producer);
		m_logger.debug("toJson blockHash\t\t\t" + blockHash);
		m_logger.debug("toJson producerSignature\t\t\t" + m_producerSignature);
		m_logger.debug("toJson producer\t\t\t" + m_producer);
		
	}
	@Override
	public void jsonParse(JsonNode node) {
		String blockHash = node.get("blockHash").asText();
		m_producerSignature = node.get("producerSignature").asText();
		m_producer = node.get("producer").asText();
		m_blockHash = Numeric.hexStringToByteArray(blockHash);
		m_logger.debug("jsonParse blockHash\t\t\t: " + m_blockHash);
		m_logger.debug("jsonParse producerSignature\t\t\t: " + m_producerSignature);
		m_logger.debug("jsonParse producer\t\t\t: " + m_producer);
		
	}
	@Override
	public String toJson() {
		try {
			ObjectMapper mapper = new ObjectMapper();
			ObjectNode node = mapper.createObjectNode();
			toJson(node);
			String jsonStr = mapper.writeValueAsString(node);
			m_logger.debug(" HeaderConfirmation Json String is :" + jsonStr);
			return jsonStr;
		} catch (Exception ex) {
			m_logger.error(" HeaderConfirmation toJson error:" + ex.getMessage());
			return "";
		}
	}
	@Override
	public void jsonParse(String json) {
		try {
			ObjectMapper mapper = new ObjectMapper();
			JsonNode node = mapper.readTree(json);
			jsonParse(node);
		} catch (IOException ex) {
			m_logger.error(" HeaderConfirmation jsonParse error:" + ex.getMessage());
		}
		
	}
	
    @Override
    public String toString() {
        return toString("\n");
    }
    
    public String toString(final String suffix) {
        StringBuilder toStringBuff = new StringBuilder();
		String blockHash = Hex.toHexString(m_blockHash);
        toStringBuff.append("  HeaderConfirmation[blockHash=").append(blockHash).append(suffix);
        toStringBuff.append("  producerSignature=").append(m_producerSignature).append(suffix);
        toStringBuff.append("  producer=").append(m_producer);
        toStringBuff.append("   ]");
        return toStringBuff.toString();
    }
}
