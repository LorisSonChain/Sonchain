package sonchain.blockchain.core;

import java.io.IOException;
import java.util.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import sonchain.blockchain.vm.LogInfo;

/**
 * TransactionReceipt
 *
 */
public class TransactionReceipt extends TransactionReceiptHeader implements IJson{

	/**
	 * Constructor
	 */
	public TransactionReceipt() {
	}
	
	/**
	 * Constructor
	 * @param transaction
	 * @param logInfoList
	 */
	public TransactionReceipt(PackedTransaction transaction, List<LogInfo> logInfoList){
		m_transaction = transaction;
		m_logInfoList = logInfoList;
	}

	private List<LogInfo> m_logInfoList = new ArrayList<>();
	private PackedTransaction m_transaction = null;

	public List<LogInfo> getLogInfoList() {
		return m_logInfoList;
	}
	
	public void setLogInfoList(List<LogInfo> logInfoList) {
		if (logInfoList == null){
			return;
		}
		m_logInfoList = logInfoList;
	}	
	
	public PackedTransaction getTransaction() {
		if (m_transaction == null){
			throw new NullPointerException(
					"Transaction is not initialized. Use TransactionInfo and BlockStore to setup Transaction instance");			
		}
		return m_transaction;
	}

	public void setTransaction(PackedTransaction transaction) {
		this.m_transaction = transaction;
	}

	public boolean isValid() {
		boolean result = super.isValid();
		return result && true;
	}
	
	@Override
    public void toJson(ObjectNode transactionReceiptNode){   
		super.toJson(transactionReceiptNode);
		if(m_transaction != null){
			ObjectNode trxNode = transactionReceiptNode.objectNode();
			m_transaction.toJson(trxNode);
			transactionReceiptNode.set("trx", trxNode);
		}
		if(m_logInfoList != null){
			ArrayNode logNode = transactionReceiptNode.arrayNode(m_logInfoList.size());
			//m_transaction.toJson(trxNode);
			transactionReceiptNode.set("logs", logNode);
		}
    }

	@Override
	public synchronized void jsonParse(JsonNode transactionReceiptNode) { 
		try {
			super.jsonParse(transactionReceiptNode);
			JsonNode trxNode = transactionReceiptNode.get("trx");
			if(trxNode != null){
				if(m_transaction == null){
					m_transaction = new PackedTransaction();
				}
				m_transaction.jsonParse(trxNode);
			}
			JsonNode logNode = transactionReceiptNode.get("logs");
			if(logNode != null){
			}
		}catch (Exception e) {
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
			m_logger.debug(" TransactionReceipt Json String is :" + jsonStr);
			return jsonStr;
		}
		catch(Exception ex){
			m_logger.error(" TransactionReceipt toJson error:" + ex.getMessage());
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
			m_logger.error(" TransactionReceipt jsonParse error:" + ex.getMessage());
		}
	}
	
    @Override
    public String toString() {
        return toStringWithSuffix("\n");
    }
    
    private String toStringWithSuffix(final String suffix) {
        StringBuilder toStringBuff = new StringBuilder();
        toStringBuff.append("  TransactionReceipt[transaction=").append(m_transaction.toString()).append(suffix);
        toStringBuff.append("   ]");
        return toStringBuff.toString();
    }
}
