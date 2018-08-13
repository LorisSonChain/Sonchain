package sonchain.blockchain.core;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.log4j.Logger;
import org.bouncycastle.util.encoders.Hex;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import sonchain.blockchain.crypto.HashUtil;
import sonchain.blockchain.service.DataCenter;
import sonchain.blockchain.util.FastByteComparisons;
import sonchain.blockchain.util.Numeric;

public class BlockHeaderState implements IJson{
	
	public static final Logger m_logger = Logger.getLogger(BlockHeaderState.class);
	
	public BlockHeaderState(){		
	}
	
	private byte[] m_hash = null;
	private int m_blockNum = 0;
	private BlockHeader m_header = new BlockHeader();
	private int m_dposProposedIrreversibleBlocknum = 0;
	private int m_dposIrreversibleBlocknum = 0;
	private int m_bftIrreversibleBlocknum = 0;
	private int m_pendingScheduleLibNum = 0; // last irr block num
	private byte[] m_pendingScheduleHash = null;
	private ProducerScheduleType m_pendingSchedule = new ProducerScheduleType();
	private ProducerScheduleType m_activeSchedule = new ProducerScheduleType();
	//private IncrementalMerkle m_blockRootMerkle = new IncrementalMerkle();
	private Map<String,Integer> m_producerToLastProduced = new HashMap<String, Integer>();
	private Map<String,Integer> m_producerToLastImpliedIrb = new HashMap<String, Integer>();
	private String m_blockSigningKey = "";
	private List<Integer> m_confirmCount = new ArrayList<Integer>();
	private List<HeaderConfirmation> m_confirmations = new ArrayList<HeaderConfirmation>();
    
	public byte[] getHash() {
		return m_hash;
	}

	public void setHash(byte[] hash) {
		m_hash = hash;
	}

	public int getBlockNum() {
		return m_blockNum;
	}

	public void setBlockNum(int blockNum) {
		m_blockNum = blockNum;
	}

	public BlockHeader getHeader() {
		return m_header;
	}

	public void setHeader(BlockHeader header) {
		m_header = header;
	}

	public int getDposProposedIrreversibleBlocknum() {
		return m_dposProposedIrreversibleBlocknum;
	}

	public void setDposProposedIrreversibleBlocknum(int dposProposedIrreversibleBlocknum) {
		m_dposProposedIrreversibleBlocknum = dposProposedIrreversibleBlocknum;
	}

	public int getDposIrreversibleBlocknum() {
		return m_dposIrreversibleBlocknum;
	}

	public void setDposIrreversibleBlocknum(int dposIrreversibleBlocknum) {
		m_dposIrreversibleBlocknum = dposIrreversibleBlocknum;
	}

	public int getBftIrreversibleBlocknum() {
		return m_bftIrreversibleBlocknum;
	}

	public void setBftIrreversibleBlocknum(int bftIrreversibleBlocknum) {
		m_bftIrreversibleBlocknum = bftIrreversibleBlocknum;
	}

	public int getPendingScheduleLibNum() {
		return m_pendingScheduleLibNum;
	}

	public void setPendingScheduleLibNum(int pendingScheduleLibNum) {
		m_pendingScheduleLibNum = pendingScheduleLibNum;
	}

	public byte[] getPendingScheduleHash() {
		return m_pendingScheduleHash;
	}

	public void setPendingScheduleHash(byte[] pendingScheduleHash) {
		m_pendingScheduleHash = pendingScheduleHash;
	}

	public ProducerScheduleType getPendingSchedule() {
		return m_pendingSchedule;
	}

	public void setPendingSchedule(ProducerScheduleType pendingSchedule) {
		m_pendingSchedule = pendingSchedule;
	}

	public ProducerScheduleType getActiveSchedule() {
		return m_activeSchedule;
	}

	public void setActiveSchedule(ProducerScheduleType activeSchedule) {
		m_activeSchedule = activeSchedule;
	}

	public Map<String, Integer> getProducerToLastProduced() {
		return m_producerToLastProduced;
	}

	public void setProducerToLastProduced(Map<String, Integer> producerToLastProduced) {
		m_producerToLastProduced = producerToLastProduced;
	}

	public Map<String, Integer> getProducerToLastImpliedIrb() {
		return m_producerToLastImpliedIrb;
	}

	public void setProducerToLastImpliedIrb(Map<String, Integer> producerToLastImpliedIrb) {
		m_producerToLastImpliedIrb = producerToLastImpliedIrb;
	}

	public String getBlockSigningKey() {
		return m_blockSigningKey;
	}

	public void setBlockSigningKey(String blockSigningKey) {
		m_blockSigningKey = blockSigningKey;
	}

	public List<Integer> getConfirmCount() {
		return m_confirmCount;
	}

	public void setConfirmCount(List<Integer> confirmCount) {
		m_confirmCount = confirmCount;
	}

	public List<HeaderConfirmation> getConfirmations() {
		return m_confirmations;
	}

	public void setConfirmations(List<HeaderConfirmation> confirmations) {
		m_confirmations = confirmations;
	}	
	
	public BlockHeaderState(byte[] m_hash, int m_blockNum, BlockHeader m_header, int m_dposProposedIrreversibleBlocknum,
			int m_dposIrreversibleBlocknum, int m_bftIrreversibleBlocknum, int m_pendingScheduleLibNum,
			byte[] m_pendingScheduleHash, ProducerScheduleType m_pendingSchedule, ProducerScheduleType m_activeSchedule,
			Map<String, Integer> m_producerToLastProduced, Map<String, Integer> m_producerToLastImpliedIrb,
			String m_blockSigningKey, List<Integer> m_confirmCount, List<HeaderConfirmation> m_confirmations) {
		this.m_hash = m_hash;
		this.m_blockNum = m_blockNum;
		this.m_header = m_header;
		this.m_dposProposedIrreversibleBlocknum = m_dposProposedIrreversibleBlocknum;
		this.m_dposIrreversibleBlocknum = m_dposIrreversibleBlocknum;
		this.m_bftIrreversibleBlocknum = m_bftIrreversibleBlocknum;
		this.m_pendingScheduleLibNum = m_pendingScheduleLibNum;
		this.m_pendingScheduleHash = m_pendingScheduleHash;
		this.m_pendingSchedule = m_pendingSchedule;
		this.m_activeSchedule = m_activeSchedule;
		this.m_producerToLastProduced = m_producerToLastProduced;
		this.m_producerToLastImpliedIrb = m_producerToLastImpliedIrb;
		this.m_blockSigningKey = m_blockSigningKey;
		this.m_confirmCount = m_confirmCount;
		this.m_confirmations = m_confirmations;
	}

	@Override
	public void toJson(ObjectNode node) {
		String hash = Hex.toHexString(getHash());
		String pendingScheduleHash = Hex.toHexString(getPendingScheduleHash());
		
		node.put("hash", hash);
		node.put("pendingScheduleHash", pendingScheduleHash);
		node.put("blockNum", m_blockNum);
		node.put("blockSigningKey", m_blockSigningKey);
		node.put("dposProposedIrreversibleBlocknum", m_dposProposedIrreversibleBlocknum );
		node.put("dposIrreversibleBlocknum", m_dposIrreversibleBlocknum);
		node.put("bftIrreversibleBlocknum", m_bftIrreversibleBlocknum);
		node.put("pendingScheduleLibNum", m_pendingScheduleLibNum);
		
		m_logger.debug("toJson hash\t\t\t: " + hash);
		m_logger.debug("toJson pendingScheduleHash\t\t\t: " + pendingScheduleHash);
		m_logger.debug("toJson blockNum\t\t\t: " + m_blockNum);
		m_logger.debug("toJson dposProposedIrreversibleBlocknum\t\t\t: " + m_dposProposedIrreversibleBlocknum);
		m_logger.debug("toJson dposIrreversibleBlocknum\t\t\t: " + m_dposIrreversibleBlocknum);
		m_logger.debug("toJson bftIrreversibleBlocknum\t\t\t: " + m_bftIrreversibleBlocknum);
		m_logger.debug("toJson pendingScheduleLibNum\t\t\t: " + m_pendingScheduleLibNum);

		ObjectNode nodeNetLimie = node.objectNode();
		if(m_header != null){
			m_header.toJson(nodeNetLimie);
		}	
		node.set("header", nodeNetLimie);
		m_logger.debug("toJson header\t\t\t: " + m_header.toString());
		
		ObjectNode nodePendingSchedule = node.objectNode();
		if(m_pendingSchedule != null){
			m_pendingSchedule.toJson(nodePendingSchedule);
		}
		node.set("pendingSchedule", nodePendingSchedule);
		m_logger.debug("toJson pendingSchedule\t\t\t: " + m_pendingSchedule.toString());
		
		ObjectNode nodeActiveSchedule = node.objectNode();
		if(m_activeSchedule != null){
			m_activeSchedule.toJson(nodeActiveSchedule);
		}	
		node.set("activeSchedule", nodeActiveSchedule);
		m_logger.debug("toJson activeSchedule\t\t\t: " + m_activeSchedule.toString());
		
		ArrayNode confirmCount = node.arrayNode();
        int sizeConfirmCount = m_confirmCount.size();
        for(int i = 0; i < sizeConfirmCount; i ++){
        	Integer a = m_confirmCount.get(i);
        	confirmCount.add(a);
        }
        node.set("confirmCount", confirmCount);
        
        ArrayNode confirmations = node.arrayNode();
        int sizeConfirmations = m_confirmations.size();
        for(int i = 0; i < sizeConfirmations; i++){
        	ObjectNode producerNode = node.objectNode();
        	m_confirmations.get(i).toJson(producerNode);
        	confirmations.add(producerNode);
        }
        node.set("confirmations", confirmations);
        
        ArrayNode producerToLastProduced = node.arrayNode();
        for(Map.Entry<String, Integer> key : m_producerToLastProduced.entrySet()) {
        	ArrayNode province = node.arrayNode();
        	province.add(key.getKey());
            province.add(key.getValue());
            producerToLastProduced.add(province);
        }
        node.set("producerToLastProduced", producerToLastProduced);
        
        ArrayNode producerToLastImpliedIrb = node.arrayNode();
        for(Map.Entry<String, Integer> key : m_producerToLastImpliedIrb.entrySet()) {
        	ArrayNode province = node.arrayNode();
        	province.add(key.getKey());
            province.add(key.getValue());
        	producerToLastImpliedIrb.add(province);
        }
        node.set("producerToLastImpliedIrb", producerToLastImpliedIrb);
	}

	@Override
	public void jsonParse(JsonNode node) {
		try {
			String hash = node.get("hash").asText();
			String pendingScheduleHash = node.get("pendingScheduleHash").asText();
			m_blockSigningKey = node.get("blockSigningKey").asText();
			m_blockNum = node.get("blockNum").asInt();
			m_dposProposedIrreversibleBlocknum = node.get("dposProposedIrreversibleBlocknum").asInt();
			m_dposIrreversibleBlocknum = node.get("dposIrreversibleBlocknum").asInt();
			m_bftIrreversibleBlocknum = node.get("bftIrreversibleBlocknum").asInt();
			m_pendingScheduleLibNum = node.get("pendingScheduleLibNum").asInt();
			m_hash = Numeric.hexStringToByteArray(hash);
			m_pendingScheduleHash = Numeric.hexStringToByteArray(pendingScheduleHash);
			
			if(m_confirmations == null){
				m_confirmations = new CopyOnWriteArrayList<>();
			}
			JsonNode waitsNode = node.get("confirmations");
			for (JsonNode producer : waitsNode) {  
				HeaderConfirmation waitWeight = new HeaderConfirmation();
				waitWeight.jsonParse(producer);
				m_confirmations.add(waitWeight);
			}
			
			if(m_confirmCount == null){
				m_confirmCount = new CopyOnWriteArrayList<>();
			}
			JsonNode confirmCountNodes = node.get("confirmCount");
			for (JsonNode authorization : confirmCountNodes) {  
				int asInt = authorization.asInt();
				m_confirmCount.add(asInt) ;
			}
			
			JsonNode ptpNode = node.get("producerToLastProduced");
			for (JsonNode authorization : ptpNode) {
				String key = authorization.get(0).asText();
				Integer value = authorization.get(1).asInt();
				m_producerToLastProduced.put(key, value);
			}
			JsonNode pttNode = node.get("producerToLastImpliedIrb");
			for (JsonNode authorization : pttNode) {
				String key = authorization.get(0).asText();
				Integer value = authorization.get(1).asInt();
				m_producerToLastImpliedIrb.put(key, value);
			}
			
			if(m_header == null){
				m_header = new BlockHeader ();
			}
			m_header.jsonParse(node.get("header"));
			m_logger.debug("jsonParse cpuLimit\t\t\t: " + m_header.toString());
			
			if(m_pendingSchedule == null){
				m_pendingSchedule = new ProducerScheduleType();
			}
			m_pendingSchedule.jsonParse(node.get("pendingSchedule"));			
			m_logger.debug("jsonParse cpuLimit\t\t\t: " + m_pendingSchedule.toString());
			
			if(m_activeSchedule == null){
				m_activeSchedule = new ProducerScheduleType();
			}
			m_activeSchedule.jsonParse(node.get("activeSchedule"));
			m_logger.debug("jsonParse cpuLimit\t\t\t: " + m_activeSchedule.toString());			
			
			 m_logger.debug("jsonParse blockNum\t\t\t: " + m_blockNum);
			 m_logger.debug("jsonParse dposProposedIrreversibleBlocknum\t\t\t: " + m_dposProposedIrreversibleBlocknum);
			 m_logger.debug("jsonParse dposIrreversibleBlocknum\t\t\t: " + m_dposIrreversibleBlocknum);
			 m_logger.debug("jsonParse bftIrreversibleBlocknum\t\t\t: " + m_bftIrreversibleBlocknum);
			 m_logger.debug("jsonParse pendingScheduleLibNum\t\t\t: " + m_pendingScheduleLibNum);
			 m_logger.debug("jsonParse hash\t\t\t: " + Hex.toHexString(m_hash));
			 m_logger.debug("jsonParse pendingScheduleHash\t\t\t: " + m_pendingScheduleHash);
			 m_logger.debug("jsonParse blockSigningKey\t\t\t: " + m_blockSigningKey);
			
		} catch (Exception e) {
			m_logger.error(e);
			throw new RuntimeException("Error on parsing Json", e);
		}		
	}

	@Override
	public String toJson() {
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
	public void jsonParse(String json) {
		try{
			ObjectMapper mapper = new ObjectMapper();
			JsonNode node = mapper.readTree(json); 
			jsonParse(node);
		}
		catch(IOException ex){
			m_logger.error(" Authority jsonParse error:" + ex.getMessage());
		}
	}
	
    @Override
    public String toString() {
        return toStringWithSuffix("\n");
    }
    
    public String toStringWithSuffix(final String suffix) {
        StringBuilder toStringBuff = new StringBuilder();
		String pendingScheduleHash = Hex.toHexString(getPendingScheduleHash());
		String hash = Hex.toHexString(m_hash);
        toStringBuff.append("  BlockHeaderState[hash=").append(hash).append(suffix);
        toStringBuff.append("  blockNum=").append(m_blockNum).append(suffix);
        toStringBuff.append("  header=").append(m_header.toFlatString()).append(suffix);
        toStringBuff.append("  dposProposedIrreversibleBlocknum=").append(m_dposProposedIrreversibleBlocknum).append(suffix);
        toStringBuff.append("  dposIrreversibleBlocknum=").append(m_dposIrreversibleBlocknum).append(suffix);
        toStringBuff.append("  bftIrreversibleBlocknum=").append(m_bftIrreversibleBlocknum).append(suffix);
        toStringBuff.append("  pendingScheduleLibNum=").append(m_pendingScheduleLibNum).append(suffix);
        toStringBuff.append("  pendingScheduleHash=").append(pendingScheduleHash).append(suffix);
        toStringBuff.append("  pendingSchedule=").append(m_pendingSchedule.toString()).append(suffix);
        toStringBuff.append("  activeSchedule=").append(m_activeSchedule.toJson()).append(suffix);
        toStringBuff.append("  blockSigningKey=").append(m_blockSigningKey).append(suffix);
        toStringBuff.append("  producerToLastProduced{").append(suffix);
        for(Map.Entry<String, Integer> key : m_producerToLastProduced.entrySet()) {
        	toStringBuff.append(key.getKey()).append("--").append(key.getValue()).append(suffix);
        }
        toStringBuff.append("  }").append(suffix);
        toStringBuff.append("  producerToLastImpliedIrb{").append(suffix);
        for(Map.Entry<String, Integer> key : m_producerToLastImpliedIrb.entrySet()) {
        	toStringBuff.append(key.getKey()).append("--").append(key.getValue()).append(suffix);
        }
        toStringBuff.append("  }").append(suffix);
        toStringBuff.append("  confirmCount{").append(suffix);
        int sizeConfirmCount = m_confirmCount.size();
        for(int i = 0; i < sizeConfirmCount; i++){
        	toStringBuff.append(m_confirmCount.get(i)).append(suffix);
        }
        toStringBuff.append("  }").append(suffix);
        toStringBuff.append("  confirmations{").append(suffix);
        int confirmationsCount = m_confirmations.size();
        for(int i = 0; i < confirmationsCount; i++){
        	toStringBuff.append(m_confirmations.get(i).toString()).append(suffix);
        }
        toStringBuff.append("  }").append(suffix);
        toStringBuff.append("   ]");
        return toStringBuff.toString();
    }

    /**
     *  Transitions the current header state into the next header state given the supplied signed block header.
     *
     *  Given a signed block header, generate the expected template based upon the header time,
     *  then validate that the provided header matches the template.
     *
     *  If the header specifies new_producers then apply them accordingly.
     */
    public BlockHeaderState next( BlockHeader header, boolean trust ){
    	m_logger.debug("next start blockHeader:" + header.toString());
    	if(header.getTimestamp() == null){
    		m_logger.debug("null");
    		return null;    	
    	}
    	if(header.getTimestamp().le(m_header.getTimestamp())){
    		m_logger.debug("block must be later in time.");
    		return null;    	
    	}
    	if(!FastByteComparisons.equal(header.getHash(), m_hash)){
    		m_logger.debug("block must link to current state.");
    		return null;    	
    	}
    	
    	BlockHeaderState result = generateNext(header.getTimestamp());
    	if(!result.getHeader().getProducer().equals(
    			header.getProducer())){
    		m_logger.debug("wrong producer specified.");
    		return null;
    	}
    	if(result.getHeader().getScheduleVersion() != header.getScheduleVersion()){
    		m_logger.debug("schedule_version in signed block is corrupted.");
    		return null;
    	} 
    	String producerName = header.getProducer();
    	if(m_producerToLastProduced.containsKey(producerName)){
        	Integer itr = m_producerToLastProduced.get(producerName);
        	if(itr >= result.getBlockNum() - header.getBlockNumber()){
        		m_logger.debug(String.format("producer {%s} double-confirming known range.", producerName));
        		return null;
        	}
    	}
    	
    	result.setConfirmed(header.getConfirmed());
    	boolean wasPendingPromote = result.maybePromotePending();
    	if(header.getNewProducers() != null){
    		if(wasPendingPromote){
        		m_logger.debug("cannot set pending producer schedule in the same block in which pending was promoted to active");
        		return null;
    		}
    		result.setNewProducers(header.getNewProducers());
    	}
    	result.getHeader().setActionRoot(header.getActionRoot());
    	result.getHeader().setMerkleTxRoot(header.getMerkleTxRoot());
    	result.getHeader().setProducerSignature(header.getProducerSignature());
    	result.setHash(result.getHeader().getHash());
    	
    	if(!trust){
    		String blockSigningKey = result.getBlockSigningKey();
    		String expectedKey = result.signee();
    		if(!blockSigningKey.equals(expectedKey)){
        		m_logger.debug(String.format("block not signed by expected key. blockSigningKey:{%s} expectedKey:{%s}"
        				, blockSigningKey, expectedKey));
        		return null;
    		}
    	}
    	m_logger.debug("next end blockHeader:" + header.toString());
    	return result;
    }
    
    public BlockHeaderState generateNext(BlockTimestamp when ){
    	BlockHeaderState result = new BlockHeaderState();  
        if( when != null ) {
        	if(when.le(m_header.getTimestamp())){
        		m_logger.debug(String.format("next block must be in the future when:{%s} headerStamp:{%s}", 
        				when.toTimePoint().toString(), m_header.getTimestamp().toTimePoint().toString()));
        		return null;
        	}
        } else {
        	when = m_header.getTimestamp();
        	when.setSlot(when.getSlot() + 1);
        }
    	result.m_header.setTimestamp(when);
    	result.m_header.setParentHash(Hex.toHexString(m_hash));
    	result.m_header.setScheduleVersion(m_activeSchedule.getVersion());
    	ProducerKey proKey = getScheduledProducer(when);
    	result.setBlockSigningKey(proKey.getBlockProducerSigningKey());
    	result.m_header.setProducer(proKey.getProducerName());
    	result.setBlockNum(m_blockNum + 1);
    	result.setProducerToLastProduced(m_producerToLastProduced);
    	result.setProducerToLastImpliedIrb(m_producerToLastImpliedIrb);
    	result.getProducerToLastProduced().put(proKey.getProducerName(), result.getBlockNum());
    	
//        result.blockroot_merkle = blockroot_merkle;
//        result.blockroot_merkle.append( id );
//        auto block_mroot = result.blockroot_merkle.get_root();

        result.setActiveSchedule(m_activeSchedule); 
        result.setPendingSchedule(m_pendingSchedule);
        result.setDposProposedIrreversibleBlocknum(m_dposProposedIrreversibleBlocknum);
        result.setBftIrreversibleBlocknum(m_bftIrreversibleBlocknum);

        result.getProducerToLastImpliedIrb().put(proKey.getProducerName(), result.getDposProposedIrreversibleBlocknum());
        result.setDposIrreversibleBlocknum(result.calcDposLastIrreversible()); 
        
        /// grow the confirmed count
        if(Short.MAX_VALUE < (DataCenter.m_config.m_maxProducers * 2 / 3)  + 1)
        {
        	m_logger.debug("8bit confirmations may not be able to hold all of the needed confirmations");
        	return null;
        }
        //static_assert(std::numeric_limits<uint8_t>::max() >= (config::max_producers * 2 / 3) + 1, "8bit confirmations may not be able to hold all of the needed confirmations");

        // This uses the previous block active_schedule because thats the "schedule" that signs and therefore confirms _this_ block
        int num_active_producers = m_activeSchedule.getProducers().size();
        int required_confs = (int)(num_active_producers * 2 / 3) + 1;

        if( m_confirmCount.size() < DataCenter.m_config.m_maximumTrackedDposConfirmations ) {
        	//List<Integer> confirmCount = new ArrayList<Integer>(m_confirmCount.size() + 1);
        	//result.setConfirmCount(confirmCount); 
        	result.setConfirmCount(m_confirmCount); 
        	//result.confirm_count.resize( m_confirmCount.size() + 1 );
        	result.getConfirmCount().add(required_confs);
        	//result.confirm_count.back() = (uint8_t)required_confs;
        } else {
        	//result.confirm_count.resize( confirm_count.size() );
        	for(int i = 1; i < m_confirmCount.size(); i++)
        	{
            	result.getConfirmCount().add(m_confirmCount.get(i));
        	}
        	//memcpy( &result.confirm_count[0], &confirm_count[1], confirm_count.size() - 1 );
        	result.getConfirmCount().add(required_confs);
        }
        return result;    	
    }
    
    public void setNewProducers(ProducerScheduleType pending){   
    	if(pending.getVersion() != m_activeSchedule.getVersion()){
    		m_logger.debug("wrong producer schedule version specified");
    		return;
    	}
    	if(m_pendingSchedule.getProducers().size() != 0){
    		m_logger.debug("cannot set new pending producers until last pending is confirmed");
    		return;
    	}
    	m_header.setNewProducers(pending);
    	m_pendingScheduleHash = Numeric.hexStringToByteArray(HashUtil.sha3(m_header.getNewProducers().toJson()));
    	m_pendingSchedule = m_header.getNewProducers();
    	m_pendingScheduleLibNum = m_blockNum;
    																						
    }
    
    public void setConfirmed(int num_prev_blocks){   
    	m_header.setConfirmed(num_prev_blocks);
    	int i = m_confirmCount.size() - 1;
    	// confirm the head block too
    	int blocks_to_confirm = num_prev_blocks + 1;
    	while(i >= 0 && blocks_to_confirm > 0){
    		m_confirmCount.set(i, m_confirmCount.get(i) - 1);
    		if(m_confirmCount.get(i) == 0){
        		int blockNumForI = m_blockNum - m_confirmCount.size() - 1 - i;
        		m_dposProposedIrreversibleBlocknum = blockNumForI;
        		if(i == m_confirmCount.size() - 1){
        			m_confirmCount.clear();
        		}else{
        			
        			//TODO
                    //memmove( &confirm_count[0], &confirm_count[i + 1], confirm_count.size() - i  - 1);
                    //confirm_count.resize( confirm_count.size() - i - 1 );
        		}
        		return;
    		}
    		--i;
    		--blocks_to_confirm;
    	}
    }
    
    public void addConfirmation(HeaderConfirmation confirmation ){   
    	m_logger.debug("addConfirmation start producer:" + confirmation.getProducer());
    	for(HeaderConfirmation confirmationHeader: m_confirmations){
    		if(confirmationHeader.getProducer().equals(confirmation.getProducer())){
    			m_logger.debug("block already confirmed by this producer");
    			return ;
    		}
    	}
    	String key = m_activeSchedule.getProducerKeyByAccount(confirmation.getProducer());
    	if(key == null){
			m_logger.debug("producer not in current schedule;" + confirmation.getProducer());
			return;    		
    	}
    	String signer = "";
    	if(!signer.equals(key)){
    		m_logger.debug("confirmation not signed by expected key");
    		return;
    	}
    	m_logger.debug("addConfirmation end producer:" + confirmation.getProducer());
    	//if(key.equals(obj))
    }
    
    public boolean maybePromotePending(){
    	if(m_pendingSchedule.getProducers().size() > 0
    			&& m_dposIrreversibleBlocknum >= m_pendingScheduleLibNum){
    		m_activeSchedule = m_pendingSchedule;
    		
    		Map<String, Integer> newProducerToLastProduced = new HashMap<String, Integer>();
    		for(ProducerKey key:m_activeSchedule.getProducers()){
    			if(m_producerToLastProduced.containsKey(key)){
    				newProducerToLastProduced.put(key.getProducerName(), m_producerToLastProduced.get(key.getProducerName()));
    			}else{
    				newProducerToLastProduced.put(key.getProducerName(), m_dposIrreversibleBlocknum);
    			}
    		}

    		Map<String, Integer> newProducerToLastImpliedIrb = new HashMap<String, Integer>();
    		for(ProducerKey key:m_activeSchedule.getProducers()){
    			if(m_producerToLastImpliedIrb.containsKey(key)){
    				newProducerToLastImpliedIrb.put(key.getProducerName(), m_producerToLastImpliedIrb.get(key.getProducerName()));
    			}else{
    				newProducerToLastImpliedIrb.put(key.getProducerName(), m_dposIrreversibleBlocknum);
    			}
    		}
    		m_producerToLastProduced.clear();
    		m_producerToLastProduced = newProducerToLastProduced;
    		m_producerToLastImpliedIrb.clear();
    		m_producerToLastImpliedIrb = newProducerToLastImpliedIrb;
    		m_producerToLastProduced.put(m_header.getProducer(), m_blockNum);
    		return true;    		
    	}
    	return false;
    }
    
    public boolean  hasPendingProducers()
    { 
    	return m_pendingSchedule.getProducers().size() > 0 ? true : false;
    }
    
    public int calcDposLastIrreversible(){
    	List<Integer> blcoknums = new ArrayList<Integer>(m_producerToLastImpliedIrb.size());
    	for(Map.Entry<String, Integer> producer:m_producerToLastImpliedIrb.entrySet()){
    		blcoknums.add(producer.getValue());
    	}
    	/// 2/3 must be greater, so if I go 1/3 into the list sorted from low to high, then 2/3 are greater
    	if(blcoknums.size() == 0){
    		return 0;
    	}
    	return blcoknums.get((blcoknums.size() - 1) / 3);
    }
    
    public boolean isActiveProducer(String accountName){
    	if(m_producerToLastProduced.containsKey(accountName)){
    		return true;
    	}
    	return false;
    }  
    
    public ProducerKey getScheduledProducer(BlockTimestamp timestamp){
    	int index = (int)(timestamp.getSlot() % m_activeSchedule.getProducers().size() * DataCenter.m_config.m_producerRepetitions);
    	index = index / DataCenter.m_config.m_producerRepetitions;
    	return m_activeSchedule.getProducers().get(index);
    }
    
    public byte[] prev()
    {     	
    	return Numeric.hexStringToByteArray(m_header.getParentHash());
    }    
    
    //TODO
    public byte[] sig_digest(){
    	//TODO
    	String hash = m_header.digest();
    	return null;
    }

    //TODO
//    public void sign( const std::function<signature_type(const digest_type&)>& signer, bool trust = false ){
//    	
//    }

    //TODO
    public String signee(){
    	return "";
    }
}
