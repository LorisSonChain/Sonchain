package sonchain.blockchain.datasource;

/**
 * Just a convenient class to store arbitrary Objects into byte[] value backing
 * Source.
 * Includes ReadCache for caching deserialized objects and object Serializer
 * @author GAIA
 *
 * @param <V>
 */
public class ObjectDataSource<V> extends SourceChainBox<byte[], V, byte[], byte[]> {
	
    private Source<byte[], byte[]> m_byteSource = null;
	//private ReadCache<byte[], V> m_cache = null;
    private SourceCodec<byte[], V, byte[], byte[]> m_codec = null;

    /**
     * Creates new instance
     * @param byteSource baking store
     * @param serializer for encode/decode byte[] <=> V
     * @param readCacheEntries number of entries to cache
     */
    public ObjectDataSource(Source<byte[], byte[]> byteSource, Serializer<V, byte[]> serializer, int readCacheEntries) {
        super(byteSource);
        m_byteSource = byteSource;
        add(m_codec = new SourceCodec<>(byteSource, new Serializers.Identity<byte[]>(), serializer));
        //if (readCacheEntries > 0) {
        //    add(m_cache = new ReadCache.BytesKey<>(m_codec).withMaxCapacity(readCacheEntries));
        //}
    }
}
