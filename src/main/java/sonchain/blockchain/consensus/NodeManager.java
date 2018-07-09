package sonchain.blockchain.consensus;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.log4j.Logger;
import org.bouncycastle.util.encoders.Hex;

import sonchain.blockchain.core.Block;
import sonchain.blockchain.core.BlockSummary;
import sonchain.blockchain.crypto.ECKey;
import sonchain.blockchain.data.BlockBodiesMessage;
import sonchain.blockchain.data.TransactionsMessage;
import sonchain.blockchain.peer.BaseServicePeer;
import sonchain.blockchain.peer.SonChainServicePeer;
import sonchain.blockchain.server.BaseServiceSV;
import sonchain.blockchain.server.SonChainServiceSV;
import sonchain.blockchain.service.DataCenter;

public class NodeManager {

    private ECKey m_key = null;
    //private List<SonChainPeerNode> m_connectedPeers = new ArrayList<SonChainPeerNode>();
    private HashMap<String, SonChainServicePeer> m_clientsonchainServices = new HashMap<String, SonChainServicePeer>();
    private SonChainServiceSV m_serversonchainService = new SonChainServiceSV();	
    private List<SonChainPeerNode> m_remotePeerNodes = null; 
	public static final Logger m_logger = Logger.getLogger(NodeManager.class);
    
    public NodeManager(){
    	m_key = ECKey.fromPrivate(Hex.decode(DataCenter.m_config.m_localNodePrivateKey));
    }
    
    public void init(){
    	m_serversonchainService.SetPort(DataCenter.m_config.m_localPort);
		int socketIDServer = BaseServiceSV.StartServer(m_serversonchainService.GetPort());
		if(socketIDServer == -1){
			m_logger.info("StartServer failed. port:" + DataCenter.m_config.m_localPort);
			return;
		}
		m_serversonchainService.SetSocketID(socketIDServer);
		
		m_remotePeerNodes = DataCenter.GetRemotePeerNodes();
		int count = m_remotePeerNodes.size();
		if(count > 0){
			for(int i = 0; i < count; i++){
				SonChainPeerNode peerNode = m_remotePeerNodes.get(i);
				int socketID = SonChainServicePeer.Connect(peerNode.getHost(), peerNode.getPort());
				if (socketID != -1)
				{
					String key = peerNode.toString();
					SonChainServicePeer clientsonchainService = new SonChainServicePeer();
					clientsonchainService.setSonChainPeerNode(peerNode);
					m_clientsonchainServices.put(key, clientsonchainService);
					BaseServicePeer.AddService(clientsonchainService);
					clientsonchainService.SetToServer(true);
					clientsonchainService.SetConnected(true);
					clientsonchainService.SetSocketID(socketID);
				}else{
					m_logger.info(String.format("Peer StartServer failed. host={%s}, port={%d}",
							peerNode.getHost(),  peerNode.getPort()));
				}
			}
		}
    }
    
    public void requestGetBlocks(){
    	
    }
	
	public SonChainServicePeer getOneSonChainServiceCT(String key)
	{
		return null;
		//return DataCenter.m_clientsonchainServices.get(key);
	}
	
	public SonChainServicePeer getAnyOneSonChainServiceCT()
	{
		return getOneSonChainServiceCT("");
	}
    
    public boolean relayBlock(Block block){
    	
    	m_logger.debug("relayBlock start blockInfo:" + block.toString());
    	if(DataCenter.getSonChainImpl().getBlockChain().containsBlock(block.getEncoded())){
        	m_logger.debug("relayBlock block existed");
    		return false;
    	}    	
    	BlockSummary summary = DataCenter.getSonChainImpl().getBlockChain().add(block);
        if (summary == null) {
        	m_logger.debug("relayBlock block add error");
    		return false;
        }
        List<byte[]> blockBodies = new ArrayList<byte[]>();
        blockBodies.add(block.getEncoded());
        BlockBodiesMessage message = new BlockBodiesMessage(blockBodies);
    	int count = 0;
    	synchronized(m_clientsonchainServices){
    		for(SonChainServicePeer peerNode : m_clientsonchainServices.values()){
    			if(peerNode.IsConnected()){
    				peerNode.sendMessage(message);
    				count ++;
    			}
    		}
    	}
    	return count > 0 ? true : false;
    }
    
    public boolean relayDirectly(ConsensusPayload inventory)
    {
    	m_logger.debug("relayDirectly start");
    	int count = 0;
    	synchronized(m_clientsonchainServices){
    		for(SonChainServicePeer peerNode : m_clientsonchainServices.values()){
    			if(peerNode.IsConnected()){
    				peerNode.sendMessageConsensusPayload(inventory);
    				count ++;
    			}
    		}
    	}
    	m_logger.debug("relayDirectly end");
        return count > 0 ? true:false;
    }
    
    public int sendAllTransactionsMessage(TransactionsMessage transactionsMessage){
    	m_logger.debug("sendAllTransactionsMessage start");
    	int count = 0;
    	synchronized(m_clientsonchainServices){
    		for(SonChainServicePeer peerNode : m_clientsonchainServices.values()){
    			if(peerNode.IsConnected()){
    				peerNode.sendMessage(transactionsMessage);
    				count ++;
    			}
    		}
    	}
    	m_logger.debug("sendAllTransactionsMessage end SendNodeCount:" + count);
    	return count;
    }
    
    public int sendTransactionsMessage(TransactionsMessage transactionsMessage){
    	m_logger.debug("sendTransactionsMessage start");
    	int count = 0;
    	synchronized(m_clientsonchainServices){
    		for(SonChainServicePeer peerNode : m_clientsonchainServices.values()){
    			if(peerNode.IsConnected()){
    				peerNode.sendMessage(transactionsMessage);
    				count ++;
    			}
    		}
    	}
    	m_logger.debug("sendTransactionsMessage end SendNodeCount:" + count);
    	return count;
    }
}
