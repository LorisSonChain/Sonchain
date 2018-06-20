
package sonchain.blockchain.client;
import sonchain.blockchain.base.*;
import sonchain.blockchain.core.*;
import sonchain.blockchain.data.*;
import sonchain.blockchain.db.StateSource;
import sonchain.blockchain.net.submit.TransactionTask;
import sonchain.blockchain.service.DataCenter;
import sonchain.blockchain.util.ByteArraySet;
import sonchain.blockchain.util.Value;
import sonchain.blockchain.validator.BlockHeaderRule;
import sonchain.blockchain.validator.BlockHeaderValidator;
import owchart.owlib.Base.CStr;
import owchart.owlib.Base.RefObject;

import java.util.*;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.log4j.Logger;
import org.bouncycastle.util.encoders.Hex;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;

public class SonChainServiceCT extends BaseServiceCT{

	public static final Logger m_logger = Logger.getLogger(SonChainServiceCT.class);
    
    public SonChainServiceCT()
    {
        SetServiceID(SERVICEID_sonchain);
    }

    protected static final int MAX_HASHES_TO_SEND = 65536;
    private final int SERVICEID_sonchain = 10000;
    public final int FUNCTIONID_GETHOSTS = 1;
    public final int FUNCTIONID_sonchain_SENDALL = 2;
    public final int FUNCTIONID_sonchain_SEND = 4;
    public final int FUNCTIONID_sonchain_ENTER = 6;

    public final int FUNCTIONID_RECEIVE_CALCULATE_POW = 11;
    public final int FUNCTIONID_CALCULATE_POW_COMPLETE = 12;
    public final int FUNCTIONID_CALCULATE_POW_COMPLETE_CONFIRM = 13;    
    public final int FUNCTIONID_SYNC_BLOCK_CHAIN_REQ = 14;
    public final int FUNCTIONID_SYNC_BLOCK_CHAIN_RTN = 15;
    
    public final int FUNCTIONID_STATUS = 20;
    public final int FUNCTIONID_NEW_BLOCK_HASHES = 21;
    public final int FUNCTIONID_TRANSACTIONS = 22;
    public final int FUNCTIONID_GET_BLOCK_HEADERS = 23;
    public final int FUNCTIONID_BLOCK_HEADERS = 24;
    public final int FUNCTIONID_GET_BLOCK_BODIES = 25;
    public final int FUNCTIONID_BLOCK_BODIES = 26;
    public final int FUNCTIONID_NEW_BLOCK = 27;
    public final int FUNCTIONID_GET_NODE_DATA = 28;
    public final int FUNCTIONID_NODE_DATA = 29;
    public final int FUNCTIONID_GET_RECEIPTS = 30;
    public final int FUNCTIONID_RECEIPTS = 31;
    
    private boolean m_connected;

    public boolean IsConnected()
    {
        return m_connected;
    }
    
    public void SetConnected(boolean connected)
    {
        m_connected = connected;
    }
    
    private SonChainHostInfo m_sonChainHostInfo;

    public SonChainHostInfo getSonChainHostInfo() {
		return m_sonChainHostInfo;
	}

	public void setSonChainHostInfo(SonChainHostInfo sonChainHostInfo) {
		m_sonChainHostInfo = sonChainHostInfo;
	}

	private boolean m_toServer;
    
    public boolean ToServer()
    {
        return m_toServer;
    }
    
    public void SetToServer(boolean toServer)
    {
        m_toServer = toServer;
    }

    private String m_serverIP;

    public String GetServerIP()
    {
        return m_serverIP;
    }
    
    public void SetServerIP(String serverIP)
    {
        m_serverIP = serverIP;
    }

    private int m_serverPort;

    public int GetServerPort()
    {
        return m_serverPort;
    }
    
    public void SetServerPort(int serverPort)
    {
        m_serverPort = serverPort;
    }
    
    protected GetBlockHeadersMessageWrapper m_headerRequest;
    protected long m_lastReqSentTime = 0;
    protected SonState m_sonState = SonState.INIT;
    protected PeerState m_peerState = PeerState.IDLE;
    protected boolean m_syncDone = false;
    protected PendingState m_pendingState;
    protected BlockChain m_blockchain;
    protected long m_connectedTime = System.currentTimeMillis();
    protected long m_processingTime = 0;
    private Map<Long, BlockHeaderValidator> m_validatorMap;
    
    /**
     * Header list sent in GET_BLOCK_BODIES message,
     * used to create blocks from headers and bodies
     * also, is useful when returned BLOCK_BODIES msg doesn't cover all sent hashes
     * or in case when peer is disconnected
     */
    protected final List<BlockHeaderWrapper> m_sentHeaders = Collections.synchronizedList(new ArrayList<BlockHeaderWrapper>());
    protected SettableFuture<List<Block>> m_futureBlocks;
    /**
     * Number and hash of best known remote block
     */
    protected BlockIdentifier m_bestKnownBlock;
    private StateSource m_stateSource;

    private List<byte[]> m_requestedReceipts;
    private SettableFuture<List<List<TransactionReceipt>>> m_requestReceiptsFuture;
    private Set<byte[]> m_requestedNodes;
    private SettableFuture<List<Pair<byte[], byte[]>>> m_requestNodesFuture;

    public int Enter()
    {
        try
        {
            Binary bw = new Binary();
            bw.WriteInt(DataCenter.m_serversonchainService.GetPort());
            bw.WriteInt(DataCenter.IsFull() ? 1 : 0);
            byte[] bytes = bw.GetBytes();
            int ret = Send(new CMessage(GetGroupID(), GetServiceID(), FUNCTIONID_sonchain_ENTER, 
            		GetSessionID(), DataCenter.GetsonchainRequestID(), GetSocketID(), 0, GetCompressType(), bytes.length, bytes));
            bw.Close();
            return ret;
        }
        catch(Exception ex)
        {
        }
        return -1;
    }
    
    public static int GetsonchainDatas(List<SonChainData> datas, RefObject<String> ips, byte[] body, int bodyLength)
    {
        try
        {
            Binary br = new Binary();
            br.Write(body, bodyLength);
            ips.argvalue = br.ReadString();
            int size = br.ReadInt();
            for (int i = 0; i < size; i++)
            {
                SonChainData data = new SonChainData();
                data.m_text = br.ReadString();
                datas.add(data);
            }
            br.Close();
        }
        catch(Exception ex)
        {
        }
        return 1;     
    }

    public static int GetHostInfos(ArrayList<SonChainHostInfo> datas,  RefObject<Integer> type, byte[] body, int bodyLength)
    {
        try
        {
            Binary br = new Binary();
            br.Write(body, bodyLength);
            int size = br.ReadInt();
            type.argvalue = br.ReadInt();
            for (int i = 0; i < size; i++)
            {
                SonChainHostInfo data = new SonChainHostInfo();
                data.m_ip = br.ReadString();
                data.m_serverPort = br.ReadInt();
                data.m_type = br.ReadInt();
                datas.add(data);
            }
            br.Close();
        }
        catch(Exception ex)
        {
            
        }
        return 1;     
    }

    @Override
    public void OnClientClose(int socketID, int localSID)
    {
        super.OnClientClose(socketID, localSID);
        m_connected = false;
    }

    @Override
    public void OnReceive(CMessage message)
    {
        super.OnReceive(message);
        if (DataCenter.IsFull() && message.m_functionID == FUNCTIONID_sonchain_SENDALL)
        {
            DataCenter.m_serversonchainService.SendAll(message);
        }
        if(message.m_functionID == FUNCTIONID_CALCULATE_POW_COMPLETE_CONFIRM)
        {
            DataCenter.m_serversonchainService.MineBlockConfirm(message);        	
        }
        else if (message.m_functionID == FUNCTIONID_GETHOSTS)
        {
            ArrayList<SonChainHostInfo> datas = new ArrayList<SonChainHostInfo>();
            int type = 0;
            RefObject<Integer> refType = new RefObject<Integer>(type);
            SonChainServiceCT.GetHostInfos(datas, refType, message.m_body, message.m_bodyLength);
            type = refType.argvalue;
            if (type != 2)
            {
                int datasSize = datas.size();
                for (int i = 0; i < datasSize; i++)
                {
                    SonChainHostInfo hostInfo = datas.get(i);
                    //全节点
                    if (hostInfo.m_type == 1)
                    {
                        if (hostInfo.m_ip != "127.0.0.1")
                        {
                            SonChainHostInfo serverHostInfo = new SonChainHostInfo();
                            serverHostInfo.m_ip = hostInfo.m_ip;
                            serverHostInfo.m_serverPort = hostInfo.m_serverPort;
                            serverHostInfo.m_type = hostInfo.m_type;
                            DataCenter.m_serversonchainService.AddServerHosts(serverHostInfo);
                            String newServer = hostInfo.m_ip + ":" + CStr.ConvertIntToStr(hostInfo.m_serverPort);
                            ArrayList<SonChainHostInfo> hostInfos = new ArrayList<SonChainHostInfo>();
                            String file = DataCenter.GetUserPath() + "/fullservers.txt"; 
                            if(CFileA.IsFileExist(file))
                            {
                                String content = "";
                                RefObject<String> refContent = new RefObject<String>(content);
                                CFileA.Read(file, refContent);
                                if(content.length() > 0)
                                {
                                    String []strs = content.split("[;]");
                                    int strsSize = strs.length;
                                    for(int s = 0; s < strsSize; s++)
                                    {
                                        SonChainHostInfo gsi = new SonChainHostInfo();
                                        String[] subStrs = strs[s].split("[:]");
                                        gsi.m_ip = subStrs[0];
                                        gsi.m_serverPort = CStr.ConvertStrToInt(subStrs[1]);
                                        gsi.m_type = 1;
                                        hostInfos.add(gsi);
                                    }
                                }
                            }
                            int hostInfosSize = hostInfos.size();
                            boolean contains = false;
                            for (int j = 0; j < hostInfosSize; j++)
                            {
                                SonChainHostInfo oldHostInfo = hostInfos.get(j);
                                String key = oldHostInfo.toString();
                                if (key.equals(newServer))
                                {
                                    contains = true;
                                    break;
                                }
                            }
                            if (!contains)
                            {
                                hostInfos.add(hostInfo);
                                int sSize = hostInfos.size();
                                String content = "";
                                for(int s = 0; s < hostInfosSize; s++)
                                {
                                    content += hostInfos.get(s).toString();
                                    if(s != hostInfosSize - 1)
                                    {
                                        content += ";";
                                    }
                                }
                            }
                            String key2 = hostInfo.toString();
                            if (!DataCenter.m_clientsonchainServices.containsKey(key2))
                            {
                                int socketID = BaseServiceCT.Connect(hostInfo.m_ip, hostInfo.m_serverPort);
                                if (socketID != -1)
                                {
                                    SonChainServiceCT clientsonchainService = new SonChainServiceCT();
                                    DataCenter.m_clientsonchainServices.put(key2, clientsonchainService);
                                    BaseServiceCT.AddService(clientsonchainService);
                                    clientsonchainService.SetConnected(true);
                                    clientsonchainService.SetToServer(type == 1);
                                    //clientsonchainService.RegisterListener(DataCenter.sonchainRequestID, new ListenerMessageCallBack(sonchainMessageCallBack));
                                    clientsonchainService.SetSocketID(socketID);
                                    clientsonchainService.Enter();
                                }
                            }
                            else
                            {
                                SonChainServiceCT clientsonchainService = DataCenter.m_clientsonchainServices.get(key2);
                                if (!clientsonchainService.IsConnected())
                                {
                                    int socketID = BaseServiceCT.Connect(hostInfo.m_ip, hostInfo.m_serverPort);
                                    if (socketID != -1)
                                    {
                                        clientsonchainService.SetConnected(true);
                                        clientsonchainService.SetSocketID(socketID);
                                        clientsonchainService.Enter();
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        else if (message.m_functionID == FUNCTIONID_RECEIVE_CALCULATE_POW)
        {
        	ReceiveTransaction(message);        	
        }
        else if (message.m_functionID == FUNCTIONID_CALCULATE_POW_COMPLETE)
        {
        
        }
        else if (message.m_functionID == FUNCTIONID_CALCULATE_POW_COMPLETE_CONFIRM)
        {
        
        }
        else if (message.m_functionID ==  FUNCTIONID_SYNC_BLOCK_CHAIN_REQ)
        {
        	SyncBlockChainReq(message);
        }
        else if (message.m_functionID == FUNCTIONID_SYNC_BLOCK_CHAIN_RTN)
        {
        	SyncBlockChainRtn(message);            	
        }
        //New Version
        else if (message.m_functionID == FUNCTIONID_STATUS)
        {
        	processStatus(message);
        }
        else if (message.m_functionID == FUNCTIONID_NEW_BLOCK_HASHES)
        {
        	processNewBlockHashes(message);        
        }
        else if (message.m_functionID == FUNCTIONID_TRANSACTIONS)
        {
        	processTransactions(message);        
        }
        else if (message.m_functionID == FUNCTIONID_GET_BLOCK_HEADERS)
        {
        	processGetBlockHeaders(message);        
        }
        else if (message.m_functionID == FUNCTIONID_BLOCK_HEADERS)
        {
        	processBlockHeaders(message);        
        }
        else if (message.m_functionID == FUNCTIONID_GET_BLOCK_BODIES)
        {
        	processGetBlockBodies(message);        
        }
        else if (message.m_functionID == FUNCTIONID_BLOCK_BODIES)
        {
        	processBlockBodies(message);        
        }
        else if (message.m_functionID == FUNCTIONID_NEW_BLOCK)
        {
        	processNewBlock(message);        
        }
        else if (message.m_functionID == FUNCTIONID_GET_NODE_DATA)
        {
        	processGetNodeData(message);        
        }
        else if (message.m_functionID == FUNCTIONID_NODE_DATA)
        {
        	processNodeData(message);        
        }
        else if (message.m_functionID == FUNCTIONID_GET_RECEIPTS)
        {
        	processGetReceipts(message);        
        }
        else if (message.m_functionID == FUNCTIONID_RECEIPTS)
        {
        	processReceipts(message);        
        }
        SendToListener(message);
    }
    
    public void processStatus(CMessage message){
        try
        {
        	m_logger.debug("processStatus start");
        	StatusMessage statusMessage = new StatusMessage(message.m_body);
            if (!Arrays.equals(statusMessage.getGenesisHash(), 
            		DataCenter.m_config.getGenesis().getHash())) {
            	m_sonState = SonState.STATUS_FAILED;
                return;
            }
//            if (statusMessage.getNetworkId() != m_config.getNetworkId()) {
//            	m_sonState = SonState.STATUS_FAILED;
//                return;
//            }

            // update bestKnownBlock info
            sendGetBlockHeaders(statusMessage.getBestHash(), 1, 0, false);      	 
        }
        catch(Exception ex)
        {
        	m_logger.error("processStatus error:" + ex.getMessage());  
        	m_logger.equals(ex);
        }
        finally
        {
        	m_logger.debug("processStatus end");        	
        }
    }

    private void updateBestBlock(Block block) {
        updateBestBlock(block.getHeader());
    }

    private void updateBestBlock(BlockHeader header) {
        if (m_bestKnownBlock == null || header.getNumber() > m_bestKnownBlock.getNumber()) 
        {
        	m_bestKnownBlock = new BlockIdentifier(header.getHash(), header.getNumber());
        }
    }
    
    private void updateBestBlock(List<BlockIdentifier> identifiers) {

        for (BlockIdentifier id : identifiers)
            if (m_bestKnownBlock == null || id.getNumber() > m_bestKnownBlock.getNumber()) {
            	m_bestKnownBlock = id;
            }
    }

    public boolean isHashRetrievingDone() {
        return m_peerState == PeerState.DONE_HASH_RETRIEVING;
    }

    public boolean isHashRetrieving() {
        return m_peerState == PeerState.HEADER_RETRIEVING;
    }

    public boolean hasStatusPassed() {
        return m_sonState.ordinal() > SonState.HASH_CONSTRAINTS_CHECK.ordinal();
    }

    public boolean hasStatusSucceeded() {
        return m_sonState == SonState.STATUS_SUCCEEDED;
    }

    public boolean isIdle() {
        return m_peerState == PeerState.IDLE;
    }
    
    public BlockIdentifier getBestKnownBlock() {
        return m_bestKnownBlock;
    }
    
    public void processNewBlockHashes(CMessage message){
        try
        {
        	m_logger.debug("processNewBlockHashes start");
        	NewBlockHashesMessage newBlockHashesMessage = new NewBlockHashesMessage(message.m_body);
            if(m_logger.isTraceEnabled()) {
            	m_logger.trace(
            			String.format(
                    "Peer {%s}: processing NewBlockHashes, size [{%d}]",
                    m_sonChainHostInfo.toString(),
                    newBlockHashesMessage.getBlockIdentifiers().size()));
            }
            
            List<BlockIdentifier> identifiers = newBlockHashesMessage.getBlockIdentifiers();

            if (identifiers.isEmpty()) {
            	return;
            }
            updateBestBlock(identifiers);
            // queueing new blocks doesn't make sense while Long sync is in progress
            if (!m_syncDone) {
            	return;
            }
            if (m_peerState != PeerState.HEADER_RETRIEVING) {
                long firstBlockAsk = Long.MAX_VALUE;
                long lastBlockAsk = 0;
                byte[] firstBlockHash = null;
                for (BlockIdentifier identifier : identifiers) {
                    long blockNumber = identifier.getNumber();
                    if (blockNumber < firstBlockAsk) {
                        firstBlockAsk = blockNumber;
                        firstBlockHash = identifier.getHash();
                    }
                    if (blockNumber > lastBlockAsk)  {
                        lastBlockAsk = blockNumber;
                    }
                }
                long maxBlocksAsk = lastBlockAsk - firstBlockAsk + 1;
                if (firstBlockHash != null && maxBlocksAsk > 0 && maxBlocksAsk < MAX_HASHES_TO_SEND) {
                    sendGetNewBlockHeaders(firstBlockHash, (int) maxBlocksAsk, 0, false);
                }
            }   	 
        }
        catch(Exception ex)
        {
        	m_logger.error("processNewBlockHashes error:" + ex.getMessage());  
        	m_logger.equals(ex);
        }
        finally
        {
        	m_logger.debug("processNewBlockHashes end");        	
        }
    }
    
    public void processTransactions(CMessage message){
        try
        {
        	m_logger.debug("processTransactions start");
        	TransactionsMessage msg = new TransactionsMessage(message.m_body);
            List<Transaction> txSet = msg.getTransactions();
            List<Transaction> newPending = m_pendingState.addPendingTransactions(txSet);
            if (!newPending.isEmpty()) {
                TransactionTask transactionTask = new TransactionTask(newPending);
                TransactionTaskExecutor.instance.submitTransaction(transactionTask);    
            }
        }
        catch(Exception ex)
        {
        	m_logger.error("processTransactions error:" + ex.getMessage());  
        	m_logger.equals(ex);
        }
        finally
        {
        	m_logger.debug("processTransactions end");        	
        }
    }
    
    public void processGetBlockHeaders(CMessage message){
        try
        {
        	m_logger.debug("processGetBlockHeaders start");
        	GetBlockHeadersMessage msg = new GetBlockHeadersMessage(message.m_body);
            List<BlockHeader> headers = m_blockchain.getListOfHeadersStartFrom(
                    msg.getBlockIdentifier(),
                    msg.getSkipBlocks(),
                    Math.min(msg.getMaxHeaders(), MAX_HASHES_TO_SEND),
                    msg.isReverse()
            );
            BlockHeadersMessage response = new BlockHeadersMessage(headers);
            byte[] bytes = response.getEncoded();
            int ret = Send(new CMessage(GetGroupID(), GetServiceID(), 
            		FUNCTIONID_GET_BLOCK_HEADERS, GetSessionID(), DataCenter.GetsonchainRequestID(), GetSocketID(), 
            		0, GetCompressType(), bytes.length, bytes));
            m_logger.debug("processGetBlockHeaders send result:" + ret);
        }
        catch(Exception ex)
        {
        	m_logger.error("processGetBlockHeaders error:" + ex.getMessage());  
        	m_logger.equals(ex);
        }
        finally
        {
        	m_logger.debug("processGetBlockHeaders end");        	
        }
    }
    
    public void processBlockHeaders(CMessage message){
    	try
    	{
	    	m_logger.debug("processGetBlockHeaders start");
	    	BlockHeadersMessage msg = new BlockHeadersMessage(message.m_body);
	        if(m_logger.isTraceEnabled()) {
	        	m_logger.trace(String.format(
	                "Peer {%s}: processing BlockHeaders, size [{%d}]",
                    m_sonChainHostInfo.toString(),
	                msg.getBlockHeaders().size())
	        	);
	        }	
	        GetBlockHeadersMessageWrapper request = m_headerRequest;
	        m_headerRequest = null;
	
	        if (!isValid(msg, request)) {	
	            dropConnection();
	            return;
	        }
	
	        List<BlockHeader> received = msg.getBlockHeaders();
	
	        if (m_sonState == SonState.STATUS_SENT || m_sonState == SonState.HASH_CONSTRAINTS_CHECK)
	        {
	            processInitHeaders(received);
	        }
	        else {
	            //syncStats.addHeaders(received.size());
	            request.getFutureHeaders().set(received);
	        }
	
	        m_processingTime += m_lastReqSentTime > 0 ? (System.currentTimeMillis() - m_lastReqSentTime) : 0;
	        m_lastReqSentTime = 0;
	        m_peerState = PeerState.IDLE;
    	}
    	catch(Exception ex)
    	{    	
        	m_logger.error("processBlockHeaders error:" + ex.getMessage());  
        	m_logger.equals(ex);
    	}
    	finally
    	{
        	m_logger.debug("processBlockHeaders end");    
    	}
    }
    
    public void processGetBlockBodies(CMessage message){
    	try
    	{
	    	m_logger.debug("processGetBlockHeaders start");
	    	GetBlockBodiesMessage msg = new GetBlockBodiesMessage(message.m_body);
	        List<byte[]> bodies = m_blockchain.getListOfBodiesByHashes(msg.getBlockHashes());	
	        
	        BlockBodiesMessage response = new BlockBodiesMessage(bodies);
            byte[] bytes = response.getEncoded();
            int ret = Send(new CMessage(GetGroupID(), GetServiceID(), 
            		FUNCTIONID_GET_BLOCK_BODIES, GetSessionID(), DataCenter.GetsonchainRequestID(), GetSocketID(), 
            		0, GetCompressType(), bytes.length, bytes));
            m_logger.debug("processGetBlockBodies send result:" + ret);
    	}
    	catch(Exception ex)
    	{    	
        	m_logger.error("processBlockHeaders error:" + ex.getMessage());  
        	m_logger.equals(ex);
    	}
    	finally
    	{
        	m_logger.debug("processBlockHeaders end");    
    	}
    }
    
    private boolean isValid(BlockBodiesMessage response) {
        return response.getBlockBodies().size() <= m_sentHeaders.size();
    }
    
    private List<Block> validateAndMerge(BlockBodiesMessage response) {
        // merging received block bodies with requested headers
        // the assumption is the following:
        // - response may miss any bodies present in the request
        // - response may not contain non-requested bodies
        // - order of response bodies should be preserved
        // Otherwise the response is assumed invalid and all bodies are dropped

        List<byte[]> bodyList = response.getBlockBodies();

        Iterator<byte[]> bodies = bodyList.iterator();
        Iterator<BlockHeaderWrapper> wrappers = m_sentHeaders.iterator();

        List<Block> blocks = new ArrayList<>(bodyList.size());
        List<BlockHeaderWrapper> coveredHeaders = new ArrayList<>(m_sentHeaders.size());

        boolean blockMerged = true;
        byte[] body = null;
        while (bodies.hasNext() && wrappers.hasNext()) {

            BlockHeaderWrapper wrapper = wrappers.next();
            if (blockMerged) {
                body = bodies.next();
            }

            Block b = new Block.Builder()
                    .withHeader(wrapper.getHeader())
                    .withBody(body)
                    .create();

            if (b == null) {
                blockMerged = false;
            } else {
                blockMerged = true;

                coveredHeaders.add(wrapper);
                blocks.add(b);
            }
        }

        if (bodies.hasNext()) {
            m_logger.info(String.format("Peer {%s}: invalid BLOCK_BODIES response:"
            		+ " at least one block body doesn't correspond to "
            		+ "any of requested headers: {%s}",
            		m_sonChainHostInfo.toString(), Hex.toHexString(bodies.next())));
            return null;
        }

        // remove headers covered by response
        m_sentHeaders.removeAll(coveredHeaders);

        return blocks;
    }
    
    public void processBlockBodies(CMessage message){
    	try
    	{
	    	m_logger.debug("processBlockBodies start");
	    	BlockBodiesMessage msg = new BlockBodiesMessage(message.m_body);
	    	if (m_logger.isTraceEnabled()){
	    		m_logger.trace(String.format(
	                "Peer {%s}: process BlockBodies, size [{%d}]",
	                m_sonChainHostInfo.toString(), msg.getBlockBodies().size()));
	    	}

	        if (!isValid(msg)) {

	            dropConnection();
	            return;
	        }

	        //syncStats.addBlocks(msg.getBlockBodies().size());

	        List<Block> blocks = null;
	        try {
	            blocks = validateAndMerge(msg);
	        } catch (Exception e) {
	        	m_logger.info(String.format(
	        			"Fatal validation error while processing block bodies "
	        			+ "from peer {%s}", 
	        			m_sonChainHostInfo.toString()));
	        }

	        if (blocks == null) {
	            // headers will be returned by #onShutdown()
	            dropConnection();
	            return;
	        }

	        m_futureBlocks.set(blocks);
	        m_futureBlocks = null;

	        m_processingTime += (System.currentTimeMillis() - m_lastReqSentTime);
	        m_lastReqSentTime = 0;
	        m_peerState = PeerState.IDLE;
    	}
    	catch(Exception ex)
    	{    	
        	m_logger.error("processBlockBodies error:" + ex.getMessage());  
        	m_logger.equals(ex);
    	}
    	finally
    	{
        	m_logger.debug("processBlockBodies end");    
    	}
    }
    
    public void processNewBlock(CMessage message){
    	try
    	{
	    	m_logger.debug("processNewBlock start");
	    	NewBlockMessage newBlockMessage = new NewBlockMessage(message.m_body);
	        Block newBlock = newBlockMessage.getBlock();
	        m_logger.debug(String.format("New block received: block.index [{%d}]", 
	        		newBlock.getNumber()));
	        updateBestBlock(newBlock);
//	        if (!syncManager.validateAndAddNewBlock(newBlock, channel.getNodeId())) {
//	            dropConnection();
//	        }
    	}
    	catch(Exception ex)
    	{    	
        	m_logger.error("processNewBlock error:" + ex.getMessage());  
        	m_logger.equals(ex);
    	}
    	finally
    	{
        	m_logger.debug("processNewBlock end");    
    	}
    }
    
    public void processGetNodeData(CMessage message){
    	try
    	{
	    	m_logger.debug("processGetNodeData start");
	    	GetNodeDataMessage getNodeDataMessage = new GetNodeDataMessage(message.m_body);
	        List<Value> nodeValues = new ArrayList<>();
	        for (byte[] nodeKey : getNodeDataMessage.getNodeKeys()) {
	            byte[] rawNode = m_stateSource.get(nodeKey);
	            if (rawNode != null) {
	                Value value = new Value(rawNode);
	                nodeValues.add(value);
	                m_logger.trace("processGetNodeData: " + Hex.toHexString(nodeKey).substring(0, 8) + " -> " + value);
	            }
	        }
	        NodeDataMessage msg = new NodeDataMessage(nodeValues);
	        byte[] bytes = msg.getEncoded();
	        int ret = Send(new CMessage(GetGroupID(), GetServiceID(), 
	        		FUNCTIONID_GET_NODE_DATA, GetSessionID(), DataCenter.GetsonchainRequestID(), GetSocketID(), 
	        		0, GetCompressType(), bytes.length, bytes));
	        m_logger.debug("processGetNodeData send result:" + ret);
//	        if (!syncManager.validateAndAddNewBlock(newBlock, channel.getNodeId())) {
//	            dropConnection();
//	        }
    	}
    	catch(Exception ex)
    	{    	
        	m_logger.error("processGetNodeData error:" + ex.getMessage());  
        	m_logger.equals(ex);
    	}
    	finally
    	{
        	m_logger.debug("processGetNodeData end");    
    	}
    }
    
    public void processNodeData(CMessage message){
    	try
    	{
	    	m_logger.debug("processNodeData start");
	    	NodeDataMessage nodeDataMessage = new NodeDataMessage(message.m_body);
	        if (m_requestedNodes == null) {
	            m_logger.debug("Received NodeDataMessage when requestedNodes == null. Dropping peer ");
	            dropConnection();
	        }

	        List<Pair<byte[], byte[]>> ret = new ArrayList<>();
	        if(nodeDataMessage.getDataList().isEmpty()) {
	            String err = "Received NodeDataMessage contains empty node data. Dropping peer ";
	            dropUselessPeer(err);
	            return;
	        }

	        for (Value nodeVal : nodeDataMessage.getDataList()) {
	            byte[] hash = nodeVal.hash();
	            if (!m_requestedNodes.contains(hash)) {
	                String err = "Received NodeDataMessage contains non-requested node with hash :"
	                		+ Hex.toHexString(hash) + " . Dropping peer ";
	                dropUselessPeer(err);
	                return;
	            }
	            ret.add(Pair.of(hash, nodeVal.encode()));
	        }
	        m_requestNodesFuture.set(ret);

	        m_requestedNodes = null;
	        m_requestNodesFuture = null;
	        m_processingTime += (System.currentTimeMillis() - m_lastReqSentTime);
	        m_lastReqSentTime = 0;
	        m_peerState = PeerState.IDLE;
//	        if (!syncManager.validateAndAddNewBlock(newBlock, channel.getNodeId())) {
//	            dropConnection();
//	        }
    	}
    	catch(Exception ex)
    	{    	
        	m_logger.error("processNodeData error:" + ex.getMessage());  
        	m_logger.equals(ex);
    	}
    	finally
    	{
        	m_logger.debug("processNodeData end");    
    	}
    }
    
    public void processGetReceipts(CMessage message){
    	try
    	{
	    	m_logger.debug("processGetReceipts start");
	    	GetReceiptsMessage getReceiptsMessage = new GetReceiptsMessage(message.m_body);
	        if (m_logger.isTraceEnabled())
	        {
	        	m_logger.trace(String.format(
	                "Peer {}: processing GetReceipts, size [{%d}]",
	                getReceiptsMessage.getBlockHashes().size()));
	        }

	        List<List<TransactionReceipt>> receipts = new ArrayList<>();
	        for (byte[] blockHash : getReceiptsMessage.getBlockHashes()) {
	            Block block = DataCenter.getSonChainImpl().getBlockChain().getBlockByHash(blockHash);
	            if (block == null) continue;

	            List<TransactionReceipt> blockReceipts = new ArrayList<>();
	            for (Transaction transaction : block.getTransactionsList()) {
	                TransactionInfo transactionInfo = DataCenter.getSonChainImpl().
	                		getBlockChain().getTransactionInfo(transaction.getHash());
	                if (transactionInfo == null) break;
	                blockReceipts.add(transactionInfo.getReceipt());
	            }
	            receipts.add(blockReceipts);
	        }
	        
	        ReceiptsMessage msg = new ReceiptsMessage(receipts);
	        byte[] bytes = msg.getEncoded();
	        int ret = Send(new CMessage(GetGroupID(), GetServiceID(), 
	        		FUNCTIONID_GET_RECEIPTS, GetSessionID(), DataCenter.GetsonchainRequestID(), GetSocketID(), 
	        		0, GetCompressType(), bytes.length, bytes));
	        m_logger.debug("processGetNodeData send result:" + ret);
    	}
    	catch(Exception ex)
    	{    	
        	m_logger.error("processNewBlock error:" + ex.getMessage());  
        	m_logger.equals(ex);
    	}
    	finally
    	{
        	m_logger.debug("processNewBlock end");    
    	}
    }
    
    public void processReceipts(CMessage message){
    	try
    	{
	    	m_logger.debug("processReceipts start");
	    	ReceiptsMessage receiptsMessage = new ReceiptsMessage(message.m_body);
	    	if (m_requestedReceipts == null) {
	    		m_logger.debug("Received ReceiptsMessage when requestedReceipts == null. Dropping peer ");
	            dropConnection();
	        }

	        if (m_logger.isTraceEnabled()) 
	        {
	        	m_logger.trace(String.format(
	                "Peer {}: processing Receipts, size [{}]",
	                receiptsMessage.getReceipts().size()));
	        }

	        List<List<TransactionReceipt>> receipts = receiptsMessage.getReceipts();
	        m_requestReceiptsFuture.set(receipts);
	        
	        m_requestedReceipts = null;
	        m_requestReceiptsFuture = null;
	        m_processingTime += (System.currentTimeMillis() - m_lastReqSentTime);
	        m_lastReqSentTime = 0;
	        m_peerState = PeerState.IDLE;
    	}
    	catch(Exception ex)
    	{    	
        	m_logger.error("processReceipts error:" + ex.getMessage());  
        	m_logger.equals(ex);
    	}
    	finally
    	{
        	m_logger.debug("processReceipts end");    
    	}
    }

    protected synchronized void processInitHeaders(List<BlockHeader> received) {
        final BlockHeader blockHeader = received.get(0);
        final long blockNumber = blockHeader.getNumber();
        if (m_sonState == SonState.STATUS_SENT) {
            updateBestBlock(blockHeader);
            m_logger.trace(String.format("Peer {%s}: init request succeeded, "
            		+ "best known block {%s}",
            		m_sonChainHostInfo.toString(), m_bestKnownBlock.toString()));
            // checking if the peer has expected block hashes
            m_sonState = SonState.HASH_CONSTRAINTS_CHECK;
            
            m_validatorMap = Collections.synchronizedMap(new HashMap<Long, BlockHeaderValidator>());
            List<Pair<Long, BlockHeaderValidator>> validators = DataCenter.m_config.
                    getConfigForBlock(blockNumber).headerValidators();
            for (Pair<Long, BlockHeaderValidator> validator : validators) {
                if (validator.getLeft() <= getBestKnownBlock().getNumber()) {
                	m_validatorMap.put(validator.getLeft(), validator.getRight());
                }
            }

            m_logger.trace("Peer " + m_sonChainHostInfo.toString() + ": Requested " + m_validatorMap.size() +
                    " headers for hash check: " + m_validatorMap.keySet());
            requestNextHashCheck();

        } else {
            BlockHeaderValidator validator = m_validatorMap.get(blockNumber);
            if (validator != null) {
                BlockHeaderRule.ValidationResult result = validator.validate(blockHeader);
                if (result.m_success) {
                	m_validatorMap.remove(blockNumber);
                    requestNextHashCheck();
                } else {
                	m_logger.debug(String.format("Peer {%s}: wrong fork ({%s}). "
                			+ "Drop the peer and reduce reputation.", 
                			m_sonChainHostInfo.toString(), result.m_error));
                }
            }
        }
        if (m_validatorMap.isEmpty()) {
        	m_sonState = SonState.STATUS_SUCCEEDED;
            m_logger.trace(String.format(
            		"Peer {%s}: all validations passed", m_sonChainHostInfo.toString()));
        }
    }

    private void requestNextHashCheck() {
       if (!m_validatorMap.isEmpty()) {
            final Long checkHeader = m_validatorMap.keySet().iterator().next();
            sendGetBlockHeaders(checkHeader, 1, false);
            m_logger.trace(String.format("Peer {%s}: Requested #{%s} header for hash check.",
            		m_sonChainHostInfo.toString(), checkHeader.toString()));
        }
    }

    protected boolean isValid(BlockHeadersMessage response, GetBlockHeadersMessageWrapper requestWrapper) {
        GetBlockHeadersMessage request = requestWrapper.getMessage();
        List<BlockHeader> headers = response.getBlockHeaders();
        // max headers
        if (headers.size() > request.getMaxHeaders()) {
            if (m_logger.isInfoEnabled())
            {
            	m_logger.info(String.format("Peer {%s}: invalid response to {}, "
            			+ "exceeds maxHeaders limit, headers count={%d}",
            			m_sonChainHostInfo.toString(), request, headers.size()));
            }
            return false;
        }
        // emptiness against best known block
        if (headers.isEmpty()) {
            // initial call after handshake
            if (m_sonState == SonState.STATUS_SENT || m_sonState == SonState.HASH_CONSTRAINTS_CHECK) {
                if (m_logger.isInfoEnabled()) 
                {
                	m_logger.info(String.format("Peer {%s}: invalid response to initial {%s}, "
                			+ "empty",
                			m_sonChainHostInfo.toString(), request.toString()));
                }
                return false;
            }
            if (request.getBlockHash() == null &&
                    request.getBlockNumber() <= m_bestKnownBlock.getNumber()) {

                if (m_logger.isInfoEnabled()) 
                {
                	m_logger.info(String.format("Peer {%s}: invalid response to {%s},"
                			+ " it's empty while bestKnownBlock is {%s}",
                			m_sonChainHostInfo.toString(), request.toString(),
                			m_bestKnownBlock.toString()));
                }
                return false;
            }
            return true;
        }
        // first header
        BlockHeader first = headers.get(0);
        if (request.getBlockHash() != null) {
            if (!Arrays.equals(request.getBlockHash(), first.getHash())) {
                if (m_logger.isInfoEnabled()) 
                {
                	m_logger.info(String.format("Peer {%s}: invalid response to {%s},"
                			+ " first header is invalid {%s}",
                			m_sonChainHostInfo.toString(), request.toString(), first.toString()));
                }
                return false;
            }
        } else {
            if (request.getBlockNumber() != first.getNumber()) {

                if (m_logger.isInfoEnabled())
                {
                	m_logger.info(String.format("Peer {%s}: invalid response to {%s}, "
                			+ "first header is invalid {%s}",
                        m_sonChainHostInfo.toString(), request.toString(), first.toString()));
                }
                return false;
            }
        }

        // skip following checks in case of NEW_BLOCK_HASHES handling
        if (requestWrapper.isNewHashesHandling()) return true;

        // numbers and ancestors
        int offset = 1 + request.getSkipBlocks();
        if (request.isReverse()) offset = -offset;

        for (int i = 1; i < headers.size(); i++) {

            BlockHeader cur = headers.get(i);
            BlockHeader prev = headers.get(i - 1);

            long num = cur.getNumber();
            long expectedNum = prev.getNumber() + offset;

            if (num != expectedNum) {
                if (m_logger.isInfoEnabled()) 
                {
                	m_logger.info(String.format("Peer {%s}: invalid response to {%s}, got #{%d}, expected #{%d}",
                			m_sonChainHostInfo.toString(), request.toString(), num, expectedNum));
                }
                return false;
            }

            if (request.getSkipBlocks() == 0) {
                BlockHeader parent;
                BlockHeader child;
                if (request.isReverse()) {
                    parent = cur;
                    child = prev;
                } else {
                    parent = prev;
                    child = cur;
                }
                if (!Arrays.equals(child.getParentHash(), parent.getHash())) {
                    if (m_logger.isInfoEnabled()) 
                    {
                    	m_logger.info(
                    			String.format("Peer {%s}: invalid response to {%s}, got parent hash "
                    					+ "{%s} for #{%d}, expected {%s}",
                            m_sonChainHostInfo.toString(), request, Hex.toHexString(child.getParentHash()),
                            prev.getNumber(), Hex.toHexString(parent.getHash()))
                    	);
                    }
                    return false;
                }
            }
        }
        return true;
    }

    private void dropUselessPeer(String err) {
        m_logger.debug(err);
        m_requestNodesFuture.setException(new RuntimeException(err));
        dropConnection();
    }
    
    public void ReceiveTransaction(CMessage message)
    {
    	try
    	{
	        Binary br = new Binary(); 
	        br.Write(message.m_body, message.m_bodyLength);
	        int len = br.ReadInt();
	        List<Transaction> trans = new ArrayList<Transaction>();
	        for(int i = 0; i < len; i++)
	        {
	        	Transaction tran = new Transaction();
	        	//tran.m_type = TransactionType.ContractCreation;
	        	//tran.m_value = BigInteger.valueOf((long)br.ReadDouble());
	        	//tran.m_fee = BigInteger.valueOf((long)br.ReadDouble());
	        	//tran.m_remark = br.ReadString();
	        	//tran.SetHash(tran.GetTxHash());
		        //System.out.println(" receive transaction remark :" + tran.m_remark);
	        	trans.add(tran);
	        }
	        br.Close();
//        	Block lastBlock = DataCenter.GetBlockChain().GetLastBlock();
//	        Block block = DataCenter.GetBlockChain().MineBlock(lastBlock, trans);
//	        ProofOfWork pow = ProofOfWork.newProofOfWork((BlockHeader)block);
//	        boolean isValidate = pow.validate();
//            System.out.println("Pow valid: " +  pow.validate() + "\n");
//            if(isValidate)
//            {            	
//            	DataCenter.GetBlockChain().AddBlock(block);
//            	String jsonBlock = block.ToJson();
//            	RedisSeq.SetBlockChain(DigestUtils.sha256Hex(block.m_prevBlockHash.getValue().toByteArray()), jsonBlock);
//            }	        
    	}
    	catch(Exception e)
    	{	
    	} 	
    }
    
    public int Send(SonChainData data)
    {
        ArrayList<SonChainData> datas = new ArrayList<SonChainData>();
        datas.add(data);
        String ips = "";
        int ret = Send(FUNCTIONID_sonchain_SEND, DataCenter.GetsonchainRequestID(), ips, datas);
        datas.clear();
        return ret > 0 ? 1 : 0;
    }

    public int Send(int functionID, int requestID, String ips, List<SonChainData> datas)
    {
        try
        {
            Binary bw = new Binary();
            int dataSize = datas.size();
            bw.WriteString(ips);
            bw.WriteInt(dataSize);
            for (int i = 0; i < dataSize; i++)
            {
                SonChainData data = datas.get(i);
                bw.WriteString(data.m_text); 
            }
            byte[] bytes = bw.GetBytes();
            int ret = Send(new CMessage(GetGroupID(), GetServiceID(), functionID, GetSessionID(), requestID, GetSocketID(), 0, GetCompressType(), bytes.length, bytes));
            bw.Close();
            return ret;
        }
        catch(Exception ex)
        {
            return -1;
        }
    }
    
    public int SendAll(SonChainData data)
    {
        ArrayList<SonChainData> datas = new ArrayList<SonChainData>();
        datas.add(data);
        String ips = "";
        int ret = Send(FUNCTIONID_sonchain_SENDALL, DataCenter.GetsonchainRequestID(), ips, datas);
        datas.clear();
        return ret > 0 ? 1 : 0;
    }
    
    public int SyncBlockChain()
    {
        try
        {
	    	//BlockChain1 blockChain = DataCenter.GetBlockChain();
	    	//Block block = blockChain.GetLastBlock();
	    	String preHashKey = "";
	    	//if(block != null)
	    	{
	    		//preHashKey = DigestUtils.sha256Hex(block.m_hash.getValue().toByteArray());
	    	}
            Binary bw = new Binary();
            bw.WriteString(preHashKey);
            byte[] bytes = bw.GetBytes();
            int ret = Send(new CMessage(GetGroupID(), GetServiceID(), FUNCTIONID_SYNC_BLOCK_CHAIN_REQ, 
            		GetSessionID(), BaseServiceCT.GetRequestID(), GetSocketID(), 0, GetCompressType(), bytes.length, bytes));
            bw.Close();
            return ret;
        }
        catch(Exception ex)
        {
        	return 0;
        }    	
    }
    public int SyncBlockChainReq(CMessage message)
    {
    	return 1;
    	/**
        try
        {
        	Binary br = new Binary();
        	br.Write(message.m_body, message.m_bodyLength);
        	String preHashKey = br.ReadString();
        	List<String> lstHashs = BlockChain1.GetBlocks(preHashKey);
        	int size = lstHashs.size();
        	br.Close();
        	Binary bw = new Binary();
        	bw.WriteInt(size);
        	for(String block : lstHashs)
        	{
        		bw.WriteString(block);
        	}
            byte[] bytes = bw.GetBytes();
            int ret = Send(new CMessage(GetGroupID(), GetServiceID(), FUNCTIONID_SYNC_BLOCK_CHAIN_RTN, 
            		GetSessionID(), BaseServiceCT.GetRequestID(), GetSocketID(), 0, GetCompressType(), bytes.length, bytes));
            bw.Close();
            return ret;        	 
        }
        catch(Exception ex)
        {
        	return 0;
        }**/
    }
    
    public int SyncBlockChainRtn(CMessage message)
    {
    	return 1;
//        try
//        {
//        	Binary br = new Binary();
//        	br.Write(message.m_body, message.m_bodyLength);
//        	int size = br.ReadInt();
//        	List<String> lstHashs = new ArrayList<String>();
//        	for(int i = 0 ; i < size; i ++)
//        	{
//        		lstHashs.add(br.ReadString());
//        	}
//        	return BlockChain1.SyncBlocks(lstHashs);
//        }
//        catch(Exception ex)
//        {
//        	return 0;
//        }
    }
    
    public ListenableFuture<List<BlockHeader>> sendGetBlockHeaders(long blockNumber, int maxBlocksAsk, boolean reverse)
    {
        if (m_sonState == SonState.STATUS_SUCCEEDED && m_peerState != PeerState.IDLE) {
        	return null;
        }
        if(m_logger.isTraceEnabled()) {
        	m_logger.trace(String.format(
                "Peer {%s}: queue GetBlockHeaders, blockNumber [{%d}], maxBlocksAsk [{%s}]",
                m_sonChainHostInfo.toString(),
                blockNumber,
                maxBlocksAsk)
        	);
        }
        if (m_headerRequest != null) {
            throw new RuntimeException("The peer is waiting for headers response: " + this);
        }
        GetBlockHeadersMessage headersRequest = new GetBlockHeadersMessage(blockNumber, null, maxBlocksAsk, 0, reverse);
        GetBlockHeadersMessageWrapper messageWrapper = new GetBlockHeadersMessageWrapper(headersRequest);
        m_headerRequest = messageWrapper;
        sendNextHeaderRequest();
        return messageWrapper.getFutureHeaders();
    }
    
    public synchronized ListenableFuture<List<BlockHeader>> sendGetBlockHeaders(byte[] blockHash, int maxBlocksAsk, 
    		int skip, boolean reverse) {
        return sendGetBlockHeaders(blockHash, maxBlocksAsk, skip, reverse, false);
    }

    protected synchronized void sendGetNewBlockHeaders(byte[] blockHash, int maxBlocksAsk, int skip, boolean reverse) {
        sendGetBlockHeaders(blockHash, maxBlocksAsk, skip, reverse, true);
    }

    public ListenableFuture<List<BlockHeader>> sendGetBlockHeaders(byte[] blockHash, int maxBlocksAsk,
    		int skip, boolean reverse, boolean newHashes)
    {
        if (m_peerState != PeerState.IDLE) {
        	return null;
        }        
    	if(m_logger.isTraceEnabled()) {
    		m_logger.trace(String.format(
                "Peer {%s}: queue GetBlockHeaders, blockHash [{%s}], maxBlocksAsk [{%d}], "
                + "skip[{%d}], reverse [{%b}]",
                m_sonChainHostInfo.toString(),
                "0x" + Hex.toHexString(blockHash).substring(0, 8),
                maxBlocksAsk, skip, reverse)
    		);
    	}
        if (m_headerRequest != null) {
            throw new RuntimeException("The peer is waiting for headers response: " + this);
        }
        GetBlockHeadersMessage headersRequest = new GetBlockHeadersMessage(0, blockHash, maxBlocksAsk, skip, reverse);
        GetBlockHeadersMessageWrapper messageWrapper = new GetBlockHeadersMessageWrapper(headersRequest, newHashes);
        m_headerRequest = messageWrapper;
        sendNextHeaderRequest();        
        m_lastReqSentTime = System.currentTimeMillis();
        return messageWrapper.getFutureHeaders();
    }

    /**
     *  Send GET_BLOCK_BODIES message to the peer
     */
    public ListenableFuture<List<Block>> sendGetBlockBodies(List<BlockHeaderWrapper> headers)
    {
        if (m_peerState != PeerState.IDLE) {
        	return null;
        }
        m_peerState = PeerState.BLOCK_RETRIEVING;
        m_sentHeaders.clear();
        m_sentHeaders.addAll(headers);

        if(m_logger.isTraceEnabled()) {
        	m_logger.trace(String.format(
                "Peer {%s}: send GetBlockBodies, hashes.count [{%d}]",
                m_sonChainHostInfo.toString(),
                m_sentHeaders.size())
        	);
        }

        List<byte[]> hashes = new ArrayList<>(headers.size());
        for (BlockHeaderWrapper header : headers) {
            hashes.add(header.getHash());
        }
        GetBlockBodiesMessage msg = new GetBlockBodiesMessage(hashes);
        byte[] bytes = msg.getEncoded();
        int ret = Send(new CMessage(GetGroupID(), GetServiceID(), 
        		FUNCTIONID_GET_BLOCK_BODIES, GetSessionID(), DataCenter.GetsonchainRequestID(), GetSocketID(), 
        		0, GetCompressType(), bytes.length, bytes));
        m_logger.debug("sendGetBlockBodies send result:" + ret);
        m_lastReqSentTime = System.currentTimeMillis();
        m_futureBlocks = SettableFuture.create();
        return m_futureBlocks;
    }  
    
    public void dropConnection()
    {
    }
       
    public synchronized void sendNewBlockHashes(Block block) {
        BlockIdentifier identifier = new BlockIdentifier(block.getHash(), block.getNumber());
        NewBlockHashesMessage msg = new NewBlockHashesMessage(Collections.singletonList(identifier));
        
        byte[] bytes = msg.getEncoded();
        int ret = Send(new CMessage(GetGroupID(), GetServiceID(), 
        		FUNCTIONID_NEW_BLOCK_HASHES, GetSessionID(), DataCenter.GetsonchainRequestID(), GetSocketID(), 
        		0, GetCompressType(), bytes.length, bytes));
        m_logger.debug("sendNewBlockHashes send result:" + ret);
    }

    public synchronized void sendTransaction(List<Transaction> txs) {
        TransactionsMessage msg = new TransactionsMessage(txs);
        byte[] bytes = msg.getEncoded();
        int ret = Send(new CMessage(GetGroupID(), GetServiceID(), 
        		FUNCTIONID_TRANSACTIONS, GetSessionID(), DataCenter.GetsonchainRequestID(), GetSocketID(), 
        		0, GetCompressType(), bytes.length, bytes));
        m_logger.debug("sendTransaction send result:" + ret);
    }

    protected synchronized void sendNextHeaderRequest() {
        GetBlockHeadersMessageWrapper wrapper = m_headerRequest;
        if (wrapper == null || wrapper.isSent()){
        	return;
        }
        wrapper.send();
        byte[] bytes = wrapper.getMessage().getEncoded();
        int ret = Send(new CMessage(GetGroupID(), GetServiceID(), 
        		FUNCTIONID_GET_BLOCK_HEADERS, GetSessionID(), DataCenter.GetsonchainRequestID(), GetSocketID(), 
        		0, GetCompressType(), bytes.length, bytes));
        m_logger.debug("sendNextHeaderRequest send result:" + ret);
        m_lastReqSentTime = System.currentTimeMillis();
    }

    public synchronized ListenableFuture<List<Pair<byte[], byte[]>>> requestTrieNodes(List<byte[]> hashes) {
        if (m_peerState != PeerState.IDLE) {
        	return null;
        }

        GetNodeDataMessage msg = new GetNodeDataMessage(hashes);
        m_requestedNodes = new ByteArraySet();
        m_requestedNodes.addAll(hashes);
        m_requestNodesFuture = SettableFuture.create();
        byte[] bytes = msg.getEncoded();
        Send(new CMessage(GetGroupID(), GetServiceID(), 
        		FUNCTIONID_GET_NODE_DATA, GetSessionID(), DataCenter.GetsonchainRequestID(), GetSocketID(), 
        		0, GetCompressType(), bytes.length, bytes));
        m_lastReqSentTime = System.currentTimeMillis();
        m_peerState = PeerState.NODE_RETRIEVING;
        return m_requestNodesFuture;
    }

    public synchronized ListenableFuture<List<List<TransactionReceipt>>> requestReceipts(List<byte[]> hashes) {
        if (m_peerState != PeerState.IDLE) {
        	return null;
        }

        GetReceiptsMessage msg = new GetReceiptsMessage(hashes);
        m_requestedReceipts = hashes;
        m_peerState = PeerState.RECEIPT_RETRIEVING;

        m_requestReceiptsFuture = SettableFuture.create();
        byte[] bytes = msg.getEncoded();
        Send(new CMessage(GetGroupID(), GetServiceID(), 
        		FUNCTIONID_GET_RECEIPTS, GetSessionID(), DataCenter.GetsonchainRequestID(), GetSocketID(), 
        		0, GetCompressType(), bytes.length, bytes));
        m_lastReqSentTime = System.currentTimeMillis();
        return m_requestReceiptsFuture;
    }
}
