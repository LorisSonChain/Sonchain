package sonchain.blockchain.sync;

import sonchain.blockchain.consensus.SonChainPeerNode;
import sonchain.blockchain.core.Block;
import sonchain.blockchain.core.BlockHeader;
import sonchain.blockchain.core.BlockHeaderWrapper;
import sonchain.blockchain.core.BlockWrapper;
import sonchain.blockchain.data.SonChainHostInfo;
import sonchain.blockchain.peer.SonChainServicePeer;
import sonchain.blockchain.service.DataCenter;
import sonchain.blockchain.validator.BlockHeaderValidator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.bouncycastle.util.encoders.Hex;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

public abstract class BlockDownloader {

	private int blockQueueLimit = 2000;
	private int headerQueueLimit = 10000;

	private static int MAX_IN_REQUEST = 192;
	private static int REQUESTS = 32;

	public static final Logger m_logger = Logger.getLogger(BlockDownloader.class);

	private boolean m_blockBodiesDownload = true;
	private boolean m_downloadComplete = false;
	private Thread m_getHeadersThread = null;
	private Thread m_getBodiesThread = null;
	private boolean m_headersDownload = true;
	protected boolean m_headersDownloadComplete = false;
	private BlockHeaderValidator m_headerValidator = null;
	private CountDownLatch m_receivedBlocksLatch = new CountDownLatch(0);
	private CountDownLatch m_receivedHeadersLatch = new CountDownLatch(0);
	private CountDownLatch m_stopLatch = new CountDownLatch(1);
	private SyncQueueInterface m_syncQueue = null;

	public BlockDownloader() {
	}

	public BlockDownloader(BlockHeaderValidator headerValidator) {
		m_headerValidator = headerValidator;
	}

	protected abstract void pushBlocks(List<BlockWrapper> blockWrappers);

	protected abstract void pushHeaders(List<BlockHeaderWrapper> headers);

	protected abstract int getBlockQueueFreeSize();
	
	private void addBlocks(List<Block> blocks, SonChainPeerNode hostInfo) {
		m_logger.info("addBlocks end.");
		if (blocks.isEmpty()) {
			return;
		}
		synchronized (this) {
            m_logger.debug("Adding new " + blocks.size() + " blocks to sync queue: " +
                    blocks.get(0).getShortDescr() + " ... " + blocks.get(blocks.size() - 1).getShortDescr());
			List<Block> newBlocks = m_syncQueue.addBlocks(blocks);
			List<BlockWrapper> wrappers = new ArrayList<>();
			for (Block b : newBlocks) {
				wrappers.add(new BlockWrapper(b, hostInfo));
			}
			m_logger.debug("Pushing " + wrappers.size() + " blocks to import queue: " 
					+ (wrappers.isEmpty() ? "" : wrappers.get(0).getBlock().getShortDescr() 
							+ " ... " +  wrappers.get(wrappers.size() - 1).getBlock().getShortDescr()));
			pushBlocks(wrappers);
		}
		m_receivedBlocksLatch.countDown();
		 if (m_logger.isDebugEnabled()) {
			 m_logger.debug(String.format("Blocks waiting to be proceed: lastBlock.number: [{%d}]",
					 blocks.get(blocks.size() - 1).getNumber()));
		 }
		m_logger.info("addBlocks end.");
	}

	protected void finishDownload() {
	}

	private void headerRetrieveLoop() {
		m_logger.debug("headerRetrieveLoop start.");
		List<SyncQueueInterface.HeadersRequest> hReq = Collections.emptyList();
		while (!Thread.currentThread().isInterrupted()) {
			try {
				if (hReq.isEmpty()) {
					synchronized (this) {
						hReq = m_syncQueue.requestHeaders(MAX_IN_REQUEST, 128, headerQueueLimit);
						if (hReq == null) {
							m_logger.info("Headers download complete.");
							m_headersDownloadComplete = true;
							if (!m_blockBodiesDownload) {
								finishDownload();
								m_downloadComplete = true;
							}
							return;
						}
						String l = "########## New header requests (" + hReq.size() + "):\n";
						for (SyncQueueInterface.HeadersRequest request : hReq) {
							l += " " + request + "\n";
						}
						;
						m_logger.debug(l);
					}
				}
				int reqHeadersCounter = 0;
				for (Iterator<SyncQueueInterface.HeadersRequest> it = hReq.iterator(); it.hasNext();) {
					SyncQueueInterface.HeadersRequest headersRequest = it.next();
					SonChainServicePeer serviceCT = DataCenter.getNodeManager().getAnyOneSonChainServiceCT();
					if (serviceCT == null) {
						m_logger.debug("headerRetrieveLoop: No IDLE peers found");
						break;
					} else {
						m_logger.debug("headerRetrieveLoop: request headers (" + headersRequest.getStart() + ") from "
								+ serviceCT.getSonChainPeerNode().getHost());
						ListenableFuture<List<BlockHeader>> futureHeaders = headersRequest.getHash() == null
								? serviceCT.sendGetBlockHeaders(headersRequest.getStart(), headersRequest.getCount(),
										headersRequest.isReverse())
								: serviceCT.sendGetBlockHeaders(headersRequest.getHash(), headersRequest.getCount(),
										headersRequest.getStep(), headersRequest.isReverse());
						if (futureHeaders != null) {
							Futures.addCallback(futureHeaders, new FutureCallback<List<BlockHeader>>() {
								@Override
								public void onSuccess(List<BlockHeader> result) {
									if (!validateAndAddHeaders(result, serviceCT.getSonChainPeerNode())) {
										onFailure(new RuntimeException("Received headers validation failed"));
									}
								}

								@Override
								public void onFailure(Throwable t) {
									m_logger.debug("Error receiving headers. Dropping the peer.", t);
									serviceCT.dropConnection();
								}
							});
							it.remove();
							reqHeadersCounter++;
						}
					}
				}
				m_receivedHeadersLatch = new CountDownLatch(Math.max(reqHeadersCounter / 2, 1));
				m_receivedHeadersLatch.await(isSyncDone() ? 10000 : 500, TimeUnit.MILLISECONDS);
			} catch (InterruptedException e) {
				m_logger.error("Unexpected: ", e);
				break;
			} catch (Exception e) {
				m_logger.error("Unexpected: ", e);
			}
		}
		m_logger.debug("headerRetrieveLoop end.");
	}

	private void blockRetrieveLoop() {
		class BlocksCallback implements FutureCallback<List<Block>> {
			private SonChainServicePeer m_serviceCT;

			public BlocksCallback(SonChainServicePeer peer) {
				m_serviceCT = peer;
			}

			@Override
			public void onSuccess(List<Block> result) {
				addBlocks(result, m_serviceCT.getSonChainPeerNode());
			}

			@Override
			public void onFailure(Throwable t) {
				m_logger.debug("Error receiving Blocks. Dropping the peer.", t);
				m_serviceCT.dropConnection();
			}
		}

		m_logger.debug("blockRetrieveLoop start");
		List<SyncQueueInterface.BlocksRequest> bReqs = Collections.emptyList();
		while (!Thread.currentThread().isInterrupted()) {
			try {
				if (bReqs.isEmpty()) {
					bReqs = m_syncQueue.requestBlocks(16 * 1024).split(MAX_IN_REQUEST);
				}

				if (bReqs.isEmpty() && m_headersDownloadComplete) {
					m_logger.info("Block download complete.");
					finishDownload();
					m_downloadComplete = true;
					return;
				}

				int blocksToAsk = getBlockQueueFreeSize();
				if (blocksToAsk > MAX_IN_REQUEST) {
					if (bReqs.size() == 1 && bReqs.get(0).getBlockHeaders().size() <= 3) {
						// new blocks are better to request from the header
						// senders first
						// to get more chances to receive block body promptly
						for (BlockHeaderWrapper blockHeaderWrapper : bReqs.get(0).getBlockHeaders()) {
							SonChainServicePeer serviceCT = DataCenter.getNodeManager()
									.getOneSonChainServiceCT(blockHeaderWrapper.getNodeId().toString());
							if (serviceCT != null) {
								ListenableFuture<List<Block>> futureBlocks = serviceCT
										.sendGetBlockBodies(Collections.singletonList(blockHeaderWrapper));
								if (futureBlocks != null) {
									Futures.addCallback(futureBlocks, new BlocksCallback(serviceCT));
								}
							}
						}
					}

					int maxRequests = blocksToAsk / MAX_IN_REQUEST;
					int maxBlocks = MAX_IN_REQUEST * Math.min(maxRequests, REQUESTS);
					int reqBlocksCounter = 0;
					int blocksRequested = 0;
					Iterator<SyncQueueInterface.BlocksRequest> it = bReqs.iterator();
					while (it.hasNext() && blocksRequested < maxBlocks) {
						// for (SyncQueueIfc.BlocksRequest blocksRequest :
						// bReq.split(MAX_IN_REQUEST)) {
						SyncQueueInterface.BlocksRequest blocksRequest = it.next();
						SonChainServicePeer serviceCT = DataCenter.getNodeManager().getAnyOneSonChainServiceCT();
						if (serviceCT == null) {
							m_logger.debug("blockRetrieveLoop: No IDLE peers found");
							break;
						} else {
							m_logger.debug("blockRetrieveLoop: Requesting " + blocksRequest.getBlockHeaders().size()
									+ " blocks from " + serviceCT.getSonChainPeerNode().toString());
							ListenableFuture<List<Block>> futureBlocks = serviceCT
									.sendGetBlockBodies(blocksRequest.getBlockHeaders());
							blocksRequested += blocksRequest.getBlockHeaders().size();
							if (futureBlocks != null) {
								Futures.addCallback(futureBlocks, new BlocksCallback(serviceCT));
								reqBlocksCounter++;
								it.remove();
							}
						}
					}
					m_receivedBlocksLatch = new CountDownLatch(Math.max(reqBlocksCounter - 2, 1));
				} else {
					m_logger.debug("blockRetrieveLoop: BlockQueue is full");
					m_receivedBlocksLatch = new CountDownLatch(1);
				}
				m_receivedBlocksLatch.await(200, TimeUnit.MILLISECONDS);
			} catch (InterruptedException e) {
				break;
			} catch (Exception e) {
				m_logger.error("Unexpected: ", e);
			}
		}
		m_logger.debug("blockRetrieveLoop end");
	}

	public void close() {
		try {
			m_logger.debug("close start");
			stop();
		} catch (Exception e) {
			m_logger.warn("Problems closing SyncManager", e);
		}
		finally
		{
			m_logger.debug("close end");
		}
	}

	public int getBlockQueueLimit() {
		return blockQueueLimit;
	}

	public void init(SyncQueueInterface syncQueue, final SyncPool pool) {
		m_syncQueue = syncQueue;
		// m_pool = pool;
		m_logger.info("Initializing BlockDownloader start.");
		if (m_headersDownload) {
			m_getHeadersThread = new Thread(new Runnable() {
				@Override
				public void run() {
					headerRetrieveLoop();
				}
			}, "SyncThreadHeaders");
			m_getHeadersThread.start();
		}

		if (m_blockBodiesDownload) {
			m_getBodiesThread = new Thread(new Runnable() {
				@Override
				public void run() {
					blockRetrieveLoop();
				}
			}, "SyncThreadBlocks");
			m_getBodiesThread.start();
		}
		m_logger.info("Initializing BlockDownloader end.");
	}

	public boolean isDownloadComplete() {
		return m_downloadComplete;
	}

	public boolean isSyncDone() {
		return false;
	}

	protected boolean isValid(BlockHeader header) {
		return m_headerValidator.validateAndLog(header);
	}

	public void setBlockBodiesDownload(boolean blockBodiesDownload) {
		m_blockBodiesDownload = blockBodiesDownload;
	}
	
	public void setBlockQueueLimit(int blockQueueLimit) {
		this.blockQueueLimit = blockQueueLimit;
	}

	public void setHeadersDownload(boolean headersDownload) {
		m_headersDownload = headersDownload;
	}

	public void setHeaderQueueLimit(int headerQueueLimit) {
		this.headerQueueLimit = headerQueueLimit;
	}

	public void stop() {
		m_logger.info("stop start.");
		if (m_getHeadersThread != null) {
			m_getHeadersThread.interrupt();
		}
		if (m_getBodiesThread != null) {
			m_getBodiesThread.interrupt();
		}
		m_stopLatch.countDown();
		m_logger.info("stop end.");
	}

	public void waitForStop() {
		try {
			m_logger.info("waitForStop start.");
			m_stopLatch.await();
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
		finally
		{
			m_logger.info("waitForStop end.");
		}
	}
	
	private boolean validateAndAddHeaders(List<BlockHeader> headers, SonChainPeerNode hostInfo) {
		if (headers.isEmpty()) {
			return true;
		}
		List<BlockHeaderWrapper> wrappers = new ArrayList<>(headers.size());
		for (BlockHeader header : headers) {
			if (!isValid(header)) {
				if (m_logger.isDebugEnabled()) {
					m_logger.debug(String.format("Invalid header RLP: {%s}",
							Hex.toHexString(header.getEncoded())));
				}
				return false;
			}
			wrappers.add(new BlockHeaderWrapper(header, hostInfo));
		}
		synchronized (this) {
			List<BlockHeaderWrapper> headersReady = m_syncQueue.addHeaders(wrappers);
			if (headersReady != null && !headersReady.isEmpty()) {
				pushHeaders(headersReady);
			}
		}
		m_receivedBlocksLatch.countDown();
		m_logger.debug(String.format("{%d} headers added", headers.size()));
		return true;
	}
}
