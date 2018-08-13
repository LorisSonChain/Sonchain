package sonchain.blockchain.datasource;

import sonchain.blockchain.datasource.base.AbstractCachedSource;
import sonchain.blockchain.datasource.base.AbstractSource;
import sonchain.blockchain.datasource.base.CachedSource;
import sonchain.blockchain.datasource.base.Source;

import java.util.Collection;
import org.apache.log4j.Logger;

public class ReadWriteCache<Key, Value> extends AbstractSource<Key, Value, Key, Value>
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
		m_writeCache = new WriteCache<>(src, cacheType);
		m_readCache = new ReadCache<>(m_writeCache);
		m_readCache.setFlushSource(true);
		m_logger.debug("ReadWriteCache init end");
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
			m_writeCache = new WriteCache.BytesKey<>(src, cacheType);
			m_readCache = new ReadCache.BytesKey<>(m_writeCache);
			m_readCache.setFlushSource(true);
			m_logger.debug("BytesKey  init end");
		}
	}

	public static class StringKey<V> extends ReadWriteCache<String, V> {
		public StringKey(Source<String, V> src, WriteCache.CacheType cacheType) {
			super(src);
			m_writeCache = new WriteCache.StringKey(src, cacheType);
			m_readCache = new ReadCache.StringKey(m_writeCache);
			m_readCache.setFlushSource(true);
			m_logger.debug("BytesKey  init end");
		}
	}

	@Override
	public void delete(Key key) {
		m_logger.debug("delete start." + " key:" + key.toString());
		m_readCache.delete(key);
		
	}

	@Override
	public Value get(Key key) {
		m_logger.debug("get start." + " key:" + key.toString());
		return m_readCache.get(key);
	}

	@Override
	public void put(Key key, Value val) {
		if(val instanceof String){
			m_logger.debug("put start." + " key:" + key.toString() + " val:" + val.toString());
		}
		m_readCache.put(key, val);
		m_logger.debug("put end.");
		
	}

	@Override
	protected boolean flushImpl() {
		m_logger.debug("flushImpl start.");
		return m_readCache.flush();
	}
}
