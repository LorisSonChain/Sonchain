package sonchain.blockchain.data;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import sonchain.blockchain.net.SonMessageCodes;
import sonchain.blockchain.util.RLP;
import sonchain.blockchain.util.RLPList;
import sonchain.blockchain.util.Value;

public class NodeDataMessage  extends BaseMessage{
	public static final Logger m_logger = Logger.getLogger(NodeDataMessage.class);
	private List<Value> m_dataList;

    public NodeDataMessage(byte[] encoded) {
        super(encoded);
        parse();
    }

    public NodeDataMessage(List<Value> dataList) {
    	m_dataList = dataList;
        m_parsed = true;
    }

    private void parse() {
        RLPList paramsList = (RLPList) RLP.decode2(m_encoded).get(0);
        m_dataList = new ArrayList<>();
        for (int i = 0; i < paramsList.size(); ++i) {
            // Need it AS IS
        	m_dataList.add(Value.fromRlpEncoded(paramsList.get(i).getRLPData()));
        }
        m_parsed = true;
    }

    private void encode() {
        List<byte[]> dataListRLP = new ArrayList<>();
        for (Value value: m_dataList) {
            if (value == null) continue; // Bad sign
            dataListRLP.add(RLP.encodeElement(value.getData()));
        }
        byte[][] encodedElementArray = dataListRLP.toArray(new byte[dataListRLP.size()][]);
        m_encoded = RLP.encodeList(encodedElementArray);
    }

    public byte[] getEncoded() {
        if (m_encoded == null) {
        	encode();
        }
        return m_encoded;
    }

    public List<Value> getDataList() {
        return m_dataList;
    }

    @Override
    public SonMessageCodes getCommand() {
        return SonMessageCodes.NODE_DATA;
    }

    @Override
    public String toString() {
        StringBuilder payload = new StringBuilder();

        payload.append("count( ").append(m_dataList.size()).append(" )");

        if (m_logger.isTraceEnabled()) {
            payload.append(" ");
            for (Value value : m_dataList) {
                payload.append(value).append(" | ");
            }
            if (!m_dataList.isEmpty()) {
                payload.delete(payload.length() - 3, payload.length());
            }
        }

        return "[" + getCommand().name() + " " + payload + "]";
    }
}
