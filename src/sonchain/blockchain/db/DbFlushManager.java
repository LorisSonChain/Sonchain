package sonchain.blockchain.db;

import java.util.*;
import java.util.concurrent.*;

import org.apache.log4j.Logger;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

import sonchain.blockchain.datasource.AbstractCachedSource;
import sonchain.blockchain.datasource.AsyncFlushable;
import sonchain.blockchain.datasource.DbSource;
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
    private AbstractCachedSource<byte[], byte[]> m_stateDbCache = null;
    private boolean m_syncDone = true;
    private List<AbstractCachedSource<byte[], byte[]>> m_writeCaches = new ArrayList<>();

//    private final BlockingQueue<Runnable> m_executorQueue = new ArrayBlockingQueue<>(1);
//    private final ExecutorService m_flushThread = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS,
//    		m_executorQueue, new ThreadFactoryBuilder().setNameFormat("DbFlushManagerThread-%d").build());
//    private Future<Boolean> m_lastFlush = Futures.immediateFuture(false);

    public DbFlushManager(Set<DbSource> dbSources, AbstractCachedSource<byte[], byte[]> stateDbCache) {
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

    public void addCache(AbstractCachedSource<byte[], byte[]> cache) {
    	m_writeCaches.add(cache);
    }

    public long getCacheSize() {
        long ret = 0;
        for (AbstractCachedSource<byte[], byte[]> writeCache : m_writeCaches) {
            ret += writeCache.estimateCacheSize();
        }
        return ret;
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
        long cacheSize = getCacheSize();
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
        for (AbstractCachedSource<byte[], byte[]> writeCache : m_writeCaches) {
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

//    public synchronized Future<Boolean> flushBack() {
//        if (!m_lastFlush.isDone()) {
//            m_logger.info("Waiting for previous flush to complete...");
//            try {
//            	m_lastFlush.get();
//            } catch (Exception e) {
//                m_logger.error("Error during last flush", e);
//            }
//        }
//        m_logger.debug("Flipping async storages");
//        for (AbstractCachedSource<byte[], byte[]> writeCache : m_writeCaches) {
//            try {
//                if (writeCache instanceof AsyncFlushable) {
//                    ((AsyncFlushable) writeCache).flipStorage();
//                }
//            } catch (InterruptedException e) {
//                throw new RuntimeException(e);
//            }
//        }
//
//        m_logger.debug("Submitting flush task");
//        return m_lastFlush = m_flushThread.submit(new Callable<Boolean>() {
//            @Override
//            public Boolean call() throws Exception {
//                boolean ret = false;
//                long s = System.nanoTime();
//                m_logger.info("Flush started");
//                for (AbstractCachedSource<byte[], byte[]> writeCache : m_writeCaches) {
//                    if (writeCache instanceof AsyncFlushable) {
//                        try {
//                            ret |= ((AsyncFlushable) writeCache).flushAsync().get();
//                        } catch (InterruptedException e) {
//                            throw new RuntimeException(e);
//                        }
//                    } else {
//                        ret |= writeCache.flush();
//                    }
//                }
//                if (m_stateDbCache != null) {
//                    m_logger.debug("Flushing to DB");
//                	m_stateDbCache.flush();
//                }
//                m_logger.info("Flush completed in " + (System.nanoTime() - s) / 1000000 + " ms");
//                return ret;
//            }
//        });
//    }

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
