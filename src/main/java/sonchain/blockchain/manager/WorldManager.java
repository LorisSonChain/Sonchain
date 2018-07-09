package sonchain.blockchain.manager;

import java.math.BigInteger;
import java.util.*;

import org.apache.log4j.Logger;
import org.bouncycastle.util.encoders.Hex;

import sonchain.blockchain.config.CommonConfig;
import sonchain.blockchain.core.Block;
import sonchain.blockchain.core.BlockChain;
import sonchain.blockchain.core.BlockChainImpl;
import sonchain.blockchain.core.BlockSummary;
import sonchain.blockchain.core.EventDispatchThread;
import sonchain.blockchain.core.Genesis;
import sonchain.blockchain.core.PendingState;
import sonchain.blockchain.core.Repository;
import sonchain.blockchain.core.TransactionExecutionSummary;
import sonchain.blockchain.core.TransactionReceipt;
import sonchain.blockchain.crypto.HashUtil;
import sonchain.blockchain.db.BlockStore;
import sonchain.blockchain.db.DbFlushManager;
import sonchain.blockchain.listener.SonChainListener;
import sonchain.blockchain.service.DataCenter;
import sonchain.blockchain.sync.FastSyncManager;
import sonchain.blockchain.sync.SyncManager;
import sonchain.blockchain.sync.SyncPool;
import sonchain.blockchain.util.Utils;
import sonchain.blockchain.listener.*;

public class WorldManager {
	private AdminInfo m_adminInfo = null;
	private BlockChainImpl m_blockchain = null;
	private BlockStore m_blockStore = null;
	private DbFlushManager m_dbFlushManager = null;
	private EventDispatchThread m_eventDispatchThread = null;
	private FastSyncManager m_fastSyncManager = null;
	private SonChainListener m_listener = null;
	private PendingState m_pendingState = null;
	private SyncPool m_pool = null;
	private Repository m_repository = null;
	private SyncManager m_syncManager = null;
	
	public static final Logger m_logger = Logger.getLogger(WorldManager.class);

	public WorldManager(Repository repository, SonChainListener listener,
			BlockChainImpl blockchain, final BlockStore blockStore) {
		m_listener = listener;
		m_blockchain = blockchain;
		m_repository = repository;
		m_blockStore = blockStore;
		m_pendingState = m_blockchain.getPendingState();
		m_dbFlushManager = CommonConfig.getDefault().getDbFlushManager();
		loadBlockchain();
	}

	private void init() {
		// syncManager.init(channelManager, pool);
	}

	public void addListener(SonChainListener listener) {
		m_logger.info("SonChain listener added");
		((CompositeSonChainListener) m_listener).addListener(listener);
	}

	public void startPeerDiscovery() {
	}

	public void stopPeerDiscovery() {
		// discoveryUdpListener.close();
		// nodeManager.close();
	}

	public void initSyncing() {
		DataCenter.m_config.m_syncEnabled = true;
		// syncManager.init(channelManager, pool);
	}

	// public ChannelManager getChannelManager() {
	// return channelManager;
	// }

	public SonChainListener getListener() {
		return m_listener;
	}

	public Repository getRepository() {
		return (Repository) m_repository;
	}

	public BlockChain getBlockchain() {
		return m_blockchain;
	}

	// public PeerClient getActivePeer() {
	// return activePeer;
	// }

	public BlockStore getBlockStore() {
		return m_blockStore;
	}

	public PendingState getPendingState() {
		return m_pendingState;
	}

	public void loadBlockchain() {
		if (!DataCenter.m_config.m_databaseReset
				|| DataCenter.m_config.m_databaseResetBlock != 0) {
			m_blockStore.load();
		}

		if (m_blockStore.getBestBlock() == null) {
			m_logger.info("DB is empty - adding Genesis");
			Genesis genesis = Genesis.getInstance();
			Genesis.populateRepository(m_repository, genesis);
			//m_repository.commitBlock(genesis.getHeader());
			m_repository.commit();
			m_blockStore.saveBlock(Genesis.getInstance(), true);
			m_blockchain.setBestBlock(Genesis.getInstance());
			BlockSummary blockSummary = new BlockSummary(Genesis.getInstance(),
					new ArrayList<TransactionReceipt>(), new ArrayList<TransactionExecutionSummary>());
			if(m_listener != null){
				m_listener.onBlock(blockSummary);
			}
			// repository.dumpState(Genesis.getInstance(config), 0, 0, null);
			m_logger.info("Genesis block loaded");
		} else {
			if (!DataCenter.m_config.m_databaseReset
					&& !Arrays.equals(m_blockchain.getBlockByNumber(0).getHash(), 
							DataCenter.m_config.getGenesis().getHash())) {
				// fatal exit
				Utils.showErrorAndExit("*** DB is incorrect, 0 block in DB doesn't match genesis");
			}
			Block bestBlock = m_blockStore.getBestBlock();
			if (DataCenter.m_config.m_databaseReset && DataCenter.m_config.m_databaseResetBlock > 0) {
				if (DataCenter.m_config.m_databaseResetBlock > bestBlock.getNumber()) {
					m_logger.error(String.format("*** Can't reset to block [{%d}] since block store is at block [{%s}].", 
							DataCenter.m_config.m_databaseResetBlock, bestBlock.toString()));
					throw new RuntimeException("Reset block ahead of block store.");				}
				bestBlock = m_blockStore.getChainBlockByNumber(DataCenter.m_config.m_databaseResetBlock);

				Repository snapshot = m_repository.getSnapshotTo(bestBlock.getStateRoot());
				if (false) { 
					// TODO: some way to tell if the snapshot hasn't
								// been pruned
					// logger.error("*** Could not reset database to block [{}]
					// with stateRoot [{}], since state information is " +
					// "unavailable. It might have been pruned from the
					// database.");
					throw new RuntimeException("State unavailable for reset block.");
				}
			}

			m_blockchain.setBestBlock(bestBlock);
			// with stateRoot [{}]",
			// blockchain.getBestBlock().getNumber(),
			// Hex.toHexString(blockchain.getBestBlock().getStateRoot()));
		}

		String rootHashStart = DataCenter.m_config.getRootHashStart();
		if (rootHashStart != null && !rootHashStart.isEmpty()) {

			// update world state by dummy hash
			byte[] rootHash = Hex.decode(DataCenter.m_config.getRootHashStart());
			m_logger.info(String.format("Loading root hash from property file: [{%s}]", DataCenter.m_config.m_rootHashStart));
			m_repository.syncToRoot(rootHash);

		} else {

			// Update world state to latest loaded block from db
			// if state is not generated from empty premine list
			// todo this is just a workaround, move EMPTY_TRIE_HASH logic to
			// Trie implementation
			if (!Arrays.equals(m_blockchain.getBestBlock().getStateRoot(), HashUtil.EMPTY_TRIE_HASH)) {
				m_repository.syncToRoot(m_blockchain.getBestBlock().getStateRoot());
			}
		}
	}

	public void close() {
		m_logger.info("close: stopping peer discovery ...");
		stopPeerDiscovery();
		m_logger.info("close: stopping ChannelManager ...");
		//channelManager.close();
		m_logger.info("close: stopping SyncManager ...");
		m_syncManager.close();
		m_logger.info("close: stopping PeerClient ...");
		//activePeer.close();
		m_logger.info("close: shutting down event dispatch thread used by EventBus ...");
		m_eventDispatchThread.shutdown();
		m_logger.info("close: closing Blockchain instance ...");
		m_blockchain.close();
		m_logger.info("close: closing main repository ...");
		m_repository.close();
		m_logger.info("close: database flush manager ...");
		m_dbFlushManager.close();
	}
}
