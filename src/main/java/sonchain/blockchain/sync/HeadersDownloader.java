package sonchain.blockchain.sync;

import java.util.List;

import org.apache.log4j.Logger;
import org.bouncycastle.util.encoders.Hex;

import sonchain.blockchain.config.CommonConfig;
import sonchain.blockchain.core.BlockHeader;
import sonchain.blockchain.core.BlockHeaderWrapper;
import sonchain.blockchain.core.BlockWrapper;
import sonchain.blockchain.datasource.DataSourceArray;
import sonchain.blockchain.db.DbFlushManager;
import sonchain.blockchain.db.IndexedBlockStore;
import sonchain.blockchain.service.DataCenter;
import sonchain.blockchain.validator.BlockHeaderValidator;

public class HeadersDownloader extends BlockDownloader {
	public static final Logger m_logger = Logger.getLogger(HeadersDownloader.class);

	private SyncPool m_syncPool;
    //ChannelManager channelManager;
    private IndexedBlockStore m_blockStore
		= (IndexedBlockStore)DataCenter.getSonChainImpl().getBlockChain().getBlockStore();
    private DataSourceArray<BlockHeader> m_headerStore
		= CommonConfig.getDefault().getHeaderSource();    
    private DbFlushManager m_dbFlushManager = CommonConfig.getDefault().getDbFlushManager();
    private byte[] m_genesisHash = null;
    private int m_headersLoaded  = 0;

    public HeadersDownloader(BlockHeaderValidator headerValidator) {
        super(headerValidator);
        setHeaderQueueLimit(200000);
        setBlockBodiesDownload(false);
        m_logger.info("HeaderDownloader created.");
    }

    public void init(byte[] startFromBlockHash) {
    	m_logger.info("HeaderDownloader init: startHash = " + Hex.toHexString(startFromBlockHash));
        SyncQueueReverseImpl syncQueue = new SyncQueueReverseImpl(startFromBlockHash, true);
        super.init(syncQueue, m_syncPool);
        //syncPool.init(channelManager);
    }

    @Override
    protected synchronized void pushBlocks(List<BlockWrapper> blockWrappers) {}

    @Override
    protected void pushHeaders(List<BlockHeaderWrapper> headers) {
        if (headers.get(headers.size() - 1).getNumber() == 0) {
        	m_genesisHash = headers.get(headers.size() - 1).getHash();
        }
        if (headers.get(headers.size() - 1).getNumber() == 1) {
        	m_genesisHash = headers.get(headers.size() - 1).getHeader().getParentHash();
        }
        m_logger.info(headers.size() + " headers loaded: " + headers.get(0).getNumber() + " - " + headers.get(headers.size() - 1).getNumber());
        for (BlockHeaderWrapper header : headers) {
        	m_headerStore.set((int) header.getNumber(), header.getHeader());
        	m_headersLoaded++;
        }
        m_dbFlushManager.commit();
    }
    
    @Override
    protected int getBlockQueueFreeSize() {
        return Integer.MAX_VALUE;
    }

    public int getHeadersLoaded() {
        return m_headersLoaded;
    }

    @Override
    protected void finishDownload() {
        stop();
    }

    public byte[] getGenesisHash() {
        return m_genesisHash;
    }
}
