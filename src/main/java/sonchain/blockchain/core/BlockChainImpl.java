package sonchain.blockchain.core;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Stack;

import org.apache.log4j.Logger;
import org.apache.log4j.Priority;
import org.bouncycastle.util.encoders.Hex;

import sonchain.blockchain.config.CommonConfig;
import sonchain.blockchain.consensus.SonChainPeerNode;
import sonchain.blockchain.crypto.HashUtil;
import sonchain.blockchain.datasource.Source;
import sonchain.blockchain.db.BlockStore;
import sonchain.blockchain.db.DbFlushManager;
import sonchain.blockchain.db.IndexedBlockStore;
import sonchain.blockchain.db.PeerSource;
import sonchain.blockchain.db.StateSource;
import sonchain.blockchain.db.TransactionStore;
import sonchain.blockchain.listener.CompositeSonChainListener;
import sonchain.blockchain.listener.SonChainListener;
import sonchain.blockchain.manager.AdminInfo;
import sonchain.blockchain.service.DataCenter;
import sonchain.blockchain.sync.SyncManager;
import sonchain.blockchain.trie.Trie;
import sonchain.blockchain.trie.TrieImpl;
import sonchain.blockchain.util.FastByteComparisons;
import sonchain.blockchain.util.RLP;
import sonchain.blockchain.validator.DependentBlockHeaderRule;
import sonchain.blockchain.validator.DependentBlockHeaderRuleAdapter;
import sonchain.blockchain.validator.ParentBlockHeaderValidator;
import sonchain.blockchain.vm.program.invoke.ProgramInvokeFactory;
import sonchain.blockchain.vm.program.invoke.ProgramInvokeFactoryImpl;
import static java.util.Collections.emptyList;
import static sonchain.blockchain.core.ImportResult.*;

public class BlockChainImpl implements BlockChain {

	private class State {
		// Repository savedRepo = repository;
		byte[] m_root = m_repository.getRoot();
		Block m_savedBest = m_bestBlock;
	}

	public static final Logger m_logger = Logger.getLogger(BlockChainImpl.class);
	public static final byte[] EMPTY_LIST_HASH = HashUtil.sha3(RLP.encodeList(new byte[0]));

	private AdminInfo m_adminInfo = new AdminInfo();
	// last current block
	private Block m_bestBlock = null;
	// Block store
	protected BlockStore m_blockStore = null;
	private CommonConfig m_commonConfig = CommonConfig.getDefault();
	private DbFlushManager m_dbFlushManager = null;
	private EventDispatchThread m_eventDispatchThread = EventDispatchThread.getDefault();
	private SonChainListener m_listener = null;
	private DependentBlockHeaderRule m_parentHeaderValidator = new DependentBlockHeaderRuleAdapter();
	private PendingState m_pendingState = null;
	private ProgramInvokeFactory m_programInvokeFactory = null;
	private Repository m_repository = m_commonConfig.getDefaultRepository();
	//private StateSource m_stateDataSource = null;
	private PeerSource m_peerSource = null;
	private Stack<State> m_stateStack = new Stack<>();
	private SyncManager m_syncManager = new SyncManager();
	private TransactionStore m_transactionStore = null;

	private List<Chain> m_altChains = new ArrayList<>();
	public boolean m_byTest = false;
	private long m_exitOn = Long.MAX_VALUE;
	private List<Block> m_garbage = new ArrayList<>();
	private byte[] m_nodeAddress = null;
	private byte[] m_minerExtraData = null;
	private List<SonChainPeerNode> m_validators = new ArrayList<SonChainPeerNode>();

	/**
	 * Constructor
	 */
	public BlockChainImpl(CompositeSonChainListener compositeSonChainListener) {
		m_blockStore = initBlockStore();
		m_transactionStore = getTransactionStore();
		m_peerSource = m_commonConfig.getPeerSource();
		m_listener = compositeSonChainListener;
		initConst();
	}

	/**
	 * Constructor
	 * @param blockStore
	 * @param repository
	 */
	public BlockChainImpl(final Repository repository, CompositeSonChainListener compositeSonChainListener) {
		m_repository = repository;
		m_adminInfo = new AdminInfo();
		m_listener = compositeSonChainListener;
		m_parentHeaderValidator = null;
		m_blockStore = initBlockStore();
		m_transactionStore = getTransactionStore();
		m_eventDispatchThread = EventDispatchThread.getDefault();
		m_programInvokeFactory = new ProgramInvokeFactoryImpl();
		m_peerSource = m_commonConfig.getPeerSource();
		initConst();
	}

    public synchronized BlockSummary add(Repository repo, final Block block) {
    	m_logger.debug("add start.");
        BlockSummary summary = addImpl(repo, block);
        if (summary == null) {
        	m_logger.warn("Trying to reimport the block for debug...");
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
            }
            BlockSummary summary1 = addImpl(repo.getSnapshotTo(getBestBlock().getStateRoot()), block);
            m_logger.warn("Second import trial " + (summary1 == null ? "FAILED" : "OK"));
            if (summary1 != null) {
            	m_logger.error("Inconsistent behavior, exiting...");
                System.exit(-1);
            }
        }
    	m_logger.debug("add end.");
        return summary;
    }

    @Override
    public BlockSummary add(Block block) {
        throw new RuntimeException("Not supported");
    }

    private BlockSummary applyBlock(Repository track, Block block) {
    	m_logger.debug(String.format("applyBlock start: block: [{%d}] tx.list: [{%d}]", 
    			block.getNumber(), block.getTransactionsList().size()));
        long saveTime = System.nanoTime();
        int i = 1;
        List<TransactionReceipt> receipts = new ArrayList<>();
        List<TransactionExecutionSummary> summaries = new ArrayList<>();
        for (Transaction tx : block.getTransactionsList()) {
        	m_logger.debug(String.format("apply block: [{%d}] tx: [{%d}] ", block.getNumber(), i));
            Repository txTrack = track.startTracking();
            TransactionExecutor executor = new TransactionExecutor(tx, block.getMinedBy(),
                    txTrack, m_blockStore, m_programInvokeFactory, block, m_listener)
                    .withCommonConfig(m_commonConfig);
            executor.init();
            executor.execute();
            executor.go();
            TransactionExecutionSummary summary = executor.finalization();
            txTrack.commit();
            final TransactionReceipt receipt = executor.getReceipt();
            receipt.setPostTxState(track.getRoot());
            m_logger.info(String.format("block: [{%d}] executed tx: [{%d}] \n  state: [{%s}]", 
            		block.getNumber(), i,
                    Hex.toHexString(track.getRoot())));
            m_logger.info(String.format("[{%s}] ", receipt.toString()));
            if (m_logger.isInfoEnabled()){
            	m_logger.info(String.format("tx[{}].receipt: [{}] ", i,
            			Hex.toHexString(receipt.getEncoded())));
            }
            receipts.add(receipt);
            if (summary != null) {
                summaries.add(summary);
            }
        }
        long totalTime = System.nanoTime() - saveTime;
        m_adminInfo.addBlockExecTime(totalTime);
        m_logger.debug(String.format("applyBlock end block : num: [{%d}] hash: [{%s}], "
        		+ "executed after: [{%d}]nano", block.getNumber(), block.getShortHash(), totalTime));
        return new BlockSummary(block, receipts, summaries);
    }

    public synchronized BlockSummary addImpl(Repository repo, final Block block) {
    	m_logger.debug("addImpl start.");
        if (m_exitOn < block.getNumber()) {
            System.out.print("Exiting after block.number: " + m_bestBlock.getNumber());
            m_dbFlushManager.flushSync();
            System.exit(-1);
        }
        if (!isValid(repo, block)) {
        	m_logger.warn(String.format("Invalid block with number: {%d}", block.getNumber()));
            return null;
        }
        byte[] origRoot = repo.getRoot();
        BlockSummary summary = processBlock(repo, block);
        final List<TransactionReceipt> receipts = summary.getReceipts();
        if (!FastByteComparisons.equal(block.getReceiptsRoot(), calcReceiptsTrie(receipts))) {
        	m_logger.warn(String.format("Block's given Receipt Hash doesn't match: {%s} != {%s}", 
            		Hex.toHexString(block.getReceiptsRoot()), 
            		Hex.toHexString(calcReceiptsTrie(receipts))));
        	m_logger.warn("Calculated receipts: " + receipts);
            repo.rollback();
            summary = null;
        }
        
        if (!FastByteComparisons.equal(block.getStateRoot(), repo.getRoot())) {
        	m_logger.warn(String.format("BLOCK: State conflict or received invalid block. "
        			+ "block: {%d} worldstate {%s} mismatch", block.getNumber(), 
        			Hex.toHexString(repo.getRoot())));
        	m_logger.warn(String.format("Conflict block dump: {}",
        			Hex.toHexString(block.getEncoded())));
            m_repository = m_repository.getSnapshotTo(origRoot);
        }
        if (summary != null) {
            repo.commit();
            if (!m_byTest) {
            	m_dbFlushManager.commit(new Runnable() {
                    @Override
                    public void run() {
                        storeBlock(block, receipts);
                        m_repository.commit();
                    }
                });
            } else {
                storeBlock(block, receipts);
            }
        }
    	m_logger.debug("addImpl end.");
        return summary;
    }

    @Override
    public byte[] calcReceiptsTrie(List<TransactionReceipt> receipts) {
    	m_logger.debug("calcReceiptsTrie start.");
        Trie receiptsTrie = new TrieImpl();
        if (receipts == null || receipts.isEmpty())
            return HashUtil.EMPTY_TRIE_HASH;

        for (int i = 0; i < receipts.size(); i++) {
            receiptsTrie.put(RLP.encodeInt(i), receipts.get(i).getReceiptTrieEncoded());
        }
    	m_logger.debug("calcReceiptsTrie end.");
        return receiptsTrie.getRootHash();
    }

    @Override
	public byte[] calcTxTrie(List<Transaction> transactions) {
    	m_logger.debug("calcTxTrie start.");
		Trie txsState = new TrieImpl();
		if (transactions == null || transactions.isEmpty()) {
			return HashUtil.EMPTY_TRIE_HASH;
		}
		for (int i = 0; i < transactions.size(); i++) {
			txsState.put(RLP.encodeInt(i), transactions.get(i).getEncoded());
		}
    	m_logger.debug("calcTxTrie end.");
		return txsState.getRootHash();
	}

    @Override
    public synchronized void close() {
    	m_logger.debug("close start.");
    	m_blockStore.close();
    	m_transactionStore.close();
    	m_logger.debug("close end.");
    }

	@Override
    public synchronized Block createNewBlock(Block parent, List<Transaction> txs) {
    	m_logger.debug("createNewBlock start.");
        long time = System.currentTimeMillis() / 1000;
        // adjust time to parent block this may happen due to system clocks difference
        if (parent.getTimestamp() >= time) {
        	time = parent.getTimestamp() + 1;
        }
    	m_logger.debug("createNewBlock end.");
        return createNewBlock(parent, txs, time);
    }  
	
	@Override
	public boolean containsBlock(byte[] hash){
        if(m_blockStore.getBlockByHash(hash) != null){
        	return true;
        }
        return false;
    }
	
	@Override
	public boolean containsTransaction(byte[] blockHash, byte[] hash){
        if(m_transactionStore.get(hash, blockHash) != null){
        	return true;
        }
        return false;
    }

    public synchronized Block createNewBlock(Block parent, List<Transaction> txs, long time) {
    	m_logger.debug("createNewBlock111 start.");
        final long blockNumber = parent.getNumber() + 1;
        Block block = new Block(parent.getHash(),
        		m_nodeAddress,
                blockNumber,
                time,  				// block time
                new byte[] {0},  	// extra data
                new byte[0],  		// receiptsRoot - computed after running all transactions
                calcTxTrie(txs),    // TransactionsRoot - computed after running all transactions
                new byte[] {0}, 	// stateRoot - computed after running all transactions
                txs);  
        
        Repository track = m_repository.getSnapshotTo(parent.getStateRoot());
        BlockSummary summary = applyBlock(track, block);
        List<TransactionReceipt> receipts = summary.getReceipts();
        block.setStateRoot(track.getRoot());
        block.getHeader().setReceiptsRoot(calcReceiptsTrie(receipts));
    	m_logger.debug("createNewBlock111 end. BlockInfo:" + block.toString());
        return block;
    }
	
    public void dropState() {
    	m_logger.debug("dropState start.");
    	m_stateStack.pop();
    	m_logger.debug("dropState end.");
    }
    
    @Override
    public void flush() {
    	m_logger.debug("flush start.");
//        repository.flush();
//        stateDataSource.flush();
//        blockStore.flush();
//        transactionStore.flush();
//
//        repository = repository.getSnapshotTo(repository.getRoot());
//
//        if (isMemoryBoundFlush()) {
//            System.gc();
//        }
    	m_logger.debug("flush end.");
    }

    @Override
    public List<Chain> getAltChains() {
    	m_logger.debug("getAltChains start.");
        return m_altChains;
    }

    @Override
    public synchronized Block getBestBlock() {
        // the method is synchronized since the bestBlock might be
        // temporarily switched to the fork while importing non-best block
    	m_logger.debug("getBestBlock start.");
        return m_bestBlock;
    }

	/**
	 * the most recent block header hash
	 */
	@Override
	public byte[] getBestBlockHash() {
    	m_logger.debug("getBestBlockHash start.");
		return getBestBlock().getHash();
	}

    @Override
    public Block getBlockByHash(byte[] hash) {
    	m_logger.debug("getBlockByHash start.");
        return m_blockStore.getBlockByHash(hash);
    }

	@Override
	public Block getBlockByNumber(long blockNumber) {
    	m_logger.debug("getBlockByNumber start.");
    	Block block = m_blockStore.getChainBlockByNumber(blockNumber);
    	m_logger.debug("getBlockByNumber end BlockInfo:" + block.toString());
    	return block;
	}
	
	@Override
    public BlockStore getBlockStore() {
		return m_blockStore;
    }

	@Override
	public PeerSource getPeerSource(){
		return m_peerSource;
	}
	
	@Override
	public SonChainPeerNode[] getValidators(){
		return DataCenter.GetStandbyPeerNodes();
	}
	
	public List<SonChainPeerNode> getValidators(boolean flag){
		//getStateSource.
		return new ArrayList<SonChainPeerNode>();
	}
	
    public BlockStore initBlockStore() {
    	m_logger.debug("initBlockStore start.");
		m_commonConfig.fastSyncCleanUp();
		IndexedBlockStore indexedBlockStore = new IndexedBlockStore();
		Source<byte[], byte[]> block = m_commonConfig.getCachedDbSource("block");
		Source<byte[], byte[]> index = m_commonConfig.getCachedDbSource("index");
		indexedBlockStore.Init(index, block);
    	m_logger.debug("initBlockStore end.");
		return indexedBlockStore;
    }
    
    private List<BlockHeader> getContinuousHeaders(long bestNumber, long blockNumber, int limit, boolean reverse) {
    	m_logger.debug("getContinuousHeaders start.");
        int qty = getQty(blockNumber, bestNumber, limit, reverse);
        byte[] startHash = getStartHash(blockNumber, qty, reverse);
        if (startHash == null) {
            return emptyList();
        }
        List<BlockHeader> headers = m_blockStore.getListHeadersEndWith(startHash, qty);

        // blocks come with falling numbers
        if (!reverse) {
            Collections.reverse(headers);
        }
    	m_logger.debug("getContinuousHeaders end.");
        return headers;
    }

    private List<BlockHeader> getGapedHeaders(Block startBlock, int skip, int limit, boolean reverse) {
    	m_logger.debug("getGapedHeaders start.");
        List<BlockHeader> headers = new ArrayList<>();
        headers.add(startBlock.getHeader());
        int offset = skip + 1;
        if (reverse) offset = -offset;
        long currentNumber = startBlock.getNumber();
        boolean finished = false;

        while(!finished && headers.size() < limit) {
            currentNumber += offset;
            Block nextBlock = m_blockStore.getChainBlockByNumber(currentNumber);
            if (nextBlock == null) {
                finished = true;
            } else {
                headers.add(nextBlock.getHeader());
            }
        }
    	m_logger.debug("getGapedHeaders end.");
        return headers;
    }
    
    @Override
    public List<byte[]> getListOfBodiesByHashes(List<byte[]> hashes) {
    	m_logger.debug("getListOfBodiesByHashes start.");
        List<byte[]> bodies = new ArrayList<>(hashes.size());
        for (byte[] hash : hashes) {
            Block block = m_blockStore.getBlockByHash(hash);
            if (block == null){
            	break;
            }
            bodies.add(block.getEncodedBody());
        }
    	m_logger.debug("getListOfBodiesByHashes end.");
        return bodies;
    }
    
    @Override
    public List<BlockHeader> getListOfHeadersStartFrom(BlockIdentifier identifier, int skip, int limit, boolean reverse) {
    	m_logger.debug("getListOfHeadersStartFrom start.");
        // Identifying block we'll move from
        Block startBlock;
        if (identifier.getHash() != null) {
            startBlock = m_blockStore.getBlockByHash(identifier.getHash());
        } else {
            startBlock = m_blockStore.getChainBlockByNumber(identifier.getNumber());
        }
        // If nothing found or provided hash is not on main chain, return empty array
        if (startBlock == null) {
            return emptyList();
        }
        if (identifier.getHash() != null) {
            Block mainChainBlock = m_blockStore.getChainBlockByNumber(startBlock.getNumber());
            if (!startBlock.equals(mainChainBlock)) return emptyList();
        }
        List<BlockHeader> headers;
        if (skip == 0) {
            long bestNumber = m_blockStore.getBestBlock().getNumber();
            headers = getContinuousHeaders(bestNumber, startBlock.getNumber(), limit, reverse);
        } else {
            headers = getGapedHeaders(startBlock, skip, limit, reverse);
        }
    	m_logger.debug("getListOfHeadersStartFrom end.");
        return headers;
    }

    @Override
    public synchronized List<byte[]> getListOfHashesStartFrom(byte[] hash, int qty) {
    	m_logger.debug("getListOfHashesStartFrom start.");
        return m_blockStore.getListHashesEndWith(hash, qty);
    }

    @Override
    public synchronized List<byte[]> getListOfHashesStartFromBlock(long blockNumber, int qty) {
    	m_logger.debug("getListOfHashesStartFromBlock start.");
        long bestNumber = m_bestBlock.getNumber();
        if (blockNumber > bestNumber) {
            return emptyList();
        }
        if (blockNumber + qty - 1 > bestNumber) {
            qty = (int) (bestNumber - blockNumber + 1);
        }
        long endNumber = blockNumber + qty - 1;
        Block block = getBlockByNumber(endNumber);
        List<byte[]> hashes = m_blockStore.getListHashesEndWith(block.getHash(), qty);
        // asc order of hashes is required in the response
        Collections.reverse(hashes);
    	m_logger.debug("getListOfHashesStartFromBlock end.");
        return hashes;
    }

	@Override
	public byte[] getNodeAddress() {
    	m_logger.debug("getNodeAddress start.");
        return m_nodeAddress;
	}
	
    public Block getParent(BlockHeader header) {
    	m_logger.debug("getParent start.");
        return m_blockStore.getBlockByHash(header.getParentHash());
    }
    
    public PendingState getPendingState() {
    	m_logger.debug("getPendingState start.");
        return m_pendingState;
    }

    @Override
    public ProgramInvokeFactory getProgramInvokeFactory() {
    	m_logger.debug("getProgramInvokeFactory start.");
        return m_programInvokeFactory;
    }

    private int getQty(long blockNumber, long bestNumber, int limit, boolean reverse) {
    	m_logger.debug("getQty start.");
    	try
    	{
	        if (reverse) {
	            return blockNumber - limit + 1 < 0 ? (int) (blockNumber + 1) : limit;
	        } else {
	            if (blockNumber + limit - 1 > bestNumber) {
	                return (int) (bestNumber - blockNumber + 1);
	            } else {
	                return limit;
	            }
	        }
    	}
    	finally
    	{
        	m_logger.debug("getQty end.");
    	}
    }

    @Override
    public Repository getRepository() {
    	m_logger.debug("getRepository start.");
        return m_repository;
    }

    @Override
	public Repository getRepositorySnapshot() {
    	m_logger.debug("getRepositorySnapshot start.");
		return m_repository.getSnapshotTo(m_blockStore.getBestBlock()
				.getStateRoot());
	}

	@Override
	public long getSize() {
    	m_logger.debug("getSize start.");
		return m_bestBlock.getNumber() + 1;
	}

    private byte[] getStartHash(long blockNumber, int qty, boolean reverse) {
    	m_logger.debug("getStartHash start.");
        long startNumber = 0;
        if (reverse) {
            startNumber = blockNumber;
        } else {
            startNumber = blockNumber + qty - 1;
        }
        Block block = m_blockStore.getChainBlockByNumber(startNumber);
        if (block == null) {
        	m_logger.debug("getStartHash end.");
            return null;
        }
    	m_logger.debug("getStartHash end.");
        return block.getHash();
    }
    
	@Override
	public TransactionInfo getTransactionInfo(byte[] hash) {
    	m_logger.debug("getTransactionInfo start.");
		List<TransactionInfo> infos = m_transactionStore.get(hash);
		if (infos == null || infos.isEmpty()){
			return null;
		}
		TransactionInfo txInfo = null;
		if (infos.size() == 1) {
			txInfo = infos.get(0);
		} else {
			// pick up the receipt from the block on the main chain
			for (TransactionInfo info : infos) {
				Block block = m_blockStore.getBlockByHash(info.getBlockHash());
				Block mainBlock = m_blockStore.getChainBlockByNumber(block.getNumber());
				if (FastByteComparisons.equal(info.getBlockHash(), mainBlock.getHash())) {
					txInfo = info;
					break;
				}
			}
		}
		if (txInfo == null) {
			m_logger.warn("Can't find block from main chain for transaction " + Hex.toHexString(hash));
	    	m_logger.debug("getTransactionInfo end.");
			return null;
		}
		Transaction tx = getBlockByHash(txInfo.getBlockHash())
				.getTransactionsList().get(txInfo.getIndex());
		txInfo.setTransaction(tx);
    	m_logger.debug("getTransactionInfo end.");
		return txInfo;
	}

    public TransactionStore getTransactionStore() {
    	m_logger.debug("getTransactionStore start.");
		m_commonConfig.fastSyncCleanUp();
		if(m_transactionStore == null){
			m_transactionStore = new TransactionStore(m_commonConfig.getCachedDbSource("transactions"));
		}
		return m_transactionStore;
    }

    public boolean hasParentOnTheChain(Block block) {
    	m_logger.debug("hasParentOnTheChain start.");
        return getParent(block.getHeader()) != null;
    }

	private void initConst() {
    	m_logger.debug("initConst start.");
    	m_dbFlushManager = m_commonConfig.getDbFlushManager();
		m_nodeAddress = DataCenter.m_config.getNodeAddress();
		//m_minerExtraData = config.getMineExtraData();
    	m_logger.debug("initConst end.");
	}
	
    public boolean isBlockExist(byte[] hash) {
    	m_logger.debug("isBlockExist start.");
        return m_blockStore.isBlockExist(hash);
    }

    public boolean isValid(BlockHeader header) {
    	m_logger.debug("isValid start.");
        if (m_parentHeaderValidator == null) {
        	m_logger.debug("isValid end.");
        	return true;
        }
        Block parentBlock = getParent(header);
        if (!m_parentHeaderValidator.validate(header, parentBlock.getHeader())) {
            if (m_logger.isEnabledFor(Priority.ERROR)){
            	m_parentHeaderValidator.logErrors(m_logger);
            }
        	m_logger.debug("isValid end.");
            return false;
        }
    	m_logger.debug("isValid end.");
        return true;
    }

    /**
     * This mechanism enforces a homeostasis in terms of the time between blocks;
     */
    private boolean isValid(Repository repo, Block block) {
    	m_logger.debug("isValid111 start.");
        boolean isValid = true;        
        if (!block.isGenesis()) {
            isValid = isValid(block.getHeader());
            String trieHash = Hex.toHexString(block.getTxMerkleRoot());
            String trieListHash = Hex.toHexString(calcTxTrie(block.getTransactionsList()));
            if (!trieHash.equals(trieListHash)) {
            	m_logger.warn(String.format("Block's given Trie Hash doesn't match: {%s} != {%s}", 
            			trieHash, trieListHash));
            	m_logger.debug("isValid111 end.");
                return false;
            }
//            List<Transaction> txs = block.getTransactionsList();
//            if (!txs.isEmpty()) {
//                Map<ByteArrayWrapper, BigInteger> curNonce = new HashMap<>();
//                for (Transaction tx : txs) {
//                    byte[] txSender = tx.getSender();
//                    ByteArrayWrapper key = new ByteArrayWrapper(txSender);
//                    BigInteger expectedNonce = curNonce.get(key);
//                    if (expectedNonce == null) {
//                        expectedNonce = repo.getNonce(txSender);
//                    }
//                    curNonce.put(key, expectedNonce.add(ONE));
//                    BigInteger txNonce = new BigInteger(1, tx.getNonce());
//                    if (!expectedNonce.equals(txNonce)) {
//                    	m_logger.warn("Invalid transaction: Tx nonce {} != expected nonce {} (parent nonce: {}): {}",
//                                txNonce, expectedNonce, repo.getNonce(txSender), tx);
//                        return false;
//                    }
//                }
//            }
        }
    	m_logger.debug("isValid111 end.");
        return isValid;
    }
    
    private void popState() {
    	m_logger.debug("popState start.");
        State state = m_stateStack.pop();
        m_repository = m_repository.getSnapshotTo(state.m_root);
        m_bestBlock = state.m_savedBest;
    }

    private BlockSummary processBlock(Repository track, Block block) {
    	m_logger.debug("processBlock start.");
        if (!block.isGenesis()) {
            return applyBlock(track, block);
        }
        else {
            return new BlockSummary(block, new ArrayList<TransactionReceipt>(), 
            		new ArrayList<TransactionExecutionSummary>());
        }
    }
    
    private State pushState(byte[] bestBlockHash) {
    	m_logger.debug("pushState start.");
        State push = m_stateStack.push(new State());
        m_bestBlock = m_blockStore.getBlockByHash(bestBlockHash);
        m_repository = m_repository.getSnapshotTo(m_bestBlock.getStateRoot());
        return push;
    }

    private void recordBlock(Block block) {  
    	m_logger.debug("recordBlock start."); 
        if (!DataCenter.m_config.m_recordBlocks) {
        	return;
        }
        String dumpDir = DataCenter.m_config.m_datebaseDir + "/" 
        		+ DataCenter.m_config.m_dumpDir;
        File dumpFile = new File(dumpDir + "/blocks-rec.dmp");
        FileWriter fw = null;
        BufferedWriter bw = null;
        try {
            dumpFile.getParentFile().mkdirs();
            if (!dumpFile.exists()) {
            	dumpFile.createNewFile();
            }
            fw = new FileWriter(dumpFile.getAbsoluteFile(), true);
            bw = new BufferedWriter(fw);
            if (m_bestBlock.isGenesis()) {
                bw.write(Hex.toHexString(m_bestBlock.getEncoded()));
                bw.write("\n");
            }
            bw.write(Hex.toHexString(block.getEncoded()));
            bw.write("\n");
        } catch (IOException e) {
        	m_logger.error(e.getMessage(), e);
        } finally {
            try {
                if (bw != null) bw.close();
                if (fw != null) fw.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        	m_logger.debug("recordBlock end."); 
        }
    }

    @Override
    public void setBestBlock(Block block) {
    	m_logger.debug("setBestBlock start."); 
    	m_bestBlock = block;
    	m_repository = m_repository.getSnapshotTo(block.getStateRoot());
    }
    
    public void setExitOn(long exitOn) {
    	m_logger.debug("setExitOn start."); 
    	m_exitOn = exitOn;
    }
    
    public void setMinerExtraData(byte[] minerExtraData) {
    	m_logger.debug("setMinerExtraData start."); 
    	m_minerExtraData = minerExtraData;
    }

    public void setNodeAddress(byte[] nodeAddress) {
    	m_logger.debug("setNodeAddress start."); 
    	m_nodeAddress = nodeAddress;
    }

    public void setParentHeaderValidator(DependentBlockHeaderRule parentHeaderValidator) {
    	m_logger.debug("setParentHeaderValidator start."); 
    	m_parentHeaderValidator = parentHeaderValidator;
    }
    
    public void setPendingState(PendingState pendingState) {
    	m_logger.debug("setPendingState start."); 
    	m_pendingState = pendingState;
    }
    
    public void setProgramInvokeFactory(ProgramInvokeFactory factory) {
    	m_logger.debug("setProgramInvokeFactory start."); 
    	m_programInvokeFactory = factory;
    }

    public void setRepository(Repository repository) {
    	m_logger.debug("setRepository start."); 
    	m_repository = repository;
    }
    
    @Override
    public synchronized void storeBlock(Block block, List<TransactionReceipt> receipts) {
    	m_logger.debug("storeBlock start."); 
    	m_blockStore.saveBlock(block, true);
        for (int i = 0; i < receipts.size(); i++) {
        	m_transactionStore.put(new TransactionInfo(receipts.get(i), block.getHash(), i));
        }
        m_logger.debug(String.format("Block saved: number: {%d}, hash: {%s}",
                block.getNumber(), block.getShortHash()));
        setBestBlock(block);
        if (m_logger.isDebugEnabled()){
        	m_logger.debug(String.format("block added to the blockChain: index: [{%d}]", 
        			block.getNumber()));
        }
        if (block.getNumber() % 100 == 0){
        	m_logger.info(String.format("*** Last block added [ #{%d} ]", block.getNumber()));
        }
    	m_logger.debug("storeBlock end."); 
    }

    public synchronized ImportResult tryToConnect(final Block block) {
    	m_logger.debug("tryToConnect start."); 
        if (m_logger.isDebugEnabled())
        	m_logger.debug(String.format("Try connect block hash: {%s}, number: {%d}",
                    Hex.toHexString(block.getHash()).substring(0, 6), block.getNumber()));

        if (m_blockStore.getMaxNumber() >= block.getNumber() &&
        		m_blockStore.isBlockExist(block.getHash())) {
            if (m_logger.isDebugEnabled())
            	m_logger.debug(String.format("Block already exist hash: {%s}, number: {%d}",
                        Hex.toHexString(block.getHash()).substring(0, 6),
                        block.getNumber()));
            // retry of well known block
        	m_logger.debug("tryToConnect end."); 
            return EXIST;
        }

        final ImportResult ret;
        // The simple case got the block to connect to the main chain
        final BlockSummary summary;
        if (m_bestBlock.isParentOf(block)) {
            recordBlock(block);
//            Repository repoSnap = repository.getSnapshotTo(bestBlock.getStateRoot());
            summary = add(m_repository, block);
            ret = summary == null ? INVALID_BLOCK : IMPORTED_BEST;
        } else {
            if (m_blockStore.isBlockExist(block.getParentHash())) {
                recordBlock(block);
                summary = tryConnectAndFork(block);
                ret = summary == null ? INVALID_BLOCK : IMPORTED_BEST;
            } else {
                summary = null;
                ret = NO_PARENT;
            }
        }

        if (ret.isSuccessful()) {
        	m_listener.onBlock(summary);
        	m_listener.trace(String.format("Block chain size: [ %d ]", this.getSize()));
            if (ret == IMPORTED_BEST) {
            	m_eventDispatchThread.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                    	m_pendingState.processBest(block, summary.getReceipts());
                    }
                });
            }
        }
    	m_logger.debug("tryToConnect end."); 
        return ret;
    }
    
    private synchronized BlockSummary tryConnectAndFork(final Block block) {
    	m_logger.debug("tryConnectAndFork start."); 
        State savedState = pushState(block.getParentHash());

        final BlockSummary summary;
        Repository repo;
        try {

            // FIXME: adding block with no option for flush
            Block parentBlock = getBlockByHash(block.getParentHash());
            repo = m_repository.getSnapshotTo(parentBlock.getStateRoot());
            summary = add(repo, block);
            if (summary == null) {
                return null;
            }
        } catch (Throwable th) {
        	m_logger.error("Unexpected error: ", th);
            return null;
        } finally {
        }
        popState();
    	m_logger.debug("tryConnectAndFork end"); 
        return summary;
    }
    
	public BlockChainImpl withAdminInfo(AdminInfo adminInfo) {
    	m_logger.debug("withAdminInfo start"); 
		m_adminInfo = adminInfo;
		return this;
	}

	public BlockChainImpl withParentBlockHeaderValidator(
			ParentBlockHeaderValidator parentHeaderValidator) {
    	m_logger.debug("withParentBlockHeaderValidator start"); 
		m_parentHeaderValidator = parentHeaderValidator;
		return this;
	}

	public BlockChainImpl withSonChainListener(SonChainListener listener) {
    	m_logger.debug("withSonChainListener start"); 
		m_listener = listener;
		return this;
	}

	public BlockChainImpl withSyncManager(SyncManager syncManager) {
    	m_logger.debug("withSyncManager start"); 
		m_syncManager = syncManager;
		return this;
	}

	public BlockChainImpl withTransactionStore(TransactionStore transactionStore) {
    	m_logger.debug("withTransactionStore start"); 
		m_transactionStore = transactionStore;
		return this;
	}
}