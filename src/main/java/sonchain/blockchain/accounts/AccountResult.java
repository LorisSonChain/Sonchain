package sonchain.blockchain.accounts;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.log4j.Logger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import sonchain.blockchain.core.Action;
import sonchain.blockchain.core.Asset;
import sonchain.blockchain.core.IJson;
import sonchain.blockchain.core.Permission;
import sonchain.blockchain.core.PermissionLevel;
import sonchain.blockchain.core.ProducerScheduleType;
import sonchain.blockchain.core.VoterInfo;
import sonchain.blockchain.util.ByteUtil;
import sonchain.blockchain.util.Utils;

public class AccountResult implements IJson {
	public static Logger m_logger = Logger.getLogger(AccountResult.class);

	public AccountResult()
	{		
	}
	
	private String m_name = "";
	private long m_headBlockNum = 0;
	private long m_headBlockTime = 0;
	private boolean m_privileged = false;
	private long m_lastCodeUpdate = 0;
	private long m_create = 0;
	private List<Asset> m_coreLiquidBalance = new ArrayList<Asset>();
	private long m_ramQuota = 0;
	private long m_netWeight = 0;
	private long cpu_weight = 0;
	private AccountResourceLimit m_netLimit = new AccountResourceLimit();
	private AccountResourceLimit m_cpuLimit = new AccountResourceLimit();
	private long m_rawUsage = 0;
	private List<Permission> m_permission = new ArrayList<Permission>();
	private VoterInfo m_voterInfo = new VoterInfo();
	
	public String getName() {
		return m_name;
	}
	public void setName(String name) {
		m_name = name;
	}
	public long getHeadBlockNum() {
		return m_headBlockNum;
	}
	public void setHeadBlockNum(long headBlockNum) {
		m_headBlockNum = headBlockNum;
	}
	public long getHeadBlockTime() {
		return m_headBlockTime;
	}
	public void setHeadBlockTime(long headBlockTime) {
		m_headBlockTime = headBlockTime;
	}
	public boolean isPrivileged() {
		return m_privileged;
	}
	public void setPrivileged(boolean privileged) {
		m_privileged = privileged;
	}
	public long getLastCodeUpdate() {
		return m_lastCodeUpdate;
	}
	public void setLastCodeUpdate(long lastCodeUpdate) {
		m_lastCodeUpdate = lastCodeUpdate;
	}
	public long getCreate() {
		return m_create;
	}
	public void setCreate(long create) {
		m_create = create;
	}
	public List<Asset> getCoreLiquidBalance() {
		return m_coreLiquidBalance;
	}
	public void setCoreLiquidBalance(List<Asset> coreLiquidBalance) {
		m_coreLiquidBalance = coreLiquidBalance;
	}
	public long getRamQuota() {
		return m_ramQuota;
	}
	public void setRamQuota(long ramQuota) {
		m_ramQuota = ramQuota;
	}
	public long getNetWeight() {
		return m_netWeight;
	}
	public void setNetWeight(long netWeight) {
		m_netWeight = netWeight;
	}
	public long getCpu_weight() {
		return cpu_weight;
	}
	public void setCpu_weight(long cpu_weight) {
		this.cpu_weight = cpu_weight;
	}
	public AccountResourceLimit getNetLimit() {
		return m_netLimit;
	}
	public void setNetLimit(AccountResourceLimit netLimit) {
		m_netLimit = netLimit;
	}
	public AccountResourceLimit getCpuLimit() {
		return m_cpuLimit;
	}
	public void setCpuLimit(AccountResourceLimit cpuLimit) {
		m_cpuLimit = cpuLimit;
	}
	public long getRawUsage() {
		return m_rawUsage;
	}
	public void setRawUsage(long rawUsage) {
		m_rawUsage = rawUsage;
	}
	public List<Permission> getPermission() {
		return m_permission;
	}
	public void setPermission(List<Permission> permission) {
		m_permission = permission;
	}
	public VoterInfo getVoterInfo() {
		return m_voterInfo;
	}
	public void setVoterInfo(VoterInfo voterInfo) {
		m_voterInfo = voterInfo;
	}
	
	
	
	public AccountResult(String m_name, long m_headBlockNum, long m_headBlockTime, boolean m_privileged,
			long m_lastCodeUpdate, long m_create, List<Asset> m_coreLiquidBalance, long m_ramQuota, long m_netWeight,
			long cpu_weight, AccountResourceLimit m_netLimit, AccountResourceLimit m_cpuLimit, long m_rawUsage,
			List<Permission> m_permission, VoterInfo m_voterInfo) {
		this.m_name = m_name;
		this.m_headBlockNum = m_headBlockNum;
		this.m_headBlockTime = m_headBlockTime;
		this.m_privileged = m_privileged;
		this.m_lastCodeUpdate = m_lastCodeUpdate;
		this.m_create = m_create;
		this.m_coreLiquidBalance = m_coreLiquidBalance;
		this.m_ramQuota = m_ramQuota;
		this.m_netWeight = m_netWeight;
		this.cpu_weight = cpu_weight;
		this.m_netLimit = m_netLimit;
		this.m_cpuLimit = m_cpuLimit;
		this.m_rawUsage = m_rawUsage;
		this.m_permission = m_permission;
		this.m_voterInfo = m_voterInfo;
	}
	@Override
	public void toJson(ObjectNode node) {
		
		node.put("name", m_name);
		node.put("headBlockNum", m_headBlockNum);
		node.put("headBlockTime", m_headBlockTime);
		node.put("privileged", m_privileged);
		node.put("lastCodeUpdate", m_lastCodeUpdate);
		node.put("create", m_create);
		node.put("ramQuota",  m_ramQuota);
		node.put("netWeight", m_netWeight);
		node.put("weight", cpu_weight);
		node.put("rawUsage",  m_rawUsage);
		
		if(m_netLimit != null){
			ObjectNode nodeNetLimie = node.objectNode();
			m_netLimit.toJson(nodeNetLimie);
			node.set("netLimit", nodeNetLimie);
		}	
		if(m_cpuLimit != null){
			ObjectNode nodeNetLimie = node.objectNode();
			m_cpuLimit.toJson(node);
			node.set("cpuLimit", nodeNetLimie);
		}	
		
		ArrayNode coreLiquidBalance = node.arrayNode();
        int sizeAccounts =  m_coreLiquidBalance.size();
        for(int i = 0; i < sizeAccounts; i ++){
        	ObjectNode producerNode = node.objectNode();
        	m_coreLiquidBalance.get(i).toJson(producerNode);
        	coreLiquidBalance.add(producerNode);
        }
        node.set("coreLiquidBalance", coreLiquidBalance);
        
        ArrayNode permission = node.arrayNode();
        int sizePermission =  m_permission.size();
        for(int i = 0; i < sizePermission; i ++){
        	ObjectNode producerNode = node.objectNode();
        	m_permission.get(i).toJson(producerNode);
        	permission.add(producerNode);
        }
        node.set("permission", permission);
		
		m_logger.debug("toJson name\t\t\t: " + m_name);
		m_logger.debug("toJson headBlockNum\t\t\t: " + m_headBlockNum);
		m_logger.debug("toJson headBlockTime\t\t\t: " + m_headBlockTime);
		m_logger.debug("toJson privileged\t\t\t: " + m_privileged);
		m_logger.debug("toJson lastCodeUpdate\t\t\t: " + m_lastCodeUpdate);
		m_logger.debug("toJson create\t\t\t: " + m_create);
		m_logger.debug("toJson  ramQuota\t\t\t: " +  m_ramQuota);
		m_logger.debug("toJson  netWeight\t\t\t: " +  m_netWeight);
		m_logger.debug("toJson  weight\t\t\t: " +  cpu_weight);
		m_logger.debug("toJson  rawUsage\t\t\t: " +  m_rawUsage);
		m_logger.debug("toJson  netLimit\t\t\t: " +  m_netLimit.toString());
		m_logger.debug("toJson  cpuLimit\t\t\t: " +  m_cpuLimit.toString());
		
	}
	@Override
	public void jsonParse(JsonNode node) {
		try {		
			m_name = node.get("name").asText();
			m_headBlockNum = node.get("headBlockNum").asLong();
			m_headBlockTime = node.get("headBlockTime").asLong();
			m_privileged = node.get("privileged").asBoolean();
			m_lastCodeUpdate = node.get("lastCodeUpdate").asLong();
			m_create = node.get("create").asLong();
			m_ramQuota = node.get("ramQuota").asLong();
			m_netWeight = node.get("netWeight").asLong();
			cpu_weight = node.get("weight").asLong();
			m_rawUsage = node.get("rawUsage").asLong();

			if(m_coreLiquidBalance == null){
				m_coreLiquidBalance = new CopyOnWriteArrayList<>();
			}
			JsonNode authorizations = node.get("coreLiquidBalance");
			for (JsonNode authorization : authorizations) {  
				Asset asset = new Asset();
				asset.jsonParse(authorization);
				m_coreLiquidBalance.add(asset);
			}			
			if(m_permission == null){
				m_permission = new CopyOnWriteArrayList<>();
			}
			JsonNode permissionNode = node.get("permission");
			for (JsonNode authorization : permissionNode) {  
				Permission permission = new Permission();
				permission.jsonParse(authorization);
				m_permission.add(permission);
			}	
			
			if(m_netLimit == null){
				m_netLimit = new AccountResourceLimit();
			}
			m_netLimit.jsonParse(node);
			m_logger.debug("jsonParse netLimit\t\t\t: " + m_netLimit.toString());
			
			if(m_cpuLimit == null){
				m_cpuLimit = new AccountResourceLimit();
			}
			m_cpuLimit.jsonParse(node);
			m_logger.debug("jsonParse cpuLimit\t\t\t: " + m_cpuLimit.toString());
			
	        m_logger.debug("jsonParse name\t\t\t: " + m_name);
	        m_logger.debug("jsonParse headBlockNum\t\t\t: " + m_headBlockNum);
	        m_logger.debug("jsonParse headBlockTime\t\t\t: " + m_headBlockTime);
	        m_logger.debug("jsonParse privileged\t\t\t: " + m_privileged);
	        m_logger.debug("jsonParse lastCodeUpdate\t\t\t: " + m_lastCodeUpdate);
	        m_logger.debug("jsonParse create\t\t\t: " + m_create);
	        m_logger.debug("jsonParse ramQuota\t\t\t: " + m_ramQuota);
	        m_logger.debug("jsonParse netWeight\t\t\t: " + m_netWeight);
	        m_logger.debug("jsonParse weight\t\t\t: " + cpu_weight);
	        m_logger.debug("jsonParse rawUsage\t\t\t: " + m_rawUsage);

		}catch (Exception e) {
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
			m_logger.debug(" BlockHeader Json String is :" + jsonStr);
			return jsonStr;
		}
		catch(Exception ex){
			m_logger.error(" BlockHeader toJson error:" + ex.getMessage());
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
			m_logger.error(" BlockHeader jsonParse error:" + ex.getMessage());
		}
	}
	
    @Override
    public String toString() {
        return toString("\n");
    }
    
    public String toString(final String suffix) {
    	 StringBuilder toStringBuff = new StringBuilder();
         toStringBuff.append("  BlockHeader[name=").append(m_name).append(suffix);
         toStringBuff.append("  headBlockNum=").append(m_headBlockNum).append(suffix);
         toStringBuff.append("  headBlockTime=").append(m_headBlockTime).append(suffix);
         toStringBuff.append("  privileged=").append(m_privileged).append(suffix);
         toStringBuff.append("  lastCodeUpdate=").append(m_lastCodeUpdate).append(suffix);
         toStringBuff.append("  create=").append(m_create).append(suffix);
         toStringBuff.append("  ramQuota=").append(m_ramQuota).append(suffix);
         toStringBuff.append("  netWeight=").append(m_netWeight).append(suffix);
         toStringBuff.append("  weight=").append(cpu_weight).append(suffix);
         toStringBuff.append("  rawUsage=").append(m_rawUsage).append(suffix);
         //toStringBuff.append("  coreLiquidBalance=").append(m_coreLiquidBalance).append(suffix);
         //toStringBuff.append("  permission=").append(m_permission).append(suffix);
         toStringBuff.append("  netLimit=").append(m_netLimit.toString()).append(suffix);
         toStringBuff.append("  cpuLimit=").append(m_cpuLimit.toString()).append(suffix);
         if(m_coreLiquidBalance != null){
         	toStringBuff.append("  coreLiquidBalance=").append(suffix);
         	int count = m_coreLiquidBalance.size();
         	for(int i = 0; i < count; i++){
         		toStringBuff.append(m_coreLiquidBalance.get(i).toString()).append(suffix);
         	}
         }
         if(m_permission != null){
        	 toStringBuff.append("  permission=").append(suffix);
        	 int count = m_permission.size();
        	 for(int i = 0; i < count; i++){
        		 toStringBuff.append(m_permission.get(i).toString()).append(suffix);
        	 }
         }
         toStringBuff.append("   ]");
         return toStringBuff.toString();
    }
}
