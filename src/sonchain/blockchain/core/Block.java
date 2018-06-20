package sonchain.blockchain.core;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.bouncycastle.util.Arrays;
import org.bouncycastle.util.encoders.Hex;

import sonchain.blockchain.service.DataCenter;
import sonchain.blockchain.trie.Trie;
import sonchain.blockchain.trie.TrieImpl;
import sonchain.blockchain.util.ByteUtil;
import sonchain.blockchain.util.RLP;
import sonchain.blockchain.util.RLPElement;
import sonchain.blockchain.util.RLPList;
import lord.common.json.EasyJSONDataUtil;

/**
 * The Define of Block
 * 
 * @author GAIA
 *
 */
public class Block {

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
	public Block(byte[] rawData) {
		// logger.debug("new from [" + Hex.toHexString(rawData) + "]");
		m_rlpEncoded = rawData;
	}

	/**
	 * Constructor
	 * 
	 * @param parentHash
	 * @param minedBy
	 * @param number
	 * @param timestamp
	 * @param extraData
	 * @param receiptsRoot
	 * @param transactionsRoot
	 * @param stateRoot
	 * @param transactionsList
	 */
	public Block(byte[] parentHash, byte[] minedBy, long number, long timestamp, byte[] extraData, byte[] receiptsRoot,
			byte[] transactionsRoot, byte[] stateRoot, List<Transaction> transactionsList) {
		this(parentHash, minedBy, number, timestamp, extraData, transactionsList);
		m_header.setTxTrieRoot((DataCenter.getSonChainImpl().getBlockChain().calcTxTrie(transactionsList)));
		if (!Hex.toHexString(transactionsRoot).equals(Hex.toHexString(m_header.getTxTrieRoot()))) {
			// logger.debug("Transaction root miss-calculate, block: {}",
			// getNumber());
		}
		m_header.setStateRoot(stateRoot);
		m_header.setReceiptsRoot(receiptsRoot);
	}

	/**
	 * Constructor
	 * 
	 * @param parentHash
	 * @param minedBy
	 * @param number
	 * @param timestamp
	 * @param extraData
	 * @param transactionsList
	 */
	public Block(byte[] parentHash, byte[] minedBy, long number, long timestamp, byte[] extraData,
			List<Transaction> transactionsList) {
		m_header = new BlockHeader(parentHash, minedBy, number, timestamp, extraData);
		m_transactions = transactionsList;
		if (m_transactions == null) {
			m_transactions = new CopyOnWriteArrayList<>();
		}
		m_parsed = true;
	}

	/**
	 * Constructor
	 * 
	 * @param header
	 * @param transactionsList
	 */
	public Block(BlockHeader header, List<Transaction> transactionsList) {

		this(header.getParentHash(), header.getMinedBy(), header.getNumber(), header.getTimestamp(),
				header.getExtraData(), transactionsList);
	}

	private BlockHeader m_header;

	private boolean m_parsed = false;

	private byte[] m_rlpEncoded = null;

	private List<Transaction> m_transactions = new ArrayList<Transaction>();

	public int getCount() {
		return m_transactions.size();
	}

	public byte[] getHash() {
		parseRLP();
		return m_header.getHash();
	}

	public BlockHeader getHeader() {
		parseRLP();
		return m_header;
	}

	public byte[] getMinedBy() {
		parseRLP();
		return m_header.getMinedBy();
	}

	public byte[] getParentHash() {
		parseRLP();
		return m_header.getParentHash();
	}

	public byte[] getExtraData() {
		parseRLP();
		return m_header.getExtraData();
	}

	public void setExtraData(byte[] data) {
		m_header.setExtraData(data);
		m_rlpEncoded = null;
	}

	public long getNumber() {
		parseRLP();
		return m_header.getNumber();
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

	public byte[] getTxMerkleRoot() {
		parseRLP();
		return m_header.getTxTrieRoot();
	}

	public byte[] getStateRoot() {
		parseRLP();
		return m_header.getStateRoot();
	}

	public void setStateRoot(byte[] stateRoot) {
		parseRLP();
		m_header.setStateRoot(stateRoot);
		m_rlpEncoded = null;
	}

	public byte[] getReceiptsRoot() {
		parseRLP();
		return m_header.getReceiptsRoot();
	}

	public long getTimestamp() {
		parseRLP();
		return m_header.getTimestamp();
	}

	public List<Transaction> getTransactionsList() {
		parseRLP();
		return m_transactions;
	}
	
	public void setTransactionsList(List<Transaction> trans) {
		m_transactions = trans;
		m_rlpEncoded = null;
	}

	private List<byte[]> getBodyElements() {
		parseRLP();
		byte[] transactions = getTransactionsEncoded();
		List<byte[]> body = new ArrayList<>();
		body.add(transactions);
		return body;
	}

	public byte[] getEncoded() {
		if (m_rlpEncoded == null) {
			byte[] header = m_header.getEncoded();
			List<byte[]> block = getBodyElements();
			block.add(0, header);
			byte[][] elements = block.toArray(new byte[block.size()][]);
			m_rlpEncoded = RLP.encodeList(elements);
		}
		return m_rlpEncoded;
	}

	public byte[] getEncodedBody() {
		List<byte[]> body = getBodyElements();
		byte[][] elements = body.toArray(new byte[body.size()][]);
		return RLP.encodeList(elements);
	}

	private byte[] getTransactionsEncoded() {

		byte[][] transactionsEncoded = new byte[m_transactions.size()][];
		int i = 0;
		for (Transaction tx : m_transactions) {
			transactionsEncoded[i] = tx.getEncoded();
			++i;
		}
		return RLP.encodeList(transactionsEncoded);
	}

	public String getShortHash() {
		parseRLP();
		return Hex.toHexString(getHash()).substring(0, 6);
	}

	public String getShortDescr() {
		return "#" + getNumber() + " (" + Hex.toHexString(getHash()).substring(0, 6) + " <~ "
				+ Hex.toHexString(getParentHash()).substring(0, 6) + ") Txs:" + getTransactionsList().size();
	}

	public boolean isEqual(Block block) {
		return Arrays.areEqual(getHash(), block.getHash());
	}

	public boolean isGenesis() {
		return m_header.isGenesis();
	}

	public boolean isParentOf(Block block) {
		return Arrays.areEqual(getHash(), block.getParentHash());
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
	public static Block newBlock(byte[] parentHash, byte[] minedBy, long number, long timestamp, byte[] extraData,
			byte[] transactionsRoot, List<Transaction> transactionsList) {
		Block block = new Block(parentHash, minedBy, number, timestamp, extraData, transactionsList);

		block.m_header.setTxTrieRoot((DataCenter.getSonChainImpl().getBlockChain().calcTxTrie(transactionsList)));
		if (!Hex.toHexString(transactionsRoot).equals(Hex.toHexString(block.m_header.getTxTrieRoot()))) {
			// logger.debug("Transaction root miss-calculate, block: {}",
			// getNumber());
		}
		return block;
	}

	/**
	 * 创建创世区块
	 *
	 * @return
	 */
	public static Block newGenesisBlock(BigInteger targetValue) {
		return null;
		// return Block.NewBlock(null, null, Instant.now().getEpochSecond() ,
		// BigInteger.ZERO, BigInteger.ZERO, null);
	}

	/**
	 * 分析RLP编码格式
	 */
	private synchronized void parseRLP() {
		if (m_parsed) {
			return;
		}
		RLPList params = RLP.decode2(m_rlpEncoded);
		RLPList block = (RLPList) params.get(0);
		RLPList header = (RLPList) block.get(0);
		m_header = new BlockHeader(header);

		RLPList txTransactions = (RLPList) block.get(1);
		parseTxs(m_header.getTxTrieRoot(), txTransactions, false);
		m_parsed = true;
	}

	/**
	 * 分析交易信息并返回交易根的Hash
	 * 
	 * @param txTransactions
	 * @param validate
	 * @return
	 */
	private byte[] parseTxs(RLPList txTransactions, boolean validate) {

		Trie<byte[]> txsState = new TrieImpl();
		for (int i = 0; i < txTransactions.size(); i++) {
			RLPElement transactionRaw = txTransactions.get(i);
			Transaction tx = new Transaction(transactionRaw.getRLPData());
			if (validate) {
				tx.verify();
			}
			m_transactions.add(tx);
			txsState.put(RLP.encodeInt(i), transactionRaw.getRLPData());
		}
		return txsState.getRootHash();
	}

	/**
	 * 分析交易信息并返回交易根的Hash
	 * 
	 * @param expectedRoot
	 * @param txTransactions
	 * @param validate
	 * @return
	 */
	private boolean parseTxs(byte[] expectedRoot, RLPList txTransactions, boolean validate) {

		byte[] rootHash = parseTxs(txTransactions, validate);
		String calculatedRoot = Hex.toHexString(rootHash);
		if (!calculatedRoot.equals(Hex.toHexString(expectedRoot))) {
			// logger.debug("Transactions trie root validation failed for block
			// #{}", this.header.getNumber());
			return false;
		}

		return true;
	}

	/**
	 * 转换成字符串
	 * 
	 * @return
	 */
	public String toFlatString() {
		parseRLP();
		StringBuffer toStringBuff = new StringBuffer();
		toStringBuff.setLength(0);
		toStringBuff.append("BlockData [");
		toStringBuff.append("hash=").append(ByteUtil.toHexString(getHash()));
		toStringBuff.append(m_header.toFlatString());

		for (Transaction tx : m_transactions) {
			toStringBuff.append("\n");
			toStringBuff.append(tx.toString());
		}
		toStringBuff.append("]");
		return toStringBuff.toString();
	}

	/**
	 * 数据类转Json
	 * 
	 * @return
	 */
	public String toJson() {
		return EasyJSONDataUtil.ConvertObjectToJSON(this);
	}

	/**
	 * 转换成字符串
	 */
	@Override
	public String toString() {
		parseRLP();
		StringBuffer toStringBuff = new StringBuffer();
		toStringBuff.setLength(0);
		toStringBuff.append(Hex.toHexString(getEncoded())).append("\n");
		toStringBuff.append("BlockData [ ");
		toStringBuff.append("hash=").append(ByteUtil.toHexString(getHash())).append("\n");
		toStringBuff.append(m_header.toString());

		if (!m_transactions.isEmpty()) {
			toStringBuff.append("Txs [\n");
			for (Transaction tx : m_transactions) {
				toStringBuff.append(tx);
				toStringBuff.append("\n");
			}
			toStringBuff.append("]\n");
		} else {
			toStringBuff.append("Txs []\n");
		}
		toStringBuff.append("]");
		return toStringBuff.toString();
	}

	/**
	 * 生成器
	 * 
	 * @author GAIA
	 *
	 */
	public static class Builder {

		private byte[] m_body = null;
		private BlockHeader m_header = null;

		public Block create() {
			if (m_header == null || m_body == null) {
				return null;
			}
			Block block = new Block();
			block.m_header = m_header;
			block.m_parsed = true;
			RLPList items = (RLPList) RLP.decode2(m_body).get(0);
			RLPList transactions = (RLPList) items.get(0);
			if (!block.parseTxs(m_header.getTxTrieRoot(), transactions, false)) {
				return null;
			}
			return block;
		}

		/**
		 * 
		 * @param header
		 * @return
		 */
		public Builder withHeader(BlockHeader header) {
			m_header = header;
			return this;
		}

		public Builder withBody(byte[] body) {
			m_body = body;
			return this;
		}
	}

	/**
	 * Json转数据类
	 * 
	 * @param json
	 * @return
	 */
	public static Block toData(String json) {
		try {
			return (Block) EasyJSONDataUtil.ConvertJSONToObject(json, Block.class);
		} catch (Exception ex) {
			return null;
		}
	}
}
