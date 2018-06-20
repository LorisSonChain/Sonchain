package sonchain.blockchain.core;

import java.math.BigInteger;

import sonchain.blockchain.util.RLP;
import sonchain.blockchain.util.RLPItem;
import sonchain.blockchain.util.RLPList;

/**
 * Contains Transaction execution info: its receipt and execution context If the
 * transaction is still in pending state the context is the hash of the parent
 * block on top of which the transaction was executed If the transaction is
 * already mined into a block the context is the containing block and the index
 * of the transaction in that block
 * 
 * @author GAIA
 *
 */
public class TransactionInfo {

	private byte[] m_blockHash = null;
	private int m_index = 0;
	private byte[] m_parentBlockHash = null;
	private TransactionReceipt m_receipt = null;

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

	/**
	 * Constructor
	 * 
	 * @param rlp
	 */
	public TransactionInfo(byte[] rlp) {
		RLPList params = RLP.decode2(rlp);
		RLPList txInfo = (RLPList) params.get(0);
		RLPList receiptRLP = (RLPList) txInfo.get(0);
		RLPItem blockHashRLP = (RLPItem) txInfo.get(1);
		RLPItem indexRLP = (RLPItem) txInfo.get(2);
		m_receipt = new TransactionReceipt(receiptRLP.getRLPData());
		m_blockHash = blockHashRLP.getRLPData();
		if (indexRLP.getRLPData() == null) {
			m_index = 0;
		} else {
			m_index = new BigInteger(1, indexRLP.getRLPData()).intValue();
		}
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

	/* [receipt, blockHash, index] */
	public byte[] getEncoded() {
		byte[] receiptRLP = m_receipt.getEncoded();
		byte[] blockHashRLP = RLP.encodeElement(m_blockHash);
		byte[] indexRLP = RLP.encodeInt(m_index);
		byte[] rlpEncoded = RLP.encodeList(receiptRLP, blockHashRLP, indexRLP);
		return rlpEncoded;
	}
	
	public boolean isPending() {
		return m_blockHash == null;
	}

	/**
	 * Set Transaction
	 * 
	 * @param tx
	 */
	public void setTransaction(Transaction tx) {
		m_receipt.setTransaction(tx);
	}
}
