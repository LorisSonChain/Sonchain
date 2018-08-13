package sonchain.blockchain.datasource.base;

import com.google.common.util.concurrent.*;

import sonchain.blockchain.datasource.WriteCache;
import sonchain.blockchain.util.CLock;

import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.log4j.Logger;

public abstract class AbstractAsyncWriteCache<Key, Value> 
	extends AbstractCachedSource<Key, Value> implements AsyncFlushable {

	public static final Logger m_logger = Logger.getLogger(AbstractAsyncWriteCache.class);
    protected volatile WriteCache<Key, Value> m_curCache = null;    
    private static ListeningExecutorService m_flushExecutor = MoreExecutors.listeningDecorator(
            Executors.newFixedThreadPool(2, new ThreadFactoryBuilder().setNameFormat("AsyncWriteCacheThread-%d").build()));
    
    protected WriteCache<Key, Value> m_flushingCache = null;
    private ListenableFuture<Boolean> m_lastFlush = Futures.immediateFuture(false);
    private String m_name = "<null>";
    private final ReadWriteLock m_rwLock = new ReentrantReadWriteLock();
    private final CLock m_rLock = new CLock(m_rwLock.readLock());
    private final CLock m_wLock = new CLock(m_rwLock.writeLock());

    protected abstract WriteCache<Key, Value> CreateCache(Source<Key, Value> source);

    public AbstractAsyncWriteCache(Source<Key, Value> source) {
        super(source);
        m_flushingCache = CreateCache(source);
        m_flushingCache.setFlushSource(true);
        m_curCache = CreateCache(m_flushingCache);
        m_logger.debug("AsyncWriteCache init end");
    }

    @Override
    public void delete(Key key) {
        m_logger.debug("AsyncWriteCache delete start");
        try (CLock l = m_rLock.lock()) {
        	m_curCache.delete(key);
        }
        m_logger.debug("AsyncWriteCache delete end");
    }

    @Override
    public synchronized boolean flush() {
        m_logger.debug("AsyncWriteCache flush start");
        try {
            flipStorage();
            flushAsync();
            return m_flushingCache.hasModified();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        finally{
            m_logger.debug("AsyncWriteCache flush end");
        }
    }

    @Override
    public Value get(Key key) {
        m_logger.debug("AsyncWriteCache get start");
        try (CLock l = m_rLock.lock()) {
            return m_curCache.get(key);
        }
    }

    @Override
	public  Entry<Value> getCached(Key key) {
        m_logger.debug("AsyncWriteCache getCached start");
        return m_curCache.getCached(key);
    }

    @Override
    public Collection<Key> getModified() {
        m_logger.debug("AsyncWriteCache getModified start");
        try (CLock l = m_rLock.lock()) {
            return m_curCache.getModified();
        }
    }

    public synchronized ListenableFuture<Boolean> flushAsync() throws InterruptedException {
        m_logger.debug("AsyncWriteCache (" + m_name + "): flush submitted");
    	m_lastFlush = m_flushExecutor.submit(new Callable<Boolean>() {
            @Override
            public Boolean call() {
            	m_logger.debug("AsyncWriteCache (" + m_name + "): flush started");
                long s = System.currentTimeMillis();
                boolean ret = m_flushingCache.flush();
                m_logger.debug("AsyncWriteCache (" + m_name + "): flush completed in " + (System.currentTimeMillis() - s) + " ms");
                return ret;
            }
        });
        return m_lastFlush;
    }

    @Override
    public synchronized void flipStorage() throws InterruptedException {
        // if previous flush still running
        m_logger.debug("AsyncWriteCache (" + m_name + "): flipStorage start");
        try {
            if (!m_lastFlush.isDone()){
            	m_logger.debug("AsyncWriteCache (" + m_name + "): waiting for previous flush to complete");            	
            }
            m_lastFlush.get();
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }

        try (CLock l = m_wLock.lock()) {
        	m_flushingCache.setCache(m_curCache.getCache());
            m_curCache = CreateCache(m_flushingCache);
        }
        m_logger.debug("AsyncWriteCache (" + m_name + "): flipStorage end");
    }

    @Override
    protected synchronized boolean flushImpl() {
        return false;
    }

    @Override
    public boolean hasModified() {
        m_logger.debug("AsyncWriteCache (" + m_name + "): hasModified start");
        try (CLock l = m_rLock.lock()) {
            return m_curCache.hasModified();
        }
    }

    @Override
    public void put(Key key, Value val) {
        m_logger.debug("AsyncWriteCache (" + m_name + "): put start");
        try (CLock l = m_rLock.lock()) {
        	m_curCache.put(key, val);
        }
    }

    public AbstractAsyncWriteCache<Key, Value> withName(String name) {
        m_logger.debug("AsyncWriteCache (" + m_name + "): withName start");
        m_name = name;
        return this;
    }
}
