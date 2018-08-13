package sonchain.blockchain.core;

import java.io.IOException;

import org.apache.log4j.Logger;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import sonchain.blockchain.util.ByteUtil;
import sonchain.blockchain.util.Utils;

/**
 * TransactionReceipt
 *
 */
public class TransactionReceiptHeader implements IJson{
	public static final Logger m_logger = Logger.getLogger(TransactionReceiptHeader.class);

	/**
	 * Constructor
	 */
	public TransactionReceiptHeader() {
	}
	
	private TransactionStatus m_transactionStatus = TransactionStatus.Executed;
	private int m_cpu_usage_us = 0;
	private int m_net_usage_words = 0;
	private String m_error = "";
	
	public String getError() {
		return m_error;
	}

	public void setError(String error) {
		m_error = error == null ? "" : error;
	}

	public boolean isValid() {
		return true;
	}

	public boolean isSuccessful() {
		return m_error.isEmpty();
	}

	public TransactionStatus getTransactionStatus() {
		return m_transactionStatus;
	}

	public void setTransactionStatus(TransactionStatus transactionStatus) {
		m_transactionStatus = transactionStatus;
	}

	public int getCpu_usage_us() {
		return m_cpu_usage_us;
	}

	public void setCpu_usage_us(int cpu_usage_us) {
		m_cpu_usage_us = cpu_usage_us;
	}

	public int getNet_usage_words() {
		return m_net_usage_words;
	}

	public void setNet_usage_words(int net_usage_words) {
		m_net_usage_words = net_usage_words;
	}	
	
	@Override
    public void toJson(ObjectNode transactionReceiptNode){     	
    	m_logger.debug("toJson TransactionStatus\t\t\t: " + m_transactionStatus.toString());
    	m_logger.debug("toJson cpu_usage_us\t\t\t: " + m_cpu_usage_us);
    	m_logger.debug("toJson net_usage_words\t\t\t: " + m_net_usage_words);
    	m_logger.debug("toJson error\t\t\t: " + m_error); 
    	
    	transactionReceiptNode.put("status", m_transactionStatus.toString());
    	transactionReceiptNode.put("cpu_usage_us", m_cpu_usage_us);
    	transactionReceiptNode.put("net_usage_words", m_net_usage_words);
    	transactionReceiptNode.put("error", m_error);
    }

	@Override
	public synchronized void jsonParse(JsonNode transactionNode) {
		try {
			m_transactionStatus = TransactionStatus.fromString((transactionNode.get("status").asText()));			
			m_cpu_usage_us = transactionNode.get("cpu_usage_us").asInt();
			m_net_usage_words = transactionNode.get("net_usage_words").asInt();
			m_error = transactionNode.get("error").asText();
		   	
	    	m_logger.debug("jsonParse TransactionStatus\t\t\t: " + m_transactionStatus.toString());
	    	m_logger.debug("jsonParse cpu_usage_us\t\t\t: " + m_cpu_usage_us);
	    	m_logger.debug("jsonParse net_usage_words\t\t\t: " + m_net_usage_words);
	    	m_logger.debug("jsonParse error\t\t\t: " + m_error); 
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
			m_logger.debug(" TransactionReceiptHeader Json String is :" + jsonStr);
			return jsonStr;
		}
		catch(Exception ex){
			m_logger.error(" TransactionReceiptHeader toJson error:" + ex.getMessage());
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
			m_logger.error(" TransactionReceiptHeader jsonParse error:" + ex.getMessage());
		}
	}
	
    @Override
    public String toString() {
        return toStringWithSuffix("\n");
    }
    
    private String toStringWithSuffix(final String suffix) {
        StringBuilder toStringBuff = new StringBuilder();
        toStringBuff.append("  TransactionReceiptHeader[transactionStatus=").append(m_transactionStatus.toString()).append(suffix);
        toStringBuff.append("  cpu_usage_us=").append(m_cpu_usage_us).append(suffix);
        toStringBuff.append("  net_usage_words=").append(m_net_usage_words).append(suffix);
        toStringBuff.append("  error=").append(m_error).append(suffix);
        toStringBuff.append("   ]");
        return toStringBuff.toString();
    }
}
