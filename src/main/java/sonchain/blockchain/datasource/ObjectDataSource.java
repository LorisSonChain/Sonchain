package sonchain.blockchain.datasource;

import sonchain.blockchain.datasource.base.AbstractSource;
import sonchain.blockchain.datasource.base.Serializer;
import sonchain.blockchain.datasource.base.Source;

public class ObjectDataSource<V> extends AbstractSource<String, V, String, String> {
	
    private Source<String, String> m_byteSource = null;
	//private ReadCache<byte[], V> m_cache = null;
    private SourceCodec<String, V, String, String> m_codec = null;

    /**
     * Creates new instance
     * @param byteSource baking store
     * @param serializer for encode/decode string <=> V
     * @param readCacheEntries number of entries to cache
     */
    public ObjectDataSource(Source<String, String> stringSource, Serializer<V, String> serializer, int readCacheEntries) {
        super(stringSource);
        m_byteSource = stringSource;
        m_codec = new SourceCodec<>(stringSource, new Serializers.Identity<String>(), serializer);
        //if (readCacheEntries > 0) {
        //    add(m_cache = new ReadCache.BytesKey<>(m_codec).withMaxCapacity(readCacheEntries));
        //}
    }

	@Override
	public void delete(String key) {
		m_logger.debug("delete start." + " key:" + key.toString());
		m_codec.delete(key);
		
	}

	@Override
	public V get(String key) {
		m_logger.debug("get start." + " key:" + key.toString());
		return m_codec.get(key);
	}

	@Override
	public void put(String key, V val) {
		if(val instanceof String){
			m_logger.debug("put start." + " key:" + key.toString() + " val:" + val.toString());
		}
		m_codec.put(key, val);
		m_logger.debug("put end.");
		
	}

	@Override
	protected boolean flushImpl() {
		m_logger.debug("flushImpl start.");
		return m_codec.flush();
	}
}
