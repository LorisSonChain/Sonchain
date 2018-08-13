package sonchain.blockchain.core;

import java.io.IOException;

import org.apache.log4j.Logger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class WaitWeight implements IJson {

	public static final Logger m_logger = Logger.getLogger(BlockHeader.class);
	
	private int m_wait_sec = 0;
	private int m_weight = 0;

	public int getWait_sec() {
		return m_wait_sec;
	}

	public void setWait_sec(int wait_sec) {
		m_wait_sec = wait_sec;
	}

	public int getWeight() {
		return m_weight;
	}

	public void setWeight(int weight) {
		m_weight = weight;
	}

	public WaitWeight()
	{		
	}
	
	public WaitWeight(int wait_sec, int weight){
		m_wait_sec = wait_sec;
		m_weight = weight;
	}
	
	@Override
	public void toJson(ObjectNode waitWeightNode) {
		
		waitWeightNode.put("wait_sec", m_wait_sec);
		waitWeightNode.put("weight", m_weight);
		
		m_logger.debug("waitWeightNode wait_sec\t\t\t "+ m_wait_sec);
		m_logger.debug("waitWeightNode weight\t\t\t "+ m_weight);
		
	}

	@Override
	public void jsonParse(JsonNode transactionNode) {
		try {
			m_wait_sec = transactionNode.get("wait_sec").asInt();
			m_weight = transactionNode.get("weight").asInt();
			
			m_logger.debug("jsonParse wait_sec\t\t\t: " + m_wait_sec);
			m_logger.debug("jsonParse weight\t\t\t: " + m_weight);
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
			m_logger.debug(" WaitWeight Json String is :" + jsonStr);
			return jsonStr;
		}
		catch(Exception ex){
			m_logger.error(" WaitWeight toJson error:" + ex.getMessage());
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
			m_logger.error(" WaitWeight jsonParse error:" + ex.getMessage());
		}
	}
	
	
	@Override
    public String toString() {
        return toStringWithSuffix("\n");
    }
    
    private String toStringWithSuffix(final String suffix) {
        StringBuilder toStringBuff = new StringBuilder();
        toStringBuff.append("  WaitWeight [wait_sec=").append(m_wait_sec).append(suffix);
        toStringBuff.append("  weight=").append(m_weight).append(suffix);
        toStringBuff.append("   ]");
        return toStringBuff.toString();
    }	
}
