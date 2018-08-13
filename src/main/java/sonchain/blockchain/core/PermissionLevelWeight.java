package sonchain.blockchain.core;

import java.io.IOException;

import org.apache.log4j.Logger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class PermissionLevelWeight implements IJson {

	public static final Logger m_logger = Logger.getLogger(BlockHeader.class);
	
	private PermissionLevel m_permission = null;
	private int m_weight = 0;

	public PermissionLevel getPermission() {
		return m_permission;
	}

	public void setPermission(PermissionLevel permission) {
		m_permission = permission;
	}

	public int getWeight() {
		return m_weight;
	}

	public void setWeight(int weight) {
		m_weight = weight;
	}
	
	public PermissionLevelWeight(){
		
	}
	
	public PermissionLevelWeight(PermissionLevel permission, int weight){
		m_permission = permission;
		m_weight = weight;
	}
	
	@Override
	public void toJson(ObjectNode plNode) {
		plNode.put("weight", m_weight);
		
		if(m_permission != null){
			m_permission.toJson(plNode);
		}
		
		m_logger.debug("toJson weight\t\t\t: " + m_weight);
		m_logger.debug("jsonParse permissions\t\t\t: " + m_permission.toString());
	}

	@Override
	public void jsonParse(JsonNode transactionNode) {
		try {
			m_weight = transactionNode.get("weight").asInt();
			
			if(m_permission == null){
				m_permission = new PermissionLevel();
				m_permission.jsonParse(transactionNode.get("permissions"));
			}
			m_logger.debug("jsonParse weight\t\t\t: " + m_weight);
			m_logger.debug("jsonParse permissions\t\t\t: " + m_permission.toString());
			
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
			m_logger.debug(" PermissionLevelWeight Json String is :" + jsonStr);
			return jsonStr;
		}
		catch(Exception ex){
			m_logger.error(" PermissionLevelWeight toJson error:" + ex.getMessage());
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
			m_logger.error(" PermissionLevelWeight jsonParse error:" + ex.getMessage());
		}
	}
	
	@Override
    public String toString() {
        return toStringWithSuffix("\n");
    }
    
    private String toStringWithSuffix(final String suffix) {
        StringBuilder toStringBuff = new StringBuilder();
        toStringBuff.append("  PermissionLevelWeight [m_permission=").append(m_permission.toString()).append(suffix);
        toStringBuff.append("  weight=").append(m_weight).append(suffix);
        toStringBuff.append("   ]");
        return toStringBuff.toString();
    }
}
