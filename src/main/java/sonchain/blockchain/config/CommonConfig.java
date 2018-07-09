package sonchain.blockchain.config;

import java.util.*;

import org.apache.log4j.Logger;

import com.google.common.util.concurrent.ListenableFuture;

import sonchain.blockchain.core.BlockHeader;
import sonchain.blockchain.core.Repository;
import sonchain.blockchain.crypto.HashUtil;
import sonchain.blockchain.datasource.*;
import sonchain.blockchain.datasource.inmem.HashMapDB;
import sonchain.blockchain.datasource.leveldb.LevelDbDataSource;
import sonchain.blockchain.datasource.redis.RedisDbDataSource;
import sonchain.blockchain.db.DbFlushManager;
import sonchain.blockchain.db.PeerSource;
import sonchain.blockchain.db.RepositoryRoot;
import sonchain.blockchain.db.RepositoryWrapper;
import sonchain.blockchain.db.StateSource;
import sonchain.blockchain.listener.SonChainListener;
import sonchain.blockchain.service.DataCenter;
import sonchain.blockchain.sync.FastSyncManager;
import sonchain.blockchain.validator.*;

public class CommonConfig {

	public static final Logger m_logger = Logger.getLogger(CommonConfig.class);
    private Set<DbSource> m_dbSources = new HashSet<>();
    private HashMap<String, DbSource> m_mapDbSources = new HashMap<String, DbSource>();
    private static CommonConfig m_defaultInstance;
    private HashMap<String, LevelDbDataSource> m_dicLevelDbDataSources = new HashMap<String, LevelDbDataSource>();
    private DbFlushManager m_dbFlushManager = null;

    public static CommonConfig getDefault() {
        if (m_defaultInstance == null) {
        	m_defaultInstance = new CommonConfig();
        }
        return m_defaultInstance;
    }
    
    public void fastSyncCleanUp() {
    	m_logger.debug("fastSyncCleanUp start");
        byte[] fastsyncStageBytes = getBlockChainDB().get(FastSyncManager.FASTSYNC_DB_KEY_SYNC_STAGE);
        if (fastsyncStageBytes == null) 
        {
        	m_logger.debug("fastSyncCleanUp end");
        	return;
        }
        SonChainListener.SyncState syncStage = SonChainListener.SyncState.values()[fastsyncStageBytes[0]];
        if (!DataCenter.m_config.m_syncFastEnabled || syncStage == SonChainListener.SyncState.UNSECURE) {
            // we need to cleanup state/blocks/tranasaction DBs when previous fast sync was not complete:
            // - if we now want to do regular sync
            // - if the first fastsync stage was not complete (thus DBs are not in consistent state)
        	m_logger.warn("Last fastsync was interrupted. Removing inconsistent DBs...");
            DbSource bcSource = getBlockChainDB();
            resetDataSource(bcSource);
        }
    	m_logger.debug("fastSyncCleanUp end");
    }
    
    public DbSource<byte[]> getBlockChainDB() {
    	m_logger.debug("getBlockChainDB start");
        return getKeyValueDataSource("sonchain");
    }
    
    public AbstractCachedSource<byte[], byte[]> getBlockChainDbCache(String cacheName) {
    	m_logger.debug("getBlockChainDbCache start");
        WriteCache.BytesKey<byte[]> ret = new WriteCache.BytesKey<>(
                new BatchSourceWriter<>(getBlockChainDB()), WriteCache.CacheType.SIMPLE);
        ret.setFlushSource(true);
        ret.withCacheName(cacheName);
    	m_logger.debug("getBlockChainDbCache end");
        return ret;
    }
    
    public Source<byte[], byte[]> getBlockChainSource(String name) {
    	m_logger.debug("getBlockChainSource start");
    	Source<byte[], byte[]> source =  new XorDataSource<>(getBlockChainDbCache(name), 
    			HashUtil.sha3(name.getBytes()), name);
    	return source;
    }
    
    public Source<byte[], byte[]> getCachedDbSource(String name) {
    	m_logger.debug("getCachedDbSource start");
//        AbstractCachedSource<byte[], byte[]>  writeCache 
//        	= new AsyncWriteCache<byte[], byte[]>(getBlockChainSource(name)) {
//            @Override
//            protected WriteCache<byte[], byte[]> CreateCache(Source<byte[], byte[]> source) {
//                WriteCache.BytesKey<byte[]> ret = new WriteCache.BytesKey<>(source, WriteCache.CacheType.SIMPLE);
//                ret.withSizeEstimators(MemSizeEstimator.ByteArrayEstimator, MemSizeEstimator.ByteArrayEstimator);
//                ret.setFlushSource(true);
//                return ret;
//            }
//        }.withName(name);
    	WriteCache<byte[], byte[]> writeCache
  			= new WriteCache.BytesKey(getBlockChainSource(name), WriteCache.CacheType.SIMPLE);
    	writeCache.setFlushSource(true);
    	writeCache.withCacheName(name);
        getDbFlushManager().addCache(writeCache);
    	m_logger.debug("getCachedDbSource end");
        return writeCache;
    }
    
    public DbFlushManager getDbFlushManager() {
    	m_logger.debug("getDbFlushManager start");
    	if(m_dbFlushManager == null){
    		m_dbFlushManager = new DbFlushManager(m_dbSources, getBlockChainDbCache(""));
    	}
        return m_dbFlushManager;
    }
    
    public DataSourceArray<BlockHeader> getHeaderSource() {
    	m_logger.debug("getHeaderSource start");
        DbSource<byte[]> dataSource = getKeyValueDataSource("headers");
        BatchSourceWriter<byte[], byte[]> batchSourceWriter = new BatchSourceWriter<>(dataSource);
        WriteCache.BytesKey<byte[]> writeCache = new WriteCache.BytesKey<>(batchSourceWriter,
        		WriteCache.CacheType.SIMPLE);
        writeCache.withSizeEstimators(MemSizeEstimator.ByteArrayEstimator, MemSizeEstimator.ByteArrayEstimator);
        writeCache.setFlushSource(true);
        writeCache.withCacheName("headers");
        ObjectDataSource<BlockHeader> objectDataSource = new ObjectDataSource<>(dataSource, Serializers.BlockHeaderSerializer, 0);
        DataSourceArray<BlockHeader> dataSourceArray = new DataSourceArray<>(objectDataSource);
    	m_logger.debug("getHeaderSource end");
        return dataSourceArray;
    }
    
    public BlockHeaderValidator getHeaderValidator() {
    	m_logger.debug("getHeaderValidator start");
        List<BlockHeaderRule> rules = new ArrayList<>(Arrays.asList(
                new ExtraDataRule(),
                new BlockHashRule()
        ));
    	m_logger.debug("getHeaderValidator end");
        return new BlockHeaderValidator(rules);
    }
    
    public Repository getDefaultRepository() {
        return new RepositoryRoot(getStateSource(), null);
    }  

	public DbSource<byte[]> getKeyValueDataSource(String name) {
		m_logger.debug("getKeyValueDataSource start name:" + name);
	    String dataSource = DataCenter.m_config.m_keyvalueDatasource;
	    try {
	    	if(m_mapDbSources.containsKey(name)){
	    		return m_mapDbSources.get(name);
	    	}
	    	else
	    	{
	            DbSource<byte[]> dbSource = null;
	            if ("inmem".equals(dataSource)) {
	            	dbSource = new HashMapDB<>();
	            }
	            else if("redis".equals(dataSource)){
	                dataSource = "redis";
	                dbSource = getRedisDbDataSource();
	            }
	            else {
	                dataSource = "leveldb";
	                dbSource = getLevelDbDataSource(name);
	            }
	            dbSource.setName(name);
	            dbSource.init();
	            m_dbSources.add(dbSource);
	            m_mapDbSources.put(name, dbSource);
	            return dbSource;
	    	}
	    } finally {
	    	m_logger.debug("getKeyValueDataSource end");
	    	m_logger.info(dataSource + " key-value data source created: " + name);
	    }
	}  
	    
    protected LevelDbDataSource getLevelDbDataSource(String name) {
    	m_logger.debug("getLevelDbDataSource start");
    	if(m_dicLevelDbDataSources.containsKey(name)){
    		return m_dicLevelDbDataSources.get(name);
    	}
    	else
    	{
    		LevelDbDataSource db = new LevelDbDataSource();
    		m_dicLevelDbDataSources.put(name, db);
            return db;
    	}
    }
    
    public PeerSource getPeerSource() {
    	m_logger.debug("getPeerSource start");
        DbSource<byte[]> dbSource = getKeyValueDataSource("peers");
        m_dbSources.add(dbSource);
        return new PeerSource(dbSource);
    }
    
    protected RedisDbDataSource getRedisDbDataSource(){
    	m_logger.debug("getRedisDbDataSource start");
    	return new RedisDbDataSource();
    }

    public Repository getRepository() {
    	m_logger.debug("getRepository start");
        return new RepositoryWrapper();
    }
    
    public Repository getRepository(byte[] stateRoot) {
    	m_logger.debug("getRepository stateRoot start");
        return new RepositoryRoot(getStateSource(), stateRoot);
    }
    
    public StateSource getStateSource() {
    	m_logger.debug("getStateSource start");
        fastSyncCleanUp();
        StateSource stateSource = new StateSource(getBlockChainSource("state"),
        		false, DataCenter.m_config.m_cacheMaxStateBloomSize << 20);
        getDbFlushManager().addCache(stateSource.getWriteCache());
    	m_logger.debug("getStateSource end");
        return stateSource;
    }
    
    public ParentBlockHeaderValidator getParentHeaderValidator() {
    	m_logger.debug("getParentHeaderValidator start");
        List<DependentBlockHeaderRule> rules = new ArrayList<>(Arrays.asList(
                new ParentNumberRule()
        ));
    	m_logger.debug("getParentHeaderValidator end");
        return new ParentBlockHeaderValidator(rules);
    }

    private void resetDataSource(Source source) {
    	m_logger.debug("resetDataSource start");
        if (source instanceof LevelDbDataSource) {
            ((LevelDbDataSource) source).reset();
        } else {
            throw new Error("Cannot cleanup non-LevelDB database");
        }
    	m_logger.debug("resetDataSource end");
        throw new Error("Cannot cleanup non-LevelDB database");
    }
}