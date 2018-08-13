package sonchain.blockchain.db;

import org.apache.log4j.Logger;

import sonchain.blockchain.datasource.WriteCache;
import sonchain.blockchain.datasource.base.AbstractCachedSource;
import sonchain.blockchain.datasource.base.AbstractSource;
import sonchain.blockchain.datasource.base.Source;

public class StateSource extends AbstractSource<byte[], byte[], byte[], byte[]> {

	public static final Logger m_logger = Logger.getLogger(StateSource.class);
	// for debug purposes
	public static StateSource INST = null;

    //private BloomedSource m_bloomedSource = null;
    //private CountingBytesSource m_countingSource = null;
    //private NoDeleteSource<byte[], byte[]> m_noDeleteSource = null;
    //private ReadCache<byte[], byte[]> m_readCache = null;
    private WriteCache<byte[], byte[]> m_writeCache = null;

	public StateSource(Source<byte[], byte[]> src, boolean pruningEnabled) {
		this(src, pruningEnabled, 0);
		m_logger.debug("StateSource init start.");
	}

	public StateSource(Source<byte[], byte[]> src, boolean pruningEnabled, int maxBloomSize) {
		super(src);
		INST = this;
		//add(m_bloomedSource = new BloomedSource(src, maxBloomSize));
		//m_bloomedSource.setFlushSource(false);
		//add(m_readCache = new ReadCache.BytesKey<>(src).withMaxCapacity(16 * 1024 * 1024 / 512)); 
		// 512-approx size of a node
		//m_readCache.setFlushSource(true);
        //add(m_countingSource = new CountingBytesSource(m_readCache, true));
        //m_countingSource.setFlushSource(true);    
//        m_writeCache = new AsyncWriteCache<byte[], byte[]>(m_countingSource) {
//			@Override
//			protected WriteCache<byte[], byte[]> CreateCache(Source<byte[], byte[]> source) {
//				m_logger.debug("CreateCache init start.");
//				WriteCache.BytesKey<byte[]> ret = new WriteCache.BytesKey<byte[]>(source,
//						WriteCache.CacheType.COUNTING);
//				ret.withSizeEstimators(MemSizeEstimator.ByteArrayEstimator, MemSizeEstimator.ByteArrayEstimator);
//				ret.setFlushSource(true);
//				m_logger.debug("CreateCache init end.");
//				return ret;
//			}
//		}.withName("state");
        m_writeCache = new WriteCache.BytesKey<byte[]>(src,
				WriteCache.CacheType.COUNTING);    
        m_writeCache.setFlushSource(true);
        m_writeCache.withCacheName("state");
		//add(m_writeCache);
		//m_noDeleteSource = new NoDeleteSource<>(m_writeCache);
        //add(m_noDeleteSource);
		m_logger.debug("StateSource init end.");
	}

//	public void setConfig() {
//		int size = DataCenter.m_config.m_cacheStateCacheSize;
//		m_readCache.withMaxCapacity(size * 1024 * 1024 / 512); // 512 - approx size of a node
//		m_logger.debug("setConfig end.");
//	}

//	public BloomedSource getBloomedSource() {
//		m_logger.debug("getBloomedSource start.");
//		return m_bloomedSource;
//	}

//	/**
//	 * Returns the source behind JournalSource
//	 */
//	public Source<byte[], byte[]> getNoJournalSource() {
//		m_logger.debug("getNoJournalSource start.");
//		return m_writeCache;
//	}

	public AbstractCachedSource<byte[], byte[]> getWriteCache() {
		m_logger.debug("getWriteCache start.");
		return m_writeCache;
	}

//	public ReadCache<byte[], byte[]> getReadCache() {
//		m_logger.debug("getReadCache start.");
//		return m_readCache;
//	}

	@Override
	public void delete(byte[] key) {
		m_writeCache.delete(key);
		
	}

	//@Override
	//public boolean flush() {
	//	return false;
	//}

	@Override
	public byte[] get(byte[] key) {
		return m_writeCache.get(key);
	}

	@Override
	public void put(byte[] key, byte[] val) {
		m_writeCache.put(key, val);	
	}

	@Override
	protected boolean flushImpl() {
		return m_writeCache.flush();
	}
}
