package sonchain.blockchain.datasource;

import java.util.Collection;

public interface CachedSource<Key, Value> extends Source<Key, Value> {

    /**
     * Estimates the size of cached entries in bytes.
     * This value shouldn't be precise size of Java objects
     * @return cache size in bytes
     */
    long estimateCacheSize();
    
    /**
     * @return The underlying Source
     */
    Source<Key, Value> getSource();

    /**
     * @return Modified entry keys if this is a write cache
     */
    Collection<Key> getModified();

    /**
     * @return indicates the cache has modified entries
     */
    boolean hasModified();

    /**
     * Just a convenient shortcut to the most popular Sources with byte[] key
     */
    interface BytesKey<Value> extends CachedSource<byte[], Value> {}
}
