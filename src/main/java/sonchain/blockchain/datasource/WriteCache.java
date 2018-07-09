package sonchain.blockchain.datasource;

import java.util.*;

import org.apache.log4j.Logger;
import org.bouncycastle.util.encoders.Hex;

import com.googlecode.concurentlocks.ReadWriteUpdateLock;
import com.googlecode.concurentlocks.ReentrantReadWriteUpdateLock;

import sonchain.blockchain.util.CLock;
import sonchain.blockchain.util.ByteArrayMap;
import sonchain.blockchain.util.ByteUtil;

public class WriteCache<Key, Value> extends AbstractCachedSource<Key, Value> {

	public static final Logger m_logger = Logger.getLogger(WriteCache.class);
    /**
     * Type of the write cache
     */
    public enum CacheType {
        SIMPLE,
        COUNTING
    }

    private static abstract class CacheEntry<V> implements Entry<V>{
        // dedicated value instance which indicates that the entry was deleted
        // (ref counter decremented) but we don't know actual value behind it
        static final Object UNKNOWN_VALUE = new Object();

        int m_counter = 0;
        V m_value;

        protected CacheEntry(V value) {
        	m_value = value;
        }

        protected abstract void added();
        protected abstract void deleted();
        protected abstract V getValue();

        @Override
        public V value() {
            V v = getValue();
            return v == UNKNOWN_VALUE ? null : v;
        }
    }

    private static final class SimpleCacheEntry<V> extends CacheEntry<V> {
        public SimpleCacheEntry(V value) {
            super(value);
        }

        public void added() {
        	m_counter = 1;
        }

        public void deleted() {
        	m_counter = -1;
        }

        @Override
        public V getValue() {
            return m_counter < 0 ? null : m_value;
        }
    }

    private static final class CountCacheEntry<V> extends CacheEntry<V> {
        public CountCacheEntry(V value) {
            super(value);
        }

        public void added() {
        	m_counter++;
        }

        public void deleted() {
        	m_counter--;
        }

        @Override
        public V getValue() {
            // for counting cache we return the cached value even if
            // it was deleted (once or several times) as we don't know
            // how many 'instances' are left behind
            return m_value;
        }
    }

    private boolean m_checked = false;
    protected volatile Map<Key, CacheEntry<Value>> m_cache = new HashMap<>();
    private final boolean m_isCounting;
    protected ReadWriteUpdateLock m_rwuLock = new ReentrantReadWriteUpdateLock();
    protected CLock m_readLock = new CLock(m_rwuLock.readLock());
    protected CLock m_writeLock = new CLock(m_rwuLock.writeLock());
    protected CLock m_updateLock = new CLock(m_rwuLock.updateLock());

    public WriteCache(Source<Key, Value> src, CacheType cacheType) {
        super(src);
        m_isCounting = cacheType == CacheType.COUNTING;
        m_logger.debug("WriteCache init end.");
    }

    public WriteCache<Key, Value> withCache(Map<Key, CacheEntry<Value>> cache) {
    	m_cache = cache;
        m_logger.debug("WriteCache init end.");
        return this;
    }
    
    public WriteCache<Key, Value> withCacheName(String cacheName) {
    	m_cacheName = cacheName;
        m_logger.debug("withCacheName init end. cacheName:" + cacheName);
        return this;
    }    

    // Guard against wrong cache Map
    // if a regular Map is accidentally used for byte[] type keys
    // the situation might be tricky to debug
    private void checkByteArrKey(Key key) {
        m_logger.debug("checkByteArrKey start key:" + Hex.toHexString((byte[])key));
        if(m_cache != null){
        	m_logger.debug("Cache HashCode:" + m_cache.hashCode());
        }
        if (m_checked){
            m_logger.debug("checkByteArrKey end.");
        	return;
        }
        if (key instanceof byte[]) {
            if (!(m_cache instanceof ByteArrayMap)) {
                throw new RuntimeException("Wrong map/set for byte[] key");
            }
        }
        m_checked = true;
        m_logger.debug("checkByteArrKey end.");
    }

    private CacheEntry<Value> createCacheEntry(Value val) {
        m_logger.debug("createCacheEntry start.");
        if(m_cache != null){
        	m_logger.debug("Cache HashCode:" + m_cache.hashCode());
        }
        if (m_isCounting) {
            m_logger.debug("createCacheEntry end.");
            return new CountCacheEntry<>(val);
        } else {
            m_logger.debug("createCacheEntry end.");
            return new SimpleCacheEntry<>(val);
        }
    }

    @Override
    public void delete(Key key) {
        m_logger.debug("delete start. cacheName:" + m_cacheName + " key:" + Hex.toHexString((byte[])key));
        if(m_cache != null){
        	m_logger.debug("Cache HashCode:" + m_cache.hashCode());
        }
        checkByteArrKey(key);
        try (CLock l = m_writeLock.lock()){
            CacheEntry<Value> curVal = m_cache.get(key);
            if (curVal == null) {
                curVal = createCacheEntry(getSource() == null ? null : unknownValue());
                CacheEntry<Value> oldVal = m_cache.put(key, curVal);
                if (oldVal != null) {
                    cacheRemoved(key, oldVal.m_value);
                }
                cacheAdded(key, curVal.m_value == unknownValue() ? null : curVal.m_value);
            }
            curVal.deleted();
        }
        m_logger.debug("delete end. cacheName:" + m_cacheName);
    }

    public long debugCacheSize() {
        m_logger.debug("debugCacheSize start. cacheName:" + m_cacheName);
        if(m_cache != null){
        	m_logger.debug("Cache HashCode:" + m_cache.hashCode());
        }
        long ret = 0;
        for (Map.Entry<Key, CacheEntry<Value>> entry : m_cache.entrySet()) {
            ret += m_keySizeEstimator.estimateSize(entry.getKey());
            ret += m_valueSizeEstimator.estimateSize(entry.getValue().value());
        }
        m_logger.debug("debugCacheSize end. result:" + ret);
        return ret;
    }

    @Override
    public boolean flush() {
        m_logger.debug("flush start.cacheName:" + m_cacheName);
        if(m_cache != null){
        	m_logger.debug("Cache HashCode:" + m_cache.hashCode());
        }
        boolean ret = false;
        try (CLock l = m_updateLock.lock()){
            for (Map.Entry<Key, CacheEntry<Value>> entry : m_cache.entrySet()) {
            	if(entry.getKey() instanceof byte[] && entry.getValue().m_value instanceof byte[]){
                	m_logger.debug("flush cacheName: " + m_cacheName + " flush key:" + ByteUtil.toHexString((byte[])entry.getKey())
                	+ "[value :]" + ByteUtil.toHexString((byte[])entry.getValue().m_value));
            	}
                if (entry.getValue().m_counter > 0) {
                    for (int i = 0; i < entry.getValue().m_counter; i++) {
                        getSource().put(entry.getKey(), entry.getValue().m_value);
                    }
                    ret = true;
                } else if (entry.getValue().m_counter < 0) {
                    for (int i = 0; i > entry.getValue().m_counter; i--) {
                        getSource().delete(entry.getKey());
                    }
                    ret = true;
                }
            }
            if (m_flushSource) {
                getSource().flush();
            }
            try (CLock l1 = m_writeLock.lock()){
            	m_cache.clear();
                cacheCleared();
            }
            m_logger.debug("flush end. result:" + ret);
            return ret;
        }
    }

    @Override
    protected boolean flushImpl() {
        m_logger.debug("flushImpl start.cacheName:" + m_cacheName);
        if(m_cache != null){
        	m_logger.debug("Cache HashCode:" + m_cache.hashCode());
        }
        return false;
    }

    @Override
    public Value get(Key key) {
        m_logger.debug("get start.cacheName:" + m_cacheName + " key:" + Hex.toHexString((byte[])key));
        if(m_cache != null){
        	m_logger.debug("Cache HashCode:" + m_cache.hashCode());
        }
        checkByteArrKey(key);
        try (CLock l = m_readLock.lock()){
            CacheEntry<Value> curVal = m_cache.get(key);
            if (curVal == null) {
            	Value value =  getSource() == null ? null : getSource().get(key);
            	return value;
            } else {
                Value value = curVal.getValue();
                if (value == unknownValue()) {
                    return getSource() == null ? null : getSource().get(key);
                } else {
                    return value;
                }
            }
        }
        catch(Exception e){
            m_logger.debug("get end.error :", e);
        }
		return null;
    }

    public Entry<Value> getCached(Key key) {
        m_logger.debug("getCached start.cacheName:" + m_cacheName + " key:" + Hex.toHexString((byte[])key));
        if(m_cache != null){
        	m_logger.debug("Cache HashCode:" + m_cache.hashCode());
        }
        try (CLock l = m_readLock.lock()){
            CacheEntry<Value> entry = m_cache.get(key);
            if (entry == null || entry.m_value == unknownValue()) {
                return null;
            }else {
                return entry;
            }
        }
    }

    @Override
    public Collection<Key> getModified() {
        m_logger.debug("getModified start.cacheName:" + m_cacheName);
        if(m_cache != null){
        	m_logger.debug("Cache HashCode:" + m_cache.hashCode());
        }
        try (CLock l = m_readLock.lock()){
            return m_cache.keySet();
        }
    }

    @Override
    public boolean hasModified() {
        m_logger.debug("hasModified start.cacheName:" + m_cacheName);
        if(m_cache != null){
        	m_logger.debug("Cache HashCode:" + m_cache.hashCode());
        }
        return !m_cache.isEmpty();
    }

    @Override
    public void put(Key key, Value val) {
        m_logger.debug("put start.cacheName:" + m_cacheName 
        		+ " key:" + Hex.toHexString((byte[])key) + " val:" + val);
        if(m_cache != null){
        	m_logger.debug("Cache HashCode:" + m_cache.hashCode());
        }
        checkByteArrKey(key);
        if (val == null)  {
            delete(key);
            m_logger.debug("put delete end."
            		+ " key:" + Hex.toHexString((byte[])key) + " val:" + val);
            return;
        }
        try (CLock l = m_writeLock.lock()){
            CacheEntry<Value> curVal = m_cache.get(key);
            if (curVal == null) {
                curVal = createCacheEntry(val);
                CacheEntry<Value> oldVal = m_cache.put(key, curVal);
                if (oldVal != null) {
                    cacheRemoved(key, oldVal.m_value == unknownValue() ? null : oldVal.m_value);
                }
                cacheAdded(key, curVal.m_value);
            }
            // assigning for non-counting cache only
            // for counting cache the value should be immutable (see HashedKeySource)
            curVal.m_value = val;
            curVal.added();
        }
        m_logger.debug("put end.");
    }

    private Value unknownValue() {
        return (Value) CacheEntry.UNKNOWN_VALUE;
    }
    
    /**
     * Shortcut for WriteCache with byte[] keys. Also prevents accidental
     * usage of regular Map implementation (non byte[])
     */
    public static class BytesKey<V> extends WriteCache<byte[], V> implements CachedSource.BytesKey<V> {
        public BytesKey(Source<byte[], V> src, CacheType cacheType) {
            super(src, cacheType);
            withCache(new ByteArrayMap<CacheEntry<V>>());
            m_logger.debug("BytesKey");
        }
    }
}