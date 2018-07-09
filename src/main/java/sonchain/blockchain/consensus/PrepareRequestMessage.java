package sonchain.blockchain.consensus;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.bouncycastle.util.encoders.Hex;

import sonchain.blockchain.core.Block;
import sonchain.blockchain.core.Transaction;
import sonchain.blockchain.net.SonMessageCodes;
import sonchain.blockchain.util.ByteUtil;
import sonchain.blockchain.util.RLP;
import sonchain.blockchain.util.RLPList;
import sonchain.blockchain.data.BaseMessage;

public class PrepareRequestMessage extends BaseMessage{

	public static final Logger m_logger = Logger.getLogger(PrepareRequestMessage.class);
	public int m_viewNumber = 0;
	public BigInteger m_nonce = BigInteger.ZERO;
	public byte[] m_nextConsensus = null;
	public List<Transaction> m_lstTransaction = new ArrayList<Transaction>();
	public Block m_block = null;
	public byte[] m_signature = null;
	
	public PrepareRequestMessage(byte[] encoded) {
		super(encoded);
	}

	public PrepareRequestMessage(BigInteger nonce, byte[] nextConsensus, 
			List<Transaction> lstTransaction, Block block, byte[] signature, int viewNumber) {
		m_nonce = nonce;
		m_nextConsensus = nextConsensus;
		m_lstTransaction = lstTransaction;
		m_block = block;
		m_signature = signature;
		m_viewNumber = viewNumber;
		m_parsed = true;
	}

	private synchronized void parse() {
		if (m_parsed){
			return;
		}
		RLPList paramsList = (RLPList) RLP.decode2(m_encoded).get(0);
		byte[] nonceBytes = ((RLPList) paramsList.get(0)).getRLPData();
		m_nonce = new BigInteger(1, nonceBytes);
		m_nextConsensus = ((RLPList) paramsList.get(1)).getRLPData();
		byte[] blockBytes = ((RLPList) paramsList.get(2)).getRLPData();
		m_block = new Block(blockBytes);
		m_signature = ((RLPList) paramsList.get(3)).getRLPData();	
        byte[] viewNumberBytes = ((RLPList) paramsList.get(4)).getRLPData();
        m_viewNumber = viewNumberBytes == null ? 0 : (new BigInteger(1, viewNumberBytes)).intValue();	
		m_lstTransaction = new ArrayList<Transaction>();
		for (int i = 5; i < paramsList.size(); ++i) {
			RLPList rlpData = ((RLPList) paramsList.get(i));
			m_lstTransaction.add(new Transaction(rlpData.getRLPData()));
		}
		m_parsed = true;
	}

	private void encode() {
		byte[] nonceBytes = RLP.encodeBigInteger(m_nonce);
		byte[] blockBytes = m_block.getEncoded();
		List<byte[]> body = new ArrayList<byte[]>();
		body.add(nonceBytes);
		body.add(m_nextConsensus);
		body.add(blockBytes);
		body.add(m_signature);
        byte[] viewNumberBytes = RLP.encodeBigInteger(BigInteger.valueOf(m_viewNumber));
		body.add(viewNumberBytes);
		for(Transaction trans : m_lstTransaction){
			body.add(trans.getEncoded());
		}
		byte[][] elements = body.toArray(new byte[body.size()][]);
		m_encoded = RLP.encodeList(elements);
	}

	@Override
	public byte[] getEncoded() {
		if (m_encoded == null){
			encode();
		}
		return m_encoded;
	}

	public List<Transaction> getTransactions() {
		parse();
		return m_lstTransaction;
	}

	@Override
	public SonMessageCodes getCommand() {
		return SonMessageCodes.PrepareRequest;
	}

	@Override
	public String toString() {
		parse();
		StringBuilder payload = new StringBuilder();
		payload.append("nonce= ").append(m_nonce).append(" )");
		payload.append("nextConsensus= ").append(ByteUtil.toHexString(m_nextConsensus)).append(" )");
		payload.append("signature= ").append(ByteUtil.toHexString(m_signature)).append(" )");
		payload.append("block= ").append(m_block.toString()).append(" )");
		payload.append("viewNumber= ").append(m_viewNumber).append(" )");
		payload.append("count( ").append(m_lstTransaction.size()).append(" )");
		if (m_logger.isTraceEnabled()) {
			payload.append(" ");
			for (Transaction body : m_lstTransaction) {
				payload.append(body.toString()).append(" | ");
			}
			if (!m_lstTransaction.isEmpty()) {
				payload.delete(payload.length() - 3, payload.length());
			}
		}
		return "[" + getCommand().name() + " " + payload + "]";
	}
}
