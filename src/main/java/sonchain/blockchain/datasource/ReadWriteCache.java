package sonchain.blockchain.datasource;

import java.util.Collection;

import org.apache.log4j.Logger;

public class ReadWriteCache<Key, Value> extends SourceChainBox<Key, Value, Key, Value>
		implements CachedSource<Key, Value> {

	public static final Logger m_logger = Logger.getLogger(ReadWriteCache.class);
	protected ReadCache<Key, Value> m_readCache = null;
	protected WriteCache<Key, Value> m_writeCache = null;

	protected ReadWriteCache(Source<Key, Value> source) {
		super(source);
		m_logger.debug("ReadWriteCache init end");
	}

	public ReadWriteCache(Source<Key, Value> src, WriteCache.CacheType cacheType) {
		super(src);
		add(m_writeCache = new WriteCache<>(src, cacheType));
		add(m_readCache = new ReadCache<>(m_writeCache));
		m_readCache.setFlushSource(true);
		m_logger.debug("ReadWriteCache init end");
	}

	@Override
	public synchronized long estimateCacheSize() {
		m_logger.debug("estimateCacheSize start");
		return m_readCache.estimateCacheSize() + m_writeCache.estimateCacheSize();
	}

	protected synchronized AbstractCachedSource.Entry<Value> getCached(Key key) {
		m_logger.debug("getCached start");
		AbstractCachedSource.Entry<Value> v = m_readCache.getCached(key);
		if (v == null) {
			v = m_writeCache.getCached(key);
		}
		return v;
	}

	@Override
	public synchronized Collection<Key> getModified() {
		m_logger.debug("getModified start");
		return m_writeCache.getModified();
	}

	@Override
	public boolean hasModified() {
		m_logger.debug("hasModified start");
		boolean result = m_writeCache.hasModified();
		m_logger.debug("hasModified end result:" + result);
		return result;
	}

	public static class BytesKey<V> extends ReadWriteCache<byte[], V> {
		public BytesKey(Source<byte[], V> src, WriteCache.CacheType cacheType) {
			super(src);
			add(m_writeCache = new WriteCache.BytesKey<>(src, cacheType));
			add(m_readCache = new ReadCache.BytesKey<>(m_writeCache));
			m_readCache.setFlushSource(true);
			m_logger.debug("BytesKey  init end");
		}
	}
}
