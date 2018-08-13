package sonchain.blockchain.datasource.base;

import java.util.Collection;

public interface CachedSource<Key, Value> extends Source<Key, Value> {
    
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

    /**
     * Just a convenient shortcut to the most popular Sources with byte[] key
     */
    interface StringKey<Value> extends CachedSource<String, Value> {}
}
