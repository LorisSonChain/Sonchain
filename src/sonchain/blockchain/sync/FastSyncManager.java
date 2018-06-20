package sonchain.blockchain.sync;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.log4j.Logger;
import org.bouncycastle.util.encoders.Hex;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import sonchain.blockchain.accounts.AccountState;
import sonchain.blockchain.client.SonChainServiceCT;
import sonchain.blockchain.config.CommonConfig;
import sonchain.blockchain.core.Block;
import sonchain.blockchain.core.BlockChain;
import sonchain.blockchain.core.BlockChainImpl;
import sonchain.blockchain.core.BlockHeader;
import sonchain.blockchain.core.BlockIdentifier;
import sonchain.blockchain.crypto.HashUtil;
import sonchain.blockchain.datasource.DbSource;
import sonchain.blockchain.db.DbFlushManager;
import sonchain.blockchain.db.IndexedBlockStore;
import sonchain.blockchain.db.StateSource;
import sonchain.blockchain.facade.SyncStatus;
import sonchain.blockchain.listener.CompositeSonChainListener;
import sonchain.blockchain.listener.SonChainListener;
import sonchain.blockchain.service.DataCenter;
import sonchain.blockchain.util.ByteArrayMap;
import sonchain.blockchain.util.CompactEncoder;
import sonchain.blockchain.util.FastByteComparisons;
import sonchain.blockchain.util.Functional;
import sonchain.blockchain.util.Value;

public class FastSyncManager {

	public static final Logger m_logger = Logger.getLogger(FastSyncManager.class);
    private final static long REQUEST_TIMEOUT = 5 * 1000;
    private final static int REQUEST_MAX_NODES = 384;
    private final static int NODE_QUEUE_BEST_SIZE = 100_000;
    private final static int MIN_PEERS_FOR_PIVOT_SELECTION = 5;
    private final static int FORCE_SYNC_TIMEOUT = 60 * 1000;
    private final static int PIVOT_DISTANCE_FROM_HEAD = 1024;
    private final static int MSX_DB_QUEUE_SIZE = 20000;

    //private static final Capability ETH63_CAPABILITY = new Capability(Capability.ETH, (byte) 63);

    public static final byte[] FASTSYNC_DB_KEY_SYNC_STAGE = HashUtil.sha3(
    		"Key in state DB indicating fastsync stage in progress".getBytes());
    public static final byte[] FASTSYNC_DB_KEY_PIVOT = HashUtil.sha3(
    		"Key in state DB with encoded selected pivot block".getBytes());

    private SyncPool m_pool = null;
    private BlockChain m_blockchain = DataCenter.getSonChainImpl().getBlockChain();

    private IndexedBlockStore m_blockStore = (IndexedBlockStore) DataCenter.getSonChainImpl().getBlockChain().getBlockStore();

    private SyncManager m_syncManager = new SyncManager();
    private DbSource<byte[]> m_blockchainDB = CommonConfig.getDefault().getBlockChainDB();

    private DbFlushManager dbFlushManager =  CommonConfig.getDefault().getDbFlushManager();
    private StateSource m_stateSource;

    private FastSyncDownloader downloader;
    private CompositeSonChainListener listener;
    private int nodesInserted = 0;

    private boolean fastSyncInProgress = false;

    private BlockingQueue<TrieNodeRequest> dbWriteQueue = new LinkedBlockingQueue<>();
    private Thread dbWriterThread;
    private Thread fastSyncThread;
    private int dbQueueSizeMonitor = -1;

    private BlockHeader m_pivot;
    private HeadersDownloader m_headersDownloader = new HeadersDownloader(CommonConfig.getDefault().getHeaderValidator());
    private BlockBodiesDownloader m_blockBodiesDownloader = new BlockBodiesDownloader(CommonConfig.getDefault().getHeaderValidator());
    private ReceiptsDownloader m_receiptsDownloader = null;
    private long forceSyncRemains;

    private void waitDbQueueSizeBelow(int size) {
        synchronized (this) {
            try {
                dbQueueSizeMonitor = size;
                while (dbWriteQueue.size() > size) {
                	wait();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                dbQueueSizeMonitor = -1;
            }
        }
    }


    void init() {
        dbWriterThread = new Thread("FastSyncDBWriter") {
            @Override
            public void run() {
                try {
                    while (!Thread.currentThread().isInterrupted()) {
                        synchronized (FastSyncManager.this) {
                            if (dbQueueSizeMonitor >= 0 && dbWriteQueue.size() <= dbQueueSizeMonitor) {
                                FastSyncManager.this.notifyAll();
                            }
                        }
                        TrieNodeRequest request = dbWriteQueue.take();
                        nodesInserted++;
                        //m_stateSource.getNoJournalSource().put(request.nodeHash, request.response);
                        if (nodesInserted % 1000 == 0) {
                            dbFlushManager.commit();
                            m_logger.debug("FastSyncDBWriter: commit: dbWriteQueue.size = " + dbWriteQueue.size());
                        }
                    }
                } catch (InterruptedException e) {
                } catch (Exception e) {
                	m_logger.error("Fatal FastSync error while writing data", e);
                }
            }
        };
        dbWriterThread.start();

        fastSyncThread = new Thread("FastSyncLoop") {
            @Override
            public void run() {
                try {
                    main();
                } catch (Exception e) {
                	m_logger.error("Fatal FastSync loop error", e);
                }
            }
        };
        fastSyncThread.start();
    }
    
    public SyncStatus getSyncState() {
        if (!isFastSyncInProgress()) return new SyncStatus(SyncStatus.SyncStage.Complete, 0, 0);

        if (m_pivot == null) {
            return new SyncStatus(SyncStatus.SyncStage.PivotBlock,
                    (FORCE_SYNC_TIMEOUT - forceSyncRemains) / 1000, FORCE_SYNC_TIMEOUT / 1000);
        }
        CompositeSonChainListener.SyncState syncStage = getSyncStage();
        switch (syncStage) {
            case UNSECURE:
                return new SyncStatus(SyncStatus.SyncStage.StateNodes, nodesInserted,
                        nodesQueue.size() + pendingNodes.size() + nodesInserted);
            case SECURE:
                return new SyncStatus(SyncStatus.SyncStage.Headers, m_headersDownloader.getHeadersLoaded(),
                		m_pivot.getNumber());
            case COMPLETE:
                if (m_receiptsDownloader != null) {
                    return new SyncStatus(SyncStatus.SyncStage.Receipts,
                            m_receiptsDownloader.getDownloadedBlocksCount(), m_pivot.getNumber());
                } else if (m_blockBodiesDownloader!= null) {
                    return new SyncStatus(SyncStatus.SyncStage.BlockBodies,
                    		m_blockBodiesDownloader.getDownloadedCount(), m_pivot.getNumber());
                } else {
                    return new SyncStatus(SyncStatus.SyncStage.BlockBodies, 0, m_pivot.getNumber());
                }
        }
        return new SyncStatus(SyncStatus.SyncStage.Complete, 0, 0);
    }

    enum TrieNodeType {
        STATE,
        STORAGE,
        CODE
    }

    int stateNodesCnt = 0;
    int codeNodesCnt = 0;
    int storageNodesCnt = 0;

    private class TrieNodeRequest {
        TrieNodeType type;
        byte[] nodeHash;
        byte[] response;
        final Map<Long, Long> requestSent = new HashMap<>();

        TrieNodeRequest(TrieNodeType type, byte[] nodeHash) {
            this.type = type;
            this.nodeHash = nodeHash;

            switch (type) {
                case STATE: stateNodesCnt++; break;
                case CODE: codeNodesCnt++; break;
                case STORAGE: storageNodesCnt++; break;
            }
        }

        List<TrieNodeRequest> createChildRequests() {
            if (type == TrieNodeType.CODE) {
                return Collections.emptyList();
            }

            List<Object> node = Value.fromRlpEncoded(response).asList();
            List<TrieNodeRequest> ret = new ArrayList<>();
            if (type == TrieNodeType.STATE) {
                if (node.size() == 2 && CompactEncoder.hasTerminator((byte[]) node.get(0))) {
                    byte[] nodeValue = (byte[]) node.get(1);
                    AccountState state = new AccountState(nodeValue);

                    if (!FastByteComparisons.equal(HashUtil.EMPTY_DATA_HASH, state.getCodeHash())) {
                        ret.add(new TrieNodeRequest(TrieNodeType.CODE, state.getCodeHash()));
                    }
                    if (!FastByteComparisons.equal(HashUtil.EMPTY_TRIE_HASH, state.getStateRoot())) {
                        ret.add(new TrieNodeRequest(TrieNodeType.STORAGE, state.getStateRoot()));
                    }
                    return ret;
                }
            }

            List<byte[]> childHashes = getChildHashes(node);
            for (byte[] childHash : childHashes) {
                ret.add(new TrieNodeRequest(type, childHash));
            }
            return ret;
        }

        public void reqSent(Long requestId) {
            synchronized (FastSyncManager.this) {
                Long timestamp = System.currentTimeMillis();
                requestSent.put(requestId, timestamp);
            }
        }

        public Set<Long> requestIdsSnapshot() {
            synchronized (FastSyncManager.this) {
                return new HashSet<Long>(requestSent.keySet());
            }
        }

        @Override
        public String toString() {
            return "TrieNodeRequest{" +
                    "type=" + type +
                    ", nodeHash=" + Hex.toHexString(nodeHash) +
                    '}';
        }
    }

    private static List<byte[]> getChildHashes(List<Object> siblings) {
        List<byte[]> ret = new ArrayList<>();
        if (siblings.size() == 2) {
            Value val = new Value(siblings.get(1));
            if (val.isHashCode() && !CompactEncoder.hasTerminator((byte[]) siblings.get(0)))
                ret.add(val.asBytes());
        } else {
            for (int j = 0; j < 16; ++j) {
                Value val = new Value(siblings.get(j));
                if (val.isHashCode())
                    ret.add(val.asBytes());
            }
        }
        return ret;
    }

    Deque<TrieNodeRequest> nodesQueue = new LinkedBlockingDeque<>();
    ByteArrayMap<TrieNodeRequest> pendingNodes = new ByteArrayMap<>();
    Long requestId = 0L;

    private synchronized void purgePending(byte[] hash) {
        TrieNodeRequest request = pendingNodes.get(hash);
        if (request.requestSent.isEmpty()) pendingNodes.remove(hash);
    }

    synchronized void processTimeouts() {
        long cur = System.currentTimeMillis();
        for (TrieNodeRequest request : new ArrayList<>(pendingNodes.values())) {
            Iterator<Map.Entry<Long, Long>> reqIterator = request.requestSent.entrySet().iterator();
            while (reqIterator.hasNext()) {
                Map.Entry<Long, Long> requestEntry = reqIterator.next();
                if (cur - requestEntry.getValue() > REQUEST_TIMEOUT) {
                    reqIterator.remove();
                    purgePending(request.nodeHash);
                    nodesQueue.addFirst(request);
                }
            }
        }
    }

    synchronized void processResponse(TrieNodeRequest req) {
        dbWriteQueue.add(req);
        for (TrieNodeRequest childRequest : req.createChildRequests()) {
            if (nodesQueue.size() > NODE_QUEUE_BEST_SIZE) {
                // reducing queue by traversing tree depth-first
                nodesQueue.addFirst(childRequest);
            } else {
                // enlarging queue by traversing tree breadth-first
                nodesQueue.add(childRequest);
            }
        }
    }

    boolean requestNextNodes(int cnt) {
		SonChainServiceCT serviceCT = DataCenter.getAnyOneSonChainServiceCT();
        if (serviceCT != null) {
            final List<byte[]> hashes = new ArrayList<>();
            final List<TrieNodeRequest> requestsSent = new ArrayList<>();
            final Set<Long> sentRequestIds = new HashSet<>();
            synchronized (this) {
                for (int i = 0; i < cnt && !nodesQueue.isEmpty(); i++) {
                    TrieNodeRequest req = nodesQueue.poll();
                    hashes.add(req.nodeHash);
                    TrieNodeRequest request = pendingNodes.get(req.nodeHash);
                    if (request == null) {
                        pendingNodes.put(req.nodeHash, req);
                        request = req;
                    }
                    sentRequestIds.add(requestId);
                    request.reqSent(requestId);
                    requestId++;
                    requestsSent.add(request);
                }
            }
            if (hashes.size() > 0) {
            	m_logger.trace("Requesting " + hashes.size() + " nodes from peer: " + serviceCT.getSonChainHostInfo().toString());
                ListenableFuture<List<Pair<byte[], byte[]>>> nodes = serviceCT.requestTrieNodes(hashes);
                final long reqTime = System.currentTimeMillis();
                Futures.addCallback(nodes, new FutureCallback<List<Pair<byte[], byte[]>>>() {
                    @Override
                    public void onSuccess(List<Pair<byte[], byte[]>> result) {
                        try {
                            synchronized (FastSyncManager.this) {
                            	m_logger.trace("Received " + result.size() + " nodes (of " + hashes.size() + ") from peer: " 
                            			+ serviceCT.getSonChainHostInfo().toString());
                                for (Pair<byte[], byte[]> pair : result) {
                                    TrieNodeRequest request = pendingNodes.get(pair.getKey());
                                    if (request == null) {
                                        long t = System.currentTimeMillis();
                                        m_logger.debug("Received node which was not requested: " + Hex.toHexString(pair.getKey())
                                        		+ " from " + serviceCT.getSonChainHostInfo().toString());
                                        return;
                                    }
                                    Set<Long> intersection = request.requestIdsSnapshot();
                                    intersection.retainAll(sentRequestIds);
                                    if (!intersection.isEmpty()) {
                                        Long inter = intersection.iterator().next();
                                        request.requestSent.remove(inter);
                                        purgePending(pair.getKey());
                                        request.response = pair.getValue();
                                        processResponse(request);
                                    }
                                }

                                FastSyncManager.this.notifyAll();
                                //TODO
                                //idle.getNodeStatistics().eth63NodesRequested.add(hashes.size());
                                //idle.getNodeStatistics().eth63NodesReceived.add(result.size());
                                //idle.getNodeStatistics().eth63NodesRetrieveTime.add(System.currentTimeMillis() - reqTime);
                            }
                        } catch (Exception e) {
                        	m_logger.error("Unexpected error processing nodes", e);
                        }
                    }

                    @Override
                    public void onFailure(Throwable t) {
                    	m_logger.warn("Error with Trie Node request: " + t);
                        synchronized (FastSyncManager.this) {
                            for (byte[] hash : hashes) {
                                final TrieNodeRequest request = pendingNodes.get(hash);
                                if (request == null) continue;
                                Set<Long> intersection = request.requestIdsSnapshot();
                                intersection.retainAll(sentRequestIds);
                                if (!intersection.isEmpty()) {
                                    Long inter = intersection.iterator().next();
                                    request.requestSent.remove(inter);
                                    nodesQueue.addFirst(request);
                                    purgePending(hash);
                                }
                            }
                            FastSyncManager.this.notifyAll();
                        }
                    }
                });
                return true;
            } else {
//                idle.getEthHandler().setStatus(SyncState.IDLE);
                return false;
            }
        } else {
            return false;
        }
    }

    void retrieveLoop() {
        try {
            while (!nodesQueue.isEmpty() || !pendingNodes.isEmpty()) {
                try {
                    processTimeouts();
                    while (requestNextNodes(REQUEST_MAX_NODES)) ;
                    synchronized (this) {
                        wait(10);
                    }
                    waitDbQueueSizeBelow(MSX_DB_QUEUE_SIZE);
                    logStat();
                } catch (InterruptedException e) {
                    throw e;
                } catch (Throwable t) {
                	m_logger.error("Error", t);
                }
            }
            waitDbQueueSizeBelow(0);
            dbWriterThread.interrupt();
        } catch (InterruptedException e) {
        	m_logger.warn("Main fast sync loop was interrupted", e);
        }
    }

    private long last = 0;
    private long lastNodeCount = 0;

    private void logStat() {
        long cur = System.currentTimeMillis();
        if (cur - last > 5000) {
        	m_logger.info("FastSync: received: " + nodesInserted + ", known: " + nodesQueue.size() + ", pending: " + pendingNodes.size()
                    + String.format(", nodes/sec: %1$.2f", 1000d * (nodesInserted - lastNodeCount) / (cur - last)));
            last = cur;
            lastNodeCount = nodesInserted;
        }
    }

    private void setSyncStage(CompositeSonChainListener.SyncState stage) {
        if (stage == null) {
            m_blockchainDB.delete(FASTSYNC_DB_KEY_SYNC_STAGE);
        } else {
            m_blockchainDB.put(FASTSYNC_DB_KEY_SYNC_STAGE, new byte[]{(byte) stage.ordinal()});
        }
    }

    private CompositeSonChainListener.SyncState getSyncStage() {
        byte[] bytes = m_blockchainDB.get(FASTSYNC_DB_KEY_SYNC_STAGE);
        if (bytes == null) {
        	return CompositeSonChainListener.SyncState.UNSECURE;
        }
        return CompositeSonChainListener.SyncState.values()[bytes[0]];
    }


    private void syncUnsecure(BlockHeader pivot) {
        byte[] pivotStateRoot = pivot.getStateRoot();
        TrieNodeRequest request = new TrieNodeRequest(TrieNodeType.STATE, pivotStateRoot);
        nodesQueue.add(request);
        m_logger.info("FastSync: downloading state trie at pivot block: " + pivot.getShortDescr());
        setSyncStage(CompositeSonChainListener.SyncState.UNSECURE);
        retrieveLoop();
        m_logger.info("FastSync: state trie download complete! (Nodes count: state: " + stateNodesCnt + ", storage: " 
        		+ storageNodesCnt + ", code: " +codeNodesCnt + ")");
        last = 0;
        logStat();

        m_logger.info("FastSync: downloading 256 blocks prior to pivot block (" + pivot.getShortDescr() + ")");
        downloader.startImporting(pivot.getHash(), 260);
        downloader.waitForStop();
        m_logger.info("FastSync: complete downloading 256 blocks prior to pivot block (" + pivot.getShortDescr() + ")");
        m_blockchain.setBestBlock(m_blockStore.getBlockByHash(pivot.getHash()));
        m_logger.info("FastSync: proceeding to regular sync...");
        final CountDownLatch syncDoneLatch = new CountDownLatch(1);
        listener.addListener(new CompositeSonChainListener() {
            @Override
            public void onSyncDone(SyncState state) {
                syncDoneLatch.countDown();
            }
        });
        m_syncManager.initRegularSync(CompositeSonChainListener.SyncState.UNSECURE);
        m_logger.info("FastSync: waiting for regular sync to reach the blockchain head...");

//        try {
//            syncDoneLatch.await();
//        } catch (InterruptedException e) {
//            throw new RuntimeException(e);
//        }

        m_blockchainDB.put(FASTSYNC_DB_KEY_PIVOT, pivot.getEncoded());
        dbFlushManager.commit();
        dbFlushManager.flush();

        m_logger.info("FastSync: regular sync reached the blockchain head.");
    }

    private void syncSecure() {
    	m_pivot = new BlockHeader(m_blockchainDB.get(FASTSYNC_DB_KEY_PIVOT));

        m_logger.info("FastSync: downloading headers from pivot down to genesis block for ensure pivot block (" + m_pivot.getShortDescr() + ") is secure...");
        m_headersDownloader.init(m_pivot.getHash());
        setSyncStage(CompositeSonChainListener.SyncState.SECURE);
        m_headersDownloader.waitForStop();
        if (!FastByteComparisons.equal(m_headersDownloader.getGenesisHash(), DataCenter.m_config.getGenesis().getHash())) {
        	m_logger.error("FASTSYNC FATAL ERROR: after downloading header chain starting from the pivot block (" +
        			m_pivot.getShortDescr() + ") obtained genesis block doesn't match ours: " 
        			+ Hex.toHexString(m_headersDownloader.getGenesisHash()));
            m_logger.error("Can't recover and exiting now. You need to restart from scratch (all DBs will be reset)");
            System.exit(-666);
        }
        dbFlushManager.commit();
        dbFlushManager.flush();
        m_logger.info("FastSync: all headers downloaded. The state is SECURE now.");
    }

    private void syncBlocksReceipts() {
    	m_pivot = new BlockHeader(m_blockchainDB.get(FASTSYNC_DB_KEY_PIVOT));
        m_logger.info("FastSync: Downloading Block bodies up to pivot block (" + m_pivot.getShortDescr() + ")...");
        setSyncStage(CompositeSonChainListener.SyncState.COMPLETE);
        m_blockBodiesDownloader.startImporting();
        m_blockBodiesDownloader.waitForStop();
        m_logger.info("FastSync: Block bodies downloaded");
        m_logger.info("FastSync: Downloading receipts...");
        m_receiptsDownloader = new ReceiptsDownloader(1, m_pivot.getNumber() + 1);
        m_receiptsDownloader.startImporting();
        m_receiptsDownloader.waitForStop();
        m_logger.info("FastSync: receipts downloaded");
        m_logger.info("FastSync: updating totDifficulties starting from the pivot block...");
        //m_blockchain.updateBlockTotDifficulties((int) pivot.getNumber());
        synchronized (m_blockchain) {
            Block bestBlock = m_blockchain.getBestBlock();
            m_logger.info("FastSync: totDifficulties updated: bestBlock: " + bestBlock.getShortDescr() );
        }
        setSyncStage(null);
        m_blockchainDB.delete(FASTSYNC_DB_KEY_PIVOT);
        dbFlushManager.commit();
        dbFlushManager.flush();
    }

    public void main() {
        if (m_blockchain.getBestBlock().getNumber() == 0 
        		|| getSyncStage() == SonChainListener.SyncState.SECURE 
        		|| getSyncStage() == SonChainListener.SyncState.COMPLETE) {
            // either no DB at all (clear sync or DB was deleted due to UNSECURE stage while initializing
            // or we have incomplete headers/blocks/receipts download

            fastSyncInProgress = true;
            //pool.setNodesSelector(new Functional.Predicate<NodeHandler>() {
            //    @Override
            //    public boolean test(NodeHandler handler) {
            //        if (!handler.getNodeStatistics().capabilities.contains(ETH63_CAPABILITY))
            //            return false;
            //        return true;
            //    }
            //});

            try {
            	CompositeSonChainListener.SyncState origSyncStage = getSyncStage();

                switch (origSyncStage) {
                    case UNSECURE:
                    	m_pivot = getPivotBlock();
                        if (m_pivot.getNumber() == 0) {
                        	m_logger.info("FastSync: too short blockchain, proceeding with regular sync...");
                        	m_syncManager.initRegularSync(CompositeSonChainListener.SyncState.COMPLETE);
                            return;
                        }

                        syncUnsecure(m_pivot);  // regularSync should be inited here
                    case SECURE:
                        if (origSyncStage == CompositeSonChainListener.SyncState.SECURE) {
                        	m_logger.info("FastSync: UNSECURE sync was completed prior to this run, proceeding with next stage...");
                        	m_logger.info("Initializing regular sync");
                        	m_syncManager.initRegularSync(CompositeSonChainListener.SyncState.UNSECURE);
                        }

                        syncSecure();

                        listener.onSyncDone(CompositeSonChainListener.SyncState.SECURE);
                    case COMPLETE:
                        if (origSyncStage == CompositeSonChainListener.SyncState.COMPLETE) {
                        	m_logger.info("FastSync: SECURE sync was completed prior to this run, proceeding with next stage...");
                        	m_logger.info("Initializing regular sync");
                        	m_syncManager.initRegularSync(CompositeSonChainListener.SyncState.SECURE);
                        }

                        syncBlocksReceipts();

                        listener.onSyncDone(CompositeSonChainListener.SyncState.COMPLETE);
                }
                m_logger.info("FastSync: Full sync done.");
            } catch (InterruptedException ex) {
            	m_logger.info("Shutting down due to interruption");
            } finally {
                fastSyncInProgress = false;
                //m_pool.setNodesSelector(null);
            }
        } else {
        	m_logger.info("FastSync: fast sync was completed, best block: (" + m_blockchain.getBestBlock().getShortDescr() + "). " +
                    "Continue with regular sync...");
        	m_syncManager.initRegularSync(CompositeSonChainListener.SyncState.COMPLETE);
        }
    }

    public boolean isFastSyncInProgress() {
        return fastSyncInProgress;
    }

    private BlockHeader getPivotBlock() throws InterruptedException {
        byte[] pivotBlockHash = DataCenter.m_config.getFastSyncPivotBlockHash();
        long pivotBlockNumber = 0;

        long start = System.currentTimeMillis();
        long s = start;

        if (pivotBlockHash != null) {
            m_logger.info("FastSync: fetching trusted pivot block with hash " + Hex.toHexString(pivotBlockHash));
        } else {
        	m_logger.info("FastSync: looking for best block number...");
            BlockIdentifier bestKnownBlock;

            while (true) {
//                List<Channel> allIdle = pool.getAllIdle();
//
//                forceSyncRemains = FORCE_SYNC_TIMEOUT - (System.currentTimeMillis() - start);
//
//                if (allIdle.size() >= MIN_PEERS_FOR_PIVOT_SELECTION || forceSyncRemains < 0 && !allIdle.isEmpty()) {
//                    Channel bestPeer = allIdle.get(0);
//                    for (Channel channel : allIdle) {
//                        if (bestPeer.getEthHandler().getBestKnownBlock().getNumber() < channel.getEthHandler().getBestKnownBlock().getNumber()) {
//                            bestPeer = channel;
//                        }
//                    }
//                    bestKnownBlock = bestPeer.getEthHandler().getBestKnownBlock();
//                    if (bestKnownBlock.getNumber() > 1000) {
//                    	m_logger.info("FastSync: best block " + bestKnownBlock + " found with peer " + bestPeer);
//                        break;
//                    }
//                }
//
//                long t = System.currentTimeMillis();
//                if (t - s > 5000) {
//                	m_logger.info("FastSync: waiting for at least " + MIN_PEERS_FOR_PIVOT_SELECTION + " peers or " + forceSyncRemains / 1000 + " sec to select pivot block... ("
//                            + allIdle.size() + " peers so far)");
//                    s = t;
//                }
//
//                Thread.sleep(500);
            }

            //pivotBlockNumber = Math.max(bestKnownBlock.getNumber() - PIVOT_DISTANCE_FROM_HEAD, 0);
            //m_logger.info("FastSync: fetching pivot block #" + pivotBlockNumber);
        }

        try {
            while (true) {
                BlockHeader result = null;

                if (pivotBlockHash != null) {
                    result = getPivotHeaderByHash(pivotBlockHash);
                } else {
                    Pair<BlockHeader, Long> pivotResult = getPivotHeaderByNumber(pivotBlockNumber);
                    if (pivotResult != null) {
                        if (pivotResult.getRight() != null) {
                            pivotBlockNumber = pivotResult.getRight();
                            if (pivotBlockNumber == 0) {
                                throw new RuntimeException("Cannot fastsync with current set of peers");
                            }
                        } else {
                            result = pivotResult.getLeft();
                        }
                    }
                }

                if (result != null) return result;

                long t = System.currentTimeMillis();
                if (t - s > 5000) {
                	m_logger.info("FastSync: waiting for a peer to fetch pivot block...");
                    s = t;
                }

                Thread.sleep(500);
            }
        } catch (InterruptedException e) {
            throw e;
        } catch (Exception e) {
        	m_logger.error("Unexpected", e);
            throw new RuntimeException(e);
        }
    }

    private BlockHeader getPivotHeaderByHash(byte[] pivotBlockHash) throws Exception {
		SonChainServiceCT serviceCT = DataCenter.getAnyOneSonChainServiceCT();
        if (serviceCT != null) {
            try {
                ListenableFuture<List<BlockHeader>> future =
                		serviceCT.sendGetBlockHeaders(pivotBlockHash, 1, 0, false);
                List<BlockHeader> blockHeaders = future.get(3, TimeUnit.SECONDS);
                if (!blockHeaders.isEmpty()) {
                    BlockHeader ret = blockHeaders.get(0);
                    if (FastByteComparisons.equal(pivotBlockHash, ret.getHash())) {
                    	m_logger.info("Pivot header fetched: " + ret.getShortDescr());
                        return ret;
                    }
                    m_logger.warn("Peer " + serviceCT.getSonChainHostInfo().toString() 
                    		+ " returned pivot block with another hash: " +
                            Hex.toHexString(ret.getHash()) + " Dropping the peer.");
                    //bestIdle.disconnect(ReasonCode.USELESS_PEER);
                } else {
                	m_logger.warn("Peer " + serviceCT.getSonChainHostInfo().toString()
                			+ " doesn't returned correct pivot block. Dropping the peer.");
                    //bestIdle.getNodeStatistics().wrongFork = true;
                    //bestIdle.disconnect(ReasonCode.USELESS_PEER);
                }
            } catch (TimeoutException e) {
            	m_logger.debug("Timeout waiting for answer", e);
            }
        }

        return null;
    }

    /**
     * 1. Get pivotBlockNumber blocks from all peers
     * 2. Ensure that pivot block available from 50% + 1 peer
     * 3. Otherwise proposes new pivotBlockNumber (stepped back)
     * @param pivotBlockNumber      Pivot block number
     * @return     null - if no peers available
     *             null, newPivotBlockNumber - if it's better to try other pivot block number
     *             BlockHeader, null - if pivot successfully fetched and verified by majority of peers
     */
    private Pair<BlockHeader, Long> getPivotHeaderByNumber(long pivotBlockNumber) throws Exception {
//        List<Channel> allIdle = pool.getAllIdle();
//        if (!allIdle.isEmpty()) {
//            try {
//                List<ListenableFuture<List<BlockHeader>>> result = new ArrayList<>();
//
//                for (Channel channel : allIdle) {
//                    ListenableFuture<List<BlockHeader>> future =
//                            channel.getEthHandler().sendGetBlockHeaders(pivotBlockNumber, 1, false);
//                    result.add(future);
//                }
//                ListenableFuture<List<List<BlockHeader>>> successfulRequests = Futures.successfulAsList(result);
//                List<List<BlockHeader>> results = successfulRequests.get(3, TimeUnit.SECONDS);
//
//                Map<BlockHeader, Integer> pivotMap = new HashMap<>();
//                for (List<BlockHeader> blockHeaders : results) {
//                    if (!blockHeaders.isEmpty()) {
//                        BlockHeader currentHeader = blockHeaders.get(0);
//                        if (pivotMap.containsKey(currentHeader)) {
//                            pivotMap.put(currentHeader, pivotMap.get(currentHeader) + 1);
//                        } else {
//                            pivotMap.put(currentHeader, 1);
//                        }
//                    }
//                }
//
//                int peerCount = allIdle.size();
//                for (Map.Entry<BlockHeader, Integer> pivotEntry : pivotMap.entrySet()) {
//                    // Require 50% + 1 peer to trust pivot
//                    if (pivotEntry.getValue() * 2 > peerCount) {
//                    	m_logger.info("Pivot header fetched: " + pivotEntry.getKey().getShortDescr());
//                        return Pair.of(pivotEntry.getKey(), null);
//                    }
//                }
//
//                Long newPivotBlockNumber = Math.max(0, pivotBlockNumber - 1000);
//                m_logger.info(String.format("Current pivot candidate not verified by majority of peers, " +
//                        "stepping back to block #{%d}", newPivotBlockNumber));
//                return Pair.of(null, newPivotBlockNumber);
//            } catch (TimeoutException e) {
//            	m_logger.debug("Timeout waiting for answer", e);
//            }
//        }

        return null;
    }

    public void close() {
    	m_logger.info("Closing FastSyncManager");
        try {
            fastSyncThread.interrupt();
            fastSyncInProgress = false;
            dbWriterThread.interrupt();
            dbFlushManager.commit();
            dbFlushManager.flushSync();
            fastSyncThread.join(10 * 1000);
            dbWriterThread.join(10 * 1000);
        } catch (Exception e) {
        	m_logger.warn("Problems closing FastSyncManager", e);
        }
    }
}
