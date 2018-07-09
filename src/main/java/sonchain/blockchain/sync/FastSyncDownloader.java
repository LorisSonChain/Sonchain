package sonchain.blockchain.sync;

import java.util.List;

import org.apache.log4j.Logger;

import sonchain.blockchain.core.BlockHeaderWrapper;
import sonchain.blockchain.core.BlockWrapper;
import sonchain.blockchain.db.IndexedBlockStore;
import sonchain.blockchain.service.DataCenter;
import sonchain.blockchain.validator.BlockHeaderValidator;

public class FastSyncDownloader extends BlockDownloader {
	public static final Logger m_logger = Logger.getLogger(FastSyncDownloader.class);

    private SyncPool m_syncPool = null;
    private IndexedBlockStore m_blockStore
			= (IndexedBlockStore)DataCenter.getSonChainImpl().getBlockChain().getBlockStore();

    private int m_counter = 0;
    private int m_maxCount = 0;
    private long m_preCurrentTimeMillis = 0;

    public FastSyncDownloader(BlockHeaderValidator headerValidator) {
        super(headerValidator);
    }

    public void startImporting(byte[] fromHash, int count) {
        SyncQueueReverseImpl syncQueueReverse = new SyncQueueReverseImpl(fromHash);
        init(syncQueueReverse, m_syncPool);
        m_maxCount = count <= 0 ? Integer.MAX_VALUE : count;
    }

    @Override
    protected void pushBlocks(List<BlockWrapper> blockWrappers) {
        if (!blockWrappers.isEmpty()) {

            for (BlockWrapper blockWrapper : blockWrappers) {
            	m_blockStore.saveBlock(blockWrapper.getBlock(), true);
            	m_counter++;
                if (m_counter >= m_maxCount) {
                	m_logger.info("All requested " + m_counter + " blocks are downloaded. (last " + blockWrapper.getBlock().getShortDescr() + ")");
                    stop();
                    break;
                }
            }

            long currentTimeMillis = System.currentTimeMillis();
            if (currentTimeMillis - m_preCurrentTimeMillis > 5000) {
            	m_preCurrentTimeMillis = currentTimeMillis;
                m_logger.info("FastSync: downloaded " + m_counter + " blocks so far. Last: "
                			+ blockWrappers.get(0).getBlock().getShortDescr());
                m_blockStore.flush();
            }
        }
    }

    @Override
    protected void pushHeaders(List<BlockHeaderWrapper> headers) {}

    @Override
    protected int getBlockQueueFreeSize() {
        return Integer.MAX_VALUE;
    }

    // TODO: receipts loading here

    public int getDownloadedBlocksCount() {
        return m_counter;
    }

    @Override
    protected void finishDownload() {
    	m_blockStore.flush();
    }
}
