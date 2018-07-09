package sonchain.blockchain.data;

import java.util.ArrayList;
import java.util.List;

import sonchain.blockchain.core.BlockIdentifier;
import sonchain.blockchain.net.SonMessageCodes;
import sonchain.blockchain.util.RLP;
import sonchain.blockchain.util.RLPList;

public class NewBlockHashesMessage extends BaseMessage{
	 /**
     * List of identifiers holding hash and number of the blocks
     */
    private List<BlockIdentifier> m_blockIdentifiers = null;
    
    public NewBlockHashesMessage(byte[] encoded) {
    	super(encoded);
    }

    public NewBlockHashesMessage(List<BlockIdentifier> blockIdentifiers) {
    	m_blockIdentifiers = blockIdentifiers;
        m_parsed = true;
    }

    private synchronized void parse() {
        if (m_parsed) {
        	return;
        }
        RLPList paramsList = (RLPList) RLP.decode2(m_encoded).get(0);

        m_blockIdentifiers = new ArrayList<>();
        for (int i = 0; i < paramsList.size(); ++i) {
            RLPList rlpData = ((RLPList) paramsList.get(i));
            m_blockIdentifiers.add(new BlockIdentifier(rlpData));
        }
        m_parsed = true;
    }

    private void encode() {
        List<byte[]> encodedElements = new ArrayList<>();
        for (BlockIdentifier identifier : m_blockIdentifiers)
            encodedElements.add(identifier.getEncoded());
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

    public List<BlockIdentifier> getBlockIdentifiers() {
        parse();
        return m_blockIdentifiers;
    }

    public SonMessageCodes getCommand() {
        return SonMessageCodes.NEW_BLOCK_HASHES;
    }

    @Override
    public String toString() {
        parse();
        return "[" + getCommand().name() + "] (" + m_blockIdentifiers.size() + ")";
    }
}
