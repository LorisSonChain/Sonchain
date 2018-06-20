/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sonchain.blockchain.config;

import java.util.Arrays;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.log4j.Logger;
import org.bouncycastle.util.encoders.Hex;

import lord.common.mysql.config.MySqlConfig;
import lord.common.redis.config.RedisConfig;
import sonchain.blockchain.core.Genesis;
import sonchain.blockchain.core.genesis.GenesisHashValidator;
import sonchain.blockchain.core.genesis.GenesisJson;
import sonchain.blockchain.core.genesis.GenesisLoader;
import sonchain.blockchain.datasource.AbstractChainedSource;
import sonchain.blockchain.service.DataCenter;
import sonchain.blockchain.util.ByteUtil;
import sonchain.blockchain.validator.BlockCustomHashRule;
import sonchain.blockchain.validator.BlockHeaderValidator;

public class BlockChainConfig implements BaseChainConfig{

	public static final Logger m_logger = Logger.getLogger(BlockChainConfig.class);
    public BlockChainConfig() {
        add(0, new SonChainConfig());
    }
	
    public boolean m_clearCache;   
    public String m_localHost = "";
    public int m_localPort = 0;    
	//public MySqlConfig m_mysqlConfig = new MySqlConfig();
	public RedisConfig m_redisConfig = new RedisConfig();
	public String m_projectVersion = "";
	public String m_databaseVersion = "";
	public String m_datebaseDir = "";
	public String m_nodeWalletAddress = "";
	// path to genesis file  has priority over `genesis` option
	public String m_genesisFilePath = "";
	public String m_blockchainConfigName = "";
	//  if we want to load a root hash for db not from the saved block chain (last block)
	//  but any manual hash this property will help.
	//  values [-1] - load from db hex hash 32 bytes] root hash
	public String m_rootHashStart = "";
	// the time we wait to the network to approve the transaction, the
	// transaction got approved when include into a transactions msg retrieved from the peer [seconds]
	public int m_transactionApproveTimeout = 0;
	public String m_cryptoProviderName = "SC";
	public String m_cryptoHashAlg256 = "";
	public String m_cryptoHashAlg512 = "";
	// Key value data source values: [leveldb/inmem]
	public String m_keyvalueDatasource = "leveldb";
	// maximum size (in Mb) the state bloom fiter can grow up to
	// when reaching this threshold the bloom filter is turned off forever
	// 128M can manage approx up to 50M of db entries
	public int m_cacheMaxStateBloomSize = 0;
	public boolean m_recordBlocks = false;
	public String m_dumpDir = "";
	// the number of blocks that should pass  before a pending transaction is removed
	public int m_transactionOutdatedThreshold = 0;
	// size in Mbytes of the write buffer for all datasources (state, blocks, transactions)
	// data is flushed to DB when write caches size exceeds this limit
	// value < 0 disables this option
	public int m_cacheFlushWriteCacheSize = 64;
	// force data flush each N blocks [10000 flush each 10000 blocks] value 0 disables this option
	public int m_cacheFlushBlocks = 0;
	// flush each block after full (long) sync complete
	public boolean m_cacheFlushShortSyncFlush = true;
	// total size in Mbytes of the state DB read cache
	public int m_cacheStateCacheSize = 0;
	//  Load the blocks from a rlp lines file and not for the net
	public String m_blocksLoader = "";
	public String m_blocksFormat= "";
	// every time the application starts the existing database will be destroyed 
	// and all the data will be downloaded from peers again [true/false]
	public boolean m_databaseReset = false;
	// If reset=true, every time the application starts the database will reset itself to
    // this block number and sync again from there.  Set to 0 for a 'full' reset.
	public int m_databaseResetBlock = 0;
	// the size of block queue cache to be imported in MBytes
	public int m_cacheBlockQueueSize = 0;
	// enables/disables fastsync
	// when enabling make sure the global sync option is enabled ('sync.enabled') enabled = false
	public boolean m_syncFastEnabled = false;
	// block chain synchronization can be: [true/false]
	public boolean m_syncEnabled = false;
	// when specified the fastsync retrieves the state for this block this is the fast and secure option to do fastsync
	// if not specified the block is selected like [peerBestBlockNumber - 1000]
	public String m_syncFastPivotBlockHash = "";
	public String m_startExecutorDate = "";
	public String m_localNodePrivateKey = "";
	public int m_maxTransactionsPerBlock = 0;

    private long[] m_blockNumbers = new long[64];
    private BlockChainConfigInterface[] m_configs = new BlockChainConfigInterface[64];
    private int m_count = 0;
	private Genesis m_genesis = null;
	private GenesisJson m_genesisJson = null;
    
    public void add(long startBlockNumber, BlockChainConfigInterface config) {
        if (m_count >= m_blockNumbers.length) {
        	throw new RuntimeException();
        }
        if (m_count > 0 && m_blockNumbers[m_count] >= startBlockNumber)
        {
            throw new RuntimeException("Block numbers should increase");
        }
        if (m_count == 0 && startBlockNumber > 0){
        	throw new RuntimeException("First config should start from block 0");
        }
        m_blockNumbers[m_count] = startBlockNumber;
        m_configs[m_count] = config;
        m_count++;
    }

    public Constants getCommonConstants() {
        // TODO make a guard wrapper which throws exception if the requested constant differs among configs
        return m_configs[0].getConstants();
    }

    public BlockChainConfigInterface getConfigForBlock(long blockNumber) {
        for (int i = 0; i < m_count; i++) {
            if (blockNumber < m_blockNumbers[i]) {
            	return m_configs[i - 1];
            }
        }
        return m_configs[m_count - 1];
    }

    @Override
    public String toString() {
        return "BaseNetConfig{" +
                "blockNumbers=" + Arrays.toString(Arrays.copyOfRange(m_blockNumbers, 0, m_count)) +
                ", configs=" + Arrays.toString(Arrays.copyOfRange(m_configs, 0, m_count)) +
                ", count=" + m_count +
                '}';
    }
	
	public Genesis getGenesis() {
		if (m_genesis == null) {			
			m_genesis = GenesisLoader.parseGenesis(getGenesisJson());
		}
    	m_logger.debug("getGenesis end GenesisInfo:" + m_genesis.toString());
		return m_genesis;
	}
	
	private GenesisJson getGenesisJson() {
		if (m_genesisJson == null) {
			m_genesisJson = GenesisLoader.loadGenesisJson();

			if (m_genesisJson.getConfig() != null 
					&& m_genesisJson.getConfig().getHeaderValidators() != null) {
				for (GenesisHashValidator validator : m_genesisJson.getConfig().getHeaderValidators()) {
					BlockHeaderValidator headerValidator = new BlockHeaderValidator(
							new BlockCustomHashRule(ByteUtil.hexStringToBytes(validator.getHash())));
					DataCenter.m_config.getConfigForBlock(validator.getNumber()).headerValidators()
							.add(Pair.of(validator.getNumber(), headerValidator));
				}
			}
		}
		return m_genesisJson;
	}
	
	public String getHash256AlgName() {
		return m_cryptoHashAlg256;
	}

	public String getHash512AlgName() {
		return m_cryptoHashAlg512;
	}	

	public byte[] getNodeAddress() {
		String sc = DataCenter.m_config.m_nodeWalletAddress;
		byte[] c = ByteUtil.hexStringToBytes(sc);
		if (c.length != 20)
			throw new RuntimeException("node.address has invalid value: '" + sc + "'");
		return c;
	}

	/**
	 * Home NodeID calculated from 'peer.privateKey' property
	 */
	public byte[] getNodeId() {
		return null;
		//return getMyKey().getNodeId();
	}

	public String getProjectVersion() {
		return DataCenter.m_config.m_projectVersion;
	}

	public String getRootHashStart() {
		return DataCenter.m_config.m_rootHashStart;
	}
	
	public int getTransactionApproveTimeout() {
		return DataCenter.m_config.m_transactionApproveTimeout * 1000;
	}
	
    public byte[] getFastSyncPivotBlockHash() {
        if (m_syncFastPivotBlockHash.equals("")) {
        	return null;
        }
        byte[] ret = Hex.decode(m_syncFastPivotBlockHash);
        if (ret.length != 32) {
        	throw new RuntimeException("Invalid block hash length: " + Hex.toHexString(ret));
        }
        return ret;
    }
}
