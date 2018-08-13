package sonchain.blockchain.accounts;

import java.io.IOException;

import org.apache.log4j.Logger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import sonchain.blockchain.core.Authority;
import sonchain.blockchain.core.IJson;

public class NewAccount implements IJson{

	public static final Logger m_logger = Logger.getLogger(NewAccount.class);
	private String m_creater = "";
	public String getCreater() {
		return m_creater;
	}
	public void setCreater(String creater) {
		m_creater = creater;
	}

	private String m_name = "";
	public String getName() {
		return m_name;
	}
	public void setName(String name) {
		m_name = name;
	}

	private Authority m_owner = new Authority();
	public Authority getOwner() {
		return m_owner;
	}
	public void setOwner(Authority owner) {
		m_owner = owner;
	}

	private Authority m_active = new Authority();
	public Authority getActive() {
		return m_active;
	}
	public void setActive(Authority active) {
		m_active = active;
	}
	
	public NewAccount(String m_creater, String m_name, Authority m_owner, Authority m_active) {
		this.m_creater = m_creater;
		this.m_name = m_name;
		this.m_owner = m_owner;
		this.m_active = m_active;
	}
	
	public NewAccount() {
	}
	
	public NewAccount(String m_creater, String m_name, Authority m_owner) {
		this.m_creater = m_creater;
		this.m_name = m_name;
		this.m_owner = m_owner;
	}

	@Override
	public void toJson(ObjectNode newAccountNode) {
		newAccountNode.put("creater", m_creater);
		newAccountNode.put("name", m_name);
		if(m_owner != null){
			m_owner.toJson(newAccountNode);
		}	
		if(m_active != null){
			m_active.toJson(newAccountNode);
		}	

		m_logger.debug("toJson creater\t\t\t: " + m_creater);
		m_logger.debug("toJson name\t\t\t: " + m_name);
		m_logger.debug("jsonParse owner\t\t\t: " + m_owner.toString());
		m_logger.debug("jsonParse active\t\t\t: " + m_active.toString());

	}
	@Override
	public void jsonParse(JsonNode newAccountNode) {

		try {
			m_creater = newAccountNode.get("creater").asText();
			m_name = newAccountNode.get("name").asText();

			if(m_owner == null){
				m_owner = new Authority();
				m_owner.jsonParse(newAccountNode.get("owner"));
				m_logger.debug("jsonParse newProducers\t\t\t: " + m_owner.toString());
			}
			if(m_active == null){
				m_active = new Authority();
				m_active.jsonParse(newAccountNode.get("active"));
				m_logger.debug("jsonParse newProducers\t\t\t: " + m_active.toString());
			}
			m_logger.debug("jsonParse creater\t\t\t: " + m_creater);
			m_logger.debug("jsonParse name\t\t\t: " + m_name);		
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
			m_logger.debug(" NewAccount Json String is :" + jsonStr);
			return jsonStr;
		}
		catch(Exception ex){
			m_logger.error(" NewAccount toJson error:" + ex.getMessage());
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
			m_logger.error(" NewAccount jsonParse error:" + ex.getMessage());
		}
	}	
}
