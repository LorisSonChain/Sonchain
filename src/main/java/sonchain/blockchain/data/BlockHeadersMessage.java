package sonchain.blockchain.data;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.bouncycastle.util.encoders.Hex;

import sonchain.blockchain.core.BlockHeader;
import sonchain.blockchain.net.SonMessageCodes;
import sonchain.blockchain.util.RLP;
import sonchain.blockchain.util.RLPList;

public class BlockHeadersMessage  extends BaseMessage{
	public static final Logger m_logger = Logger.getLogger(BlockHeadersMessage.class);
	 /**
     * List of block headers from the peer
     */
    private List<BlockHeader> m_blockHeaders = null;

    public BlockHeadersMessage(byte[] encoded) {
    	super(encoded);
    }

    public BlockHeadersMessage(List<BlockHeader> headers) {
    	m_blockHeaders = headers;
        m_parsed = true;
    }

    private synchronized void parse() {
        if (m_parsed) {
        	return;
        }
        RLPList paramsList = (RLPList) RLP.decode2(m_encoded).get(0);
        m_blockHeaders = new ArrayList<>();
        for (int i = 0; i < paramsList.size(); ++i) {
            RLPList rlpData = ((RLPList) paramsList.get(i));
	        //TODO
            //m_blockHeaders.add(new BlockHeader(rlpData));
        }
        m_parsed = true;
    }

    private void encode() {
        List<byte[]> encodedElements = new ArrayList<>();
        for (BlockHeader blockHeader : m_blockHeaders)
            encodedElements.add(blockHeader.getEncoded());
        byte[][] encodedElementArray = encodedElements.toArray(new byte[encodedElements.size()][]);
        m_encoded = RLP.encodeList(encodedElementArray);
    }

	@Override
    public byte[] getEncoded() {
        if (m_encoded == null){
        	encode();
        }
        return m_encoded;
    }

    public List<BlockHeader> getBlockHeaders() {
        parse();
        return m_blockHeaders;
    }

    public SonMessageCodes getCommand() {
        return SonMessageCodes.BLOCK_HEADERS;
    }

    @Override
    public String toString() {
        parse();
        StringBuilder payload = new StringBuilder();
        payload.append("count( ").append(m_blockHeaders.size()).append(" )");
        if (m_logger.isTraceEnabled()) {
            payload.append(" ");
            for (BlockHeader header : m_blockHeaders) {
                payload.append(Hex.toHexString(header.getHash()).substring(0, 6)).append(" | ");
            }
            if (!m_blockHeaders.isEmpty()) {
                payload.delete(payload.length() - 3, payload.length());
            }
        } else {
            if (m_blockHeaders.size() > 0) {
                payload.append("#").append(m_blockHeaders.get(0).getBlockNumber()).append(" (")
                        .append(Hex.toHexString(m_blockHeaders.get(0).getHash()).substring(0, 8)).append(")");
            }
            if (m_blockHeaders.size() > 1) {
                payload.append(" ... #").append(m_blockHeaders.get(m_blockHeaders.size() - 1).getBlockNumber()).append(" (")
                        .append(Hex.toHexString(m_blockHeaders.get(m_blockHeaders.size() - 1).getHash()).substring(0, 8)).append(")");
            }
        }

        return "[" + getCommand().name() + " " + payload + "]";
    }
}
