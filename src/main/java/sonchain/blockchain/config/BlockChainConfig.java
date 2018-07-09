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
	public RedisConfig m_redisConfig = new RedisConfig();
	public String m_projectVersion = "";
	public String m_databaseVersion = "";
	public String m_datebaseDir = "";
	public String m_nodeWalletAddress = "";
	public String m_genesisFilePath = "";
	public String m_blockchainConfigName = "";
	public String m_rootHashStart = "";
	public int m_transactionApproveTimeout = 0;
	public String m_cryptoProviderName = "SC";
	public String m_cryptoHashAlg256 = "";
	public String m_cryptoHashAlg512 = "";
	public String m_keyvalueDatasource = "leveldb";
	public int m_cacheMaxStateBloomSize = 0;
	public boolean m_recordBlocks = false;
	public String m_dumpDir = "";
	public int m_transactionOutdatedThreshold = 0;
	public int m_cacheFlushWriteCacheSize = 64;
	public int m_cacheFlushBlocks = 0;
	public boolean m_cacheFlushShortSyncFlush = true;
	public int m_cacheStateCacheSize = 0;
	public String m_blocksLoader = "";
	public String m_blocksFormat= "";
	public boolean m_databaseReset = false;
	public int m_databaseResetBlock = 0;
	public int m_cacheBlockQueueSize = 0;
	public boolean m_syncFastEnabled = false;
	public boolean m_syncEnabled = false;
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
