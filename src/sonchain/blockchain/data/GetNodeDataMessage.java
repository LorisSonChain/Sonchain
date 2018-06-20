package sonchain.blockchain.data;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.bouncycastle.util.encoders.Hex;

import sonchain.blockchain.net.SonMessageCodes;
import sonchain.blockchain.util.RLP;
import sonchain.blockchain.util.RLPList;
import sonchain.blockchain.util.Utils;

public class GetNodeDataMessage extends BaseMessage{
	public static final Logger m_logger = Logger.getLogger(GetNodeDataMessage.class);
	
	/**
     * List of node hashes for which is state requested
     */
    private List<byte[]> m_nodeKeys;

    public GetNodeDataMessage(byte[] encoded) {
        super(encoded);
    }

    public GetNodeDataMessage(List<byte[]> nodeKeys) {
    	m_nodeKeys = nodeKeys;
        m_parsed = true;
    }

    private synchronized void parse() {
        if (m_parsed){
        	return;
        }
        RLPList paramsList = (RLPList) RLP.decode2(m_encoded).get(0);

        m_nodeKeys = new ArrayList<>();
        for (int i = 0; i < paramsList.size(); ++i) {
        	m_nodeKeys.add(paramsList.get(i).getRLPData());
        }

        m_parsed = true;
    }

    private void encode() {
        List<byte[]> encodedElements = new ArrayList<>();
        for (byte[] hash : m_nodeKeys)
            encodedElements.add(RLP.encodeElement(hash));
        byte[][] encodedElementArray = encodedElements.toArray(new byte[encodedElements.size()][]);

        m_encoded = RLP.encodeList(encodedElementArray);
    }

    public byte[] getEncoded() {
        if (m_encoded == null) {
        	encode();
        }
        return m_encoded;
    }
    public List<byte[]> getNodeKeys() {
        parse();
        return m_nodeKeys;
    }

    @Override
    public SonMessageCodes getCommand() {
        return SonMessageCodes.GET_NODE_DATA;
    }

    @Override
    public String toString() {
        parse();
        StringBuilder payload = new StringBuilder();
        payload.append("count( ").append(m_nodeKeys.size()).append(" ) ");
        if (m_logger.isDebugEnabled()) {
            for (byte[] hash : m_nodeKeys) {
                payload.append(Hex.toHexString(hash).substring(0, 6)).append(" | ");
            }
            if (!m_nodeKeys.isEmpty()) {
                payload.delete(payload.length() - 3, payload.length());
            }
        } else {
            payload.append(Utils.getHashListShort(m_nodeKeys));
        }
        return "[" + getCommand().name() + " " + payload + "]";
    }
}
