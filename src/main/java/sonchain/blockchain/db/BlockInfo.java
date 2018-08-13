package sonchain.blockchain.db;

import java.io.IOException;

import org.apache.log4j.Logger;
import org.bouncycastle.util.encoders.Hex;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import sonchain.blockchain.core.IJson;
import sonchain.blockchain.util.ByteUtil;

/**
 *
 */
public class BlockInfo implements IJson {
	public static final Logger m_logger = Logger.getLogger(BlockInfo.class);
	
    private byte[] m_hash = null;
    private boolean m_mainChain = true;

    public byte[] getHash() {
        return m_hash;
    }

    public void setHash(byte[] hash) {
        this.m_hash = hash;
    }

    public boolean isMainChain() {
        return m_mainChain;
    }

    public void setMainChain(boolean mainChain) {
        this.m_mainChain = mainChain;
    }

    @Override
	public String toJson(){
		try{
			ObjectMapper mapper = new ObjectMapper();
			ObjectNode blockHeaderNode = mapper.createObjectNode();
			toJson(blockHeaderNode);
			String jsonStr =  mapper.writeValueAsString (blockHeaderNode);
			m_logger.debug(" BlockInfo Json String is :" + jsonStr);
			return jsonStr;
		}
		catch(Exception ex){
			m_logger.error(" BlockInfo toJson error:" + ex.getMessage());
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
			m_logger.error(" BlockInfo jsonParse error:" + ex.getMessage());
		}
	}

	@Override
	public void toJson(ObjectNode node) {
    	String hash = Hex.toHexString(m_hash);
    	node.put("hash", hash);
    	node.put("mainChain", m_mainChain);
    	
        m_logger.debug("toJson hash\t\t\t: " + hash);
        m_logger.debug("toJson mainChain\t\t\t: " + m_mainChain);
		
	}

	@Override
	public void jsonParse(JsonNode node) {
		try {
		
			String hash = node.get("hash").asText();
			m_mainChain = node.get("mainChain").asBoolean();

			m_logger.debug("jsonParse hash\t\t\t: " + hash);
			m_logger.debug("jsonParse mainChain\t\t\t: " + m_mainChain);
			
		} catch (Exception e) {
	        m_logger.error(e);
			throw new RuntimeException("Error on parsing Json", e);
		}
		
	}
    @Override
    public String toString() {
        return toStringWithSuffix("\n");
    }
    
    private String toStringWithSuffix(final String suffix) {
        StringBuilder toStringBuff = new StringBuilder();
        toStringBuff.append("  BlockInfo[mainChain=").append(m_mainChain).append(suffix);
        toStringBuff.append("  hash=").append(ByteUtil.toHexString(getHash())).append(suffix);
        toStringBuff.append("   ]");
        return toStringBuff.toString();
    }
}