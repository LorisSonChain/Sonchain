package sonchain.blockchain.sync;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.log4j.Logger;

import sonchain.blockchain.config.CommonConfig;
import sonchain.blockchain.consensus.SonChainPeerNode;
import sonchain.blockchain.core.Block;
import sonchain.blockchain.core.BlockHeader;
import sonchain.blockchain.core.BlockHeaderWrapper;
import sonchain.blockchain.core.BlockWrapper;
import sonchain.blockchain.crypto.HashUtil;
import sonchain.blockchain.data.SonChainHostInfo;
import sonchain.blockchain.datasource.DataSourceArray;
import sonchain.blockchain.db.DbFlushManager;
import sonchain.blockchain.db.IndexedBlockStore;
import sonchain.blockchain.service.DataCenter;
import sonchain.blockchain.util.FastByteComparisons;
import sonchain.blockchain.validator.BlockHeaderValidator;

public class BlockBodiesDownloader extends BlockDownloader {

	public static final Logger m_logger = Logger.getLogger(BlockBodiesDownloader.class);
    public final static byte[] EMPTY_BODY = new byte[] {-62, -64, -64};
    
    private IndexedBlockStore m_blockStore
    	= (IndexedBlockStore)DataCenter.getSonChainImpl().getBlockChain().getBlockStore();
    private DataSourceArray<BlockHeader> m_headerStore 
    	= CommonConfig.getDefault().getHeaderSource();    
    private DbFlushManager m_dbFlushManager = CommonConfig.getDefault().getDbFlushManager();
    private Thread m_headersThread = null;
    private int m_downloadCnt = 0;
    private int m_curBlockIdx = 1;
    private SyncPool m_syncPool = null;
    private SyncQueueInterface m_syncQueue = null;
    private long m_preCurrentTimeMillis = 0;
    
    public BlockBodiesDownloader(BlockHeaderValidator headerValidator) {
        super(headerValidator);
    }
    
    public void startImporting() {
        Block genesis = m_blockStore.getChainBlockByNumber(0);
        m_syncQueue = new SyncQueueImpl(Collections.singletonList(genesis));
        m_headersThread = new Thread("FastsyncHeadersFetchThread") {
            @Override
            public void run() {
                headerLoop();
            }
        };
        m_headersThread.start();
        setHeadersDownload(false);
        init(m_syncQueue, m_syncPool);
    }
    
    private void headerLoop() {
        while (m_curBlockIdx < m_headerStore.size() && !Thread.currentThread().isInterrupted()) {
            List<BlockHeaderWrapper> wrappers = new ArrayList<>();
            List<BlockHeader> emptyBodyHeaders =  new ArrayList<>();
            for (int i = 0; i < 10000 - m_syncQueue.getHeadersCount() 
            		&& m_curBlockIdx < m_headerStore.size(); i++) {
                BlockHeader header = m_headerStore.get(m_curBlockIdx++);
                wrappers.add(new BlockHeaderWrapper(header, new SonChainPeerNode()));

                // Skip bodies download for blocks with empty body
                boolean emptyBody = FastByteComparisons.equal(header.getTxTrieRoot(), 
                		HashUtil.EMPTY_TRIE_HASH);
                if (emptyBody){
                	emptyBodyHeaders.add(header);
                }
            }

            synchronized (this) {
            	m_syncQueue.addHeaders(wrappers);
                if (!emptyBodyHeaders.isEmpty()) {
                    addEmptyBodyBlocks(emptyBodyHeaders);
                }
            }

            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                break;
            }
        }
        m_headersDownloadComplete = true;
    }
    
    private void addEmptyBodyBlocks(List<BlockHeader> blockHeaders) {
        m_logger.debug(String.format("Adding {%d} empty body blocks to sync queue: {%s} ... {%s}", 
        		blockHeaders.size(),
                blockHeaders.get(0).getShortDescr(), 
                blockHeaders.get(blockHeaders.size() - 1).getShortDescr()));

        List<Block> finishedBlocks = new ArrayList<>();
        for (BlockHeader header : blockHeaders) {
            Block block = new Block.Builder()
                    .withHeader(header)
                    .withBody(EMPTY_BODY)
                    .create();
            finishedBlocks.add(block);
        }

        List<Block> startTrimmedBlocks = m_syncQueue.addBlocks(finishedBlocks);
        List<BlockWrapper> trimmedBlockWrappers = new ArrayList<>();
        for (Block b : startTrimmedBlocks) {
            trimmedBlockWrappers.add(new BlockWrapper(b, null));
        }

        pushBlocks(trimmedBlockWrappers);
    }

    @Override
    protected void pushBlocks(List<BlockWrapper> blockWrappers) {
        if (!blockWrappers.isEmpty()) {

            for (BlockWrapper blockWrapper : blockWrappers) {
            	m_blockStore.saveBlock(blockWrapper.getBlock(), true);
            	m_downloadCnt++;
            }
            m_dbFlushManager.commit();

            long currentTimeMillis = System.currentTimeMillis();
            if (currentTimeMillis - m_preCurrentTimeMillis > 5000) {
            	m_preCurrentTimeMillis = currentTimeMillis;
                m_logger.info("FastSync: downloaded blocks. Last: " 
                		+ blockWrappers.get(blockWrappers.size() - 1).getBlock().getShortDescr());
            }
        }
    }
    
    @Override
    protected void pushHeaders(List<BlockHeaderWrapper> headers) {}

    @Override
    protected int getBlockQueueFreeSize() {
        return Integer.MAX_VALUE;
    }

    public int getDownloadedCount() {
        return m_downloadCnt;
    }

    @Override
    public void stop() {
    	m_headersThread.interrupt();
        super.stop();
    }

    @Override
    protected void finishDownload() {
        stop();
    }
}
