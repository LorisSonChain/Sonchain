package sonchain.blockchain.data;

import java.math.BigInteger;

import org.apache.log4j.Logger;

import sonchain.blockchain.core.BlockIdentifier;
import sonchain.blockchain.net.SonMessageCodes;
import sonchain.blockchain.util.ByteUtil;
import sonchain.blockchain.util.RLP;
import sonchain.blockchain.util.RLPList;

public class GetBlockHeadersMessage  extends BaseMessage{

	public static final Logger m_logger = Logger.getLogger(GetBlockHeadersMessage.class);
	private static final int DEFAULT_SIZE_BYTES = 32;
    
    /**
     * Block number from which to start sending block headers
     */
    private long m_blockNumber;

    /**
     * Block hash from which to start sending block headers <br>
     * Initial block can be addressed by either {@code blockNumber} or {@code blockHash}
     */
    private byte[] m_blockHash;

    /**
     * The maximum number of headers to be returned. <br>
     * <b>Note:</b> the peer could return fewer.
     */
    private int m_maxHeaders;

    /**
     * Blocks to skip between consecutive headers. <br>
     * Direction depends on {@code reverse} param.
     */
    private int m_skipBlocks;

    /**
     * The direction of headers enumeration. <br>
     * <b>false</b> is for rising block numbers. <br>
     * <b>true</b> is for falling block numbers.
     */
    private boolean m_reverse;

    public GetBlockHeadersMessage(byte[] encoded) {
    	super(encoded);
    }

    public GetBlockHeadersMessage(long blockNumber, int maxHeaders) {
        this(blockNumber, null, maxHeaders, 0, false);
    }

    public GetBlockHeadersMessage(long blockNumber, byte[] blockHash, int maxHeaders, int skipBlocks, boolean reverse) {
    	m_blockNumber = blockNumber;
    	m_blockHash = blockHash;
    	m_maxHeaders = maxHeaders;
    	m_skipBlocks = skipBlocks;
    	m_reverse = reverse;

        m_parsed = true;
        encode();
    }

    private void encode() {
        byte[] maxHeaders  = RLP.encodeInt(m_maxHeaders);
        byte[] skipBlocks = RLP.encodeInt(m_skipBlocks);
        byte[] reverse  = RLP.encodeByte((byte) (m_reverse ? 1 : 0));
        if (m_blockHash != null) {
            byte[] hash = RLP.encodeElement(m_blockHash);
            m_encoded = RLP.encodeList(hash, maxHeaders, skipBlocks, reverse);
        } else {
            byte[] number = RLP.encodeBigInteger(BigInteger.valueOf(m_blockNumber));
            m_encoded = RLP.encodeList(number, maxHeaders, skipBlocks, reverse);
        }
    }

    private synchronized void parse() {
        if (m_parsed) {
        	return;
        }
        RLPList paramsList = (RLPList) RLP.decode2(m_encoded).get(0);
        byte[] blockBytes = paramsList.get(0).getRLPData();
        // it might be either a hash or number
        if (blockBytes == null) {
            m_blockNumber = 0;
        } else if (blockBytes.length == DEFAULT_SIZE_BYTES) {
            m_blockHash = blockBytes;
        } else {
            m_blockNumber = ByteUtil.byteArrayToLong(blockBytes);
        }

        byte[] maxHeaders = paramsList.get(1).getRLPData();
        m_maxHeaders = ByteUtil.byteArrayToInt(maxHeaders);

        byte[] skipBlocks = paramsList.get(2).getRLPData();
        m_skipBlocks = ByteUtil.byteArrayToInt(skipBlocks);

        byte[] reverse = paramsList.get(3).getRLPData();
        m_reverse = ByteUtil.byteArrayToInt(reverse) == 1;

        m_parsed = true;
    }

    public long getBlockNumber() {
        parse();
        return m_blockNumber;
    }

    public byte[] getBlockHash() {
        parse();
        return m_blockHash;
    }

    public BlockIdentifier getBlockIdentifier() {
        parse();
        return new BlockIdentifier(m_blockHash, m_blockNumber);
    }

    public int getMaxHeaders() {
        parse();
        return m_maxHeaders;
    }

    public int getSkipBlocks() {
        parse();
        return m_skipBlocks;
    }

    public boolean isReverse() {
        parse();
        return m_reverse;
    }

	@Override
    public byte[] getEncoded() {
        if (m_encoded == null) {
        	encode();
        }
        return m_encoded;
    }

    public SonMessageCodes getCommand() {
        return SonMessageCodes.GET_BLOCK_HEADERS;
    }

    public byte getCode() {
            return m_code;
    }

    @Override
    public String toString() {
        parse();
        return "[" + getCommand().name() +
                " blockNumber=" + String.valueOf(m_blockNumber) +
                " blockHash=" + ByteUtil.toHexString(m_blockHash) +
                " maxHeaders=" + m_maxHeaders +
                " skipBlocks=" + m_skipBlocks +
                " reverse=" + m_reverse + "]";
    }
}
