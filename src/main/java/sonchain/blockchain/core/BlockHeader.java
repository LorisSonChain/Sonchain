package sonchain.blockchain.core;

import java.io.IOException;

import org.apache.log4j.Logger;
import org.bouncycastle.util.Arrays;
import org.bouncycastle.util.encoders.Hex;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import sonchain.blockchain.crypto.HashUtil;
import sonchain.blockchain.util.ByteUtil;
import sonchain.blockchain.util.FastByteComparisons;
import sonchain.blockchain.util.Numeric;
import sonchain.blockchain.util.Utils;

/**
 * The Define of BlockHeader
 * @author GAIA
 *
 */
public class BlockHeader implements IJson {

	public static final Logger m_logger = Logger.getLogger(BlockHeader.class);
	
    public static final int NONCE_LENGTH = 8;
    public static final int HASH_LENGTH = 32;
    public static final int ADDRESS_LENGTH = 20;
    public static final int MAX_HEADER_SIZE = 800;
    public static long GENESIS_NUMBER = 0;

	/**
	 * Constructor
	 */
	public BlockHeader() {
    }

	/**
	 * Constructor
	 * 
	 * @param rawData
	 */
	public BlockHeader(String jsonStr) {
		jsonParse(jsonStr);
		// logger.debug("new from [" + Hex.toHexString(rawData) + "]");
		//m_rlpEncoded = rawData;
	}

    /**
     * Constructor
     * @param parentHash
     * @param producer
     * @param blockNumber
     * @param timestamp
     * @param extraData
     */
    public BlockHeader(String parentHash, String producer, long blockNumber, BlockTimestamp timestamp,
    		String extraData) {
    	m_parentHash = parentHash;
    	m_producer = producer;
    	m_blockNumber = blockNumber;
    	m_timestamp = timestamp;
    	m_extraData = extraData;
    	m_merkleTxRoot = "";
    	m_stateRoot = "";
    }
    
    // ADD according EOS Begin
    private int m_confirmed = 1;
    private int m_scheduleVersion = 0;
    private String m_producerSignature = "";
    private ProducerScheduleType m_newProducers = null;
    // ADD according EOS End

    private String m_actionRoot = "";
	private String m_extraData = "";	
    private long m_blockNumber = 0;    
    private String m_merkleTxRoot = "";    
    private String m_producer = "";    
    private String m_parentHash = "";     
    private String m_stateRoot = "";     
    private BlockTimestamp m_timestamp = null;    
    private int m_version = 0;
    private byte[] m_hashCache = null;

	public String getActionRoot() {
		return m_actionRoot;
	}

	public void setActionRoot(String actionRoot) {
		m_actionRoot = actionRoot;
	}

    public long getBlockNumber() {
        return m_blockNumber;
    }

    public void setBlockNumber(long blockNumber) {
    	m_blockNumber = blockNumber;
    	m_hashCache = null;  
    }
    
    public int getConfirmed() {
		return m_confirmed;
	}

	public void setConfirmed(int confirmed) {
		m_confirmed = confirmed;
    	m_hashCache = null;  
	}
    
    public String getExtraData() {
        return m_extraData;
    }

    public void setExtraData(String extraData) {
    	m_extraData = extraData;
    	m_hashCache = null;  
    }

	public String getMerkleTxRoot() {
		return m_merkleTxRoot;
	}

	public void setMerkleTxRoot(String merkleTxRoot) {
		m_merkleTxRoot = merkleTxRoot;
    	m_hashCache = null;  
	}

	public ProducerScheduleType getNewProducers() {
		return m_newProducers;
	}

	public void setNewProducers(ProducerScheduleType newProducers) {
		m_newProducers = newProducers;
    	m_hashCache = null;  
	}

    public String getParentHash() {
        return m_parentHash;
    }

	public void setParentHash(String parentHash) {
		m_parentHash = parentHash;
    	m_hashCache = null;  
	}
    
    public String getProducer() {
        return m_producer;
    }

    public void setProducer(String producer) {
    	m_producer = producer;
    	m_hashCache = null;  
    }

	public String getProducerSignature() {
		return m_producerSignature;
	}

	public void setProducerSignature(String producerSignature) {
		m_producerSignature = producerSignature;
    	m_hashCache = null;  
	}

    public String getStateRoot() {
        return m_stateRoot;
    }

    public void setStateRoot(String stateRoot) {
        this.m_stateRoot = stateRoot;
    	m_hashCache = null;  
    }

    public BlockTimestamp getTimestamp() {
        return m_timestamp;
    }

    public void setTimestamp(BlockTimestamp timestamp) {
    	m_timestamp = timestamp;
    	m_hashCache = null;  
    }

	public int getScheduleVersion() {
		return m_scheduleVersion;
	}

	public void setScheduleVersion(int scheduleVersion) {
		m_scheduleVersion = scheduleVersion;
    	m_hashCache = null;  
	}

	public int getVersion() {
		return m_version;
	}

	public void setVersion(int version) {
		m_version = version;
    	m_hashCache = null;  
	}
	
	public String digest(){
		byte[] hash = getHash();
		return Hex.toHexString(hash);
	}

	public byte[] getHash() {
		if(m_hashCache  == null){
			m_hashCache = HashUtil.sha3(getEncoded());
		}
		return m_hashCache;
    }

    public byte[] getEncoded() {
    	try
    	{
			ObjectMapper mapper = new ObjectMapper();
		    ObjectNode blockHeaderNode = mapper.createObjectNode();
			
			TimePoint point = m_timestamp.toTimePoint();
			String timestamp = point.toString();

			blockHeaderNode.put("timestamp", timestamp);			
			blockHeaderNode.put("producer", m_producer);
			blockHeaderNode.put("confirmed", m_confirmed);
			blockHeaderNode.put("parentHash", m_parentHash);	
			blockHeaderNode.put("merkleTxRoot", m_merkleTxRoot);
			blockHeaderNode.put("actionRoot", m_actionRoot);
			blockHeaderNode.put("scheduleVersion", m_scheduleVersion);		
			blockHeaderNode.put("stateRoot", m_stateRoot);	
			blockHeaderNode.put("version", m_version);			
			if(m_newProducers != null){
				m_newProducers.toJson(blockHeaderNode);
				m_logger.debug("getEncoded newProducers\t\t\t: " + m_newProducers.toString());
			}
			blockHeaderNode.put("blockNumber", m_blockNumber);	
			blockHeaderNode.put("extraData", m_extraData);	
			
	        m_logger.debug("getEncoded version\t\t\t: " + m_version);
	        m_logger.debug("getEncoded timestamp\t\t\t: " + timestamp);
	        m_logger.debug("getEncoded actionRoot\t\t\t: " + m_actionRoot);
	        m_logger.debug("getEncoded parentHash\t\t\t: " + m_parentHash);
	        m_logger.debug("getEncoded merkleTxRoot\t\t\t: " + m_merkleTxRoot);
	        m_logger.debug("getEncoded producer\t\t\t: " + m_producer);
	        m_logger.debug("getEncoded stateRoot\t\t\t: " + m_stateRoot);
	        m_logger.debug("getEncoded extraData\t\t\t: " + m_extraData);
	        m_logger.debug("getEncoded blockNumber\t\t\t: " + m_blockNumber);
	        m_logger.debug("getEncoded confirmed\t\t\t: " + m_confirmed);
	        m_logger.debug("getEncoded scheduleVersion\t\t\t: " + m_scheduleVersion);
	    	String content = mapper.writeValueAsString (blockHeaderNode);
	    	m_logger.debug(" getEncoded Content:" + content);
		    return content.getBytes();
    	}catch(JsonProcessingException ex){
    		m_logger.error("getEncoded error:" + ex.getMessage());
    		return null;
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
	public void toJson(ObjectNode blockHeaderNode){
    	String hash = Hex.toHexString(getHash());
		TimePoint point = m_timestamp.toTimePoint();
		String timestamp = point.toString();

		blockHeaderNode.put("timestamp", timestamp);	
		blockHeaderNode.put("producer", m_producer);
		blockHeaderNode.put("confirmed", m_confirmed);	
		blockHeaderNode.put("parentHash", m_parentHash);	
		blockHeaderNode.put("merkleTxRoot", m_merkleTxRoot);
		blockHeaderNode.put("actionRoot", m_actionRoot);	
		blockHeaderNode.put("scheduleVersion", m_scheduleVersion);	
		blockHeaderNode.put("producerSignature", m_producerSignature);
		blockHeaderNode.put("hash", hash);
		blockHeaderNode.put("version", m_version);				
		blockHeaderNode.put("stateRoot", m_stateRoot);		
		blockHeaderNode.put("extraData", m_extraData);		
		blockHeaderNode.put("blockNumber", m_blockNumber);	
		if(m_newProducers != null){
			m_newProducers.toJson(blockHeaderNode);
		}		
        m_logger.debug("toJson hash\t\t\t: " + hash);
        m_logger.debug("toJson version\t\t\t: " + m_version);
        m_logger.debug("toJson timestamp\t\t\t: " + timestamp);
        m_logger.debug("toJson parentHash\t\t\t: " + m_parentHash);
        m_logger.debug("toJson merkleTxRoot\t\t\t: " + m_merkleTxRoot);
        m_logger.debug("toJson actionRoot\t\t\t: " + m_actionRoot);
        m_logger.debug("toJson producer\t\t\t: " + m_producer);
        m_logger.debug("toJson stateRoot\t\t\t: " + m_stateRoot);
        m_logger.debug("toJson extraData\t\t\t: " + m_extraData);
        m_logger.debug("toJson blockNumber\t\t\t: " + m_blockNumber);
        m_logger.debug("toJson confirmed\t\t\t: " + m_confirmed);
        m_logger.debug("toJson scheduleVersion\t\t\t: " + m_scheduleVersion);
        m_logger.debug("toJson producerSignature\t\t\t: " + m_producerSignature);
		m_logger.debug("toJson newProducers\t\t\t: " + m_newProducers.toString());
	}

    @Override
	public synchronized void jsonParse(JsonNode node) {
		try {
		
			String hash = node.get("hash").asText();
			m_version = node.get("version").asInt();
			String timestamp = node.get("timestamp").asText();
			TimePoint point = TimePoint.from_iso_string(timestamp);
			m_timestamp = new BlockTimestamp(point);
			m_stateRoot = node.get("stateRoot").asText();
			m_parentHash = node.get("parentHash").asText();
			m_producer = node.get("producer").asText();
			m_merkleTxRoot = node.get("merkleTxRoot").asText();
			m_actionRoot = node.get("actionRoot").asText();	
			m_blockNumber = node.get("blockNumber").asLong();
			m_extraData = node.get("extraData").asText();
			m_producerSignature = node.get("producerSignature").asText();
			m_scheduleVersion = node.get("scheduleVersion").asInt();
			m_confirmed = node.get("confirmed").asInt();
			m_hashCache = Numeric.hexStringToByteArray(hash);			
			if(m_newProducers == null){
				m_newProducers = new ProducerScheduleType();
			}
			m_newProducers.jsonParse(node);
			m_logger.debug("jsonParse newProducers\t\t\t: " + m_newProducers.toString());

			m_logger.debug("jsonParse hash\t\t\t: " + hash);
			m_logger.debug("jsonParse version\t\t\t: " + m_version);
			m_logger.debug("jsonParse timestamp\t\t\t: " + timestamp);
			m_logger.debug("jsonParse stateRoot\t\t\t: " + m_stateRoot);
			m_logger.debug("jsonParse actionRoot\t\t\t: " + m_actionRoot);
			m_logger.debug("jsonParse producerSignature\t\t\t: " + m_producerSignature);
			m_logger.debug("jsonParse producer\t\t\t: " + m_producer);
			m_logger.debug("jsonParse merkleTxRoot\t\t\t: " + m_merkleTxRoot);
			m_logger.debug("jsonParse blockNumber\t\t\t: " + m_blockNumber);
			m_logger.debug("jsonParse extraData\t\t\t: " + m_extraData);
			m_logger.debug("jsonParse parentHash\t\t\t: " + m_parentHash);
			m_logger.debug("jsonParse scheduleVersion\t\t\t: " + m_scheduleVersion);
			m_logger.debug("jsonParse confirmed\t\t\t: " + m_confirmed);
			
		} catch (Exception e) {
	        m_logger.error(e);
			throw new RuntimeException("Error on parsing Json", e);
		}
	}

    public String getShortDescr() {
        return "#" + m_blockNumber + " (" + Hex.toHexString(getHash()).substring(0,6) + " <~ "
                + m_parentHash.substring(0,6) + ")";
    }
    
    public boolean isGenesis() {
        return m_blockNumber == GENESIS_NUMBER;
    }
    
    public String toFlatString() {
        return toStringWithSuffix("");
    }
    
    
    
    public BlockHeader(int m_confirmed, int m_scheduleVersion, String m_producerSignature,
			ProducerScheduleType m_newProducers, String m_actionRoot, String m_extraData, long m_blockNumber,
			String m_merkleTxRoot, String m_producer, String m_parentHash, String m_stateRoot,
			BlockTimestamp m_timestamp, int m_version, byte[] m_hashCache) {
		super();
		this.m_confirmed = m_confirmed;
		this.m_scheduleVersion = m_scheduleVersion;
		this.m_producerSignature = m_producerSignature;
		this.m_newProducers = m_newProducers;
		this.m_actionRoot = m_actionRoot;
		this.m_extraData = m_extraData;
		this.m_blockNumber = m_blockNumber;
		this.m_merkleTxRoot = m_merkleTxRoot;
		this.m_producer = m_producer;
		this.m_parentHash = m_parentHash;
		this.m_stateRoot = m_stateRoot;
		this.m_timestamp = m_timestamp;
		this.m_version = m_version;
		this.m_hashCache = m_hashCache;
	}

	@Override
    public String toString() {
        return toStringWithSuffix("\n");
    }
    
    private String toStringWithSuffix(final String suffix) {
        StringBuilder toStringBuff = new StringBuilder();
        toStringBuff.append("  BlockHeader[version=").append(m_version).append(suffix);
        toStringBuff.append("  hash=").append(ByteUtil.toHexString(getHash())).append(suffix);
        toStringBuff.append("  parentHash=").append(m_parentHash).append(suffix);
        toStringBuff.append("  producer=").append(m_producer).append(suffix);
        toStringBuff.append("  stateRoot=").append(m_stateRoot).append(suffix);
        toStringBuff.append("  merkleRoot=").append(m_merkleTxRoot).append(suffix);
        toStringBuff.append("  actionRoot=").append(m_actionRoot).append(suffix);
        toStringBuff.append("  blockNumber=").append(m_blockNumber).append(suffix);
		TimePoint point = m_timestamp.toTimePoint();
		String timestamp = point.toString();
        toStringBuff.append("  timestamp=").
        	append(" (").append(timestamp).append(")").append(suffix);
        toStringBuff.append("  extraData=").append(m_extraData).append(suffix);
        toStringBuff.append("  version=").append(m_version).append(suffix);
        toStringBuff.append("  confirmed=").append(m_confirmed).append(suffix);
        toStringBuff.append("  scheduleVersion=").append(m_scheduleVersion).append(suffix);
        toStringBuff.append("  producerSignature=").append(m_producerSignature).append(suffix);
        toStringBuff.append("  newProducers=").append(m_newProducers.toString());
        toStringBuff.append("   ]");
        return toStringBuff.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()){
        	return false;
        }
        BlockHeader that = (BlockHeader) o;
        return FastByteComparisons.equal(getHash(), that.getHash());
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(getHash());
    }
}
