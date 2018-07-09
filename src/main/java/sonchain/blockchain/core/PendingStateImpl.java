package sonchain.blockchain.core;

import sonchain.blockchain.config.CommonConfig;
import sonchain.blockchain.db.BlockStore;
import sonchain.blockchain.db.ByteArrayWrapper;
import sonchain.blockchain.db.TransactionStore;
import sonchain.blockchain.listener.SonChainListener;
import sonchain.blockchain.listener.SonChainListenerAdapter;
import sonchain.blockchain.util.ByteUtil;
import sonchain.blockchain.util.FastByteComparisons;
import sonchain.blockchain.vm.program.invoke.ProgramInvokeFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import org.apache.commons.collections4.map.LRUMap;
import org.apache.log4j.Logger;
import org.bouncycastle.util.encoders.Hex;

import static sonchain.blockchain.listener.SonChainListener.PendingTransactionState.DROPPED;
import static sonchain.blockchain.listener.SonChainListener.PendingTransactionState.INCLUDED;
import static sonchain.blockchain.listener.SonChainListener.PendingTransactionState.NEW_PENDING;
import static sonchain.blockchain.listener.SonChainListener.PendingTransactionState.PENDING;
import sonchain.blockchain.listener.SonChainListener.PendingTransactionState;
import sonchain.blockchain.service.DataCenter;

public class PendingStateImpl implements PendingState {

    public static class TransactionSortedSet extends TreeSet<Transaction> {
        public TransactionSortedSet() {
            super(new Comparator<Transaction>() {
                @Override
                public int compare(Transaction tx1, Transaction tx2) {
                    long nonceDiff = ByteUtil.byteArrayToLong(tx1.getNonce()) -
                            ByteUtil.byteArrayToLong(tx2.getNonce());
                    if (nonceDiff != 0) {
                        return nonceDiff > 0 ? 1 : -1;
                    }
                    return FastByteComparisons.compareTo(tx1.getHash(), 0, 32, tx2.getHash(), 0, 32);
                }
            });
        }
    }

	public static final Logger m_logger = Logger.getLogger(PendingStateImpl.class);
    // to filter out the transactions we have already processed
    // transactions could be sent by peers even if they were already included into blocks
    private Block m_best = null;
    private BlockChain m_blockchain = null;
    private BlockStore m_blockStore = null;
    private CommonConfig m_commonConfig = CommonConfig.getDefault();
    private Object m_dummyObject = new Object();
    private SonChainListener m_listener = null;
    private Repository m_pendingState = null;
    private List<PendingTransaction> m_pendingTransactions = new ArrayList<>();
    private ProgramInvokeFactory m_programInvokeFactory = null;
    private Map<ByteArrayWrapper, Object> m_receivedTxs = new LRUMap<>(100000);
    private TransactionStore m_transactionStore = null;
    
    public PendingStateImpl(SonChainListener listener, BlockChain blockchain) {
    	m_listener = listener;
    	m_blockchain = blockchain;
    	m_blockStore = blockchain.getBlockStore();
        m_transactionStore = blockchain.getTransactionStore();
        m_programInvokeFactory = blockchain.getProgramInvokeFactory();        
    }

    private boolean addNewTxIfNotExist(Transaction tx) {
    	m_logger.debug("addNewTxIfNotExist start. TransactionInfo:" + tx.toString());
        return m_receivedTxs.put(new ByteArrayWrapper(tx.getHash()), m_dummyObject) == null;
    }

    @Override
    public void addPendingTransaction(Transaction tx) {
    	m_logger.debug("addPendingTransaction start.");
        addPendingTransactions(Collections.singletonList(tx));
    	m_logger.debug("addPendingTransaction end.");
    }
    
    private boolean addPendingTransactionImpl(Transaction tx) {
    	m_logger.debug("addPendingTransactionImpl start. TransactionInfo:" + tx.toString());
        TransactionReceipt newReceipt = new TransactionReceipt();
        newReceipt.setTransaction(tx);
        String err = validate(tx);
        TransactionReceipt txReceipt = null;
        if (err != null && err.length() > 0) {
            txReceipt = createDroppedReceipt(tx, err);
        } else {
            txReceipt = executeTx(tx);
        }
        if (!txReceipt.isValid()) {
            fireTxUpdate(txReceipt, DROPPED, getBestBlock());
        } else {
        	m_pendingTransactions.add(new PendingTransaction(tx, getBestBlock().getNumber()));
            fireTxUpdate(txReceipt, NEW_PENDING, getBestBlock());
        }
        boolean result = txReceipt.isValid();
    	m_logger.debug("addPendingTransactionImpl end.result:" + result);
    	return result;
    }

    @Override
    public synchronized List<Transaction> addPendingTransactions(List<Transaction> transactions) {
    	m_logger.debug("addPendingTransactions start.");
        int unknownTxCount = 0;
        List<Transaction> newPending = new ArrayList<>();
        for (Transaction tx : transactions) {
            if (addNewTxIfNotExist(tx)) {
            	unknownTxCount++;
                if (addPendingTransactionImpl(tx)) {
                    newPending.add(tx);
                }
            }
        }
        m_logger.debug(String.format(
        		"Wire transaction list added: total: {%d}, new: {%d}, valid (added to pending): {%s} "
        		+ "(current #of known txs: {%d})",
                transactions.size(), unknownTxCount, newPending.toString(), m_receivedTxs.size()));
        if (!newPending.isEmpty()) {
        	m_listener.onPendingTransactionsReceived(newPending);
            m_listener.onPendingStateChanged(PendingStateImpl.this);
        }
    	m_logger.debug("addPendingTransactions end.");
        return newPending;
    }
    
    private void clearOutdated(final long blockNumber) {
    	m_logger.debug("clearOutdated start.");
        List<PendingTransaction> outdated = new ArrayList<>();
        for (PendingTransaction tx : m_pendingTransactions) {
            if (blockNumber - tx.getBlockNumber() > DataCenter.m_config.m_transactionOutdatedThreshold) {
                outdated.add(tx);

                fireTxUpdate(createDroppedReceipt(tx.getTransaction(),
                        "Tx was not included into last " 
                        		+ DataCenter.m_config.m_transactionOutdatedThreshold + " blocks"),
                        DROPPED, getBestBlock());
            }
        }
        if (outdated.isEmpty()) {
        	return;
        }
        if (m_logger.isDebugEnabled())
        {
            for (PendingTransaction tx : outdated){
            	m_logger.trace(String.format(
                        "Clear outdated pending transaction, block.number: [{%d}] hash: [{%s}]",
                        tx.getBlockNumber(),
                        Hex.toHexString(tx.getHash()))
                );
            }
        }
        m_pendingTransactions.removeAll(outdated);
    	m_logger.debug("clearOutdated end.");
    }
    
    private void clearPending(Block block, List<TransactionReceipt> receipts) {
    	m_logger.debug("clearPending start.");
        for (int i = 0; i < block.getTransactionsList().size(); i++) {
            Transaction tx = block.getTransactionsList().get(i);
            PendingTransaction pend = new PendingTransaction(tx);
            if (m_pendingTransactions.remove(pend)) {
                try {
                	m_logger.trace(String.format("Clear pending transaction, hash: [{%s}]",            
                			Hex.toHexString(tx.getHash())));
                    TransactionReceipt receipt;
                    if (receipts != null) {
                        receipt = receipts.get(i);
                    } else {
                        TransactionInfo info = getTransactionInfo(tx.getHash(), block.getHash());
                        receipt = info.getReceipt();
                    }
                    fireTxUpdate(receipt, INCLUDED, block);
                } catch (Exception e) {
                	m_logger.error("Exception creating onPendingTransactionUpdate (block: " + block.getShortDescr() + ", tx: " + i, e);
                }
            }
        }
    	m_logger.debug("clearPending end.");
    }

    private TransactionReceipt createDroppedReceipt(Transaction tx, String error) {
    	m_logger.debug("createDroppedReceipt start. TransactionInfo:" + tx.toString() + " error:" + error);
        TransactionReceipt txReceipt = new TransactionReceipt();
        txReceipt.setTransaction(tx);
        txReceipt.setError(error);
    	m_logger.debug("createDroppedReceipt end. TransactionReceiptInfo:" + txReceipt.toString());
        return txReceipt;
    }

    private Block createFakePendingBlock() {
    	m_logger.debug("createFakePendingBlock start.");
        // creating fake lightweight calculated block with no hashes calculations
        Block block = new Block(m_best.getHash(),
                new byte[32],
                m_best.getNumber() + 1,
                m_best.getTimestamp() + 1,  // block time
                new byte[0],  	// extra data
                new byte[32],  	// receiptsRoot
                new byte[32],   // TransactionsRoot
                new byte[32], 	// stateRoot
                Collections.<Transaction>emptyList());// tx list
    	m_logger.debug("createFakePendingBlock end.");
        return block;
    }
    
    private TransactionReceipt executeTx(Transaction tx) {
    	m_logger.debug("executeTx start. TransactionInfo:" + tx.toString());
    	m_logger.trace(String.format("Apply pending state tx: {%s}", Hex.toHexString(tx.getHash())));
        Block best = getBestBlock();
        TransactionExecutor executor = new TransactionExecutor(
                tx, best.getMinedBy(), getRepository(),
                m_blockStore, m_programInvokeFactory, createFakePendingBlock(), new SonChainListenerAdapter())
                .withCommonConfig(m_commonConfig);
        executor.init();
        executor.execute();
        executor.go();
        executor.finalization();
    	m_logger.debug("executeTx end.");
        return executor.getReceipt();
    }

    private Block findCommonAncestor(Block b1, Block b2) {
    	m_logger.debug("findCommonAncestor start.");
        while(!b1.isEqual(b2)) {
            if (b1.getNumber() >= b2.getNumber()) {
                b1 = m_blockchain.getBlockByHash(b1.getParentHash());
            }

            if (b1.getNumber() < b2.getNumber()) {
                b2 = m_blockchain.getBlockByHash(b2.getParentHash());
            }
            if (b1 == null || b2 == null) {
                // shouldn't happen
                throw new RuntimeException("Pending state can't find common ancestor: one of blocks has a gap");
            }
        }
    	m_logger.debug("findCommonAncestor end.");
        return b1;
    }

    private void fireTxUpdate(TransactionReceipt txReceipt, PendingTransactionState state, Block block) {
    	m_logger.debug("fireTxUpdate start. TransactionReceipt:" + txReceipt.toString()
    			+ " PendingTransactionState:" + state.toString() + " BlockInfo:" + block.toString());
        if (m_logger.isDebugEnabled()) {
        	m_logger.debug(String.format("PendingTransactionUpdate: (Total: %3s) %12s : %s %8s %s [%s]",
                    getPendingTransactions().size(),
                    state, Hex.toHexString(txReceipt.getTransaction().getSender()).substring(0, 8),
                    ByteUtil.byteArrayToLong(txReceipt.getTransaction().getNonce()),
                    block.getShortDescr(), txReceipt.getError()));
        }
        if(m_listener != null)
        {
        	m_listener.onPendingTransactionUpdate(txReceipt, state, block);
        }
    	m_logger.debug("fireTxUpdate end.");
    }

    public Block getBestBlock() {
    	m_logger.debug("getBestBlock start.");
        if (m_best == null) {
        	m_best = m_blockchain.getBestBlock();
        }
        return m_best;
    }

    private Repository getOrigRepository() {
    	m_logger.debug("getOrigRepository start.");
    	return m_blockchain.getRepositorySnapshot();
    }

    @Override
    public synchronized List<Transaction> getPendingTransactions() {
    	m_logger.debug("getPendingTransactions start.");
        List<Transaction> txs = new ArrayList<>();
        for (PendingTransaction tx : m_pendingTransactions) {
            txs.add(tx.getTransaction());
            if(txs.size() >= DataCenter.m_config.m_maxTransactionsPerBlock){
            	break;
            }
        }
    	m_logger.debug("getPendingTransactions end.");
        return txs;
    }

    @Override
    public synchronized Repository getRepository() {
    	m_logger.debug("getRepository start.");
        if (m_pendingState == null) {
            init();
        }
        return m_pendingState;
    }
    
    private TransactionInfo getTransactionInfo(byte[] txHash, byte[] blockHash) {
    	m_logger.debug("getTransactionInfo start.");
        TransactionInfo info = m_transactionStore.get(txHash, blockHash);
        Transaction tx = m_blockchain.getBlockByHash(info.getBlockHash()).getTransactionsList().get(info.getIndex());
        info.getReceipt().setTransaction(tx);
    	m_logger.debug("getTransactionInfo end.");
        return info;
    }

    public void init() {
    	m_logger.debug("init start.");
    	m_pendingState = getOrigRepository().startTracking();
    	m_logger.debug("init end.");
    }

    @Override
    public synchronized void processBest(Block newBlock, List<TransactionReceipt> receipts) {
    	m_logger.debug("processBest start.");
        if (getBestBlock() != null && !getBestBlock().isParentOf(newBlock)) {
            // need to switch the state to another fork
            Block commonAncestor = findCommonAncestor(getBestBlock(), newBlock);
            if (m_logger.isDebugEnabled()) {
            	m_logger.debug("New best block from another fork: "
                    + newBlock.getShortDescr() + ", old best: " + getBestBlock().getShortDescr()
                    + ", ancestor: " + commonAncestor.getShortDescr());
            }
            // first return back the transactions from forked blocks
            Block rollback = getBestBlock();
            while(!rollback.isEqual(commonAncestor)) {
                List<PendingTransaction> blockTxs = new ArrayList<>();
                for (Transaction tx : rollback.getTransactionsList()) {
                	m_logger.trace("Returning transaction back to pending: " + tx);
                    blockTxs.add(new PendingTransaction(tx, commonAncestor.getNumber()));
                }
                m_pendingTransactions.addAll(0, blockTxs);
                rollback = m_blockchain.getBlockByHash(rollback.getParentHash());
            }
            // rollback the state snapshot to the ancestor
            m_pendingState = getOrigRepository().getSnapshotTo(commonAncestor.getStateRoot()).startTracking();
            // next process blocks from new fork
            Block main = newBlock;
            List<Block> mainFork = new ArrayList<>();
            while(!main.isEqual(commonAncestor)) {
                mainFork.add(main);
                main = m_blockchain.getBlockByHash(main.getParentHash());
            }
            // processing blocks from ancestor to new block
            for (int i = mainFork.size() - 1; i >= 0; i--) {
                processBestInternal(mainFork.get(i), null);
            }
        } else {
        	m_logger.debug("PendingStateImpl.processBest: " + newBlock.getShortDescr());
            processBestInternal(newBlock, receipts);
        }
        m_best = newBlock;
        updateState(newBlock);
        m_listener.onPendingStateChanged(PendingStateImpl.this);
    	m_logger.debug("processBest end.");
    }
    
    private void processBestInternal(Block block, List<TransactionReceipt> receipts) {
        clearPending(block, receipts);
        clearOutdated(block.getNumber());
    }
    
    public void setBlockchain(BlockChainImpl blockchain) {
        m_blockchain = blockchain;
    }

    public synchronized void trackTransaction(Transaction tx) {
    	m_logger.debug("trackTransaction start.");
        List<TransactionInfo> infos = m_transactionStore.get(tx.getHash());
        if (!infos.isEmpty()) {
            for (TransactionInfo info : infos) {
                Block txBlock = m_blockStore.getBlockByHash(info.getBlockHash());
                if (txBlock.isEqual(m_blockStore.getChainBlockByNumber(txBlock.getNumber()))) {
                    // transaction included to the block on main chain
                    info.getReceipt().setTransaction(tx);
                    fireTxUpdate(info.getReceipt(), INCLUDED, txBlock);
                    return;
                }
            }
        }
        addPendingTransaction(tx);
    	m_logger.debug("trackTransaction end.");
    }

    private void updateState(Block block) {
    	m_logger.debug("updateState start.");
    	m_pendingState = getOrigRepository().startTracking();
        for (PendingTransaction tx : m_pendingTransactions) {
            TransactionReceipt receipt = executeTx(tx.getTransaction());
            fireTxUpdate(receipt, PENDING, block);
        }
    	m_logger.debug("updateState end.");
    }
    
    private String validate(Transaction tx) {
    	m_logger.debug("validate start. TransactionInfo:" + tx.toString());
        try {
            tx.verify();
        } catch (Exception e) {
            String error = String.format("Invalid transaction: %s", e.getMessage());
            m_logger.error(error);
            return error;
        }
        finally{
        	m_logger.debug("validate end.");
            return "";
        }
    }
}
