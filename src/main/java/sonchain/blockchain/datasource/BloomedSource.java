package sonchain.blockchain.datasource;

import sonchain.blockchain.crypto.HashUtil;

public class BloomedSource extends AbstractChainedSource<byte[], byte[], byte[], byte[]> {

    private boolean m_dirty = false;
    private int m_falseMisses = 0;
    private byte[] m_filterKey = HashUtil.sha3("filterKey".getBytes());
    private QuotientFilter m_filter;
    private int m_hits = 0;
    private int m_maxBloomSize = 0;
    private int m_misses = 0;

    public BloomedSource(Source<byte[], byte[]> source, int maxBloomSize) {
        super(source);
        m_maxBloomSize = maxBloomSize;
        byte[] filterBytes = source.get(m_filterKey);
        if (filterBytes != null) {
            if (filterBytes.length > 0) {
            	m_filter = QuotientFilter.deserialize(filterBytes);
            } else {
                // filter size exceeded limit and is disabled forever
            	m_filter = null;
            }
        } else {
            if (maxBloomSize > 0) {
            	m_filter = QuotientFilter.create(50_000_000, 100_000);
            } else {
                // we can't re-enable filter later
                getSource().put(m_filterKey, new byte[0]);
            }
        }
//
//        new Thread() {
//            @Override
//            public void run() {
//                while(true) {
//                    synchronized (BloomedSource.this) {
//                        logger.debug("BloomedSource: hits: " + hits + ", misses: " + misses + ", false: " + falseMisses);
//                        hits = misses = falseMisses = 0;
//                    }
//
//                    try {
//                        Thread.sleep(5000);
//                    } catch (InterruptedException e) {}
//                }
//            }
//        }.start();
    }

    @Override
    public void delete(byte[] key) {
        if (m_filter != null) {
        	m_filter.remove(key);
        }
        getSource().delete(key);
    }
    
    @Override
    public byte[] get(byte[] key) {
        if (m_filter == null) 
        {
        	return getSource().get(key);
        }
        if (!m_filter.maybeContains(key)) {
        	m_hits++;
            return null;
        } else {
            byte[] ret = getSource().get(key);
            if (ret == null) {
            	m_falseMisses++;
            }
            else {
            	m_misses++;
            }
            return ret;
        }
    }

    @Override
    protected boolean flushImpl() {
        if (m_filter != null && m_dirty) {
            getSource().put(m_filterKey, m_filter.serialize());
            m_dirty = false;
            return true;
        } else {
            return false;
        }
    }
    
    @Override
    public void put(byte[] key, byte[] val) {
        if (m_filter != null) {
        	m_filter.insert(key);
        	m_dirty = true;
            if (m_filter.getAllocatedBytes() > m_maxBloomSize) {
                m_logger.debug("Bloom filter became too large (" 
                		+ m_filter.getAllocatedBytes() + " exceeds max threshold " 
                		+ m_maxBloomSize + ") and is now disabled forever.");
                getSource().put(m_filterKey, new byte[0]);
                m_filter = null;
                m_dirty = false;
            }
        }
        getSource().put(key, val);
    }
    
    public void startBlooming(QuotientFilter filter) {
    	m_filter = filter;
    }

    public void stopBlooming() {
    	m_filter = null;
    }
}
