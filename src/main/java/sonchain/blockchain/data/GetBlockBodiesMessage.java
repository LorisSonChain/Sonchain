package sonchain.blockchain.data;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.bouncycastle.util.encoders.Hex;

import sonchain.blockchain.net.SonMessageCodes;
import sonchain.blockchain.util.RLP;
import sonchain.blockchain.util.RLPList;
import sonchain.blockchain.util.Utils;

public class GetBlockBodiesMessage extends BaseMessage{
	public static final Logger m_logger = Logger.getLogger(GetBlockBodiesMessage.class);
	 /**
     * List of block hashes for which to retrieve the block bodies
     */
    private List<byte[]> m_blockHashes;

    public GetBlockBodiesMessage(byte[] encoded) {
    	super(encoded);
    }

    public GetBlockBodiesMessage(List<byte[]> blockHashes) {
    	m_blockHashes = blockHashes;
        m_parsed = true;
    }

    private synchronized void parse() {
        if (m_parsed){
        	return;
        }
        RLPList paramsList = (RLPList) RLP.decode2(m_encoded).get(0);
        m_blockHashes = new ArrayList<>();
        for (int i = 0; i < paramsList.size(); ++i) {
        	m_blockHashes.add(paramsList.get(i).getRLPData());
        }
        m_parsed = true;
    }

    private void encode() {
        List<byte[]> encodedElements = new ArrayList<>();
        for (byte[] hash : m_blockHashes)
            encodedElements.add(RLP.encodeElement(hash));
        byte[][] encodedElementArray = encodedElements.toArray(new byte[encodedElements.size()][]);
        m_encoded = RLP.encodeList(encodedElementArray);
    }

	@Override
    public byte[] getEncoded() {
        if (m_encoded == null) {
        	encode();
        }
        return m_encoded;
    }

    public List<byte[]> getBlockHashes() {
        parse();
        return m_blockHashes;
    }

    public SonMessageCodes getCommand() {
        return SonMessageCodes.GET_BLOCK_BODIES;
    }

    @Override
    public String toString() {
        parse();
        StringBuilder payload = new StringBuilder();
        payload.append("count( ").append(m_blockHashes.size()).append(" ) ");

        if (m_logger.isDebugEnabled()) {
            for (byte[] hash : m_blockHashes) {
                payload.append(Hex.toHexString(hash).substring(0, 6)).append(" | ");
            }
            if (!m_blockHashes.isEmpty()) {
                payload.delete(payload.length() - 3, payload.length());
            }
        } else {
            payload.append(Utils.getHashListShort(m_blockHashes));
        }
        return "[" + getCommand().name() + " " + payload + "]";
    }
}
