package sonchain.blockchain.core;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;

import sonchain.blockchain.consensus.SonChainProducerNode;
import sonchain.blockchain.data.SonChainHostInfo;
import sonchain.blockchain.util.RLP;
import sonchain.blockchain.util.RLPElement;
import sonchain.blockchain.util.RLPList;
import sonchain.blockchain.util.TimeUtils;

public class BlockWrapper {
	
	private static final long SOLID_BLOCK_DURATION_THRESHOLD = TimeUtils.secondsToMillis(60);

    private Block m_block = null;
    private long m_importFailedAt = 0;
    private long m_receivedAt = 0;
    private boolean m_newBlock = false;
    private SonChainProducerNode m_hostInfo = null;

    
    public BlockWrapper(Block block, SonChainProducerNode hostInfo) {
        this(block, false, hostInfo);
    }
    
    public BlockWrapper(Block block, boolean newBlock, SonChainProducerNode hostInfo) {
    	m_block = block;
    	m_newBlock = newBlock;
    	m_hostInfo = hostInfo;
    }

    public BlockWrapper(byte[] bytes) {
        parse(bytes);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()){
        	return false;
        }
        BlockWrapper wrapper = (BlockWrapper) o;
        return m_block.isEqual(wrapper.m_block);
    }

    public Block getBlock() {
        return m_block;
    }

    public byte[] getBytes() {
        byte[] blockBytes = m_block.getEncoded();
        byte[] importFailedBytes = RLP.encodeBigInteger(BigInteger.valueOf(m_importFailedAt));
        byte[] receivedAtBytes = RLP.encodeBigInteger(BigInteger.valueOf(m_receivedAt));
        byte[] newBlockBytes = RLP.encodeByte((byte) (m_newBlock ? 1 : 0));
        byte[] nodeIdBytes = RLP.encodeElement(m_hostInfo.toBytes());
        return RLP.encodeList(blockBytes, importFailedBytes,
                receivedAtBytes, newBlockBytes, nodeIdBytes);
    }

    public byte[] getEncoded() {
        return m_block.getEncoded();
    }

    public byte[] getHash() {
        return m_block.getHash();
    }

    public long getImportFailedAt() {
        return m_importFailedAt;
    }

    public SonChainProducerNode getNodeId() {
        return m_hostInfo;
    }
    
    public long getNumber() {
        return m_block.getBlockNumber();
    }

    public String getParentHash() {
        return m_block.getParentHash();
    }

    public long getReceivedAt() {
        return m_receivedAt;
    }

    public String getShortHash() {
        return m_block.getShortHash();
    }

    public void importFailed() {
        if (m_importFailedAt == 0) {
        	m_importFailedAt = System.currentTimeMillis();
        }
    }

    public boolean isEqual(BlockWrapper wrapper) {
        return wrapper != null && m_block.isEqual(wrapper.getBlock());
    }

    public boolean isNewBlock() {
        return m_newBlock;
    }

    public boolean isSolidBlock() {
        return !m_newBlock || timeSinceReceiving() > SOLID_BLOCK_DURATION_THRESHOLD;
    }

    private void parse(byte[] bytes) {
        List<RLPElement> params = RLP.decode2(bytes);
        List<RLPElement> wrapper = (RLPList) params.get(0);

        byte[] blockBytes = wrapper.get(0).getRLPData();
        byte[] importFailedBytes = wrapper.get(1).getRLPData();
        byte[] receivedAtBytes = wrapper.get(2).getRLPData();
        byte[] newBlockBytes = wrapper.get(3).getRLPData();

		//TODO
        //m_block = new Block(blockBytes);
        m_importFailedAt = importFailedBytes == null ? 0 : new BigInteger(1, importFailedBytes).longValue();
        m_receivedAt = receivedAtBytes == null ? 0 : new BigInteger(1, receivedAtBytes).longValue();
        byte newBlock = newBlockBytes == null ? 0 : new BigInteger(1, newBlockBytes).byteValue();
        m_newBlock = newBlock == 1;
        byte[] nodeBytes = wrapper.get(4).getRLPData();
        m_hostInfo = SonChainProducerNode.fromBytes(nodeBytes);
    }
    
    public void resetImportFail() {
    	m_importFailedAt = 0;
    }
    
    public boolean sentBy(SonChainProducerNode hostInfo) {
    	if(m_hostInfo.getHost().equals(hostInfo.getHost()) 
    			&& m_hostInfo.getPort() == hostInfo.getPort()){
    		return true;
    	}
        return false;
    }

    public void setImportFailedAt(long importFailedAt) {
    	m_importFailedAt = importFailedAt;
    }

    public void setReceivedAt(long receivedAt) {
    	m_receivedAt = receivedAt;
    }
    public long timeSinceFail() {
        if(m_importFailedAt == 0) {
            return 0;
        } else {
            return System.currentTimeMillis() - m_importFailedAt;
        }
    }

    public long timeSinceReceiving() {
        return System.currentTimeMillis() - m_receivedAt;
    }
}
