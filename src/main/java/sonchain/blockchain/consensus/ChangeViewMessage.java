package sonchain.blockchain.consensus;

import java.math.BigInteger;

import org.apache.log4j.Logger;

import sonchain.blockchain.data.BaseMessage;
import sonchain.blockchain.net.SonMessageCodes;
import sonchain.blockchain.util.RLP;
import sonchain.blockchain.util.RLPList;

public class ChangeViewMessage extends BaseMessage{

	public static final Logger m_logger = Logger.getLogger(ChangeViewMessage.class);
	public int m_viewNumber = 0;
	public int m_newViewNumber = 0;

	public ChangeViewMessage(byte[] encoded) {
		super(encoded);
	}

	public ChangeViewMessage(int newViewNumber, int viewNumber) {
		m_newViewNumber = newViewNumber;
		m_viewNumber = viewNumber;
		m_parsed = true;
	}

	private synchronized void parse() {
		if (m_parsed){
			return;
		}
		RLPList paramsList = (RLPList) RLP.decode2(m_encoded).get(0);
		RLPList rlpData = ((RLPList) paramsList.get(0));
        byte[] newViewNumberBytes = rlpData.get(0).getRLPData();
        byte[] viewNumberBytes = rlpData.get(1).getRLPData();
        m_newViewNumber = newViewNumberBytes == null ? 0 : (new BigInteger(1, newViewNumberBytes)).intValue();
        m_viewNumber = viewNumberBytes == null ? 0 : (new BigInteger(1, viewNumberBytes)).intValue();
		m_parsed = true;
	}

	private void encode() {
        byte[] newViewNumberBytes = RLP.encodeBigInteger(BigInteger.valueOf(m_newViewNumber));
        byte[] viewNumberBytes = RLP.encodeBigInteger(BigInteger.valueOf(m_viewNumber));
		m_encoded = RLP.encodeList(newViewNumberBytes, viewNumberBytes);
	}

	@Override
	public byte[] getEncoded() {
		if (m_encoded == null){
			encode();
		}
		return m_encoded;
	}

	public int getNewViewNumber() {
		parse();
		return m_newViewNumber;
	}
	
	public int getViewNumber() {
		parse();
		return m_viewNumber;
	}

	@Override
	public SonMessageCodes getCommand() {
		return SonMessageCodes.ChangeView;
	}

	@Override
	public String toString() {
		parse();
		StringBuilder payload = new StringBuilder();
		payload.append("NewViewNumber( ").append(m_newViewNumber).append(" )");
		payload.append("ViewNumber( ").append(m_viewNumber).append(" )");
		return "[" + getCommand().name() + " " + payload + "]";
	}
}
