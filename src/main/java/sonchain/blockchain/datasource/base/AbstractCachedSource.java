package sonchain.blockchain.datasource.base;

import org.apache.log4j.Logger;

public abstract class AbstractCachedSource<Key, Value> 
		extends AbstractSource<Key, Value, Key, Value> implements CachedSource<Key, Value> {
	public static final Logger m_logger = Logger.getLogger(AbstractCachedSource.class);
	protected String m_cacheName = "";
	
	/**
	 * Like the Optional interface represents either the value cached or null
	 * cached (i.e. cache knows that underlying storage contain null)
	 */
	public interface Entry<V> {
		V value();
	}

	public static final class SimpleEntry<V> implements Entry<V> {
		private V m_val;
		public SimpleEntry(V val) {
			m_val = val;
		}
		public V value() {
			return m_val;
		}
	}

	public AbstractCachedSource(Source<Key, Value> source) {
		super(source);
		m_logger.debug("AbstractCachedSource init end");
	}

	/**
	 * Returns the cached value if exist. Method doesn't look into the
	 * underlying storage
	 * 
	 * @return The value Entry if it cached (Entry may has null value if null
	 *         value is cached), or null if no information in the cache for this
	 *         key
	 */
	public abstract Entry<Value> getCached(Key key);

	/**
	 * Needs to be called by the implementation when cache entry is added Only
	 * new entries should be accounted for accurate size tracking If the value
	 * for the key is changed the {@link #cacheRemoved} needs to be called first
	 */
	protected void cacheAdded(Key key, Value value) {
		m_logger.debug("cacheAdded init start");
		m_logger.debug("cacheAdded init end");
	}

	/**
	 * Needs to be called by the implementation when cache is cleared
	 */
	protected void cacheCleared() {
		m_logger.debug("cacheCleared init start");
		m_logger.debug("cacheCleared init end");
	}

	/**
	 * Needs to be called by the implementation when cache entry is removed
	 */
	protected void cacheRemoved(Key key, Value value) {
		m_logger.debug("cacheRemoved init start");
		m_logger.debug("cacheRemoved init end");
	}
}
