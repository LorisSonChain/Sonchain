package sonchain.blockchain.core;

import java.io.IOException;

import org.apache.log4j.Logger;
import org.bouncycastle.util.encoders.Hex;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import sonchain.blockchain.util.ByteUtil;
import sonchain.blockchain.util.Numeric;

public class TransactionInfo implements IJson{
	public static final Logger m_logger = Logger.getLogger(TransactionInfo.class);

	private byte[] m_blockHash = null;
	private int m_index = 0;
	private byte[] m_parentBlockHash = null;
	private TransactionReceipt m_receipt = null;

	public TransactionInfo(){
		
	}
	
	/**
	 * Constructor
	 * @param receipt
	 * @param blockHash
	 * @param index
	 */
	public TransactionInfo(TransactionReceipt receipt, byte[] blockHash, int index) {
		this.m_receipt = receipt;
		this.m_blockHash = blockHash;
		this.m_index = index;
	}

	/**
	 * Creates a pending tx info
	 */
	public TransactionInfo(TransactionReceipt receipt) {
		this.m_receipt = receipt;
	}

	public byte[] getBlockHash() {
		return m_blockHash;
	}
	
	public int getIndex() {
		return m_index;
	}
	
	public byte[] getParentBlockHash() {
		return m_parentBlockHash;
	}
	
	public void setParentBlockHash(byte[] parentBlockHash) {
		m_parentBlockHash = parentBlockHash;
	}
	
	public TransactionReceipt getReceipt() {
		return m_receipt;
	}
	
	public boolean isPending() {
		return m_blockHash == null;
	}

	/**
	 * Set Transaction
	 * 
	 * @param tx
	 */
	public void setTransaction(TransactionReceipt tx) {
		m_receipt = tx;
	}

	@Override
	public void toJson(ObjectNode node) {
    	String blockHash = Hex.toHexString(m_blockHash);		
		node.put("index", m_index);
		node.put("blockHash", blockHash);
		if(m_receipt != null){
			ObjectNode receiptNode = node.objectNode();
			m_receipt.toJson(receiptNode);
			node.set("receipt", receiptNode);
		}	
		m_logger.debug("toJson index\t\t\t: " + m_index);
		m_logger.debug("toJson blockHash\t\t\t: " + blockHash);
	}

	@Override
	public void jsonParse(JsonNode node) {
		try {		
			String blockHash = node.get("blockHash").asText();
			m_index = node.get("index").asInt();
			m_blockHash = Numeric.hexStringToByteArray(blockHash);
			
			if(m_receipt == null){
				m_receipt = new TransactionReceipt();
				m_receipt.jsonParse(node.get("receipt"));
			}
			m_logger.debug("jsonParse index\t\t\t: " + m_index);
			m_logger.debug("jsonParse blockHash\t\t\t: " + blockHash);
			
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
			m_logger.debug(" TransactionInfo Json String is :" + jsonStr);
			return jsonStr;
		}
		catch(Exception ex){
			m_logger.error(" TransactionInfo toJson error:" + ex.getMessage());
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
			m_logger.error(" TransactionInfo jsonParse error:" + ex.getMessage());
		}
	}
	
    @Override
    public String toString() {
        return toStringWithSuffix("\n");
    }
    
    private String toStringWithSuffix(final String suffix) {
        StringBuilder toStringBuff = new StringBuilder();
        toStringBuff.append("  TransactionInfo[transaction=").append(m_receipt.toString()).append(suffix);
        toStringBuff.append("  index=").append(m_index).append(suffix);
        toStringBuff.append("  blockHash=").append(ByteUtil.toHexString(m_blockHash)).append(suffix);
        toStringBuff.append("  parentBlockHash=").append(ByteUtil.toHexString(m_parentBlockHash)).append(suffix);
        toStringBuff.append("   ]");
        return toStringBuff.toString();
    }
}
