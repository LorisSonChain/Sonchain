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
	 * Init
	 * @param rawData
	 */
	public InternalTransaction(byte[] rawData) {
		super(rawData);
	}

	/**
	 * Init
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
	
	private static int bytesToInt(byte[] bytes) {
		return ArrayUtils.isEmpty(bytes) ? 0 : ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).getInt();
	}
	
	private static int decodeInt(byte[] encoded) {
		return bytesToInt(encoded);
	}
	
	private static byte[] encodeInt(int value) {
		return RLP.encodeElement(intToBytes(value));
	}
	
	private static byte[] getData(DataWord gasPrice) {
		return (gasPrice == null) ? ByteUtil.EMPTY_BYTE_ARRAY : gasPrice.getData();
	}
	
	public int getDeep() {
		rlpParse();
		return m_deep;
	}
	
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

	@Override
	public byte[] getEncodedRaw() {
		return getEncoded();
	}

	public int getIndex() {
		rlpParse();
		return m_index;
	}

	public String getNote() {
		rlpParse();
		return m_note;
	}

	public byte[] getParentHash() {
		rlpParse();
		return m_parentHash;
	}

	@Override
	public byte[] getSender() {
		rlpParse();
		return m_senderAddress;
	}
	
	public int getVersion(){
		rlpParse();
		return m_version;
	}
	
	private static byte[] intToBytes(int value) {
		return ByteBuffer.allocate(Integer.SIZE / Byte.SIZE).order(ByteOrder.LITTLE_ENDIAN).putInt(value).array();
	}

	public boolean isRejected() {
		rlpParse();
		return m_rejected;
	}

	public void reject() {
		m_rejected = true;
	}

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
