package sonchain.blockchain.core;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.log4j.Logger;
import org.bouncycastle.util.Arrays;
import org.bouncycastle.util.encoders.Hex;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import sonchain.blockchain.service.DataCenter;
import sonchain.blockchain.util.ByteUtil;
import sonchain.blockchain.util.Numeric;

/**
 * The Define of Block
 * 
 * @author GAIA
 *
 */
public class Block implements IJson{

	public static final Logger m_logger = Logger.getLogger(Block.class);
	/**
	 * Constructor
	 */
	public Block() {
	}

	/**
	 * Constructor
	 * 
	 * @param rawData
	 */
	public Block(String jsonStr) {
		jsonParse(jsonStr);
		// logger.debug("new from [" + Hex.toHexString(rawData) + "]");
		//m_rlpEncoded = rawData;
	}

	/**
	 * Constructor
	 * 
	 * @param parentHash
	 * @param producer
	 * @param number
	 * @param timestamp
	 * @param extraData
	 * @param transactionsRoot
	 * @param stateRoot
	 * @param transactionsList
	 */
	public Block(String parentHash, String producer, long number, BlockTimestamp timestamp, String extraData,
			String transactionsRoot, String stateRoot, List<TransactionReceipt> transactionsList) {
		this(parentHash, producer, number, timestamp, extraData, transactionsList);
		m_header.setMerkleTxRoot(Hex.toHexString((DataCenter.getSonChainImpl().getBlockChain().calcTxTrie(transactionsList))));
		if (!transactionsRoot.equals(m_header.getMerkleTxRoot())) {
			// logger.debug("Transaction root miss-calculate, block: {}",
			// getNumber());
		}
		m_header.setStateRoot(stateRoot);
		//m_header.setReceiptTrieRoot(receiptsRoot);
	}

	/**
	 * Constructor
	 * 
	 * @param parentHash
	 * @param producer
	 * @param number
	 * @param timestamp
	 * @param extraData
	 * @param transactionsList
	 */
	public Block(String parentHash, String producer, long number, BlockTimestamp timestamp, String extraData,
			List<TransactionReceipt> transactionsList) {
		m_header = new BlockHeader(parentHash, producer, number, timestamp, extraData);
		m_transactions = transactionsList;
		if (m_transactions == null) {
			m_transactions = new CopyOnWriteArrayList<>();
		}
	}

	/**
	 * Constructor
	 * 
	 * @param header
	 * @param transactionsList
	 */
	public Block(BlockHeader header, List<TransactionReceipt> transactionsList) {

		this(header.getParentHash(), header.getProducer(), header.getBlockNumber(), header.getTimestamp(),
				header.getExtraData(), transactionsList);
	}

	private BlockHeader m_header;

	private List<TransactionReceipt> m_transactions = new ArrayList<TransactionReceipt>();

	public int getCount() {
		return m_transactions.size();
	}

	public byte[] getHash() {
		return m_header.getHash();
	}

	public BlockHeader getHeader() {
		return m_header;
	}

	public String getProducer() {
		return m_header.getProducer();
	}

	public String getParentHash() {
		return m_header.getParentHash();
	}

	public String getExtraData() {
		return m_header.getExtraData();
	}

	public void setExtraData(String data) {
		m_header.setExtraData(data);
	}

	public long getBlockNumber() {
		return m_header.getBlockNumber();
	}

	public int getSize() {
		return 0;
	}

	public int getTransactionCount() {
		if (m_transactions == null) {
			return 0;
		}
		return m_transactions.size();
	}

	public String getTxMerkleRoot() {
		return m_header.getMerkleTxRoot();
	}
	
	public String getActionRoot() {
		return m_header.getActionRoot();
	}

	public void setActionRoot(String actionRoot) {
		m_header.setActionRoot(actionRoot);
	}

	public String getStateRoot() {
		return m_header.getStateRoot();
	}

	public void setStateRoot(String stateRoot) {
		m_header.setStateRoot(stateRoot);
	}

	public BlockTimestamp getTimestamp() {
		return m_header.getTimestamp();
	}

	public List<TransactionReceipt> getTransactionsList() {
		return m_transactions;
	}
	
	public void setTransactionsList(List<TransactionReceipt> trans) {
		m_transactions = trans;
	}

	public byte[] getEncoded() {
		String jsonStr = toJson();
		return jsonStr.getBytes(Charset.forName("UTF-8"));
	}

	public byte[] getEncodedBody() {
		String jsonStr = toBodyJson();
		return jsonStr.getBytes(Charset.forName("UTF-8"));
	}

	public String getShortHash() {
		return Hex.toHexString(getHash()).substring(0, 6);
	}

	public String getShortDescr() {
		return "#" + getBlockNumber() + " (" + Hex.toHexString(getHash()).substring(0, 6) + " <~ "
				+ getParentHash().substring(0, 6) + ") Txs:" + getTransactionsList().size();
	}

	public boolean isEqual(Block block) {
		return Arrays.areEqual(getHash(), block.getHash());
	}

	public boolean isGenesis() {
		return m_header.isGenesis();
	}

	public boolean isParentOf(Block block) {
		return Arrays.areEqual(getHash(), Numeric.hexStringToByteArray(block.getParentHash()));
	}

	/**
	 * @param parentHash
	 * @param minedBy
	 * @param number
	 * @param timestamp
	 * @param extraData
	 * @param transactionsList
	 * @return
	 */
	public static Block newBlock(String parentHash, String producer, long number, BlockTimestamp timestamp, String extraData,
			String transactionsRoot, List<TransactionReceipt> transactionsList) {
		Block block = new Block(parentHash, producer, number, timestamp, extraData, transactionsList);

		block.m_header.setMerkleTxRoot(Hex.toHexString((DataCenter.getSonChainImpl().getBlockChain().calcTxTrie(transactionsList))));
		if (!transactionsRoot.equals(block.m_header.getMerkleTxRoot())) {
			// logger.debug("Transaction root miss-calculate, block: {}",
			// getNumber());
		}
		return block;
	}

    @Override
	public String toJson(){
		try{
			ObjectMapper mapper = new ObjectMapper();
			ObjectNode blockNode = mapper.createObjectNode();
			toJson(blockNode);
			String jsonStr =  mapper.writeValueAsString (blockNode);
			m_logger.debug(" Block Json String is :" + jsonStr);
			return jsonStr;
		}
		catch(Exception ex){
			m_logger.error(" Block toJson error:" + ex.getMessage());
			return "";
		}
	}

    @Override
	public synchronized void jsonParse(String json) {
		try{
			ObjectMapper mapper = new ObjectMapper();
			JsonNode blockNode = mapper.readTree(json); 
			jsonParse(blockNode);
		}
		catch(IOException ex){
			m_logger.error(" Block jsonParse error:" + ex.getMessage());
		}
	}
	
	public String toBodyJson(){
		try{
			ObjectMapper mapper = new ObjectMapper();
			ObjectNode blockNode = mapper.createObjectNode();
	    	if(m_transactions != null){
	    		ArrayNode transNodes = blockNode.arrayNode();
	    		int size = m_transactions.size();
	    		for(int i = 0 ; i < size; i++){
	    			ObjectNode transNode = blockNode.objectNode();
	    			m_transactions.get(i).toJson(transNode);
	    			transNodes.add(transNode);
	    		}
	    		blockNode.set("transactions", transNodes);
	    	}
			String jsonStr =  mapper.writeValueAsString (blockNode);
			m_logger.debug(" Block Body Json String is :" + jsonStr);
			return jsonStr;
		}
		catch(Exception ex){
			m_logger.error(" Block Body toJson error:" + ex.getMessage());
			return "";
		}
	}

    @Override
	public void toJson(ObjectNode blockHeaderNode){
    	if(m_header != null){
    		m_header.toJson(blockHeaderNode);
    	}
    	if(m_transactions != null){
    		ArrayNode transNodes = blockHeaderNode.arrayNode();
    		int size = m_transactions.size();
    		for(int i = 0 ; i < size; i++){
    			ObjectNode transNode = blockHeaderNode.objectNode();
    			m_transactions.get(i).toJson(transNode);
    			transNodes.add(transNode);
    		}
    		blockHeaderNode.set("transactions", transNodes);
    	}
	}

    @Override
	public synchronized void jsonParse(JsonNode blockNode) {
		try {
	    	if(m_header == null){
	    		m_header = new BlockHeader();
	    	}	
    		m_header.jsonParse(blockNode);		
			if(m_transactions == null){
				m_transactions = new CopyOnWriteArrayList<>();
			}
			JsonNode trans = blockNode.get("transactions");
			for (JsonNode tran : trans) {  
				TransactionReceipt transactionReceipt = new TransactionReceipt();
				transactionReceipt.jsonParse(tran);
				m_transactions.add(transactionReceipt);
			}
			
		} catch (Exception e) {
	        m_logger.error(e);
			throw new RuntimeException("Error on parsing Json", e);
		}
	}

	/**
	 * 
	 * @return
	 */
	public String toFlatString() {
		StringBuffer toStringBuff = new StringBuffer();
		toStringBuff.setLength(0);
		toStringBuff.append("BlockData [");
		toStringBuff.append("hash=").append(ByteUtil.toHexString(getHash()));
		toStringBuff.append(m_header.toFlatString());

		for (TransactionReceipt tx : m_transactions) {
			toStringBuff.append("\n");
			toStringBuff.append(tx.toString());
		}
		toStringBuff.append("]");
		return toStringBuff.toString();
	}

	/**
	 */
	@Override
	public String toString() {
		StringBuffer toStringBuff = new StringBuffer();
		toStringBuff.setLength(0);
		toStringBuff.append(Hex.toHexString(getEncoded())).append("\n");
		toStringBuff.append("BlockData [ ");
		toStringBuff.append("hash=").append(ByteUtil.toHexString(getHash())).append("\n");
		toStringBuff.append(m_header.toString());

		if (!m_transactions.isEmpty()) {
			toStringBuff.append("Txs [\n");
			for (TransactionReceipt tx : m_transactions) {
				toStringBuff.append(tx.toString());
				toStringBuff.append("\n");
			}
			toStringBuff.append("]\n");
		} else {
			toStringBuff.append("Txs []\n");
		}
		toStringBuff.append("]");
		return toStringBuff.toString();
	}
}
