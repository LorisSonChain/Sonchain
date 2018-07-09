package sonchain.blockchain.facade;

import java.math.BigInteger;
import java.net.InetAddress;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.log4j.Logger;
import org.bouncycastle.util.encoders.Hex;

import sonchain.blockchain.config.BlockChainConfig;
import sonchain.blockchain.config.CommonConfig;
import sonchain.blockchain.consensus.ConsensusService;
import sonchain.blockchain.consensus.NodeManager;
import sonchain.blockchain.consensus.SonChainPeerNode;
import sonchain.blockchain.core.Block;
import sonchain.blockchain.core.BlockChain;
import sonchain.blockchain.core.BlockChainImpl;
import sonchain.blockchain.core.BlockSummary;
import sonchain.blockchain.core.FutureAdapter;
import sonchain.blockchain.core.ImportResult;
import sonchain.blockchain.core.PendingState;
import sonchain.blockchain.core.PendingStateImpl;
import sonchain.blockchain.core.Transaction;
import sonchain.blockchain.core.TransactionExecutionSummary;
import sonchain.blockchain.net.submit.*;
import sonchain.blockchain.core.TransactionReceipt;
import sonchain.blockchain.core.TransactionReceiptStatus;
import sonchain.blockchain.crypto.ECKey;
import sonchain.blockchain.data.BaseMessage;
import sonchain.blockchain.listener.CompositeSonChainListener;
import sonchain.blockchain.listener.SonChainListener;
import sonchain.blockchain.listener.SonChainListenerAdapter;
import sonchain.blockchain.manager.AdminInfo;
import sonchain.blockchain.manager.BlockLoader;
import sonchain.blockchain.manager.WorldManager;
import sonchain.blockchain.net.submit.TransactionTask;
import sonchain.blockchain.service.DataCenter;
import sonchain.blockchain.sync.SyncManager;
import sonchain.blockchain.util.ByteUtil;
import sonchain.blockchain.vm.program.invoke.ProgramInvokeFactory;
import sonchain.blockchain.core.Repository;

public class SonChainImpl implements SonChain {

	public static final Logger m_logger = Logger.getLogger(SonChainImpl.class);
    private final static AtomicBoolean m_stopBlockGeneration = new AtomicBoolean(false);

    private AdminInfo m_adminInfo = null;
    private BlockChainImpl m_blockChain = null;
    private BlockLoader m_blockLoader = null;    
    private CommonConfig m_commonConfig = CommonConfig.getDefault();
    private CompositeSonChainListener m_compositeSonChainListener = null;
    private PendingState m_pendingState = null;    
    private SyncManager m_syncManager = new SyncManager();
    private WorldManager m_worldManager = null;
    private ProgramInvokeFactory m_programInvokeFactory = null;
    private Block m_miningBlock = null;
    private HashMap<String, TransactionReceiptStatus> m_transactionReceiptStatus 
		= new HashMap<String, TransactionReceiptStatus>();	
    private ConsensusService m_consensusService = null;
    public static int SecondsPerBlock = 10;
    public static int MaxValidators = 32;
	
//    private static ScheduledExecutorService m_statTimer =
//        Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
//             public Thread newThread(Runnable r) {
//                 return new Thread(r, "BlockStatTimer");
//          }
//     });
    
    public SonChainImpl() {
    	try
    	{
	    	m_compositeSonChainListener = new CompositeSonChainListener();
	    	m_blockChain = new BlockChainImpl(m_compositeSonChainListener);
	    	m_blockLoader = new BlockLoader();
	    	m_pendingState = new PendingStateImpl(m_compositeSonChainListener, m_blockChain);
	    	m_blockChain.setPendingState(m_pendingState);
	    	m_worldManager = new WorldManager(m_blockChain.getRepository(), 
	    			m_compositeSonChainListener, m_blockChain, m_blockChain.getBlockStore());  
	    	m_consensusService = new ConsensusService();
	    	InitListener();
	    	//InitStartTimer();
	    	m_consensusService.start();
    	}
    	catch(Exception ex){
    		m_logger.error(ex);
    	}
    }

    public SonChainImpl(CompositeSonChainListener compositeSonChainListener) {
    	try
    	{
	    	m_compositeSonChainListener = compositeSonChainListener;
	    	m_blockChain = new BlockChainImpl(m_compositeSonChainListener);
	    	m_blockLoader = new BlockLoader();
	    	m_pendingState = new PendingStateImpl(m_compositeSonChainListener, m_blockChain);
	    	m_blockChain.setPendingState(m_pendingState);
	    	m_worldManager = new WorldManager(m_blockChain.getRepository(),
	    			m_compositeSonChainListener, m_blockChain, m_blockChain.getBlockStore()); 
	    	m_consensusService = new ConsensusService();
	    	InitListener();
	    	//InitStartTimer();
	    	m_consensusService.start();
		}
		catch(Exception ex){
			m_logger.error(ex);
		}
    }
    
    public ConsensusService getConsensusService(){
    	return m_consensusService;
    }    

    public Block getNewBlockForMining() {
        Block bestBlockchain = m_blockChain.getBestBlock();
        Block bestPendingState = ((PendingStateImpl) m_pendingState).getBestBlock();
        m_logger.debug("getNewBlockForMining best blocks: PendingState: " + bestPendingState.getShortDescr() +
                ", Blockchain: " + bestBlockchain.getShortDescr());
        Block newMiningBlock = m_blockChain.createNewBlock(bestPendingState, getAllPendingTransactions());
        return newMiningBlock;
    }
    
    public int getTransactionStatue(String hash){
    	if(!m_transactionReceiptStatus.containsKey(hash)){
    		return -1;
    	}else{
    		TransactionReceiptStatus status = m_transactionReceiptStatus.get(hash);
    		return status.ordinal();
    	}
    }
    
    public void initTransactionStatue(String hash){
    	m_transactionReceiptStatus.put(hash, TransactionReceiptStatus.None);
    }
    
    @Override
    public void startPeerDiscovery() {
    	m_worldManager.startPeerDiscovery();
    }

    @Override
    public void stopPeerDiscovery() {
    	m_worldManager.stopPeerDiscovery();
    }

    @Override
    public void connect(InetAddress addr, int port, String remoteId) {
        connect(addr.getHostName(), port, remoteId);
    }
    
    @Override
    public void connect(final String ip, final int port, final String remoteId) {
        //logger.debug("Connecting to: {}:{}", ip, port);
        //m_worldManager.getActivePeer().connectAsync(ip, port, remoteId, false);
    }
    
    @Override
    public void connect(SonChainPeerNode node) {
        connect(node.getHost(), node.getPort(), Hex.toHexString(node.getId()));
    }
    
    @Override
    public BlockChain getBlockChain() {
    	return m_blockChain;
    }

    public ImportResult addNewMinedBlock(Block block) {
        ImportResult importResult = m_worldManager.getBlockchain().tryToConnect(block);
        if (importResult == ImportResult.IMPORTED_BEST) {
            //channelManager.sendNewBlock(block);
        }
        return importResult;
    }

    @Override
    public void addListener(SonChainListener listener) {
    	m_worldManager.addListener(listener);;
    }

    @Override
    public void close() {
        //logger.info("### Shutdown initiated ### ");
        //((AbstractApplicationContext) getApplicationContext()).close();
    }
    
    @Override
    public SyncStatus getSyncStatus() {
        return m_syncManager.getSyncStatus();
    }
    
//    @Override
//    public PeerClient getDefaultPeer() {
//        return worldManager.getActivePeer();
//    }
    
    public void InitListener(){
    	addListener(new SonChainListenerAdapter() {
            @Override
            public void onBlock(BlockSummary blockSummary) {
        		m_logger.debug("onBlock BlockInfo :" 
        				+ blockSummary.getBlock().toString());
                if (blockSummary.getBlock().getNumber() != 0L) {
                	List<TransactionReceipt> trans = blockSummary.getReceipts();
                	int size = trans.size();
                	for(int i = 0 ; i < size; i ++){
                		TransactionReceipt transactionReceipt = trans.get(i);
                		byte[] hash = transactionReceipt.getTransaction().getHash();
                		String strHash = Hex.toHexString(hash);
                		if( transactionReceipt.isSuccessful()){
                			m_transactionReceiptStatus.put(strHash, TransactionReceiptStatus.Success);
                		}
                		else{
                			m_transactionReceiptStatus.put(strHash, TransactionReceiptStatus.Failed);                			
                		}
                	}
                    //blocks.put(Hex.toHexString(blockSummary.getBlock().getHash()), Boolean.FALSE);
                }
                if(m_consensusService != null){
                	m_consensusService.blockChainPersistCompleted(blockSummary);
                }
            }
            
            @Override
            //public void onRecvMessage(Channel channel, Message message) {
            public void onRecvMessage(BaseMessage message) {
            }       
            
            @Override
            public void onPendingStateChanged(PendingState pendingState) {
                DataCenter.getSonChainImpl().onPendingStateChanged();
            }
            
            @Override
            public void onSyncDone(SyncState state) {
            	m_logger.info("Sync complete, start mining...");
                //if (config.minerStart() && config.isSyncEnabled()) {
                //    startMining();
                //}
            }
        	@Override
        	public void onPendingTransactionsReceived(List<Transaction> transactions) { 
        		int size = transactions.size();
        		m_logger.debug("onPendingTransactionsReceived size :" + size);
        		for(int i = 0; i > size; i++){
            		m_logger.debug("onPendingTransactionsReceived TransactionInfo:" + transactions.get(i).toString());
        		}
        	}

        	@Override
        	public void onTransactionExecuted(TransactionExecutionSummary summary) {
        		m_logger.debug("onTransactionExecuted TransactionInfo :" 
        				+ summary.getTransaction().toString());
        		m_logger.debug("onTransactionExecuted Result :" 
        				+ Hex.toHexString(summary.getResult()));
        	}
    	});
    }
    
    protected List<Transaction> getAllPendingTransactions() {
        PendingStateImpl.TransactionSortedSet ret = new PendingStateImpl.TransactionSortedSet();
        ret.addAll(m_pendingState.getPendingTransactions());
        return new ArrayList<>(ret);
    }

    private void onPendingStateChanged() {
    }
    
//    public void InitStartTimer(){
//    	try
//    	{
//	    	long initDelay = getTimeMillis(DataCenter.m_config.m_startExecutorDate) 
//	    			- System.currentTimeMillis();
//	    	m_statTimer.scheduleAtFixedRate(new Runnable() {
//	            @Override
//	            public void run() {
//	                try {
// 	                    logStats();
//	                    Block newBlock =  getNewBlockForMining();
//	                    ImportResult importResult = addNewMinedBlock(newBlock);
//	                    m_logger.debug("Mined block import result is " + importResult);
//	                } catch (Throwable t) {
//	                	m_logger.error("Unhandled exception", t);
//	                }
//	            }
//	        }, initDelay, SecondsPerBlock * 1000, TimeUnit.MILLISECONDS);        
//    	}
//    	catch(Exception ex){
//    		m_logger.error(ex.getMessage());
//    	}
//    }
    
    private static long getTimeMillis(String dateTime) {  
        try {  
            DateFormat dateFormat = new SimpleDateFormat("yy-MM-dd HH:mm:ss");  
            Date curDate = dateFormat.parse(dateTime);  
            return curDate.getTime();  
        } catch (ParseException e) {  
            e.printStackTrace();  
        }  
        return 0;  
    }  
    
    public BigInteger getNonce(byte[] address){
    	m_logger.debug("GetNonce start, address:" + Hex.toHexString(address));
    	BigInteger nonce = m_pendingState.getRepository().getNonce(address);
    	m_logger.debug("GetNonce end, address:" + Hex.toHexString(address) + " Nonce:" + nonce);
    	return nonce;
    }

    @Override
    public boolean isConnected() {
    	return false;
        //return m_worldManager.getActivePeer() != null;
    }

    @Override
    public Transaction createTransaction(BigInteger nonce,
                                         byte[] receiveAddress,
                                         BigInteger value, 
                                         byte[] data) {

        byte[] nonceBytes = ByteUtil.bigIntegerToBytes(nonce);
        byte[] valueBytes = ByteUtil.bigIntegerToBytes(value);

        return new Transaction(nonceBytes, receiveAddress, valueBytes, data);
    }
    
    @Override
    public List<Transaction> addTransactions(List<Transaction> transactions){
        return m_pendingState.addPendingTransactions(transactions);
    }

    @Override
    public Future<Transaction> submitTransaction(Transaction transaction) {
        //TransactionTask transactionTask = new TransactionTask(transaction, channelManager);
        TransactionTask transactionTask = new TransactionTask(transaction);
        Future<List<Transaction>> listFuture =
                TransactionExecutor.instance.submitTransaction(transactionTask);
        m_pendingState.addPendingTransaction(transaction);
        return new FutureAdapter<Transaction, List<Transaction>>(listFuture) {
			@Override
			protected Transaction adapt(List<Transaction> adapteeResult) throws ExecutionException {
                return adapteeResult.get(0);
			}
        };
    }

    @Override
    public TransactionReceipt callConstant(Transaction tx, Block block) {
        if (tx.getSignature() == null) {
            tx.sign(ECKey.fromPrivate(new byte[32]));
        }
        return callConstantImpl(tx, block).getReceipt();
    }

    public BlockSummary replayBlock(Block block) {
        List<TransactionReceipt> receipts = new ArrayList<>();
        List<TransactionExecutionSummary> summaries = new ArrayList<>();
        return new BlockSummary(block, receipts, summaries);
    }

    private sonchain.blockchain.core.TransactionExecutor callConstantImpl(Transaction tx, Block block) {
        Repository repository = ((Repository) m_worldManager.getRepository())
                .getSnapshotTo(block.getStateRoot())
                .startTracking();
        try {
        	sonchain.blockchain.core.TransactionExecutor executor = 
        			new sonchain.blockchain.core.TransactionExecutor
                    (tx, block.getMinedBy(), repository, m_worldManager.getBlockStore(),
                    		m_programInvokeFactory, block, new SonChainListenerAdapter())
                    .withCommonConfig(m_commonConfig)
                    .setLocalCall(true);
            executor.init();
            executor.execute();
            executor.go();
            executor.finalization();
            return executor;
        } finally {
            repository.rollback();
        }
    }

    @Override
    public sonchain.blockchain.facade.Repository getRepository() {
        return m_worldManager.getRepository();
    }

    @Override
    public sonchain.blockchain.facade.Repository getLastRepositorySnapshot() {
        return getSnapshotTo(getBlockChain().getBestBlock().getStateRoot());
    }

    @Override
    public sonchain.blockchain.facade.Repository getPendingState() {
        return m_worldManager.getPendingState().getRepository();
    }

    @Override
    public sonchain.blockchain.facade.Repository getSnapshotTo(byte[] root) {

        Repository repository = (Repository) m_worldManager.getRepository();
        sonchain.blockchain.facade.Repository snapshot = repository.getSnapshotTo(root);

        return snapshot;
    }

    @Override
    public AdminInfo getAdminInfo() {
        return m_adminInfo;
    }

    @Override
    public List<Transaction> getWireTransactions() {
        return m_worldManager.getPendingState().getPendingTransactions();
    }

    @Override
    public List<Transaction> getPendingStateTransactions() {
        return m_worldManager.getPendingState().getPendingTransactions();
    }

    @Override
    public BlockLoader getBlockLoader() {
        return m_blockLoader;
    }

//    @Override
//    public Whisper getWhisper() {
//        return whisper;
//    }

    @Override
    public Integer getChainIdForNextBlock() {
        BlockChainConfig nextBlockConfig = 
        		(BlockChainConfig) DataCenter.m_config.getConfigForBlock(getBlockChain()
		.getBestBlock().getNumber() + 1);
        return 0;
        //return nextBlockConfig.GetChainId();
    }

    @Override
    public void exitOn(long number) {
        m_worldManager.getBlockchain().setExitOn(number);
    }
    
    private boolean logStats() {
    	m_logger.info("---------====--------- start");
//        int arrivedBlocks = 0;
//        for (Boolean arrived : blocks.values()) {
//            if (arrived) arrivedBlocks++;
//        }
//        m_logger.info("Arrived blocks / Total: {}/{}", arrivedBlocks, blocks.size());
//        int arrivedTxs = 0;
//        for (Boolean arrived : txs.values()) {
//            if (arrived) arrivedTxs++;
//        }
//        m_logger.info("Arrived txs / Total: {}/{}", arrivedTxs, txs.size());
//        m_logger.info("fatalErrors: {}", fatalErrors);
//        m_logger.info("---------====---------");
//
//        return fatalErrors.get() == 0 && blocks.size() == arrivedBlocks && txs.size() == arrivedTxs;
    	m_logger.info("---------====--------- end");
    	return true;
    }

    @Override
    public void initSyncing() {
    	m_worldManager.initSyncing();
    }
}
