package sonchain.blockchain.core;

import java.io.IOException;

import org.apache.log4j.Logger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class PermissionLevel implements IJson{

	public static final Logger m_logger = Logger.getLogger(TransactionHeader.class);

	private String  m_actor;    
	private String  m_permission;


	public String getActor() {
		return m_actor;
	}
	public void setActor(String actor) {
		m_actor = actor;
	}
	public String getPermission() {
		return m_permission;
	}
	public void setPermission(String permission) {
		m_permission = permission;
	}

	public PermissionLevel() {

	}

	public PermissionLevel(String actor, String permission) {
		m_actor = actor;
		m_permission = permission;
	}

	@Override
	public void toJson(ObjectNode permissionLevelNode) {
		ObjectNode Node = permissionLevelNode.objectNode();
		m_logger.debug("toJson actor/t/t/t:" + m_actor);
		m_logger.debug("toJson permission/t/t/t:" + m_permission);
		Node.put("actor", m_actor);
		Node.put("permission", m_permission);
		permissionLevelNode.set("permissions", Node);
	}

	@Override
	public void jsonParse(JsonNode permissionLevelNode) {
		try {
			m_actor = permissionLevelNode.get("actor").asText();
			m_permission = permissionLevelNode.get("permission").asText();
			
			m_logger.debug("jsonParse actor\t\t\t" + m_actor);
			m_logger.debug("jsonParse permission\t\t\t" + m_permission);
			
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
			m_logger.debug(" PermissionLevel Json String is :" + jsonStr);
			return jsonStr;
		}
		catch(Exception ex){
			m_logger.error(" PermissionLevel toJson error:" + ex.getMessage());
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
			m_logger.error(" PermissionLevel jsonParse error:" + ex.getMessage());
		}
	}
	
	@Override
	public String toString() {
		return toStringWithSuffix("\n");
	}

	private String toStringWithSuffix(final String suffix) {
		StringBuilder toStringBuff = new StringBuilder();
		toStringBuff.append("  PermissionLevel[actor=").append(m_actor).append(suffix);
		toStringBuff.append("  permission=").append(m_permission).append(suffix);
		toStringBuff.append("  ]");
		return toStringBuff.toString();
	}
}
