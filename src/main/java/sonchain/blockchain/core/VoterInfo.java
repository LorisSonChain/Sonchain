package sonchain.blockchain.core;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.log4j.Logger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class VoterInfo implements IJson{
	public static Logger m_logger = Logger.getLogger(VoterInfo.class);

	public VoterInfo()
	{		
	}

	//The voter
	private String m_owner = "";
	// the proxy set by the voter, if any
	private String m_proxy = "";
	// the producers approved by this voter if no proxy set
	private List<String> m_producers = new ArrayList<String>();
	private long m_staked = 0;
	/// the vote weight cast the last time the vote was updated
	private double m_lastVoteWeight = 0;
	// the total vote weight delegated to this voter as a proxy
	private double m_proxiedVoteWeight = 0;
	// whether the voter is a proxy for others
	private boolean m_isProxy = false;

	private int m_reserved1 = 0;
	private long m_reserved2 = 0;
	private Asset m_reserved3 = new Asset();
	public static Logger getmLogger() {
		return m_logger;
	}
	public static void setmLogger(Logger mLogger) {
		m_logger = mLogger;
	}
	public String getOwner() {
		return m_owner;
	}
	public void setOwner(String owner) {
		m_owner = owner;
	}
	public String getProxy() {
		return m_proxy;
	}
	public void setProxy(String proxy) {
		m_proxy = proxy;
	}
	public List<String> getProducers() {
		return m_producers;
	}
	public void setProducers(List<String> producers) {
		m_producers = producers;
	}
	public long getStaked() {
		return m_staked;
	}
	public void setStaked(long staked) {
		m_staked = staked;
	}
	public double getLastVoteWeight() {
		return m_lastVoteWeight;
	}
	public void setLastVoteWeight(double lastVoteWeight) {
		m_lastVoteWeight = lastVoteWeight;
	}
	public double getProxiedVoteWeight() {
		return m_proxiedVoteWeight;
	}
	public void setProxiedVoteWeight(double proxiedVoteWeight) {
		m_proxiedVoteWeight = proxiedVoteWeight;
	}
	public boolean isProxy() {
		return m_isProxy;
	}
	public void setProxy(boolean isProxy) {
		m_isProxy = isProxy;
	}
	public int getReserved1() {
		return m_reserved1;
	}
	public void setReserved1(int reserved1) {
		m_reserved1 = reserved1;
	}
	public long getReserved2() {
		return m_reserved2;
	}
	public void setReserved2(long reserved2) {
		m_reserved2 = reserved2;
	}
	public Asset getReserved3() {
		return m_reserved3;
	}
	public void setReserved3(Asset reserved3) {
		m_reserved3 = reserved3;
	}



	public VoterInfo(String m_owner, String m_proxy, List<String> m_producers, long m_staked, double m_lastVoteWeight,
			double m_proxiedVoteWeight, boolean m_isProxy, int m_reserved1, long m_reserved2, Asset m_reserved3) {
		super();
		this.m_owner = m_owner;
		this.m_proxy = m_proxy;
		this.m_producers = m_producers;
		this.m_staked = m_staked;
		this.m_lastVoteWeight = m_lastVoteWeight;
		this.m_proxiedVoteWeight = m_proxiedVoteWeight;
		this.m_isProxy = m_isProxy;
		this.m_reserved1 = m_reserved1;
		this.m_reserved2 = m_reserved2;
		this.m_reserved3 = m_reserved3;
	}
	@Override
	public void toJson(ObjectNode node) {
		ObjectNode oNode = node.objectNode();
		m_logger.debug("toJson owner/t/t/t:\" + m_owner");
		m_logger.debug("toJson proxy/t/t/t:\" + m_proxy");
		m_logger.debug("toJson lastVoteWeight/t/t/t:\" + m_staked");
		m_logger.debug("toJson lastVoteWeight/t/t/t:\" + m_lastVoteWeight");
		m_logger.debug("toJson proxiedVoteWeight/t/t/t:\" + m_proxiedVoteWeight");
		m_logger.debug("toJson isProxy/t/t/t:\" + m_isProxy");
		m_logger.debug("toJson reserved1/t/t/t:\" + m_reserved1");
		m_logger.debug("toJson reserved2/t/t/t:\" + m_reserved2");
		ArrayNode producers = node.arrayNode();
		if(m_producers != null){
			int size = m_producers.size();
			for(int i = 0 ; i < size; i++){
				//List<String> list = new ArrayList<>();
				String string = m_producers.get(i);
				producers.add(string);
			}
			oNode.set("producers", producers);
		}
		if(m_reserved3 != null){
			ObjectNode nodeNetLimie = node.objectNode();
			m_reserved3.toJson(oNode);
			node.set("reserved3", nodeNetLimie);
		}

		oNode.put("owner", m_owner);
		oNode.put("proxy", m_proxy);
		oNode.put("staked", m_staked);
		oNode.put("lastVoteWeight", m_lastVoteWeight);
		oNode.put("lastVoteWeight", m_lastVoteWeight);
		oNode.put("proxiedVoteWeight", m_proxiedVoteWeight);
		oNode.put("isProxy", m_isProxy);
		oNode.put("reserved1", m_reserved1);
		oNode.put("reserved2", m_reserved2);
		oNode.put("producerNodes", m_producers.toString());
		oNode.put("Assets", m_reserved3.toString());
		node.set("voterInfo", oNode);
	}
	@Override
	public synchronized void jsonParse(JsonNode node) {
		// TODO Auto-generated method stub
		try {	
			m_owner = node.get("owner").asText();
			m_proxy = node.get("proxy").asText();
			m_staked = node.get("staked").asLong();
			m_lastVoteWeight = node.get("lastVoteWeight").asDouble();
			m_proxiedVoteWeight = node.get("proxiedVoteWeight").asDouble();
			m_isProxy = node.get("isProxy").asBoolean();
			m_reserved1 = node.get("reserved1").asInt();
			m_reserved2 = node.get("reserved2").asLong();
			if(m_reserved3 == null){
				m_reserved3 = new Asset();
			}
			m_reserved3.jsonParse(node);
			m_logger.debug("jsonParse Assets\t\t\t: " + m_reserved3.toString());

			JsonNode producerNodes = node.get("producers");
			for (JsonNode authorization : producerNodes) {  
				String asText = authorization.asText();
				m_producers.add(asText) ;
			}
			m_logger.debug("jsonParse owner\t\t\t: " + m_owner);
			m_logger.debug("jsonParse proxy\t\t\t: " + m_proxy);
			m_logger.debug("jsonParse staked\t\t\t: " + m_staked);
			m_logger.debug("jsonParse lastVoteWeight\t\t\t: " + m_lastVoteWeight);
			m_logger.debug("jsonParse proxiedVoteWeight\t\t\t: " + m_proxiedVoteWeight);
			m_logger.debug("jsonParse isProxy\t\t\t: " + m_isProxy);
			m_logger.debug("jsonParse reserved1\t\t\t: " + m_reserved1);
			m_logger.debug("jsonParse reserved2\t\t\t: " + m_reserved2);

		}catch (Exception e) {
			m_logger.error(e);
			throw new RuntimeException("Error on parsing Json", e);
		}

	}
	@Override
	public String toJson() {
		try{
			ObjectMapper mapper = new ObjectMapper();
			ObjectNode VoterInfoNode = mapper.createObjectNode();
			toJson(VoterInfoNode);
			String jsonStr =  mapper.writeValueAsString (VoterInfoNode);
			m_logger.debug(" VoterInfo Json String is :" + jsonStr);
			return jsonStr;
		}
		catch(Exception ex){
			m_logger.error(" VoterInfo toJson error:" + ex.getMessage());
			return "";
		}
	}


	@Override
	public synchronized void jsonParse(String json) {
		try{
			ObjectMapper mapper = new ObjectMapper();
			JsonNode VoterInfoNode = mapper.readTree(json); 
			jsonParse(VoterInfoNode);
		}
		catch(IOException ex){
			m_logger.error(" VoterInfo jsonParse error:" + ex.getMessage());
		}
	}

	@Override
	public String toString() {
		return toString("\n");
	}
	
	public String toString(final String suffix) {
		StringBuilder toStringBuff = new StringBuilder();
        toStringBuff.append("  VoterInfo[owner=").append(m_owner).append(suffix);
        toStringBuff.append("  proxy").append(m_proxy).append(suffix);
        toStringBuff.append("  staked").append(m_staked).append(suffix);
        toStringBuff.append("  lastVoteWeight=").append(m_lastVoteWeight).append(suffix);
        toStringBuff.append("  proxiedVoteWeight=").append(m_proxiedVoteWeight).append(suffix);
        toStringBuff.append("  isProxy=").append(m_isProxy).append(suffix);
        toStringBuff.append("  reserved1=").append(m_reserved1).append(suffix);
        toStringBuff.append("  reserved2=").append(m_reserved2).append(suffix);
        toStringBuff.append("  reserved3=").append(m_reserved3.toString()).append(suffix);
        if(m_producers != null){
         	toStringBuff.append("  producers=").append(suffix);
         	int count = m_producers.size();
         	for(int i = 0; i < count; i++){
         		toStringBuff.append(m_producers.get(i).toString()).append(suffix);
         	}
         }
        return toStringBuff.toString();
	}

	public String getPrimaryKey(){
		return m_owner;
	}
}
