package sonchain.blockchain.datasource;

import java.util.*;

import org.apache.commons.collections4.map.LRUMap;
import org.apache.log4j.Logger;
import org.bouncycastle.util.encoders.Hex;

import sonchain.blockchain.datasource.base.AbstractCachedSource;
import sonchain.blockchain.datasource.base.CachedSource;
import sonchain.blockchain.datasource.base.Source;
import sonchain.blockchain.db.ByteArrayWrapper;
import sonchain.blockchain.db.StringWrapper;
import sonchain.blockchain.util.ByteArrayMap;
import sonchain.blockchain.util.StringMap;

public class ReadCache<Key, Value> extends AbstractCachedSource<Key, Value> {

	public static final Logger m_logger = Logger.getLogger(ReadCache.class);
    private boolean m_stringKeyMap = false;
    private Map<Key, Value> m_cache = null;
    // the guard against incorrect Map implementation for byte[] keys
    private boolean m_checked = false;

    public ReadCache(Source<Key, Value> src) {
        super(src);
        withCache(new HashMap<Key, Value>());
        m_logger.debug("ReadCache init end");
    }
    
    private void checkStringKey(Key key) {
        m_logger.debug("ReadCache checkByteArrKey start");
        if(m_cache != null){
        	m_logger.debug("Cache HashCode:" + m_cache.hashCode());
        }
        if (m_checked){
        	return;
        }
        if (key instanceof String) {
            if (!m_stringKeyMap) {
                throw new RuntimeException("Wrong map/set for String key");
            }
        }
        m_checked = true;
        m_logger.debug("ReadCache checkByteArrKey end");
    }
    
    private void checkByteArrKey(Key key) {
        m_logger.debug("ReadCache checkByteArrKey start");
        if(m_cache != null){
        	m_logger.debug("Cache HashCode:" + m_cache.hashCode());
        }
        if (m_checked){
        	return;
        }
        if (key instanceof byte[]) {
            if (m_stringKeyMap) {
                throw new RuntimeException("Wrong map/set for byte[] key");
            }
        }
        m_checked = true;
        m_logger.debug("ReadCache checkByteArrKey end");
    }

    @Override
    public void delete(Key key) {
    	checkStringKey(key);
        m_logger.debug("ReadCache delete start key:" + key.toString());
        if(m_cache != null){
        	m_logger.debug("Cache HashCode:" + m_cache.hashCode());
        }
        Value value = m_cache.remove(key);
        cacheRemoved(key, value);
        getSource().delete(key);
        m_logger.debug("ReadCache delete end");
    }

    @Override
    public Value get(Key key) {
    	checkStringKey(key);
        m_logger.debug("ReadCache get start key:" + key.toString());
        if(m_cache != null){
        	m_logger.debug("Cache HashCode:" + m_cache.hashCode());
        }
        Value ret = m_cache.get(key);
        if (ret == null) {
            m_logger.debug("ReadCache get end null key:" +  key.toString());
        }
        if (ret == null) {
            ret = getSource().get(key);
            m_cache.put(key, ret == null ? null : ret);
            cacheAdded(key, ret);
        }
        m_logger.debug("ReadCache get end result:" + ret);
        return ret;
    }

    @Override
    public synchronized Entry<Value> getCached(Key key) {
    	checkStringKey(key);
        m_logger.debug("ReadCache getCached start key:" + key.toString());
        if(m_cache != null){
        	m_logger.debug("Cache HashCode:" + m_cache.hashCode());
        }
        Value value = m_cache.get(key);
        return value == null ? null : new SimpleEntry<>(value == null ? null : value);
    }

    public synchronized Collection<Key> getModified() {
        m_logger.debug("ReadCache getModified start");
        if(m_cache != null){
        	m_logger.debug("Cache HashCode:" + m_cache.hashCode());
        }
        return Collections.emptyList();
    }

    @Override
    protected boolean flushImpl() {
        m_logger.debug("ReadCache flushImpl start");
        if(m_cache != null){
        	m_logger.debug("Cache HashCode:" + m_cache.hashCode());
        }
        return false;
    }

    @Override
    public boolean hasModified() {
        m_logger.debug("ReadCache hasModified start");
        if(m_cache != null){
        	m_logger.debug("Cache HashCode:" + m_cache.hashCode());
        }
        return false;
    }

    @Override
    public void put(Key key, Value val) {
    	checkStringKey(key);
        m_logger.debug("ReadCache put start cacheID:" + m_cache.hashCode() 
        	+ " key:" + key.toString() + " val: " + val);
        if(m_cache != null){
        	m_logger.debug("Cache HashCode:" + m_cache.hashCode());
        }
        if (val == null) {
            delete(key);
            m_logger.debug("ReadCache put delete end key:" + key.toString() + " val: " + val);
        } else {
        	m_cache.put(key, val);
            cacheAdded(key, val);
            getSource().put(key, val);
        }
        m_logger.debug("ReadCache put end key:" + key.toString() + " val: " + val);
    }

    /**
     * Installs the specific cache Map implementation
     */
    public ReadCache<Key, Value> withCache(Map<Key, Value> cache) {
        m_logger.debug("ReadCache withCache start");
        if(m_cache != null){
        	m_logger.debug("Cache HashCode:" + m_cache.hashCode());
        }
        m_stringKeyMap = cache instanceof StringMap;
        m_cache = Collections.synchronizedMap(cache);
        m_logger.debug("ReadCache withCache end");
        return this;
    }

    /**
     * Sets the max number of entries to cache
     */
    public ReadCache<Key, Value> withMaxCapacity(int maxCapacity) {
        return withCache(new LRUMap<Key, Value>(maxCapacity) {
            @Override
            protected boolean removeLRU(LinkEntry<Key, Value> entry) {
                cacheRemoved(entry.getKey(), entry.getValue());
                return super.removeLRU(entry);
            }
        });
    }

    /**
     * Shortcut for ReadCache with byte[] keys. Also prevents accidental
     * usage of regular Map implementation (non byte[])
     */
    public static class BytesKey<V> extends ReadCache<byte[], V> implements CachedSource.BytesKey<V> {

        public BytesKey(Source<byte[], V> src) {
            super(src);
            withCache(new ByteArrayMap<V>());
        }

        public ReadCache.BytesKey<V> withMaxCapacity(int maxCapacity) {
            m_logger.debug("ReadCache withMaxCapacity start");
            withCache(new ByteArrayMap<V>(new LRUMap<ByteArrayWrapper, V>(maxCapacity) {
                @Override
                protected boolean removeLRU(LinkEntry<ByteArrayWrapper, V> entry) {
                    cacheRemoved(entry.getKey().getData(), entry.getValue());
                    return super.removeLRU(entry);
                }
            }));
            m_logger.debug("ReadCache withMaxCapacity end");
            return this;
        }
    }

    /**
     * Shortcut for ReadCache with byte[] keys. Also prevents accidental
     * usage of regular Map implementation (non byte[])
     */
    public static class StringKey<V> extends ReadCache<String, V> implements CachedSource.StringKey<V> {

        public StringKey(Source<String, V> src) {
            super(src);
            withCache(new StringMap<V>());
        }

        public ReadCache.StringKey<V> withMaxCapacity(int maxCapacity) {
            m_logger.debug("ReadCache withMaxCapacity start");
            withCache(new StringMap<V>(new LRUMap<StringWrapper, V>(maxCapacity) {
                @Override
                protected boolean removeLRU(LinkEntry<StringWrapper, V> entry) {
                    cacheRemoved(entry.getKey().getData(), entry.getValue());
                    return super.removeLRU(entry);
                }
            }));
            m_logger.debug("ReadCache withMaxCapacity end");
            return this;
        }
    }
}
