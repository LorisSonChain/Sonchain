package sonchain.blockchain.core;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.log4j.Logger;
import org.bouncycastle.util.encoders.Hex;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import sonchain.blockchain.util.ByteUtil;
import sonchain.blockchain.util.Numeric;

public class Action implements IJson{
	public static final Logger m_logger = Logger.getLogger(Action.class);
	/**
	 * Name of the account the action is intended for
	 */
	private String m_account = "";
	/**
	 * Name of the action
	 */
	private String m_name = "";
	/**
	 * List of permissions that authorize this action
	 */
	private List<PermissionLevel> m_authorizations = new ArrayList<PermissionLevel>();
	
	private String m_hexData = "";
	
	private String m_data = "";
	
	public String getAccount() {
		return m_account;
	}
	
	public void setAccount(String account) {
		m_account = account;
	}
	
	public String getName() {
		return m_name;
	}
	
	public void setName(String name) {
		m_name = name;
	}
	
	public List<PermissionLevel> getAuthorizations() {
		return m_authorizations;
	}
	
	public void setAuthorizations(List<PermissionLevel> authorizations) {
		m_authorizations = authorizations;
	}
	
	public String getHexData() {
		return m_hexData;
	}
	
	public void setHexData(String hexData) {
		m_hexData = hexData;
	}
	
	public String getData() {
		return m_data;
	}
	
	public void setData(String data) {
		m_data = data;
	}
	
	@Override
	public void toJson(ObjectNode actionNode) {	
		
    	m_logger.debug("toJson account\t\t\t: " + m_account);
    	m_logger.debug("toJson name\t\t\t: " + m_name);
    	m_logger.debug("toJson hexData\t\t\t: " + m_hexData);
    	m_logger.debug("toJson data\t\t\t: " + m_data);
    	
    	if(m_authorizations != null){
    		ArrayNode authorizationNodes = actionNode.arrayNode();
    		int size = m_authorizations.size();
    		for(int i = 0 ; i < size; i++){
    			ObjectNode authorizationNode = actionNode.objectNode();
    			m_authorizations.get(i).toJson(authorizationNode);
    			authorizationNodes.add(authorizationNode);
    		}
    		actionNode.set("authorization", authorizationNodes);
    	}
    	
    	actionNode.put("account", m_account);
    	actionNode.put("name", m_name);
    	actionNode.put("hex_data", m_hexData);
    	actionNode.put("data", m_data);
	}
	
	@Override
	public synchronized void jsonParse(JsonNode actionNode) {
		try {		
			m_account = actionNode.get("account").asText();
			m_name = actionNode.get("name").asText();
			m_hexData = actionNode.get("hex_data").asText();
			m_data = actionNode.get("data").asText();
	        
			if(m_authorizations == null){
				m_authorizations = new CopyOnWriteArrayList<>();
			}
			JsonNode authorizations = actionNode.get("authorization");
			for (JsonNode authorization : authorizations) {  
				PermissionLevel permission = new PermissionLevel();
				permission.jsonParse(authorization);
				m_authorizations.add(permission);
			}			
	        m_logger.debug("jsonParse account\t\t\t: " + m_account);
	        m_logger.debug("jsonParse name\t\t\t: " + m_name);
	        m_logger.debug("jsonParse hexData\t\t\t: " + m_hexData);;
		}catch (Exception e) {
			 m_logger.error(e);
			 throw new RuntimeException("Error on parsing Json", e);
		}	
	}

    @Override
    public String toString() {
        return toString("\n");
    }
    
    public String toString(final String suffix) {

        StringBuilder toStringBuff = new StringBuilder();
        toStringBuff.append("  Action[account=").append(m_account).append(suffix);
        toStringBuff.append("  name=").append(m_name).append(suffix);
        toStringBuff.append("  hex_data=").append(m_hexData).append(suffix);	
        toStringBuff.append("  data=").append(m_data).append(suffix);	
        if (!m_authorizations.isEmpty()) {
			toStringBuff.append("authorizations [\n");
			for (PermissionLevel authorizastion : m_authorizations) {
				toStringBuff.append(authorizastion.toString());
				toStringBuff.append("\n");
			}
			toStringBuff.append("]\n");
		} else {
			toStringBuff.append("authorizations []\n");
		}
        toStringBuff.append("  ]");
        return toStringBuff.toString();
    }
	
    @Override
	public String toJson(){
		try{
			ObjectMapper mapper = new ObjectMapper();
			ObjectNode actionNode = mapper.createObjectNode();
			toJson(actionNode);
			String jsonStr =  mapper.writeValueAsString (actionNode);
			m_logger.debug(" Action Json String is :" + jsonStr);
			return jsonStr;
		}
		catch(Exception ex){
			m_logger.error(" Action toJson error:" + ex.getMessage());
			return "";
		}
	}

    @Override
	public synchronized void jsonParse(String json) {
		try{
			ObjectMapper mapper = new ObjectMapper();
			JsonNode actionNode = mapper.readTree(json); 
			jsonParse(actionNode);
		}
		catch(IOException ex){
			m_logger.error(" Action jsonParse error:" + ex.getMessage());
		}
	}
}
