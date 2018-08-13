package sonchain.blockchain.accounts;

import java.io.IOException;

import org.apache.log4j.Logger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import sonchain.blockchain.core.IJson;

public class AccountResourceLimit implements IJson{
	public static final Logger m_logger = Logger.getLogger(AccountResourceLimit.class);

	//quantity used in current window
	private long m_used = 0;
	//quantity available in current window (based upon fractional reserve)
	private long m_available = 0;
	// max per window under current congestion
	private long m_max = 0;
	
	public long getUsed() {
		return m_used;
	}
	
	public void setUsed(long used) {
		m_used = used;
	}
	
	public long getAvailable() {
		return m_available;
	}
	
	public void setAvailable(long available) {
		m_available = available;
	}
	
	public long getMax() {
		return m_max;
	}
	
	public void setMax(long max) {
		m_max = max;
	}
	
	public AccountResourceLimit(){		
	}
	
	public AccountResourceLimit(long used, long available, long max){
		m_used = used;
		m_available = available;
		m_max = max;
	}
	
	@Override
	public void toJson(ObjectNode node) {
		
		node.put("used", m_used);
		node.put("available", m_available);
		node.put("max", m_max);
		
		m_logger.debug("toJson used\t\t\t: " + m_used);
		m_logger.debug("toJson available\t\t\t: " + m_available);
		m_logger.debug("toJson max\t\t\t: " + m_max);
		
	}
	
	@Override
	public void jsonParse(JsonNode node) {
		try {
			m_used = node.get("used").asLong();
			m_available = node.get("available").asLong();
			m_max = node.get("max").asLong();
			
			m_logger.debug("jsonParse used\t\t\t: " + m_used);
			m_logger.debug("jsonParse available\t\t\t: " + m_available);	
			m_logger.debug("jsonParse max\t\t\t: " + m_max);			
		} catch (Exception e) {
			m_logger.error(e);
			throw new RuntimeException("Error on parsing Json", e);
		}		
	}
	
	@Override
	public String toJson() {
		try{
			ObjectMapper mapper = new ObjectMapper();
			ObjectNode actionNode = mapper.createObjectNode();
			toJson(actionNode);
			String jsonStr =  mapper.writeValueAsString (actionNode);
			m_logger.debug(" AccountResourceLimit Json String is :" + jsonStr);
			return jsonStr;
		}
		catch(Exception ex){
			m_logger.error(" AccountResourceLimit toJson error:" + ex.getMessage());
			return "";
		}
	}
	
	@Override
	public void jsonParse(String json) {
		try{
			ObjectMapper mapper = new ObjectMapper();
			JsonNode node = mapper.readTree(json); 
			jsonParse(node);
		}
		catch(IOException ex){
			m_logger.error(" AccountResourceLimit jsonParse error:" + ex.getMessage());
		}		
	}

    @Override
    public String toString() {
        return toString("\n");
    }
    
    public String toString(final String suffix) {
    	StringBuilder toStringBuff = new StringBuilder();
        toStringBuff.append("  BlockHeader[used=").append(m_used).append(suffix);
        toStringBuff.append("  available=").append(m_available).append(suffix);
        toStringBuff.append("  max=").append(m_max).append(suffix);
        toStringBuff.append("   ]");
        return toStringBuff.toString();
    }
}
