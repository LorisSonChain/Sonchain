package sonchain.blockchain.vm;

import java.util.ArrayList;
import java.util.List;

import org.bouncycastle.util.encoders.Hex;

import sonchain.blockchain.crypto.HashUtil;
import sonchain.blockchain.util.RLP;
import sonchain.blockchain.util.RLPElement;
import sonchain.blockchain.util.RLPItem;
import sonchain.blockchain.util.RLPList;

public class LogInfo {
	private byte[] m_address = new byte[]{};
	private byte[] m_data = new byte[]{};
	private List<DataWord> m_topics = new ArrayList<>();

    /* Log info in encoded form */
    private byte[] m_rlpEncoded = null;

    public LogInfo(byte[] rlp) {

        RLPList params = RLP.decode2(rlp);
        RLPList logInfo = (RLPList) params.get(0);

        RLPItem address = (RLPItem) logInfo.get(0);
        RLPList topics = (RLPList) logInfo.get(1);
        RLPItem data = (RLPItem) logInfo.get(2);

        m_address = address.getRLPData() != null ? address.getRLPData() : new byte[]{};
        m_data = data.getRLPData() != null ? data.getRLPData() : new byte[]{};

        for (RLPElement topic1 : topics) {
            byte[] topic = topic1.getRLPData();
            m_topics.add(new DataWord(topic));
        }
        m_rlpEncoded = rlp;
    }

    public LogInfo(byte[] address, List<DataWord> topics, byte[] data) {
    	m_address = (address != null) ? address : new byte[]{};
    	m_topics = (topics != null) ? topics : new ArrayList<DataWord>();
    	m_data = (data != null) ? data : new byte[]{};
    }

    public byte[] getAddress() {
        return m_address;
    }

    public List<DataWord> getTopics() {
        return m_topics;
    }

    public byte[] getData() {
        return m_data;
    }

    /*  [address, [topic, topic ...] data] */
    public byte[] getEncoded() {
        byte[] addressEncoded = RLP.encodeElement(m_address);
        byte[][] topicsEncoded = null;
        if (m_topics != null) {
            topicsEncoded = new byte[m_topics.size()][];
            int i = 0;
            for (DataWord topic : m_topics) {
                byte[] topicData = topic.getData();
                topicsEncoded[i] = RLP.encodeElement(topicData);
                ++i;
            }
        }
        byte[] dataEncoded = RLP.encodeElement(m_data);
        return RLP.encodeList(addressEncoded, RLP.encodeList(topicsEncoded), dataEncoded);
    }

    @Override
    public String toString() {
        StringBuilder topicsStr = new StringBuilder();
        topicsStr.append("[");
        for (DataWord topic : m_topics) {
            String topicStr = Hex.toHexString(topic.getData());
            topicsStr.append(topicStr).append(" ");
        }
        topicsStr.append("]");
        return "LogInfo{" +
                "address=" + Hex.toHexString(m_address) +
                ", topics=" + topicsStr +
                ", data=" + Hex.toHexString(m_data) +
                '}';
    }
}
