package sonchain.blockchain.core;

import java.io.IOException;
import java.math.BigInteger;

import org.apache.log4j.Logger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import sonchain.blockchain.util.ByteUtil;
import sonchain.blockchain.util.Utils;

public class Asset implements IJson{

	public static final Logger m_logger = Logger.getLogger(ProducerScheduleType.class);

	private BigInteger m_amount = BigInteger.ZERO;
	private String m_symbol = "";

	public BigInteger getAmount() {
		return m_amount;
	}
	public void setAmount(BigInteger amount) {
		m_amount = amount;
	}
	public String getSymbol() {
		return m_symbol;
	}
	public void setSymbol(String symbol) {
		m_symbol = symbol;
	}

	@Override
	public void toJson(ObjectNode node) {
		ObjectNode oNode = node.objectNode();		
		node.put("amount", m_amount);
		m_logger.debug("toJson amount\t\t\t: " + m_amount);
		node.put("symbol", m_symbol);
		m_logger.debug("toJson symbol\t\t\t: " + m_symbol);
	}
	@Override
	public synchronized void jsonParse(JsonNode node) {
		try {
			m_amount = node.get("amount").bigIntegerValue();
	        m_logger.debug("jsonParse amount\t\t\t: " + m_amount);

	        m_symbol = node.get("amount").asText();
			m_logger.debug("jsonParse symbol\t\t\t: " + m_symbol);
		} catch (Exception e) {
			m_logger.error(e);
			throw new RuntimeException("Error on parsing Json", e);
		}

	}
	@Override
	public String toJson() {
		try{
			ObjectMapper mapper = new ObjectMapper();
			ObjectNode node = mapper.createObjectNode();
			toJson(node);
			String jsonStr =  mapper.writeValueAsString (node);
			m_logger.debug(" Asset String is :" + jsonStr);
			return jsonStr;
		}
		catch(Exception ex){
			m_logger.error(" Asset toJson error:" + ex.getMessage());
			return "";
		}
	}


	@Override
	public void jsonParse(String json) {
		try{
			
			ObjectMapper mapper = new ObjectMapper();
			JsonNode blockHeaderNode = mapper.readTree(json); 
			jsonParse(blockHeaderNode);
		}
		catch(IOException ex){
			m_logger.error(" Asset jsonParse error:" + ex.getMessage());
		}

	}

	@Override
	public String toString() {
		return toString("\n");
	}

	public String toString(final String suffix) {
		StringBuilder toStringBuff = new StringBuilder();
        toStringBuff.append("  Assets[amount=").append(m_amount).append(suffix);
        toStringBuff.append("  symbol=").append(m_symbol).append(suffix);
        toStringBuff.append("   ]");
        return toStringBuff.toString();
	}
}
