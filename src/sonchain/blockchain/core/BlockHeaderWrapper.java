package sonchain.blockchain.core;

import java.util.List;

import org.bouncycastle.util.encoders.Hex;

import sonchain.blockchain.data.SonChainHostInfo;
import sonchain.blockchain.util.RLP;
import sonchain.blockchain.util.RLPList;
import sonchain.blockchain.util.RLPElement;

public class BlockHeaderWrapper {

    private BlockHeader m_header;
    private SonChainHostInfo m_nodeInfo;

    public BlockHeaderWrapper(BlockHeader header, SonChainHostInfo nodeInfo) {
    	m_header = header;
    	m_nodeInfo = nodeInfo;
    }

    public BlockHeaderWrapper(byte[] bytes) {
        parse(bytes);
    }

    public byte[] getBytes() {
        byte[] headerBytes = m_header.getEncoded();
        byte[] nodeIdBytes = RLP.encodeElement(m_nodeInfo.toBytes());
        return RLP.encodeList(headerBytes, nodeIdBytes);
    }

    public byte[] getHash() {
        return m_header.getHash();
    }

    public BlockHeader getHeader() {
        return m_header;
    }

    public String getHexStrShort() {
        return Hex.toHexString(m_header.getHash()).substring(0, 6);
    }
    
    public SonChainHostInfo getNodeId() {
        return m_nodeInfo;
    }
    
    public long getNumber() {
        return m_header.getNumber();
    }
    
    private void parse(byte[] bytes) {
        List<RLPElement> params = RLP.decode2(bytes);
        List<RLPElement> wrapper = (RLPList) params.get(0);
        byte[] headerBytes = wrapper.get(0).getRLPData();
        m_header= new BlockHeader(headerBytes);
        byte[] nodeBytes = wrapper.get(1).getRLPData();
        m_nodeInfo = SonChainHostInfo.fromBytes(nodeBytes);
    }

    public boolean sentBy(SonChainHostInfo nodeInfo) {
    	if(m_nodeInfo.m_ip.equals(nodeInfo.m_ip) 
    			&& m_nodeInfo.m_serverPort == nodeInfo.m_serverPort){
    		return true;
    	}
        return false;
    }

    @Override
    public String toString() {
        return "BlockHeaderWrapper {" +
                "header=" + m_header +
                ", nodeId=" + Hex.toHexString(m_nodeInfo.toBytes()) +
                '}';
    }
}
