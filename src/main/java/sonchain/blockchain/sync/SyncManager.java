package sonchain.blockchain.sync;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.bouncycastle.util.encoders.Hex;

import sonchain.blockchain.core.BlockChain;
import sonchain.blockchain.core.BlockHeader;
import sonchain.blockchain.core.BlockHeaderWrapper;
import sonchain.blockchain.core.BlockWrapper;
import sonchain.blockchain.core.ImportResult;
import sonchain.blockchain.core.Transaction;
import sonchain.blockchain.facade.SyncStatus;
import sonchain.blockchain.listener.CompositeSonChainListener;
import sonchain.blockchain.listener.SonChainListener;
import sonchain.blockchain.service.DataCenter;
import sonchain.blockchain.util.ExecutorPipeline;
import sonchain.blockchain.util.Functional;
import sonchain.blockchain.validator.BlockHeaderValidator;

public class SyncManager extends BlockDownloader {

	private final static AtomicLong blockQueueByteSize = new AtomicLong(0);
	private final static int BLOCK_BYTES_ADDON = 4;
    private SyncQueueImpl m_syncQueue;

	private ExecutorPipeline<BlockWrapper, BlockWrapper> exec1 = new ExecutorPipeline<>(4, 1000, true,
			new Functional.Function<BlockWrapper, BlockWrapper>() {
				public BlockWrapper apply(BlockWrapper blockWrapper) {
                	//TODO
//					for (Transaction tx : blockWrapper.getBlock().getTransactionsList()) {
//						tx.getSenderAddress();
//					}
					return blockWrapper;
				}
			}, new Functional.Consumer<Throwable>() {
				public void accept(Throwable throwable) {
					//logger.error("Unexpected exception: ", throwable);
				}
			});

	private ExecutorPipeline<BlockWrapper, Void> exec2 = exec1.Add(1, 1, new Functional.Consumer<BlockWrapper>() {
		@Override
		public void accept(BlockWrapper blockWrapper) {
			blockQueueByteSize.addAndGet(estimateBlockSize(blockWrapper));
			blockQueue.add(blockWrapper);
		}
	});

    private long estimateBlockSize(BlockWrapper blockWrapper) {
        return blockWrapper.getEncoded().length + BLOCK_BYTES_ADDON;
    }

	/**
	 * Queue with validated blocks to be added to the blockchain
	 */
	private BlockingQueue<BlockWrapper> blockQueue = new LinkedBlockingQueue<>();

	private BlockChain blockchain;

	private CompositeSonChainListener compositeEthereumListener;

	private FastSyncManager fastSyncManager;

	//ChannelManager channelManager;

	private SyncPool pool;

	//private SyncQueueImpl syncQueue;

	private Thread syncQueueThread;

	private long m_blockBytesLimit = 32 * 1024 * 1024;
	private long m_lastKnownBlockNumber = 0;
	private boolean m_syncDone = false;
	private AtomicLong m_importIdleTime = new AtomicLong();
	private long m_importStart;
	private SonChainListener.SyncState m_syncDoneType = SonChainListener.SyncState.COMPLETE;
	private ScheduledExecutorService m_logExecutor = Executors.newSingleThreadScheduledExecutor();

	public SyncManager() {
		//super(null);
	}

	public SyncManager(BlockHeaderValidator validator) {
		//super(validator);
		m_blockBytesLimit = DataCenter.m_config.m_cacheBlockQueueSize;
		//SetHeaderQueueLimit(config.GetHeaderQueueSize() / BlockHeader.MAX_HEADER_SIZE);
	}
	
    public SyncStatus getSyncStatus() {
        if (DataCenter.m_config.m_syncFastEnabled) {
            SyncStatus syncStatus = fastSyncManager.getSyncState();
            if (syncStatus.getStage() == SyncStatus.SyncStage.Complete) {
                return getSyncStateImpl();
            } else {
                return new SyncStatus(syncStatus, blockchain.getBestBlock().getBlockNumber(), getLastKnownBlockNumber());
            }
        } else {
            return getSyncStateImpl();
        }
    }

    private SyncStatus getSyncStateImpl() {
        if (!DataCenter.m_config.m_syncEnabled)
            return new SyncStatus(SyncStatus.SyncStage.Off, 0, 0, blockchain.getBestBlock().getBlockNumber(),
                    blockchain.getBestBlock().getBlockNumber());

        return new SyncStatus(isSyncDone() ? SyncStatus.SyncStage.Complete : SyncStatus.SyncStage.Regular,
                0, 0, blockchain.getBestBlock().getBlockNumber(), getLastKnownBlockNumber());
    }
    
    public boolean isSyncDone() {
        return m_syncDone;
    }

    public long getLastKnownBlockNumber() {
        long ret = Math.max(blockchain.getBestBlock().getBlockNumber(), m_lastKnownBlockNumber);
//        for (Channel channel : pool.getActivePeers()) {
//            BlockIdentifier bestKnownBlock = channel.getEthHandler().getBestKnownBlock();
//            if (bestKnownBlock != null) {
//                ret = max(bestKnownBlock.getNumber(), ret);
//            }
//        }
        return ret;
    }

    public void close() {
        try {
            //logger.info("Shutting down SyncManager");
        	exec1.Shutdown();
            exec1.Join();
            m_logExecutor.shutdown();
            //pool.close();
            if (syncQueueThread != null) {
                syncQueueThread.interrupt();
                syncQueueThread.join(10 * 1000);
            }
            //if (config.isFastSyncEnabled()) 
            //	fastSyncManager.close();
        } catch (Exception e) {
            //logger.warn("Problems closing SyncManager", e);
        }
        super.close();
    }

	@Override
	protected void pushBlocks(List<BlockWrapper> blockWrappers) {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void pushHeaders(List<BlockHeaderWrapper> headers) {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected int getBlockQueueFreeSize() {
		// TODO Auto-generated method stub
		return 0;
	}
	
    void initRegularSync(SonChainListener.SyncState syncDoneType) {
        m_logger.info("Initializing SyncManager regular sync.");
        m_syncDoneType = syncDoneType;

        m_syncQueue = new SyncQueueImpl(blockchain);
        super.init(m_syncQueue, pool);
        Runnable queueProducer = new Runnable(){

            @Override
            public void run() {
                produceQueue();
            }
        };

        syncQueueThread = new Thread (queueProducer, "SyncQueueThread");
        syncQueueThread.start();
    }

    /**
     * Processing the queue adding blocks to the chain.
     */
    private void produceQueue() {

        DecimalFormat timeFormat = new DecimalFormat("0.000");
        timeFormat.setDecimalFormatSymbols(DecimalFormatSymbols.getInstance(Locale.US));

        while (!Thread.currentThread().isInterrupted()) {

            BlockWrapper wrapper = null;
            try {

                long stale = !isSyncDone() && m_importStart > 0 && blockQueue.isEmpty() ? System.nanoTime() : 0;
                wrapper = blockQueue.take();
                blockQueueByteSize.addAndGet(-estimateBlockSize(wrapper));

                if (stale > 0) {
                	m_importIdleTime.addAndGet((System.nanoTime() - stale) / 1_000_000);
                }
                if (m_importStart == 0){
                	m_importStart = System.currentTimeMillis();
                }

                m_logger.debug(String.format("BlockQueue size: {%d}, headers queue size: {%d}", 
                		blockQueue.size(), m_syncQueue.getHeadersCount()));

                long s = System.nanoTime();
                long sl;
                ImportResult importResult;
                synchronized (blockchain) {
                    sl = System.nanoTime();
                    importResult = blockchain.tryToConnect(wrapper.getBlock());
                }
                long f = System.nanoTime();
                long t = (f - s) / 1_000_000;
                String ts = timeFormat.format(t / 1000d) + "s";
                t = (sl - s) / 1_000_000;
                ts += t < 10 ? "" : " (lock: " + timeFormat.format(t / 1000d) + "s)";

                if (importResult == ImportResult.IMPORTED_BEST) {
                	m_logger.info(String.format("Success importing BEST: block.number: {%d}, block.hash: {%s}, tx.size: {%d}, time: {%s}",
                            wrapper.getNumber(), wrapper.getBlock().getShortHash(),
                            wrapper.getBlock().getTransactionsList().size(), ts));

                    if (wrapper.isNewBlock() && !m_syncDone) {
                    	m_syncDone = true;
                        //channelManager.onSyncDone(true);
                        compositeEthereumListener.onSyncDone(m_syncDoneType);
                    }
                }

                if (importResult == ImportResult.IMPORTED_NOT_BEST)
                	m_logger.info(String.format("Success importing NOT_BEST: block.number: {%d}, block.hash: {%s}, tx.size: {%d}, time: {%s}",
                            wrapper.getNumber(), wrapper.getBlock().getShortHash(),
                            wrapper.getBlock().getTransactionsList().size(), ts));

                if (m_syncDone && (importResult == ImportResult.IMPORTED_BEST || importResult == ImportResult.IMPORTED_NOT_BEST)) {
                    if (m_logger.isDebugEnabled())
                    {
                    	m_logger.debug("Block dump: " + Hex.toHexString(wrapper.getBlock().getEncoded()));
                    }
                    // Propagate block to the net after successful import asynchronously
                    if (wrapper.isNewBlock()) {
                    	//channelManager.onNewForeignBlock(wrapper);
                    }
                }

                // In case we don't have a parent on the chain
                // return the try and wait for more blocks to come.
                if (importResult == ImportResult.NO_PARENT) {
                	m_logger.error(String.format("No parent on the chain for block.number: {%d} block.hash: {%s}",
                            wrapper.getNumber(), wrapper.getBlock().getShortHash()));
                }

            } catch (InterruptedException e) {
                break;
            } catch (Throwable e) {
                if (wrapper != null) {
                	m_logger.error(String.format("Error processing block {%s}: ", wrapper.getBlock().getShortDescr(), e));
                	m_logger.error(String.format("Block dump: {%s}", Hex.toHexString(wrapper.getBlock().getEncoded())));
                } else {
                	m_logger.error("Error processing unknown block", e);
                }
            }
        }
    }
}
