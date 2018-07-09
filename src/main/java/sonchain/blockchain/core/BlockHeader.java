package sonchain.blockchain.core;

import java.math.BigInteger;

import org.bouncycastle.util.Arrays;
import org.bouncycastle.util.BigIntegers;
import org.bouncycastle.util.encoders.Hex;

import sonchain.blockchain.crypto.HashUtil;
import sonchain.blockchain.util.ByteUtil;
import sonchain.blockchain.util.FastByteComparisons;
import sonchain.blockchain.util.RLP;
import sonchain.blockchain.util.RLPList;
import sonchain.blockchain.util.Utils;

/**
 * The Define of BlockHeader
 *
 */
public class BlockHeader {
	
    public static final int NONCE_LENGTH = 8;
    public static final int HASH_LENGTH = 32;
    public static final int ADDRESS_LENGTH = 20;
    public static final int MAX_HEADER_SIZE = 800;
    public static long GENESIS_NUMBER = 0;

	/**
	 * Constructor
	 */
	public BlockHeader() {
    }

	/**
	 * Constructor
	 * @param encoded
	 */
    public BlockHeader(byte[] encoded) {
        this((RLPList) RLP.decode2(encoded).get(0));
    }

    /**
     * Constructor
     * @param parentHash
     * @param minedBy
     * @param number
     * @param timestamp
     * @param extraData
     */
    public BlockHeader(byte[] parentHash, byte[] minedBy,long number, long timestamp,
                       byte[] extraData) {
    	m_parentHash = parentHash;
    	m_minedBy = minedBy;
    	m_number = number;
    	m_timestamp = timestamp;
    	m_extraData = extraData;
    	m_merkleTxRoot = HashUtil.EMPTY_TRIE_HASH;
    	m_stateRoot = HashUtil.EMPTY_TRIE_HASH;
    }
    
    /**
     * 初始化
     * @param rlpHeader
     */
    public BlockHeader(RLPList rlpHeader) {
        byte[] versionBytes = rlpHeader.get(0).getRLPData();
        m_version = versionBytes == null ? 0 : (new BigInteger(1, versionBytes)).intValue();
        m_parentHash = rlpHeader.get(1).getRLPData();
        m_minedBy = rlpHeader.get(2).getRLPData();
        m_stateRoot = rlpHeader.get(3).getRLPData();
        
        m_merkleTxRoot = rlpHeader.get(4).getRLPData();
        if (m_merkleTxRoot == null)
        {
            m_merkleTxRoot = HashUtil.EMPTY_TRIE_HASH;
        }
        
        m_receiptTrieRoot = rlpHeader.get(5).getRLPData();
        if (m_receiptTrieRoot == null)
        {
        	m_receiptTrieRoot = HashUtil.EMPTY_TRIE_HASH;
        }
        byte[] nrBytes = rlpHeader.get(6).getRLPData();
        byte[] tsBytes = rlpHeader.get(7).getRLPData();
        m_number = nrBytes == null ? 0 : (new BigInteger(1, nrBytes)).longValue();
        m_timestamp = tsBytes == null ? 0 : (new BigInteger(1, tsBytes)).longValue();
        m_extraData = rlpHeader.get(8).getRLPData();
    }
    	
	private byte[] m_extraData = null;	
    private byte[] m_hashCache = null;    
    private long m_number = 0;    
    private byte[] m_merkleTxRoot = null;    
    private byte[] m_minedBy = null;    
    private byte[] m_parentHash = null;    
    private byte[] m_receiptTrieRoot = null;    
    private byte[] m_stateRoot = null;        
    private long m_timestamp = 0;    
    private int m_version = 0;

    public byte[] getExtraData() {
        return m_extraData;
    }

    public void setExtraData(byte[] extraData) {
    	m_extraData = extraData;
    	m_hashCache = null;
    }
    
    public byte[] getMinedBy() {
        return m_minedBy;
    }

    public void setMinedBy(byte[] minedBy) {
    	m_minedBy = minedBy;
        m_hashCache = null;
    }

    public long getNumber() {
        return m_number;
    }

    public void setNumber(long number) {
    	m_number = number;
    	m_hashCache = null;
    }

    public byte[] getParentHash() {
        return m_parentHash;
    }

    public byte[] getReceiptsRoot() {
        return m_receiptTrieRoot;
    }

    public void setReceiptsRoot(byte[] receiptTrieRoot) {
        m_receiptTrieRoot = receiptTrieRoot;
        m_hashCache = null;
    }

    public byte[] getStateRoot() {
        return m_stateRoot;
    }

    public void setStateRoot(byte[] stateRoot) {
        this.m_stateRoot = stateRoot;
        m_hashCache = null;
    }

    public byte[] getTxTrieRoot() {
        return m_merkleTxRoot;
    }

    public void setTxTrieRoot(byte[] merkleTxRoot) {
    	m_merkleTxRoot = merkleTxRoot;
    	m_hashCache = null;
    }

    public long getTimestamp() {
        return m_timestamp;
    }

    public void setTimestamp(long timestamp) {
    	m_timestamp = timestamp;
    	m_hashCache = null;
    }
    
    public byte[] getHash() {
        if (m_hashCache == null) {
        	m_hashCache = HashUtil.sha3(getEncoded());
        }
        return m_hashCache;
    }

    public byte[] getEncoded() {
        byte[] version = RLP.encodeBigInteger(BigInteger.valueOf(m_version));
        byte[] parentHash = RLP.encodeElement(m_parentHash);
        byte[] minedBy = RLP.encodeElement(m_minedBy);
        byte[] stateRoot = RLP.encodeElement(m_stateRoot);
        if (m_merkleTxRoot == null) 
        {
        	m_merkleTxRoot = HashUtil.EMPTY_TRIE_HASH;
        }
        byte[] merkleRoot = RLP.encodeElement(m_merkleTxRoot);
        
        if (m_receiptTrieRoot == null) 
        {
        	m_receiptTrieRoot = HashUtil.EMPTY_TRIE_HASH;
        }
        byte[] receiptTrieRoot = RLP.encodeElement(m_receiptTrieRoot);
        byte[] number = RLP.encodeBigInteger(BigInteger.valueOf(m_number));
        byte[] timestamp = RLP.encodeBigInteger(BigInteger.valueOf(m_timestamp));
        byte[] extraData = RLP.encodeElement(m_extraData);
        return RLP.encodeList(version, parentHash, minedBy, stateRoot, merkleRoot, receiptTrieRoot, 
        		number, timestamp, extraData);
    }

    public String getShortDescr() {
        return "#" + m_number + " (" + Hex.toHexString(getHash()).substring(0,6) + " <~ "
                + Hex.toHexString(m_parentHash).substring(0,6) + ")";
    }
    
    public boolean isGenesis() {
        return m_number == GENESIS_NUMBER;
    }
    
    public String toFlatString() {
        return toStringWithSuffix("");
    }
    
    public String toString() {
        return toStringWithSuffix("\n");
    }
    
    private String toStringWithSuffix(final String suffix) {
        StringBuilder toStringBuff = new StringBuilder();
        toStringBuff.append("  version=").append(m_version).append(suffix);
        toStringBuff.append("  hash=").append(ByteUtil.toHexString(getHash())).append(suffix);
        toStringBuff.append("  parentHash=").append(ByteUtil.toHexString(m_parentHash)).append(suffix);
        toStringBuff.append("  minedBy=").append(ByteUtil.toHexString(m_minedBy)).append(suffix);
        toStringBuff.append("  stateRoot=").append(ByteUtil.toHexString(m_stateRoot)).append(suffix);
        toStringBuff.append("  merkleRoot=").append(ByteUtil.toHexString(m_merkleTxRoot)).append(suffix);
        toStringBuff.append("  receiptsTrieHash=").append(ByteUtil.toHexString(m_receiptTrieRoot)).append(suffix);
        toStringBuff.append("  number=").append(m_number).append(suffix);
        toStringBuff.append("  timestamp=").append(m_timestamp).
        	append(" (").append(Utils.longToDateTime(m_timestamp)).append(")").append(suffix);
        toStringBuff.append("  extraData=").append(ByteUtil.toHexString(m_extraData)).append(suffix);
        toStringBuff.append("  version=").append(m_version).append(suffix);
        return toStringBuff.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()){
        	return false;
        }
        BlockHeader that = (BlockHeader) o;
        return FastByteComparisons.equal(getHash(), that.getHash());
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(getHash());
    }
}
