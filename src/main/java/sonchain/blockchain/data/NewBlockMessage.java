package sonchain.blockchain.data;

import sonchain.blockchain.core.Block;
import sonchain.blockchain.net.SonMessageCodes;
import sonchain.blockchain.util.RLP;
import sonchain.blockchain.util.RLPList;

public class NewBlockMessage extends BaseMessage{
	private Block m_block = null;
	
	public NewBlockMessage(byte[] encoded) {
    	super(encoded);
	}

	public NewBlockMessage(Block block) {
		m_block = block;
		m_parsed = true;
		encode();
	}

	private void encode() {
		byte[] block = m_block.getEncoded();
		m_encoded = RLP.encodeList(block);
	}

	private synchronized void parse() {
		if (m_parsed){
			return;
		}
		RLPList paramsList = (RLPList) RLP.decode2(m_encoded).get(0);
		RLPList blockRLP = ((RLPList) paramsList.get(0));
		m_block = new Block(blockRLP.getRLPData());
		m_parsed = true;
	}

	public Block getBlock() {
		parse();
		return m_block;
	}

	@Override
	public byte[] getEncoded() {
		return m_encoded;
	}

	public SonMessageCodes getCommand() {
		return SonMessageCodes.NEW_BLOCK;
	}
    
    @Override
	public String toString() {
		parse();

		String hash = this.getBlock().getShortHash();
		long number = this.getBlock().getNumber();
		return "NEW_BLOCK [ number: " + number + " hash:" + hash + " ]";
	}
}
