package sonchain.blockchain.data;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.bouncycastle.util.encoders.Hex;

import sonchain.blockchain.net.SonMessageCodes;
import sonchain.blockchain.util.RLP;
import sonchain.blockchain.util.RLPList;

public class BlockBodiesMessage extends BaseMessage{
	public static final Logger m_logger = Logger.getLogger(BlockBodiesMessage.class);
	private List<byte[]> m_blockBodies = null;

	public BlockBodiesMessage(byte[] encoded) {
		super(encoded);
	}

	public BlockBodiesMessage(List<byte[]> blockBodies) {
		m_blockBodies = blockBodies;
		m_parsed = true;
	}

	private synchronized void parse() {
		if (m_parsed){
			return;
		}
		RLPList paramsList = (RLPList) RLP.decode2(m_encoded).get(0);
		m_blockBodies = new ArrayList<>();
		for (int i = 0; i < paramsList.size(); ++i) {
			RLPList rlpData = ((RLPList) paramsList.get(i));
			m_blockBodies.add(rlpData.getRLPData());
		}
		m_parsed = true;
	}

	private void encode() {
		byte[][] encodedElementArray = m_blockBodies.toArray(new byte[m_blockBodies.size()][]);
		m_encoded = RLP.encodeList(encodedElementArray);
	}

	@Override
	public byte[] getEncoded() {
		if (m_encoded == null){
			encode();
		}
		return m_encoded;
	}

	public List<byte[]> getBlockBodies() {
		parse();
		return m_blockBodies;
	}

	@Override
	public SonMessageCodes getCommand() {
		return SonMessageCodes.BLOCK_BODIES;
	}

	@Override
	public String toString() {
		parse();
		StringBuilder payload = new StringBuilder();
		payload.append("count( ").append(m_blockBodies.size()).append(" )");
		if (m_logger.isTraceEnabled()) {
			payload.append(" ");
			for (byte[] body : m_blockBodies) {
				payload.append(Hex.toHexString(body)).append(" | ");
			}
			if (!m_blockBodies.isEmpty()) {
				payload.delete(payload.length() - 3, payload.length());
			}
		}
		return "[" + getCommand().name() + " " + payload + "]";
	}
}
