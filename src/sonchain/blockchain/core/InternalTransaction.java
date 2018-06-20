package sonchain.blockchain.core;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.apache.commons.lang3.ArrayUtils;

import sonchain.blockchain.crypto.ECKey;
import sonchain.blockchain.util.ByteUtil;
import sonchain.blockchain.util.RLP;
import sonchain.blockchain.util.RLPList;
import sonchain.blockchain.vm.DataWord;

public class InternalTransaction extends Transaction {

	private int m_deep = 0;
	private int m_index;
	private String m_note = "";
	private byte[] m_parentHash = null;
	private boolean m_rejected = false;

	/**
	 * 初始化
	 * @param rawData
	 */
	public InternalTransaction(byte[] rawData) {
		super(rawData);
	}

	/**
	 * 初始化
	 * @param parentHash
	 * @param deep
	 * @param index
	 * @param nonce
	 * @param sendAddress
	 * @param receiveAddress
	 * @param value
	 * @param data
	 * @param note
	 */
	public InternalTransaction(byte[] parentHash, int deep, int index, byte[] nonce, 
			byte[] sendAddress, byte[] receiveAddress, byte[] value, byte[] data, String note) {
		super(nonce, receiveAddress, ArrayUtils.nullToEmpty(value),
				ArrayUtils.nullToEmpty(data));
		m_parentHash = parentHash;
		m_deep = deep;
		m_index = index;
		m_senderAddress = ArrayUtils.nullToEmpty(sendAddress);
		m_note = note;
		m_parsed = true;
	}

	/**
	 * 比特数组转int
	 * @param bytes
	 * @return
	 */
	private static int bytesToInt(byte[] bytes) {
		return ArrayUtils.isEmpty(bytes) ? 0 : ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).getInt();
	}

	/**
	 * 解码Int
	 * @param encoded
	 * @return
	 */
	private static int decodeInt(byte[] encoded) {
		return bytesToInt(encoded);
	}
	
	/**
	 * Int编码
	 * @param value
	 * @return
	 */
	private static byte[] encodeInt(int value) {
		return RLP.encodeElement(intToBytes(value));
	}

	/**
	 * 获取数据
	 * @param gasPrice
	 * @return
	 */
	private static byte[] getData(DataWord gasPrice) {
		return (gasPrice == null) ? ByteUtil.EMPTY_BYTE_ARRAY : gasPrice.getData();
	}

	/**
	 * 获取深度
	 * @return
	 */
	public int getDeep() {
		rlpParse();
		return m_deep;
	}
	
	/**
	 * 获取编码
	 */
	@Override
	public byte[] getEncoded() {
		if (m_rlpEncoded == null) {
			byte[] nonce = getNonce();
			boolean isEmptyNonce = ArrayUtils.isEmpty(nonce) || (ArrayUtils.getLength(nonce) == 1 && nonce[0] == 0);

			m_rlpEncoded = RLP.encodeList(RLP.encodeElement(isEmptyNonce ? null : nonce), RLP.encodeElement(m_parentHash),
					RLP.encodeElement(getSender()), RLP.encodeElement(getReceiveAddress()),
					RLP.encodeElement(getValue()), RLP.encodeElement(getData()),
					RLP.encodeString(m_note), encodeInt(m_deep), encodeInt(m_index),
					encodeInt(m_rejected ? 1 : 0));
		}
		return m_rlpEncoded;
	}

	/**
	 * 获取原始编码格式
	 */
	@Override
	public byte[] getEncodedRaw() {
		return getEncoded();
	}

	/**
	 * 获取索引
	 * @return
	 */
	public int getIndex() {
		rlpParse();
		return m_index;
	}

	/**
	 * 获取备注
	 * @return
	 */
	public String getNote() {
		rlpParse();
		return m_note;
	}

	/**
	 * 获取父节点hash值
	 * @return
	 */
	public byte[] getParentHash() {
		rlpParse();
		return m_parentHash;
	}

	/**
	 * 获取发送人
	 */
	@Override
	public byte[] getSender() {
		rlpParse();
		return m_senderAddress;
	}
	
	public int getVersion(){
		rlpParse();
		return m_version;
	}

	/**
	 * int转比特数组
	 * @param value
	 * @return
	 */
	private static byte[] intToBytes(int value) {
		return ByteBuffer.allocate(Integer.SIZE / Byte.SIZE).order(ByteOrder.LITTLE_ENDIAN).putInt(value).array();
	}

	/**
	 * 获取拒绝标志
	 * @return
	 */
	public boolean isRejected() {
		rlpParse();
		return m_rejected;
	}

	/**
	 * 拒绝
	 */
	public void reject() {
		m_rejected = true;
	}

	/**
	 * 解析RLP格式
	 */
	@Override
	public synchronized void rlpParse() {
		if (m_parsed) {
			return;
		}
		RLPList decodedTxList = RLP.decode2(m_rlpEncoded);
		RLPList transaction = (RLPList) decodedTxList.get(0);
		setNonce(transaction.get(0).getRLPData());
		m_parentHash = transaction.get(1).getRLPData();
		m_senderAddress = transaction.get(2).getRLPData();
		setReceiveAddress(transaction.get(3).getRLPData());
		setValue(transaction.get(4).getRLPData());
		setData(transaction.get(5).getRLPData());
		m_note = new String(transaction.get(6).getRLPData());
		m_deep = decodeInt(transaction.get(7).getRLPData());
		m_index = decodeInt(transaction.get(8).getRLPData());
		m_rejected = decodeInt(transaction.get(9).getRLPData()) == 1;
		m_parsed = true;
	}
	
	@Override
	public ECKey getKey() {
		throw new UnsupportedOperationException("Cannot sign internal transaction.");
	}

	@Override
	public String toString() {
		return "TransactionData [" + "  version=" + getVersion()
				+ "  parentHash=" + ByteUtil.toHexString(getParentHash()) + ", hash="
				+ ByteUtil.toHexString(getHash()) + ", nonce=" + ByteUtil.toHexString(getNonce())
				+ ", sendAddress=" + ByteUtil.toHexString(getSender()) + ", receiveAddress="
				+ ByteUtil.toHexString(getReceiveAddress()) + ", value=" + ByteUtil.toHexString(getValue()) + ", data="
				+ ByteUtil.toHexString(getData()) + ", note=" + getNote() + ", deep=" + getDeep() + ", index="
				+ getIndex() + ", rejected=" + isRejected() + "]";
	}
}
