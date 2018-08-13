package sonchain.blockchain.sync;

import java.util.*;
import java.util.concurrent.CountDownLatch;

import org.apache.log4j.Logger;

import sonchain.blockchain.config.CommonConfig;
import sonchain.blockchain.core.Block;
import sonchain.blockchain.core.BlockHeader;
import sonchain.blockchain.core.TransactionInfo;
import sonchain.blockchain.core.TransactionReceipt;
import sonchain.blockchain.crypto.HashUtil;
import sonchain.blockchain.datasource.DataSourceArray;
import sonchain.blockchain.db.DbFlushManager;
import sonchain.blockchain.db.IndexedBlockStore;
import sonchain.blockchain.db.TransactionStore;
import sonchain.blockchain.service.DataCenter;
import sonchain.blockchain.util.FastByteComparisons;

public class ReceiptsDownloader {
	public static final Logger m_logger = Logger.getLogger(ReceiptsDownloader.class);
    private SyncPool m_syncPool = null;
    private DbFlushManager m_dbFlushManager = CommonConfig.getDefault().getDbFlushManager();
    private TransactionStore m_txStore = DataCenter.getSonChainImpl().getBlockChain().getTransactionStore();
    private IndexedBlockStore m_blockStore
    	= (IndexedBlockStore)DataCenter.getSonChainImpl().getBlockChain().getBlockStore();
    private DataSourceArray<BlockHeader> m_headerStore 
    	= CommonConfig.getDefault().getHeaderSource(); 
    private long m_fromBlock = 0;
    private long m_toBlock = 0;
    private Set<Long> m_completedBlocks = new HashSet<>();

    private long m_preCurrentTimeMillis = 0;
    private int m_cnt = 0;

    private Thread m_retrieveThread = null;
    private CountDownLatch m_stopLatch = new CountDownLatch(1);

    public ReceiptsDownloader(long fromBlock, long toBlock) {
    	m_fromBlock = fromBlock;
    	m_toBlock = toBlock;
    }

    public void startImporting() {
    	m_retrieveThread = new Thread("FastsyncReceiptsFetchThread") {
            @Override
            public void run() {
                retrieveLoop();
            }
        };
        m_retrieveThread.start();
    }

    private List<List<byte[]>> getToDownload(int maxAskSize, int maxAsks) {
        List<byte[]> toDownload = getToDownload(maxAskSize * maxAsks);
        List<List<byte[]>> ret = new ArrayList<>();
        for (int i = 0; i < toDownload.size(); i += maxAskSize) {
            ret.add(toDownload.subList(i, Math.min(toDownload.size(), i + maxAskSize)));
        }
        return ret;
    }

    private synchronized List<byte[]> getToDownload(int maxSize) {
        List<byte[]> ret = new ArrayList<>();
        for (long i = m_fromBlock; i < m_toBlock && maxSize > 0; i++) {
            if (!m_completedBlocks.contains(i)) {
                BlockHeader header = m_headerStore.get((int) i);

                // Skipping download for blocks with no transactions
            	//TODO
//                if (FastByteComparisons.equal(header.getReceiptsRoot(), HashUtil.EMPTY_TRIE_HASH)) {
//                    finalizeBlock(header.getNumber());
//                    continue;
//                }

                ret.add(header.getHash());
                maxSize--;
            }
        }
        return ret;
    }

    private void processDownloaded(byte[] blockHash, List<TransactionReceipt> receipts) {
        Block block = m_blockStore.getBlockByHash(blockHash);
        if (block.getBlockNumber() >= m_fromBlock
        		&& validate(block, receipts)
        		&& !m_completedBlocks.contains(block.getBlockNumber())) {
            for (int i = 0; i < receipts.size(); i++) {
                TransactionReceipt receipt = receipts.get(i);
                TransactionInfo txInfo = new TransactionInfo(receipt, block.getHash(), i);
                txInfo.setTransaction(block.getTransactionsList().get(i));
                m_txStore.put(txInfo);
            }
            finalizeBlock(block.getBlockNumber());
        }
    }

    private void finalizeBlock(Long blockNumber) {
        synchronized (this) {
        	m_completedBlocks.add(blockNumber);
            while (m_fromBlock < m_toBlock && m_completedBlocks.remove(m_fromBlock)){
            	m_fromBlock++;
            }
            if (m_fromBlock >= m_toBlock) {
            	finishDownload();
            }
            m_cnt++;
            if (m_cnt % 1000 == 0){
            	m_logger.info("FastSync: downloaded receipts for " + m_cnt + " blocks.");
            }
        }
        m_dbFlushManager.commit();
    }

    private boolean validate(Block block, List<TransactionReceipt> receipts) {
        byte[] receiptsRoot = DataCenter.getSonChainImpl().getBlockChain().calcReceiptsTrie(receipts);
        return false;
        //TODO
        //return FastByteComparisons.equal(receiptsRoot, block.getReceiptsRoot());
    }

    private void retrieveLoop() {
        List<List<byte[]>> toDownload = Collections.emptyList();
        while (!Thread.currentThread().isInterrupted()) {
            try {
                if (toDownload.isEmpty()) {
                    toDownload = getToDownload(100, 20);
                }

//                Channel idle = getAnyPeer();
//                if (idle != null) {
//                    final List<byte[]> list = toDownload.remove(0);
//                    ListenableFuture<List<List<TransactionReceipt>>> future =
//                            ((Eth63) idle.getEthHandler()).requestReceipts(list);
//                    if (future != null) {
//                        Futures.addCallback(future, new FutureCallback<List<List<TransactionReceipt>>>() {
//                            @Override
//                            public void onSuccess(List<List<TransactionReceipt>> result) {
//                                for (int i = 0; i < result.size(); i++) {
//                                    processDownloaded(list.get(i), result.get(i));
//                                }
//                            }
//                            @Override
//                            public void onFailure(Throwable t) {}
//                        });
//                    }
//                } else {
//                    try {
//                        Thread.sleep(100);
//                    } catch (InterruptedException e) {
//                        break;
//                    }
//                }
            } catch (Exception e) {
            	m_logger.warn("Unexpected during receipts downloading", e);
            }
        }
    }

    /**
     * Download could block chain synchronization occupying all peers
     * Prevents this by leaving one peer without work
     * Fallbacks to any peer when low number of active peers available
     */
    //Channel getAnyPeer() {
    //    return syncPool.getActivePeersCount() > 2 ? syncPool.getNotLastIdle() : syncPool.getAnyIdle();
    //}

    public int getDownloadedBlocksCount() {
        return m_cnt;
    }

    public void stop() {
    	m_retrieveThread.interrupt();
    	m_stopLatch.countDown();
    }

    public void waitForStop() {
        try {
        	m_stopLatch.await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    protected void finishDownload() {
        stop();
    }

}
