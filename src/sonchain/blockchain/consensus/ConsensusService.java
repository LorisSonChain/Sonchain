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
import sonchain.blockchain.core.ImportResult;
import sonchain.blockchain.core.Transaction;
import sonchain.blockchain.db.RepositoryImpl;
import sonchain.blockchain.facade.SonChainImpl;
import sonchain.blockchain.service.DataCenter;
import sonchain.blockchain.util.ByteArrayMap;
import sonchain.blockchain.util.FastByteComparisons;

public class ConsensusService{

	public static final Logger m_logger = Logger.getLogger(ConsensusService.class);
	public ConsensusContext m_context = new ConsensusContext();
	public int m_timerHeight = 0;
	public int m_timerView = 0;
	public Date m_blockReceivedTime;
	public boolean m_started = false;
	public BlockChain m_blockChain = null;
	
	public static ScheduledExecutorService m_statTimer =
        Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
             public Thread newThread(Runnable r) {
                 return new Thread(r, "ConsensusServiceStatTimer");
          }
    });
	
	public ConsensusService(){
    	try
    	{
    		m_blockChain = DataCenter.m_sonChainImpl.getBlockChain();
	    	m_statTimer.scheduleAtFixedRate(new Runnable() {
	            @Override
	            public void run() {
	            }
	        }, Long.MAX_VALUE, Long.MAX_VALUE, TimeUnit.MILLISECONDS);        
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
			//TODO
//            if (Blockchain.GetConsensusAddress(Blockchain.Default.GetValidators(context.Transactions.Values).ToArray()).Equals(context.NextConsensus))
//            {
//                Log($"send perpare response");
//                context.State |= ConsensusState.SignatureSent;
//                context.Signatures[context.MyIndex] = context.MakeHeader().Sign(context.KeyPair);
//                SignAndRelay(context.MakePrepareResponse(context.Signatures[context.MyIndex]));
//                CheckSignatures();
//            }
//            else
//            {
//                RequestChangeView();
//                return false;
//            }
		}
		return true;
	}
	
	public void BlockChainPersistCompleted(Block block){
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
    	boolean isHasTransHash = false;
    	for(Transaction tx : m_context.m_lstTransaction){
    		if(m_context.m_transactions.containsKey(tx.getHash())){
    			isHasTransHash = true;
    			break;
    		}
    	}
	    m_logger.info(String.format("checkSignatures the count of not null is {%d}, "
	    		+ "the min allow count is {%d}. HasTransHash is {%b}", count, m_context.getMinAllowCount(), isHasTransHash));
    	if(count >= m_context.getMinAllowCount() && isHasTransHash){
    	    m_logger.debug(" checkSignatures start. 1111");
    	    /**
    		Contract contract = Contract.CreateMultiSigContract(m_context.getMinAllowCount(), m_context.m_validators);
            Block block = m_context.makeHeader();
            ContractParametersContext sc = new ContractParametersContext(block);
            int minCount = m_context.getMinAllowCount();
            int length = m_context.m_validators.length;
            for (int i = 0, j = 0; i < length && j < minCount; i++)
            {
                if (m_context.m_signatures[i] != null)
                {
                    sc.AddSignature(contract, m_context.m_validators[i], m_context.m_signatures[i]);
                    j++;
                }
            }
            sc.Verifiable.Scripts = sc.GetScripts();
            List<Transaction> trans = new ArrayList<Transaction>();
        	for(Transaction tx : m_context.m_lstTransaction){
        		if(m_context.m_transactions.containsKey(tx.getHash())){
        			trans.add(tx);
        		}
        	}
            block.setTransactionsList(trans);
            m_logger.debug("checkSignatures relay block: blockInfo:" + block.toString());
            //TODO
            //if (!localNode.Relay(block)){
            //	m_logger.debug("checkSignatures reject block: blockInfo:" + block.toString());
            //}
            m_context.m_state |= ConsensusContext.BlockSent;
            **////
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
            //Blockchain.PersistCompleted -= Blockchain_PersistCompleted;
            //LocalNode.InventoryReceiving -= LocalNode_InventoryReceiving;
            //LocalNode.InventoryReceived -= LocalNode_InventoryReceived;
    	}
    	m_logger.debug("OnStop end.");    	
    }
    
    private void fillContext()
    {
    	//TODO
    
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
    		if(m_context.m_myIndex == m_context.m_primaryIndex){
    			m_context.m_state |= ConsensusContext.Primary;
    			if((m_context.m_state & ConsensusContext.SignatureSent) == 0){
    				fillContext();
    			}
    			if(m_context.m_lstTransaction.size() > 1){
    				InvPayload invPayload = InvPayload.create(InventoryType.TX, m_context.m_lstTransaction);
    				//TODO
//    				for(RemoteNode node in localNode.GetRemoteNodes()){
//    					node.EnqueueMessage("inv", invPayload);
//    				}
    			}
    			m_timerHeight = m_context.m_blockNumber;
    			m_timerView = view_number;
    			Calendar now = Calendar.getInstance(Locale.CHINA);
    			//秒数
    			long span = now.getTime().getTime() - m_blockReceivedTime.getTime() / 1000;
    			if(span >= DataCenter.getSonChainImpl().SecondsPerBlock){
    				//Timer.change(0, TimeOut.Infinite);
    			}
    			else
    			{
    				//timer.Change(DataCenter.getSonChainImpl().SecondsPerBlock - span, Timeout.InfiniteTimeSpan);
    			}    			
    		}
    		else
    		{
    			m_context.m_state = ConsensusContext.Backup;
    			m_timerHeight = m_context.m_blockNumber;
    			m_timerView = view_number;
				//TODO
    			//timer.Change(TimeSpan.FromSeconds(DataCenter.getSonChainImpl().SecondsPerBlock << (view_number + 1)), Timeout.InfiniteTimeSpan);
    		}
    	}
    	m_logger.debug("initializeConsensus end. view_number:" + view_number);
    }
    
    private void LocalNodeInventoryReceived(IInventory inventory)
    {
    	m_logger.debug("LocalNodeInventoryReceived start.");
        ConsensusPayload payload = (ConsensusPayload)inventory;
        if (payload != null)
        {
            synchronized (m_context)
            {
                if (payload.m_validatorIndex == m_context.m_myIndex){
                	return;
                }

                if (payload.m_version != ConsensusContext.Version){
                    return;
                }
                if(!FastByteComparisons.equal(payload.m_preHash, m_context.m_preHash)
                		|| payload.m_blockNumber != m_context.m_blockNumber){
                	// Request blocks
                    if (m_blockChain.getBestBlock().getNumber() + 1 < payload.m_blockNumber)
                    {                    	
                        //m_logger.debug("chain sync: expected={payload.BlockIndex} current: {Blockchain.Default?.Height}");
                        m_logger.debug(String.format("chain sync: expected={%d} current: {%d}", payload.m_blockNumber,
                        		m_blockChain.getBestBlock().getNumber()));
                        //TODO
                        //localNode.RequestGetBlocks();
                    }
                    return;
                }

                if (payload.m_validatorIndex >= m_context.m_validators.length) {
                	return;
                }
                ConsensusMessage message;
                try
                {
                    message = ConsensusMessage.deserializeFrom(payload.m_data);
                }
                catch(Exception ex)
                {
                	m_logger.error("LocalNode_InventoryReceived error :" + ex.getMessage());
                    return;
                }
                if (message.m_viewNumber != m_context.m_viewNumber 
                		&& message.m_type != ConsensusMessageType.ChangeView){
                    return;
                }
                if(message.m_type == ConsensusMessageType.ChangeView){
                    OnChangeViewReceived(payload, (ChangeView)message);
                	
                }else if(message.m_type == ConsensusMessageType.ChangeView){
                    OnPrepareRequestReceived(payload, (PrepareRequest)message);
                	
                }else if(message.m_type == ConsensusMessageType.ChangeView){
                    OnPrepareResponseReceived(payload, (PrepareResponse)message);                	
                }
            }
        }
    	m_logger.debug("LocalNodeInventoryReceived end.");
    }

    private void LocalNodeInventoryReceiving(InventoryReceivingEventArgs e)
    {
    	m_logger.debug("LocalNodeInventoryReceiving start.");
        Transaction tx = e.getTransaction();
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
                e.setCancel(true);
            }
        }
    	m_logger.debug("LocalNodeInventoryReceiving end.");
    }

    private void OnChangeViewReceived(ConsensusPayload payload, ChangeView message)
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
    
    private void OnPrepareRequestReceived(ConsensusPayload payload, PrepareRequest message)
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
        //TODO
//        Dictionary<UInt256, Transaction> mempool = LocalNode.GetMemoryPool().ToDictionary(p => p.Hash);
//        foreach (UInt256 hash in context.TransactionHashes.Skip(1))
//        {
//            if (mempool.TryGetValue(hash, out Transaction tx))
//                if (!AddTransaction(tx, false))
//                    return;
//        }
//        if (!AddTransaction(message.MinerTransaction, true)){
//        	return;
//        }
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

    private void OnPrepareResponseReceived(ConsensusPayload payload, PrepareResponse message)
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
//        ContractParametersContext sc;
//        try
//        {
//            sc = new ContractParametersContext(payload);
//            wallet.Sign(sc);
//        }
//        catch (InvalidOperationException)
//        {
//            return;
//        }
//        sc.Verifiable.Scripts = sc.GetScripts();
//        localNode.RelayDirectly(payload);
    }
    
    public void start(){
    	m_logger.debug("start ");
    	m_started = true;
    	initializeConsensus(0);
    	m_logger.debug("end");
    }
}
