package sonchain.blockchain.db;

import java.util.*;
import java.util.concurrent.*;

import org.apache.log4j.Logger;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

import sonchain.blockchain.datasource.base.AbstractCachedSource;
import sonchain.blockchain.datasource.base.AsyncFlushable;
import sonchain.blockchain.datasource.base.DbSource;
import sonchain.blockchain.listener.CompositeSonChainListener;
import sonchain.blockchain.listener.SonChainListenerAdapter;
import sonchain.blockchain.service.DataCenter;

public class DbFlushManager {

	public static final Logger m_logger = Logger.getLogger(DbFlushManager.class);
    private Set<DbSource> m_dbSources = new HashSet<>();
    private int m_commitCount = 0;
    private int m_commitsCountThreshold = 0;
    private boolean m_flushAfterSyncDone = false;
    private long m_sizeThreshold = 0;
    private AbstractCachedSource<String, String> m_stateDbCache = null;
    private boolean m_syncDone = true;
    private List<AbstractCachedSource<String, String>> m_writeCaches = new ArrayList<>();

//    private final BlockingQueue<Runnable> m_executorQueue = new ArrayBlockingQueue<>(1);
//    private final ExecutorService m_flushThread = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS,
//    		m_executorQueue, new ThreadFactoryBuilder().setNameFormat("DbFlushManagerThread-%d").build());
//    private Future<Boolean> m_lastFlush = Futures.immediateFuture(false);

    public DbFlushManager(Set<DbSource> dbSources, AbstractCachedSource<String, String> stateDbCache) {
        m_dbSources = dbSources;
        m_sizeThreshold = DataCenter.m_config.m_cacheFlushWriteCacheSize * 1024 * 1024;
        m_commitsCountThreshold = DataCenter.m_config.m_cacheFlushBlocks;
        m_flushAfterSyncDone = DataCenter.m_config.m_cacheFlushShortSyncFlush;
        m_stateDbCache = stateDbCache;
    }

    public void setEthereumListener(CompositeSonChainListener listener) {
        if (!m_flushAfterSyncDone){
        	return;
        }
        listener.addListener(new SonChainListenerAdapter() {
            @Override
            public void onSyncDone(SyncState state) {
                if (state == SyncState.COMPLETE) {
                	m_logger.info("DbFlushManager: long sync done, flushing each block now");
                	m_syncDone = true;
                }
            }
        });
    }

    public void setSizeThreshold(long sizeThreshold) {
    	m_sizeThreshold = sizeThreshold;
    }

    public void addCache(AbstractCachedSource<String, String> cache) {
    	m_writeCaches.add(cache);
    }

    public synchronized void commit(Runnable atomicUpdate) {
        atomicUpdate.run();
        commit();
    }
    
    public synchronized void commit() {
        long cacheSize = 0; //getCacheSize();
        if (m_sizeThreshold >= 0 && cacheSize >= m_sizeThreshold) {
        	m_logger.info("DbFlushManager: flushing db due to write cache size (" 
        			+ cacheSize + ") reached threshold (" + m_sizeThreshold + ")");
        	flush();
        } else if (m_commitsCountThreshold > 0 && m_commitCount >= m_commitsCountThreshold) {
        	m_logger.info("DbFlushManager: flushing db due to commits (" + m_commitCount 
        			+ ") reached threshold (" + m_commitsCountThreshold + ")");
        	flush();
        	m_commitCount = 0;
        } else if (m_flushAfterSyncDone && m_syncDone) {
        	m_logger.debug("DbFlushManager: flushing db due to short sync");
        	flush();
        }
        m_commitCount++;
    }

    public synchronized void commitBack() {
        if (m_sizeThreshold >= 0) {
        	m_logger.info("DbFlushManager: flushing db due to reached threshold (" + m_sizeThreshold + ")");
        	flush();
        } else if (m_commitsCountThreshold > 0 && m_commitCount >= m_commitsCountThreshold) {
        	m_logger.info("DbFlushManager: flushing db due to commits (" + m_commitCount 
        			+ ") reached threshold (" + m_commitsCountThreshold + ")");
        	flush();
        	m_commitCount = 0;
        } else if (m_flushAfterSyncDone && m_syncDone) {
        	m_logger.debug("DbFlushManager: flushing db due to short sync");
        	flush();
        }
        m_commitCount++;
    }

    public synchronized boolean flushSync() {
        try {
        	//flush().get();
        	return true;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public synchronized boolean flush() {
        m_logger.debug("flush start");
        for (AbstractCachedSource<String, String> writeCache : m_writeCaches) {
            try {
            	writeCache.flush();
            } catch (Exception e) {
            	m_logger.debug("flush error:" + e.getMessage());
                throw new RuntimeException(e);
            }
        }
        m_logger.debug("flush end");
    	return true;
    }

    /**
     * Flushes all caches and closes all databases
     */
    public synchronized void close() {
        m_logger.info("Flushing DBs...");
        flushSync();
        m_logger.info("Flush done.");
        for (DbSource dbSource : m_dbSources) {
            m_logger.info(String.format("Closing DB: {%s}", dbSource.getName()));
            try {
                dbSource.close();
            } catch (Exception ex) {
            	m_logger.error(String.format("Caught error while closing DB: %s", dbSource.getName()), ex);
            }
        }
    }
}
