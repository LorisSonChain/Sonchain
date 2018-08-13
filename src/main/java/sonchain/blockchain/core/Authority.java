package sonchain.blockchain.core;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import sonchain.blockchain.util.ByteUtil;
import sonchain.blockchain.util.Utils;

public class Authority implements IJson{

	public static final Logger m_logger = Logger.getLogger(ProducerScheduleType.class);

	private int m_threshold = 0;
	public int getThreshold() {
		return m_threshold;
	}

	public void setThreshold(int threshold) {
		m_threshold = threshold;
	}

	private List<KeyWeight> m_keys = new ArrayList<KeyWeight>();
	public List<KeyWeight> getKeys() {
		return m_keys;
	}

	public void setKeys(List<KeyWeight> keys) {
		m_keys = keys;
	}

	private List<PermissionLevelWeight> m_accounts = new ArrayList<PermissionLevelWeight>();
	public List<PermissionLevelWeight> getAccounts() {
		return m_accounts;
	}

	public void setAccounts(List<PermissionLevelWeight> accounts) {
		m_accounts = accounts;
	}

	private List<WaitWeight> m_waits = new ArrayList<WaitWeight>();
	public List<WaitWeight> getWaits() {
		return m_waits;
	}

	public void setWaits(List<WaitWeight> waits) {
		m_waits = waits;
	}

	public Authority(){

	}

	public Authority(byte[] publicKey, int delay_sec){
		m_threshold = 1;
		KeyWeight keyWeight = new KeyWeight(publicKey, 1);
		m_keys.add(keyWeight);
		if(delay_sec > 0){
			m_threshold = 2;
			WaitWeight waitWeight = new WaitWeight(delay_sec, 1);
			m_waits.add(waitWeight);
		}		
	}

	public Authority(int threshold, List<KeyWeight> keys){
		m_threshold = threshold;
		m_keys = keys;
	}

	@Override
	public void toJson(ObjectNode authorityNode) {
		//ObjectNode node = authorityNode.objectNode();
		authorityNode.put("threshold", m_threshold);
		m_logger.debug("toJson threshold\t\t\t: " + m_threshold);
		ArrayNode keys = authorityNode.arrayNode();
		int sizeKeys = m_keys.size();
		for(int i = 0; i < sizeKeys; i ++){
			ObjectNode producerNode = authorityNode.objectNode();
			m_keys.get(i).toJson(producerNode);
			keys.add(producerNode);
		}
		authorityNode.set("keys", keys);
		ArrayNode accounts = authorityNode.arrayNode();
		int sizeAccounts = m_accounts.size();
		for(int i = 0; i < sizeAccounts; i ++){
			ObjectNode producerNode = authorityNode.objectNode();
			m_accounts.get(i).toJson(producerNode);
			accounts.add(producerNode);
		}
		authorityNode.set("accounts", accounts);
		ArrayNode waits = authorityNode.arrayNode();
		int sizeWaits = m_waits.size();
		for(int i = 0; i < sizeWaits; i ++){
			ObjectNode producerNode = authorityNode.objectNode();
			m_waits.get(i).toJson(producerNode);
			waits.add(producerNode);
		}
		authorityNode.set("waits", waits);
		// authorityNode.set("authoritys", node);
	}

	@Override
	public synchronized void jsonParse(JsonNode authorityNode) {
		try {
			m_threshold = authorityNode.get("threshold").asInt();
			m_logger.debug("jsonParse threshold\t\t\t: " + m_threshold);
			JsonNode keys = authorityNode.get("keys");
			for (JsonNode producer : keys) {  
				KeyWeight key = new KeyWeight();
				key.jsonParse(producer);
				m_keys.add(key);
			}
			JsonNode accounts = authorityNode.get("accounts");
			for (JsonNode producer : accounts) {  
				PermissionLevelWeight plw = new PermissionLevelWeight();
				plw.jsonParse(producer);
				m_accounts.add(plw);
			}
			JsonNode waits = authorityNode.get("waits");
			for (JsonNode producer : waits) {  
				WaitWeight waitWeight = new WaitWeight();
				waitWeight.jsonParse(producer);
				m_waits.add(waitWeight);
			}
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
			m_logger.debug(" Authority Json String is :" + jsonStr);
			return jsonStr;
		}
		catch(Exception ex){
			m_logger.error(" Authority toJson error:" + ex.getMessage());
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
			m_logger.error(" Authority jsonParse error:" + ex.getMessage());
		}
	}

	public Authority(int m_threshold, List<KeyWeight> m_keys, List<PermissionLevelWeight> m_accounts,
			List<WaitWeight> m_waits) {
		this.m_threshold = m_threshold;
		this.m_keys = m_keys;
		this.m_accounts = m_accounts;
		this.m_waits = m_waits;
	}

	@Override
	public String toString() {
		return toStringWithSuffix("\n");
	}

	private String toStringWithSuffix(final String suffix) {
		StringBuilder toStringBuff = new StringBuilder();
		toStringBuff.append("  Authority[threshold=").append(m_threshold).append(suffix);
		if (!m_keys.isEmpty()) {
			toStringBuff.append("keys [\n");
			for (KeyWeight keyWeight : m_keys) {
				toStringBuff.append(keyWeight.toString());
				toStringBuff.append("\n");
			}
			toStringBuff.append("]\n");
		} else {
			toStringBuff.append("keyWeight []\n");
		}
		if (!m_accounts.isEmpty()) {
			toStringBuff.append("accounts [\n");
			for (PermissionLevelWeight permissionLevelWeight : m_accounts) {
				toStringBuff.append(permissionLevelWeight.toString());
				toStringBuff.append("\n");
			}
			toStringBuff.append("]\n");
		} else {
			toStringBuff.append("accounts []\n");
		}
		if (!m_waits.isEmpty()) {
			toStringBuff.append("waits [\n");
			for (WaitWeight waitWeight : m_waits) {
				toStringBuff.append(waitWeight.toString());
				toStringBuff.append("\n");
			}
			toStringBuff.append("]\n");
		} else {
			toStringBuff.append("waits []\n");
		}
		toStringBuff.append("   ]");
		return toStringBuff.toString();
	}


}
