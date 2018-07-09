package sonchain.blockchain.consensus;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.bouncycastle.util.encoders.Hex;

import sonchain.blockchain.core.Block;
import sonchain.blockchain.core.BlockChain;
import sonchain.blockchain.core.BlockChainImpl;
import sonchain.blockchain.core.BlockSummary;
import sonchain.blockchain.core.ImportResult;
import sonchain.blockchain.core.Transaction;
import sonchain.blockchain.data.TransactionsMessage;
import sonchain.blockchain.db.RepositoryImpl;
import sonchain.blockchain.facade.SonChainImpl;
import sonchain.blockchain.net.SonMessageCodes;
import sonchain.blockchain.service.DataCenter;
import sonchain.blockchain.util.ByteArrayMap;
import sonchain.blockchain.util.FastByteComparisons;
import sonchain.blockchain.util.Numeric;

public class ConsensusService{

	public static final Logger m_logger = Logger.getLogger(ConsensusService.class);
	public BlockChain m_blockChain = null;
	public Date m_blockReceivedTime;
	public ConsensusContext m_context = new ConsensusContext();
	public boolean m_started = false;
	public int m_timerHeight = 0;
	public int m_timerView = 0;
	
	public static ScheduledExecutorService m_statTimer =
        Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
             public Thread newThread(Runnable r) {
                 return new Thread(r, "ConsensusServiceStatTimer");
          }
    });
	
	public ConsensusService(){
    	try
    	{
    		m_blockChain = DataCenter.getSonChainImpl().getBlockChain();
    	}
    	catch(Exception ex){
    		m_logger.error(ex.getMessage());
    	}
	}
	
	public boolean addTransaction(Transaction tx, boolean verify){		
		m_logger.debug("addTransaction start: TransactionInfo:" + tx.toString() + " verifyStatus:" + verify);		
		if(m_blockChain.containsTransaction(m_blockChain.getBestBlockHash(), tx.getHash())
				|| (verify && !tx.verify()) || !checkPolicy(tx))
		{
			m_logger.debug("reject tx: TransactionInfo:" + tx.toString());
			requestChangeView();
			return false;
		}
		m_context.m_transactions.put(tx.getHash(), tx);
		if(m_context.m_lstTransaction.size() == m_context.m_transactions.size()){
			
			SonChainPeerNode[] nodes = m_blockChain.getValidators();
            if (nodes.length > m_context.m_myIndex 
            		&& FastByteComparisons.equal(Numeric.hexStringToByteArray(nodes[m_context.m_myIndex].getAddress()), 
            				m_context.m_nextConsensus))
            {
            	m_logger.debug("send perpare response");
            	m_context.m_state |= ConsensusContext.SignatureSent;
            	//m_context.Signatures[context.MyIndex] = m_context.MakeHeader().Sign(context.KeyPair);
            	m_context.m_signatures[m_context.m_myIndex] = m_context.makeHeader().getHash();
                signAndRelay(m_context.makePrepareResponse(m_context.m_signatures[m_context.m_myIndex]));
                checkSignatures();
            }
            else
            {
                requestChangeView();
                return false;
            }
		}
		return true;
	}
	
	public void blockChainPersistCompleted(BlockSummary block){
		m_logger.debug("BlockChainPersistCompleted BlockInfo:" + block.toString());
		Calendar now = Calendar.getInstance(Locale.CHINA);
		m_blockReceivedTime = now.getTime();
		initializeConsensus(0);
	}
    
    private void checkExpectedView(int view_number){
    	m_logger.debug(" checkExpectedView start viewNumber:" + view_number);
    	if(m_context.m_viewNumber == view_number){
    		return;
    	}
    	int count = 0;
    	int size = m_context.m_expectedView.length;
    	for(int i = 0; i < size; i++){
    		if(m_context.m_expectedView[i] == view_number){
    			count++;
    		}
    	}
    	if(count >= m_context.getMinAllowCount()){
    		initializeConsensus(view_number);
    	}
    	m_logger.debug(" checkExpectedView end viewNumber:" + view_number);
    }
	
    protected boolean checkPolicy(Transaction tx)
    {
    	m_logger.debug(" checkPolicy start Transaction:" + tx.toString());
        return true;
    }
    
    private void checkSignatures()
    {
    	m_logger.debug(" checkSignatures start.");
    	int count = 0;
    	int size = m_context.m_signatures.length;
    	for(int i = 0; i < size; i++){
    		if(m_context.m_signatures[i] != null)
    		{
    			count++;
    		}
    	}
    	boolean isHasTransHash = true;
    	for(Transaction tx : m_context.m_lstTransaction){
    		if(!m_context.m_transactions.containsKey(tx.getHash())){
    			isHasTransHash = false;
    			break;
    		}
    	}
	    m_logger.info(String.format("checkSignatures the count of not null is {%d}, "
	    		+ "the min allow count is {%d}. HasTransHash is {%b}", count, m_context.getMinAllowCount(), isHasTransHash));
    	if(count >= m_context.getMinAllowCount() && isHasTransHash){
    	    m_logger.debug(" checkSignatures start. 1111");
            Block block = m_context.makeHeader();
            List<Transaction> trans = new ArrayList<Transaction>();
        	for(Transaction tx : m_context.m_lstTransaction){
        		if(m_context.m_transactions.containsKey(tx.getHash())){
        			trans.add(tx);
        		}
        	}
            block.setTransactionsList(trans);
            m_logger.debug("checkSignatures relay block: blockInfo:" + block.toString());
            if (!DataCenter.getNodeManager().relayBlock(block)){
            	m_logger.debug("checkSignatures reject block: blockInfo:" + block.toString());
            }
            m_context.m_state |= ConsensusContext.BlockSent;
    	}
    }
    
    public void dispose(){
    	m_logger.debug("OnStop start.");
    	if(m_statTimer != null)
    	{
    		m_statTimer.shutdown();
    	}
    	if(m_started){
    		//TODO
            //LocalNode.InventoryReceiving -= LocalNode_InventoryReceiving;
            //LocalNode.InventoryReceived -= LocalNode_InventoryReceived;
    	}
    	m_logger.debug("OnStop end.");    	
    }
    
    private void fillContext()
    {
    	m_logger.debug("fillContext start.");
    	List<Transaction> transaction = DataCenter.getSonChainImpl().getPendingStateTransactions();
    	m_context.m_lstTransaction.clear();
    	m_context.m_transactions.clear();
    	m_context.m_lstTransaction.addAll(transaction);
    	for(Transaction trans : transaction){
    		m_context.m_transactions.put(trans.getEncoded(), trans);
    	}
    	m_context.m_nextConsensus = DataCenter.m_config.getNodeAddress();
    	m_logger.debug("fillContext end.");
    }

    public void requestChangeView()
    {
    	m_logger.debug("requestChangeView start.");
    	
        m_context.m_state |= m_context.ViewChanging;
        m_context.m_expectedView[m_context.m_myIndex]++;
        m_logger.debug(String.format("request change view: height={%d} view={%d}"
        		+ " nv={%d} state={%s}", m_context.m_blockNumber, m_context.m_viewNumber
        		, m_context.m_expectedView[m_context.m_myIndex], String.valueOf(m_context.m_state)));
        
        //m_statTimer.Change(TimeSpan.FromSeconds(DataCenter.m_sonChainImpl.SecondsPerBlock << 
        //		(m_context.m_expectedView[m_context.m_myIndex] + 1)), Timeout.InfiniteTimeSpan);

        m_statTimer.schedule(new ConsensusServiceTaskExecutor(), 
        		DataCenter.getSonChainImpl().SecondsPerBlock << (m_context.m_expectedView[m_context.m_myIndex] + 1), 
        		TimeUnit.SECONDS);
        signAndRelay(m_context.makeChangeView());
        checkExpectedView(m_context.m_expectedView[m_context.m_myIndex]);
    	m_logger.debug("requestChangeView end.");
    }
    
    private void initializeConsensus(int view_number){
    	m_logger.debug("initializeConsensus start. view_number:" + view_number);
    	synchronized(m_context)
    	{
    		if(view_number == 0){
    	    	m_logger.debug("initializeConsensus reset.");
    			m_context.reset();
    		}else{
    	    	m_logger.debug("initializeConsensus changeView.");
    			m_context.changeView(view_number);
    		}
    		if(m_context.m_myIndex < 0){
    			return;
    		}
    		m_logger.debug(String.format(
    				"initialize: height={%d} view={%d} index={%d} role={%s}", m_context.m_blockNumber,
    				view_number, m_context.m_myIndex, m_context.m_myIndex == m_context.m_primaryIndex?
    						ConsensusState.Primary.toString() : ConsensusState.Backup.toString()));
    		// Create Block
    		if(m_context.m_myIndex == m_context.m_primaryIndex){
    			m_context.m_state |= ConsensusContext.Primary;
    			if((m_context.m_state & ConsensusContext.SignatureSent) == 0){
    				fillContext();
    			}
    			if(m_context.m_lstTransaction.size() > 1){
    				TransactionsMessage transactionsMessage = new TransactionsMessage(m_context.m_lstTransaction);
    				DataCenter.getNodeManager().sendAllTransactionsMessage(transactionsMessage);
    			}
    			m_timerHeight = m_context.m_blockNumber;
    			m_timerView = view_number;
    			Calendar now = Calendar.getInstance(Locale.CHINA);
    			int span = (int)(now.getTime().getTime() - m_blockReceivedTime.getTime() / 1000);
    			m_logger.debug("the separator time is:" + span);
    			if(span >= DataCenter.getSonChainImpl().SecondsPerBlock){    				
    		        m_statTimer.schedule(new ConsensusServiceTaskExecutor(), 0, TimeUnit.SECONDS);
    			}
    			else
    			{
        			int delayTime = DataCenter.getSonChainImpl().SecondsPerBlock - span;
        			m_logger.debug(String.format("Primary ConsensusServiceTaskExecutor timeout:{%d}", delayTime));
    		        m_statTimer.schedule(new ConsensusServiceTaskExecutor(), delayTime, TimeUnit.SECONDS);
    			}    			
    		}
    		else
    		{
    			m_context.m_state = ConsensusContext.Backup;
    			m_timerHeight = m_context.m_blockNumber;
    			m_timerView = view_number;
		        m_statTimer.schedule(new ConsensusServiceTaskExecutor(), 
		        		DataCenter.getSonChainImpl().SecondsPerBlock << (view_number + 1), TimeUnit.SECONDS);
    		}
    	}
    	m_logger.debug("initializeConsensus end. view_number:" + view_number);
    }
    
    public void localNodeInventoryReceived(ConsensusPayload inventory)
    {
    	m_logger.debug("LocalNodeInventoryReceived start.");
        if (inventory != null)
        {
            synchronized (m_context)
            {
                if (inventory.m_validatorIndex == m_context.m_myIndex){
                	return;
                }

                if (inventory.m_version != ConsensusContext.Version){
                    return;
                }
                if(!FastByteComparisons.equal(inventory.m_preHash, m_context.m_preHash)
                		|| inventory.m_blockNumber != m_context.m_blockNumber){
                	// Request blocks
                    if (m_blockChain.getBestBlock().getNumber() + 1 < inventory.m_blockNumber)
                    {                    	
                        //m_logger.debug("chain sync: expected={payload.BlockIndex} current: {Blockchain.Default?.Height}");
                        m_logger.debug(String.format("chain sync: expected={%d} current: {%d}", inventory.m_blockNumber,
                        		m_blockChain.getBestBlock().getNumber()));
                        DataCenter.getNodeManager().requestGetBlocks();
                    }
                    return;
                }

                if (inventory.m_validatorIndex >= m_context.m_validators.length) {
                	return;
                }
                if(inventory.m_messageType == SonMessageCodes.ChangeView){
                	ChangeViewMessage changeViewMessage = new ChangeViewMessage(inventory.m_data);
                    onChangeViewReceived(inventory, changeViewMessage);
                }
                else if(inventory.m_messageType == SonMessageCodes.PrepareRequest){
                	PrepareRequestMessage prepareRequestMessage = new PrepareRequestMessage(inventory.m_data);
                	if(prepareRequestMessage.m_viewNumber != m_context.m_viewNumber){
                		return;
                	}
                    onPrepareRequestReceived(inventory, prepareRequestMessage);
                }
                else if(inventory.m_messageType == SonMessageCodes.PrepareResponse){
                	PrepareResponseMessage prepareResponseMessage = new PrepareResponseMessage(inventory.m_data);
                	if(prepareResponseMessage.m_viewNumber != m_context.m_viewNumber){
                		return;
                	}
                    onPrepareResponseReceived(inventory, prepareResponseMessage);
                }
            }
        }
    	m_logger.debug("LocalNodeInventoryReceived end.");
    }

    public void localNodeInventoryReceiving(Transaction tx)
    {
    	m_logger.debug("LocalNodeInventoryReceiving start.");
        if (tx != null)
        {
        	m_logger.debug("LocalNodeInventoryReceiving TransactionInfo：" + tx.toString());
        	m_logger.debug("LocalNodeInventoryReceiving State：" + m_context.m_state);
            synchronized (m_context)
            {
            	if((m_context.m_state & ConsensusContext.Backup) == 0
            			|| (m_context.m_state & ConsensusContext.RequestReceived) == 0
            			|| (m_context.m_state & ConsensusContext.SignatureSent) != 0
                        || (m_context.m_state & ConsensusContext.ViewChanging) != 0){
                	m_logger.debug("LocalNodeInventoryReceiving State not satisfied.");
                    return;
            	}
                if (m_context.m_transactions.containsKey(tx.getHash())){
                	m_logger.debug("LocalNodeInventoryReceiving tx is existed TransactionHash:" + Hex.toHexString(tx.getHash()));
                	return;
                }
                int size = m_context.m_lstTransaction.size();
                for(int i = 0; i < size; i++){
                	Transaction trans = m_context.m_lstTransaction.get(i);
                	if(!FastByteComparisons.equal(trans.getHash(), tx.getHash())){
                    	m_logger.debug("LocalNodeInventoryReceiving lstTransaction is not existed TransactionHash:"
                    			+ Hex.toHexString(tx.getHash()));
                		return;
                	}
                }
                addTransaction(tx, true);
                //e.setCancel(true);
            }
        }
    	m_logger.debug("LocalNodeInventoryReceiving end.");
    }

    /**
     * 收到更新视图的请求
     * @param payload
     * @param message
     */
    private void onChangeViewReceived(ConsensusPayload payload, ChangeViewMessage message)
    {
    	m_logger.debug(String.format("OnChangeViewReceived start height={%d} view={%d} index={%d} nv={%d}",
        		payload.m_blockNumber, message.m_viewNumber, payload.m_validatorIndex, message.m_newViewNumber));
        if (message.m_newViewNumber <= m_context.m_expectedView[payload.m_validatorIndex]){
            return;
        }
        m_context.m_expectedView[payload.m_validatorIndex] = message.m_newViewNumber;
        checkExpectedView(message.m_newViewNumber);
    	m_logger.debug(String.format("OnChangeViewReceived end height={%d} view={%d} index={%d} nv={%d}",
        		payload.m_blockNumber, message.m_viewNumber, payload.m_validatorIndex, message.m_newViewNumber));
    }
    
    private void onPrepareRequestReceived(ConsensusPayload payload, PrepareRequestMessage message)
    {
    	m_logger.debug(String.format("OnPrepareRequestReceived start height={%d} view={%d} index={%d} tx={%d} state={%d}",
        		payload.m_blockNumber, message.m_viewNumber, payload.m_validatorIndex, message.m_lstTransaction.size(), m_context.m_state));
    	if ((m_context.m_state & ConsensusContext.Backup) == 0
        		|| (m_context.m_state & ConsensusContext.RequestReceived) != 0){
            return;
        }
        if (payload.m_validatorIndex != m_context.m_primaryIndex) {
        	return;
        }
		Calendar now = Calendar.getInstance(Locale.CHINA);
		now.add(Calendar.MINUTE, 10);
        if (payload.m_timestamp <= m_blockChain.getBlockByHash(m_context.m_preHash).getTimestamp() 
        		|| payload.m_timestamp > now.getTimeInMillis())
        {
        	m_logger.debug(String.format("Timestamp incorrect: {%d}", payload.m_timestamp));
            return;
        }
        m_context.m_state |= ConsensusContext.RequestReceived;
        m_context.m_timestamp = payload.m_timestamp;
        m_context.m_nonce = message.m_nonce;
        m_context.m_nextConsensus = message.m_nextConsensus;
        m_context.m_lstTransaction = message.m_lstTransaction;
        m_context.m_transactions = new ByteArrayMap<Transaction>();
        //TODO
//        if (!Crypto.Default.VerifySignature(m_context.MakeHeader().GetHashData(), 
//        		message.m_signature, m_context.m_validators[payload.m_validatorIndex].EncodePoint(false))) {
//        	return;
//        }
        m_context.m_signatures = new byte[m_context.m_validators.length][];
        m_context.m_signatures[payload.m_validatorIndex] = message.m_signature;
        List<Transaction> localTransactions = DataCenter.getSonChainImpl().getPendingStateTransactions();
        for(Transaction trans1 : m_context.m_lstTransaction){
            for(Transaction trans2 : localTransactions){
            	if(FastByteComparisons.equal(trans1.getHash(), trans2.getHash())){
            		if(!addTransaction(trans2, false)){
            			return;
            		}
            	}
            }
        }
        if (m_context.m_transactions.size() < m_context.m_lstTransaction.size())
        {
//            UInt256[] hashes = m_context.m_lstTransaction.Where(i => !context.Transactions.ContainsKey(i)).ToArray();
//            LocalNode.AllowHashes(hashes);
//            InvPayload msg = InvPayload.Create(InventoryType.TX, hashes);
//            foreach (RemoteNode node in localNode.GetRemoteNodes())
//                node.EnqueueMessage("getdata", msg);
        }
    	m_logger.debug(String.format("OnPrepareRequestReceived end height={%d} view={%d} index={%d} tx={%d} state={%d}",
        		payload.m_blockNumber, message.m_viewNumber, payload.m_validatorIndex, message.m_lstTransaction.size(), m_context.m_state));
    }

    private void onPrepareResponseReceived(ConsensusPayload payload, PrepareResponseMessage message)
    {
    	m_logger.debug(String.format("OnPrepareResponseReceived start height={%d} view={%d} index={%d} state={%d}",
        		payload.m_blockNumber, message.m_viewNumber, payload.m_validatorIndex,  m_context.m_state));
    	
        if ((m_context.m_state & ConsensusContext.BlockSent) != 0){
        	m_logger.debug("OnPrepareResponseReceived has BlockSent");
            return;
        }
        
        if (m_context.m_signatures[payload.m_validatorIndex] != null) {
        	m_logger.debug("OnPrepareResponseReceived m_signatures is not null");
        	return;
        }
        Block header = m_context.makeHeader();
//        if (header == null || !Crypto.Default.VerifySignature(header.getHash(), 
//        		message.m_signature, m_context.m_validators[payload.m_validatorIndex].EncodePoint(false))){
//        	return;
//        }
        m_context.m_signatures[payload.m_validatorIndex] = message.m_signature;
        checkSignatures();
    }

    public void signAndRelay(ConsensusPayload payload)
    {
    	DataCenter.getNodeManager().relayDirectly(payload);
    }
    
    public void start(){
    	m_logger.debug("start ");
    	m_started = true;
    	initializeConsensus(0);
    	m_logger.debug("end");
    }
}
