package sonchain.blockchain.core;

import java.nio.charset.StandardCharsets;
import java.util.*;

import org.apache.commons.lang3.ArrayUtils;
import org.bouncycastle.util.encoders.Hex;

import sonchain.blockchain.util.ByteUtil;
import sonchain.blockchain.util.RLP;
import sonchain.blockchain.util.RLPElement;
import sonchain.blockchain.util.RLPItem;
import sonchain.blockchain.util.RLPList;
import sonchain.blockchain.vm.LogInfo;

/**
 * TransactionReceipt
 * @author GAIA
 *
 */
public class TransactionReceipt {

	/**
	 * Constructor
	 */
	public TransactionReceipt() {
	}

	/**
	 * Constructor
	 * @param rlp
	 */
	public TransactionReceipt(byte[] rlp) {

		RLPList params = RLP.decode2(rlp);
		RLPList receipt = (RLPList) params.get(0);

		RLPItem postTxStateRLP = (RLPItem) receipt.get(0);
		RLPList logs = (RLPList) receipt.get(1);
		RLPItem result = (RLPItem) receipt.get(2);

		m_postTxState = ArrayUtils.nullToEmpty(postTxStateRLP.getRLPData());
		m_executionResult = (m_executionResult = result.getRLPData()) == null ? 
				ByteUtil.EMPTY_BYTE_ARRAY : m_executionResult;

		if (receipt.size() > 3) {
			byte[] errBytes = receipt.get(3).getRLPData();
			m_error = errBytes != null ? new String(errBytes, StandardCharsets.UTF_8) : "";
		}

		for (RLPElement log : logs) {
			LogInfo logInfo = new LogInfo(log.getRLPData());
			m_logInfoList.add(logInfo);
		}
		m_rlpEncoded = rlp;
	}

	/**
	 * Constructor
	 * @param postTxState
	 * @param logInfoList
	 */
	public TransactionReceipt(byte[] postTxState, List<LogInfo> logInfoList) {
		m_postTxState = postTxState;
		m_logInfoList = logInfoList;
	}

	/**
	 * Constructor
	 * @param rlpList
	 */
	public TransactionReceipt(final RLPList rlpList) {
		if (rlpList == null || rlpList.size() != 2){
			throw new RuntimeException(
					"Should provide RLPList with postTxState, cumulativeGas, bloomFilter, logInfoList");
		}

		m_postTxState = rlpList.get(0).getRLPData();
		List<LogInfo> logInfos = new ArrayList<>();
		for (RLPElement logInfoEl : (RLPList) rlpList.get(1)) {
			LogInfo logInfo = new LogInfo(logInfoEl.getRLPData());
			logInfos.add(logInfo);
		}
		m_logInfoList = logInfos;
	}
	private String m_error = "";
	private byte[] m_executionResult = ByteUtil.EMPTY_BYTE_ARRAY;	
	private List<LogInfo> m_logInfoList = new ArrayList<>();
	private byte[] m_postTxState = ByteUtil.EMPTY_BYTE_ARRAY;
	private byte[] m_rlpEncoded;
	private Transaction m_transaction = null;
	
	public String getError() {
		return m_error;
	}

	public void setError(String error) {
		m_error = error == null ? "" : error;
	}

	public byte[] getExecutionResult() {
		return m_executionResult;
	}

	public void setExecutionResult(byte[] executionResult) {
		m_executionResult = executionResult;
		m_rlpEncoded = null;
	}

	public List<LogInfo> getLogInfoList() {
		return m_logInfoList;
	}
	
	public void setLogInfoList(List<LogInfo> logInfoList) {
		if (logInfoList == null){
			return;
		}
		m_logInfoList = logInfoList;
		m_rlpEncoded = null;
	}	

	public byte[] getPostTxState() {
		return m_postTxState;
	}
	
	public void setPostTxState(byte[] postTxState) {
		m_postTxState = postTxState;
		m_rlpEncoded = null;
	}
	
	public Transaction getTransaction() {
		if (m_transaction == null)
			throw new NullPointerException(
					"Transaction is not initialized. Use TransactionInfo and BlockStore to setup Transaction instance");
		return m_transaction;
	}

	public void setTransaction(Transaction transaction) {
		this.m_transaction = transaction;
	}

	public boolean isValid() {
		return true;
	}

	public boolean isSuccessful() {
		return m_error.isEmpty();
	}
	/**
	 * Used for Receipt trie hash calculation. Should contain only the following
	 * items encoded: [postTxState, bloomFilter, logInfoList]
	 */
	public byte[] getReceiptTrieEncoded() {
		return getEncoded(true);
	}
	
	public byte[] getEncoded() {
		if (m_rlpEncoded == null) {
			m_rlpEncoded = getEncoded(false);
		}
		return m_rlpEncoded;
	}
	
	public byte[] getEncoded(boolean receiptTrie) {

		byte[] postTxStateRLP = RLP.encodeElement(m_postTxState);
		final byte[] logInfoListRLP;
		if (m_logInfoList != null) {
			byte[][] logInfoListE = new byte[m_logInfoList.size()][];
			int i = 0;
			for (LogInfo logInfo : m_logInfoList) {
				logInfoListE[i] = logInfo.getEncoded();
				++i;
			}
			logInfoListRLP = RLP.encodeList(logInfoListE);
		} else {
			logInfoListRLP = RLP.encodeList();
		}

		return receiptTrie ? RLP.encodeList(postTxStateRLP, logInfoListRLP)
				: RLP.encodeList(postTxStateRLP, logInfoListRLP,
						RLP.encodeElement(m_executionResult), 
						RLP.encodeElement(m_error.getBytes(StandardCharsets.UTF_8)));

	}

	@Override
	public String toString() {
		return "TransactionReceipt[" 
				+ "\n  , postTxState=" + Hex.toHexString(m_postTxState) 
				+ "\n  , error=" + m_error
				+ "\n  , executionResult=" + Hex.toHexString(m_executionResult) 
				+ "\n  , logs=" + m_logInfoList + ']';
	}
}
