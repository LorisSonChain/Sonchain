package sonchain.blockchain.datasource;

import sonchain.blockchain.datasource.base.AbstractCachedSource;
import sonchain.blockchain.datasource.base.CachedSource;
import sonchain.blockchain.datasource.base.Source;

public abstract class MultiCache<V extends CachedSource> extends ReadWriteCache.StringKey<V> {

    public MultiCache(Source<String, V> src) {
        super(src, WriteCache.CacheType.SIMPLE);
    }
    
    /**
     * Creates a local child cache instance based on the child cache instance
     * (or null) from the MultiCache backing Source
     */
    protected abstract V create(String key, V srcCache);

    /**
     * Is invoked to flush child cache if it has backing Source
     * Some additional tasks may be performed by subclasses here
     */
    protected boolean flushChild(String key, V childCache) {
        return childCache != null ? childCache.flush() : true;
    }

    /**
     * each child is just flushed if it has backing Source or the whole
     * child cache is put to the MultiCache backing source
     */
    @Override
    public synchronized boolean flushImpl() {
        boolean ret = false;
        for (String key: m_writeCache.getModified()) {
            V value = super.get(key);
            if (value == null) {
                // cache was deleted
                ret |= flushChild(key, value);
                if (getSource() != null) {
                    getSource().delete(key);
                }
            } else if (value.getSource() != null){
                ret |= flushChild(key, value);
            } else {
                getSource().put(key, value);
                ret = true;
            }
        }
        return ret;
    }

    /**
     * When a child cache is not found in the local cache it is looked up in the backing Source
     * Based on this child backing cache (or null if not found) the new local cache is created
     * via create() method
     */
    @Override
    public synchronized V get(String key) {
        AbstractCachedSource.Entry<V> ownCacheEntry = getCached(key);
        V ownCache = ownCacheEntry == null ? null : ownCacheEntry.value();
        if (ownCache == null) {
            V v = getSource() != null ? super.get(key) : null;
            ownCache = create(key, v);
            put(key, ownCache);
        }
        return ownCache;
    }
}