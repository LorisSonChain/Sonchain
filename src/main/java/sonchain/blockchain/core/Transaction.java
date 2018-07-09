package sonchain.blockchain.core;

import java.math.BigInteger;
import java.security.SignatureException;
import java.util.Arrays;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.log4j.Logger;
import org.bouncycastle.util.BigIntegers;
import org.bouncycastle.util.encoders.Hex;

import sonchain.blockchain.crypto.ECKey;
import sonchain.blockchain.crypto.ECKey.ECDSASignature;
import sonchain.blockchain.crypto.ECKey.MissingPrivateKeyException;
import sonchain.blockchain.crypto.HashUtil;
import sonchain.blockchain.util.ByteUtil;
import sonchain.blockchain.util.RLP;
import sonchain.blockchain.util.RLPElement;
import sonchain.blockchain.util.RLPItem;
import sonchain.blockchain.util.RLPList;


/**
 * Transaction
 *
 */
public class Transaction{

	public static final Logger m_logger = Logger.getLogger(Transaction.class);
	public static final int HASH_LENGTH = 32;
	public static final int ADDRESS_LENGTH = 20;
    private static final int LOWER_REAL_V = 27;
    
	/**
	 * Constructor
	 * 
	 */
	public Transaction() {
		m_parsed = false;
	}

	/**
	 * Constructor
	 * 
	 * @param rawData
	 */
	public Transaction(byte[] rawData) {
		m_rlpEncoded = rawData;
		m_parsed = false;
	}

	/**
	 * Constructor
	 * 
	 * @param nonce
	 * @param receiveAddress
	 * @param value
	 * @param data
	 */
	public Transaction(byte[] nonce, byte[] receiveAddress, byte[] value, byte[] data) {
		m_nonce = nonce;
		m_receiveAddress = receiveAddress;
		if (ByteUtil.isSingleZero(value)) {
			m_value = ByteUtil.EMPTY_BYTE_ARRAY;
		} else {
			m_value = value;
		}
		m_inputData = data;
		if (receiveAddress == null) {
			m_receiveAddress = ByteUtil.EMPTY_BYTE_ARRAY;
		}
		m_parsed = true;
	}

	/**
	 * Constructor
	 * 
	 * @param nonce
	 * @param receiveAddress
	 * @param value
	 * @param data
	 * @param r
	 * @param s
	 * @param v
	 */
	public Transaction(byte[] nonce, byte[] receiveAddress, byte[] value, byte[] data,
			byte[] r, byte[] s, byte v) {
		this(nonce, receiveAddress, value, data);
		this.m_signature = ECDSASignature.fromComponents(r, s, v);
	}
	
	private long m_blockHeight = 0;
	private byte[] m_hash;
	private byte[] m_inputData;
	private byte[] m_nonce;
	protected boolean m_parsed = false;
	private String m_privateNote = "";
	private byte[] m_receiveAddress;
	private String m_remark = "";
	protected byte[] m_senderAddress;
    private ECDSASignature m_signature;
	private long m_timeStamp = 0;
	//private TransactionReceiptStatus m_txReceiptStatus = TransactionReceiptStatus.None;
	//public TransactionType m_type = TransactionType.NullTransaction;
	private byte[] m_value;
	protected int m_version = 0;
	protected byte[] m_rlpEncoded;
	private byte[] m_rlpRaw;
	
    public byte[] getData() {
    	rlpParse();
        return m_inputData;
    }
    
    protected void setData(byte[] data) {
        m_inputData = data;
        m_parsed = true;
    }
    
    public byte[] getHash()	 
    {
        if (!ArrayUtils.isEmpty(m_hash)) 
    	{
        	return m_hash;
    	}
        rlpParse();
        byte[] plainMsg = getEncoded();
        byte[] hash = HashUtil.sha3(plainMsg);
        m_logger.debug("getHash hash:" + Hex.toHexString(hash));
        return hash;
    }
    
    public byte[] getNonce() {
    	rlpParse();
        return m_nonce == null ? ByteUtil.ZERO_BYTE_ARRAY : m_nonce;
    }

    protected void setNonce(byte[] nonce) {
        m_nonce = nonce;
        m_parsed = true;
    }
    
    public boolean isParsed() {
        return m_parsed;
    }
    
    public boolean isValueTx() {
    	rlpParse();
        return m_value != null;
    }
    
    public byte[] getRawHash() {
    	rlpParse();
        byte[] plainMsg = getEncodedRaw();
        byte[] rawHash = HashUtil.sha3(plainMsg);
        m_logger.debug("getRawHash rawHash:" + Hex.toHexString(rawHash));
        return rawHash;
    }
    
    public byte[] getReceiveAddress() {
    	rlpParse();
        return m_receiveAddress;
    }
    
    protected void setReceiveAddress(byte[] receiveAddress) {
        m_receiveAddress = receiveAddress;
        m_parsed = true;
    }
    
    private byte getRealV(BigInteger bv) {
        if (bv.bitLength() > 31)
    	{
        	return 0; // chainId is limited to 31 bits, longer are not valid for now
    	}
        long v = bv.longValue();
        if (v == LOWER_REAL_V || v == (LOWER_REAL_V + 1)) 
    	{
    		return (byte) v;
    	}
        byte realV = LOWER_REAL_V;
        int inc = 0;
        if ((int) v % 2 == 0) {
        	inc = 1;
        }
        return (byte) (realV + inc);
    }
    
    public synchronized byte[] getSender() {
        try {
            if (m_senderAddress == null) {
            	m_senderAddress = ECKey.signatureToAddress(getRawHash(), getSignature());
            }
            return m_senderAddress;
        } catch (SignatureException e) {
        	m_logger.error(e);
            //logger.error(e.getMessage(), e);
        }
        return null;
    }

    public ECDSASignature getSignature() {
        rlpParse();
        return m_signature;
    }
    
    public byte[] getValue() {
    	rlpParse();
        return m_value == null ? ByteUtil.ZERO_BYTE_ARRAY : m_value;
    }
    
    protected void setValue(byte[] value) {
        this.m_value = value;
        m_parsed = true;
    }
    
	public synchronized void rlpParse() {
		if (m_parsed)
		{
			return;
		}
		try {
			RLPList decodedTxList = RLP.decode2(m_rlpEncoded);
			RLPList transaction = (RLPList) decodedTxList.get(0);
			if (transaction.size() > 8)
			{
				m_logger.error("Too many RLP elements");
				throw new RuntimeException("Too many RLP elements");
			}
			for (RLPElement rlpElement : transaction) 
			{
				if (!(rlpElement instanceof RLPItem))
				{
			        m_logger.error("Transaction RLP elements shouldn't be lists");
					throw new RuntimeException("Transaction RLP elements shouldn't be lists");
				}
			}
			byte[] versionBytes = transaction.get(0).getRLPData();
	        m_version = versionBytes == null ? 0 : (new BigInteger(1, versionBytes)).intValue();
			m_nonce = transaction.get(1).getRLPData();
			m_receiveAddress = transaction.get(2).getRLPData();
			m_value = transaction.get(3).getRLPData();
			m_inputData = transaction.get(4).getRLPData();
			// only parse signature in case tx is signed
			if (transaction.get(5).getRLPData() != null) {
				byte[] vData = transaction.get(5).getRLPData();
				BigInteger v = ByteUtil.bytesToBigInteger(vData);
				// this.chainId = extractChainIdFromV(v);
				byte[] r = transaction.get(6).getRLPData();
				byte[] s = transaction.get(7).getRLPData();
				m_signature = ECDSASignature.fromComponents(r, s, getRealV(v));
			} else {
				m_logger.error("RLP encoded tx is not signed!");
			}
			m_parsed = true;
			m_hash = getHash();
		} catch (Exception e) {
	        m_logger.error(e);
			throw new RuntimeException("Error on parsing RLP", e);
		}
	}

    private boolean validate() {
        if (getNonce().length > HASH_LENGTH) 
        {
			m_logger.error("Nonce is not valid!");
			return false;
        }
        if (m_receiveAddress != null 
        		&& m_receiveAddress.length != 0 
        		&& m_receiveAddress.length != ADDRESS_LENGTH)
        {
			m_logger.error("Receive address is not valid!");
			return false;
        }
        if (m_value != null  && m_value.length > HASH_LENGTH)
        {
			m_logger.error("Value is not valid!");
			return false;
        }
        if (getSignature() != null) {
            if (BigIntegers.asUnsignedByteArray(m_signature.r).length > HASH_LENGTH)
            {
    			m_logger.error("Signature R is not valid!");
    			return false;
            }
            if (BigIntegers.asUnsignedByteArray(m_signature.s).length > HASH_LENGTH)
            {
    			m_logger.error("Signature S is not valid!");
    			return false;
            }
            if (getSender() != null && getSender().length != ADDRESS_LENGTH)
            {
    			m_logger.error("Sender is not valid!");
    			return false;
            }
        }
        return true;
    }
    
	public synchronized boolean verify() {
		m_logger.error("Transaction verify:  TransactionInfo:" + toString());
		rlpParse();
		return validate();
	}
    
    public long nonZeroDataBytes() {
        if (m_inputData == null) 
        {
        	return 0;
        }
        int counter = 0;
        for (final byte aData : m_inputData) {
            if (aData != 0)
            {
            	++counter;
            }
        }
        return counter;
    }

    public long zeroDataBytes() {
        if (m_inputData == null)
        {
        	return 0;
        }
        int counter = 0;
        for (final byte aData : m_inputData) {
            if (aData == 0)
            {
            	++counter;
            }
        }
        return counter;
    }

    public byte[] getContractAddress() {
        if (!isContractCreation()) 
        {
        	return null;
        }
        return HashUtil.calcNewAddr(getSender(), getNonce());
    }
    
    public boolean isContractCreation() {
    	rlpParse();
        return m_receiveAddress == null || Arrays.equals(m_receiveAddress, ByteUtil.EMPTY_BYTE_ARRAY);
    }

    /**
     * @deprecated should prefer #sign(ECKey) over this method
     */
    public void sign(byte[] privKeyBytes) throws MissingPrivateKeyException {
        sign(ECKey.fromPrivate(privKeyBytes));
    }
    
    public void sign(ECKey key) throws MissingPrivateKeyException {
        m_signature = key.sign(getRawHash());
        m_logger.debug("getRawHash sign signature:" + m_signature.toHex());
        m_rlpEncoded = null;
    }
    
    @Override
    public String toString() {
        return toString(Integer.MAX_VALUE);
    }
    
    public String toString(int maxDataSize) {
    	rlpParse();
        String dataS = "";
        if (m_inputData == null) {
            dataS = "";
        } else if (m_inputData.length < maxDataSize) {
            dataS = ByteUtil.toHexString(m_inputData);
        } else {
            dataS = ByteUtil.toHexString(Arrays.copyOfRange(m_inputData, 0, maxDataSize)) +
                    "... (" + m_inputData.length + " bytes)";
        }
        String result = "TransactionData [" + " version=" + m_version
        		+", hash=" + ByteUtil.toHexString(m_hash) +
                ", nonce=" + ByteUtil.toHexString(m_nonce) +
                ", receiveAddress=" + ByteUtil.toHexString(m_receiveAddress) +
                ", sendAddress=" + ByteUtil.toHexString(getSender()) +
                ", value=" + ByteUtil.toHexString(m_value) +
                ", data=" + dataS +
                ", signatureV=" + (m_signature == null ? "" : m_signature.v) +
                ", signatureR=" + (m_signature == null ? "" : 
                	ByteUtil.toHexString(BigIntegers.asUnsignedByteArray(m_signature.r))) +
                ", signatureS=" + (m_signature == null ? "" : 
                	ByteUtil.toHexString(BigIntegers.asUnsignedByteArray(m_signature.s))) +
                "]";
        m_logger.debug("toString result:" + result);
        return result;
    }
    
    public byte[] getEncodedRaw() {
    	rlpParse();
        if (m_rlpRaw != null) 
        {
        	return m_rlpRaw;
        }
        byte[] version = RLP.encodeInt(m_version);
        // parse null as 0 for nonce
        byte[] nonce = null;
        if (m_nonce == null || m_nonce.length == 1 && m_nonce[0] == 0) {
            nonce = RLP.encodeElement(null);
        } else {
            nonce = RLP.encodeElement(m_nonce);
        }
        byte[] receiveAddress = RLP.encodeElement(m_receiveAddress);
        byte[] value = RLP.encodeElement(m_value);
        byte[] data = RLP.encodeElement(m_inputData);

    	m_rlpRaw = RLP.encodeList(version, nonce, receiveAddress, value, data);
        return m_rlpRaw;
    }
    
    public byte[] getEncoded() {

        if (m_rlpEncoded != null) 
        {
        	return m_rlpEncoded;
        }
        byte[] version = RLP.encodeBigInteger(BigInteger.valueOf(m_version));
        byte[] nonce = null;
        if (m_nonce == null || m_nonce.length == 1 && m_nonce[0] == 0)
        {
            nonce = RLP.encodeElement(null);
        } 
        else
        {
            nonce = RLP.encodeElement(m_nonce);
        }
        byte[] receiveAddress = RLP.encodeElement(m_receiveAddress);
        byte[] value = RLP.encodeElement(m_value);
        byte[] data = RLP.encodeElement(m_inputData);
        byte[] v = null;
        byte[] r = null;
        byte[] s = null;
        if (m_signature != null) {
            int encodeV = m_signature.v;
            v = RLP.encodeInt(encodeV);
            r = RLP.encodeElement(BigIntegers.asUnsignedByteArray(m_signature.r));
            s = RLP.encodeElement(BigIntegers.asUnsignedByteArray(m_signature.s));
        } else {
            v = RLP.encodeElement(ByteUtil.EMPTY_BYTE_ARRAY);
            r = RLP.encodeElement(ByteUtil.EMPTY_BYTE_ARRAY);
            s = RLP.encodeElement(ByteUtil.EMPTY_BYTE_ARRAY);
        }
        m_logger.debug("getRawHash v\t\t\t: " + Hex.toHexString(v));
        m_logger.debug("getRawHash r\t\t\t: " + Hex.toHexString(r));
        m_logger.debug("getRawHash s\t\t\t: " + Hex.toHexString(s));
        m_rlpEncoded = RLP.encodeList(version, nonce, receiveAddress, value, data, v, r, s);
        m_logger.debug("getRawHash m_rlpEncoded\t\t\t: " + Hex.toHexString(m_rlpEncoded));
        m_hash = getHash();
        m_logger.debug("getRawHash getEncoded m_hash:" + Hex.toHexString(m_hash));
        return m_rlpEncoded;
    }
    
    public ECKey getKey() {
        byte[] hash = getRawHash();
        //return ECKey.recoverFromSignature(m_signature.v, m_signature, hash);
        try {
			return ECKey.signatureToKey(hash, m_signature);
		} catch (SignatureException e) {
			// TODO Auto-generated catch block
			m_logger.error(e);
			e.printStackTrace();
			return null;
		}
    }
    
    @Override
    public int hashCode() {
        byte[] hash = getHash();
        int hashCode = 0;

        for (int i = 0; i < hash.length; ++i) {
            hashCode += hash[i] * i;
        }
        return hashCode;
    }
    
    @Override
    public boolean equals(Object obj) {

        if (!(obj instanceof Transaction)) {
        	return false;
        }
        Transaction tx = (Transaction) obj;
        return tx.hashCode() == this.hashCode();
    }
    
    public static Transaction createDefault(String to, BigInteger amount, BigInteger nonce, Integer chainId){
        return create(to, amount, nonce, chainId);
    }
    
    public static Transaction create(String to, BigInteger amount, BigInteger nonce, Integer chainId){
        return new Transaction(BigIntegers.asUnsignedByteArray(nonce),
                Hex.decode(to),
                BigIntegers.asUnsignedByteArray(amount),
                null);
    }
}
