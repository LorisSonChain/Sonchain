package sonchain.blockchain.core;

import java.io.IOException;

import org.apache.log4j.Logger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class Permission implements IJson{

	private String m_permName = "";
	private String m_parent = "";
	private Authority m_requiredAuth = new Authority();

	public static final Logger m_logger = Logger.getLogger(ProducerScheduleType.class);
	public String getPermName() {
		return m_permName;
	}

	public void setPermName(String permName) {
		m_permName = permName;
	}

	public String getParent() {
		return m_parent;
	}

	public void setParent(String parent) {
		m_parent = parent;
	}

	public Authority getRequiredAuth() {
		return m_requiredAuth;
	}

	public void setRequiredAuth(Authority requiredAuth) {
		m_requiredAuth = requiredAuth;
	}

	public Permission()
	{		
	}



	public Permission(String m_permName, String m_parent, Authority m_requiredAuth) {
		this.m_permName = m_permName;
		this.m_parent = m_parent;
		this.m_requiredAuth = m_requiredAuth;
	}

	@Override
	public void toJson(ObjectNode node) {
		//ObjectNode oNode = node.objectNode();
		node.put("permName",m_permName);
		m_logger.debug("toJson permName\t\t\t: " + m_permName);

		node.put("parent",m_parent);
		m_logger.debug("toJson parent\t\t\t: " + m_parent);

		if(m_requiredAuth != null){
			ObjectNode nodeNetLimie = node.objectNode();
			m_requiredAuth.toJson(node);
			node.set("requiredAuth", nodeNetLimie);
		}
		m_logger.debug("jsonParse requiredAuth\t\t\t: " + m_requiredAuth.toString());

	}

	@Override
	public void jsonParse(JsonNode node) {
		try {
			m_permName = node.get("permName").asText();
			m_parent = node.get("parent").asText();

			if(m_requiredAuth == null){
				m_requiredAuth = new Authority();
				m_requiredAuth.jsonParse(node.get("permissions"));
			}
			m_logger.debug("jsonParse permName\t\t\t: " + m_permName);
			m_logger.debug("jsonParse parent\t\t\t: " + m_parent);
			m_logger.debug("jsonParse permissions\t\t\t: " + m_requiredAuth.toString());

		} catch (Exception e) {
			m_logger.error(e);
			throw new RuntimeException("Error on parsing Json", e);
		}

	}

	@Override
	public String toJson(){
		try{
			ObjectMapper mapper = new ObjectMapper();
			ObjectNode blockHeaderNode = mapper.createObjectNode();
			toJson(blockHeaderNode);
			String jsonStr =  mapper.writeValueAsString (blockHeaderNode);
			m_logger.debug(" Permission Json String is :" + jsonStr);
			return jsonStr;
		}
		catch(Exception ex){
			m_logger.error(" Permission toJson error:" + ex.getMessage());
			return "";
		}
	}

	@Override
	public synchronized void jsonParse(String json) {
		try{
			ObjectMapper mapper = new ObjectMapper();
			JsonNode blockHeaderNode = mapper.readTree(json); 
			jsonParse(blockHeaderNode);
		}
		catch(IOException ex){
			m_logger.error(" Permission jsonParse error:" + ex.getMessage());
		}
	}

	@Override
	public String toString() {
		return toString("\n");
	}

	public String toString(final String suffix) {
		StringBuilder toStringBuff = new StringBuilder();
		toStringBuff.append("  Permission[permName=").append(m_permName).append(suffix);
		toStringBuff.append("  parent=").append(m_parent).append(suffix);
		toStringBuff.append("  requiredAuth=").append(m_requiredAuth).append(suffix);
		toStringBuff.append("   ]");
		return toStringBuff.toString();
	}
}
