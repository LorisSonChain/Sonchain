package sonchain.blockchain.datasource.base;

import org.apache.log4j.Logger;

import sonchain.blockchain.core.genesis.GenesisLoader;
import sonchain.blockchain.datasource.base.AbstractCachedSource.Entry;

public abstract class AbstractSource<Key, Value, SourceKey, SourceValue> 
			implements Source<Key, Value> {

	public static final Logger m_logger = Logger.getLogger(AbstractSource.class);
    protected boolean m_flushSource = false;
    private Source<SourceKey, SourceValue> m_source = null;
    protected String m_name = "";

    /**
     * Intended for subclasses which wishes to initialize the source
     * later via {@link #setSource(Source)} method
     */
    protected AbstractSource() {
    }

    public AbstractSource(Source<SourceKey, SourceValue> source) {
    	m_logger.debug("init start");
    	m_source = source;
    	m_logger.debug("init end");
    }

    public Source<SourceKey, SourceValue> getSource() {
    	m_logger.debug("getSource start");
        return m_source;
    }

    /**
     * Intended for subclasses which wishes to initialize the source later
     */
    protected void setSource(Source<SourceKey, SourceValue> src) {
    	m_logger.debug("setSource start");
    	m_source = src;
    }

    public void setFlushSource(boolean flushSource) {
    	m_logger.debug("setFlushSource start");
    	m_flushSource = flushSource;
    	m_logger.debug("setFlushSource end");
    }

    /**
     * Invokes {@link #flushImpl()} and does backing Source flush if required
     * @return true if this or source flush did any changes
     */
    @Override
    public synchronized boolean flush() {
    	m_logger.debug("flush start name:" + m_name);
        boolean ret = flushImpl();
        if (m_flushSource) {
            ret |= getSource().flush();
        }
    	m_logger.debug("flush end result:" + ret);
        return ret;
    }

    /**
     * Should be overridden to do actual source flush
     */
    protected abstract boolean flushImpl();
}
