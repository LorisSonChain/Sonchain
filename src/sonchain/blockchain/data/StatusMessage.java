package sonchain.blockchain.data;

import org.bouncycastle.util.encoders.Hex;

import sonchain.blockchain.net.SonMessageCodes;
import sonchain.blockchain.util.ByteUtil;
import sonchain.blockchain.util.RLP;
import sonchain.blockchain.util.RLPList;

public class StatusMessage extends BaseMessage{
    protected int m_networkId;
    /**
     * The hash of the best (i.e. highest TD) known block.
     */
    protected byte[] m_bestHash;
    /**
     * The hash of the Genesis block
     */
    protected byte[] m_genesisHash;

    public StatusMessage(byte[] encoded) {
    	super(encoded);
    }

    public StatusMessage(int networkId,
                         byte[] totalDifficulty, byte[] bestHash, byte[] genesisHash) {
    	m_networkId = networkId;
    	m_bestHash = bestHash;
    	m_genesisHash = genesisHash;
        m_parsed = true;
    }

    protected synchronized void parse() {
        if (m_parsed){
        	return;
        }        
        RLPList paramsList = (RLPList) RLP.decode2(m_encoded).get(0);
        byte[] networkIdBytes = paramsList.get(0).getRLPData();
        m_networkId = networkIdBytes == null ? 0 : ByteUtil.byteArrayToInt(networkIdBytes);
        m_bestHash = paramsList.get(1).getRLPData();
        m_genesisHash = paramsList.get(2).getRLPData();
        m_parsed = true;
    }

    protected void encode() {
        byte[] networkId = RLP.encodeInt(m_networkId);
        byte[] bestHash = RLP.encodeElement(m_bestHash);
        byte[] genesisHash = RLP.encodeElement(this.m_genesisHash);

        m_encoded = RLP.encodeList( networkId, bestHash, genesisHash);
    }

    public byte[] getEncoded() {
        if (m_encoded == null) {
        	encode();
        }
        return m_encoded;
    }

    public int getNetworkId() {
        parse();
        return m_networkId;
    }

    public byte[] getBestHash() {
        parse();
        return m_bestHash;
    }

    public byte[] getGenesisHash() {
        parse();
        return m_genesisHash;
    }

    public SonMessageCodes getCommand() {
        return SonMessageCodes.STATUS;
    }


    @Override
    public String toString() {
        parse();
        return "[" + this.getCommand().name() +
                " networkId=" + m_networkId +
                " bestHash=" + Hex.toHexString(m_bestHash) +
                " genesisHash=" + Hex.toHexString(m_genesisHash) +
                "]";
    }
}
