package sonchain.blockchain.consensus;

import java.math.BigInteger;

import org.apache.log4j.Logger;
import sonchain.blockchain.data.BaseMessage;
import sonchain.blockchain.net.SonMessageCodes;
import sonchain.blockchain.util.ByteUtil;
import sonchain.blockchain.util.RLP;
import sonchain.blockchain.util.RLPList;

public class PrepareResponseMessage extends BaseMessage{

	public static final Logger m_logger = Logger.getLogger(PrepareResponseMessage.class);
	public int m_viewNumber = 0;
	public byte[] m_signature = null;

	public PrepareResponseMessage(byte[] encoded) {
		super(encoded);
	}

	public PrepareResponseMessage(byte[] signature, int viewNumber) {
		m_signature = signature;
		m_viewNumber = viewNumber;
		m_parsed = true;
	}

	private synchronized void parse() {
		if (m_parsed){
			return;
		}
		RLPList paramsList = (RLPList) RLP.decode2(m_encoded).get(0);
		m_signature = ((RLPList) paramsList.get(0)).getRLPData();
        byte[] viewNumberBytes = ((RLPList) paramsList.get(1)).getRLPData();
        m_viewNumber = viewNumberBytes == null ? 0 : (new BigInteger(1, viewNumberBytes)).intValue();
		m_parsed = true;
	}

	private void encode() {
        byte[] viewNumberBytes = RLP.encodeBigInteger(BigInteger.valueOf(m_viewNumber));
		m_encoded = RLP.encodeList(m_signature, viewNumberBytes);
	}

	@Override
	public byte[] getEncoded() {
		if (m_encoded == null){
			encode();
		}
		return m_encoded;
	}

	@Override
	public SonMessageCodes getCommand() {
		return SonMessageCodes.PrepareRequest;
	}

	@Override
	public String toString() {
		parse();
		StringBuilder payload = new StringBuilder();
		payload.append("signature= ").append(ByteUtil.toHexString(m_signature)).append(" )");
		payload.append("ViewNumber( ").append(m_viewNumber).append(" )");
		return "[" + getCommand().name() + " " + payload + "]";
	}
}
