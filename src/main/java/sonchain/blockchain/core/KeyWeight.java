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

public class KeyWeight implements IJson {

	public static final Logger m_logger = Logger.getLogger(BlockHeader.class);

	private byte[] m_key = null;
	private int m_weight = 0;

	public byte[] getKey() {
		return m_key;
	}

	public void setKey(byte[] key) {
		m_key = key;
	}

	public int getWeight() {
		return m_weight;
	}

	public void setWeight(int weight) {
		m_weight = weight;
	}

	public KeyWeight() {
	}

	public KeyWeight(byte[] key, int weight) {
		m_key = key;
		m_weight = weight;
	}

	@Override
	public void toJson(ObjectNode keyWeighttNode) {
		String key = Hex.toHexString(m_key);

		keyWeighttNode.put("key", key);
		keyWeighttNode.put("weight", m_weight);

		m_logger.debug("toJson key\t\t\t" + key);
		m_logger.debug("toJson weight\t\t\t" + m_weight);

	}

	@Override
	public synchronized void jsonParse(JsonNode transactionNode) {
		String key = transactionNode.get("key").asText();
		m_weight = transactionNode.get("weight").asInt();

		m_key = Numeric.hexStringToByteArray(key);

		m_logger.debug("jsonParse key\t\t\t: " + key);
		m_logger.debug("jsonParse weight\t\t\t: " + m_weight);

	}

	@Override
	public String toString() {
		return toStringWithSuffix("\n");
	}

	private String toStringWithSuffix(final String suffix) {
		StringBuilder toStringBuff = new StringBuilder();
		toStringBuff.append("  KeyWeight [key=").append(m_key).append(suffix);
		toStringBuff.append("  weight=").append(m_weight).append(suffix);
		toStringBuff.append("   ]");
		return toStringBuff.toString();
	}

	@Override
	public String toJson() {
		try {
			ObjectMapper mapper = new ObjectMapper();
			ObjectNode node = mapper.createObjectNode();
			toJson(node);
			String jsonStr = mapper.writeValueAsString(node);
			m_logger.debug(" KeyWeight Json String is :" + jsonStr);
			return jsonStr;
		} catch (Exception ex) {
			m_logger.error(" KeyWeight toJson error:" + ex.getMessage());
			return "";
		}
	}

	@Override
	public synchronized void jsonParse(String json) {
		try {
			ObjectMapper mapper = new ObjectMapper();
			JsonNode node = mapper.readTree(json);
			jsonParse(node);
		} catch (IOException ex) {
			m_logger.error(" KeyWeight jsonParse error:" + ex.getMessage());
		}
	}
	
	    public String toString(final String suffix) {
	    	return "";
	    }
}
