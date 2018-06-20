package sonchain.blockchain.datasource;

import org.apache.log4j.Logger;

public abstract class AbstractCachedSource<Key, Value> 
		extends AbstractChainedSource<Key, Value, Key, Value>
		implements CachedSource<Key, Value> {
	public static final Logger m_logger = Logger.getLogger(AbstractCachedSource.class);

	private final Object m_lock = new Object();
	protected String m_cacheName = "";

	/**
	 * Like the Optional interface represents either the value cached or null
	 * cached (i.e. cache knows that underlying storage contain null)
	 */
	public interface Entry<V> {
		V value();
	}

	static final class SimpleEntry<V> implements Entry<V> {
		private V m_val;
		public SimpleEntry(V val) {
			m_val = val;
		}
		public V value() {
			return m_val;
		}
	}

	protected MemSizeEstimator<Key> m_keySizeEstimator = null;
	protected MemSizeEstimator<Value> m_valueSizeEstimator = null;
	private int m_size = 0;

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
	abstract Entry<Value> getCached(Key key);

	/**
	 * Needs to be called by the implementation when cache entry is added Only
	 * new entries should be accounted for accurate size tracking If the value
	 * for the key is changed the {@link #cacheRemoved} needs to be called first
	 */
	protected void cacheAdded(Key key, Value value) {
		m_logger.debug("cacheAdded init start");
		synchronized (m_lock) {
			if (m_keySizeEstimator != null) {
				m_size += m_keySizeEstimator.estimateSize(key);
			}
			if (m_valueSizeEstimator != null) {
				m_size += m_valueSizeEstimator.estimateSize(value);
			}
		}
		m_logger.debug("cacheAdded init end");
	}

	/**
	 * Needs to be called by the implementation when cache is cleared
	 */
	protected void cacheCleared() {
		m_logger.debug("cacheCleared init start");
		synchronized (m_lock) {
			m_size = 0;
		}
	}

	/**
	 * Needs to be called by the implementation when cache entry is removed
	 */
	protected void cacheRemoved(Key key, Value value) {
		m_logger.debug("cacheRemoved init start");
		synchronized (m_lock) {
			if (m_keySizeEstimator != null) {
				m_size -= m_keySizeEstimator.estimateSize(key);
			}
			if (m_valueSizeEstimator != null) {
				m_size -= m_valueSizeEstimator.estimateSize(value);
			}
		}
		m_logger.debug("cacheRemoved init end");
	}

	/**
	 * Sets the key/value size estimators
	 */
	public AbstractCachedSource<Key, Value> withSizeEstimators(MemSizeEstimator<Key> keySizeEstimator,
			MemSizeEstimator<Value> valueSizeEstimator) {
		m_logger.debug("withSizeEstimators init start");
		m_keySizeEstimator = keySizeEstimator;
		m_valueSizeEstimator = valueSizeEstimator;
		m_logger.debug("withSizeEstimators init end");
		return this;
	}

	@Override
	public long estimateCacheSize() {
		m_logger.debug("estimateCacheSize start");
		return m_size;
	}
}
