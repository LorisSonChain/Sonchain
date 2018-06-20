package sonchain.blockchain.datasource;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.bouncycastle.util.encoders.Hex;

/**
 * Represents a chain of Sources as a single Source
 * All calls to this Source are delegated to the last Source in the chain
 * On flush all Sources in chain are flushed in reverse order
 * @author GAIA
 * @param <Key>
 * @param <Value>
 * @param <SourceKey>
 * @param <SourceValue>
 */
public class SourceChainBox<Key, Value, SourceKey, SourceValue>
		extends AbstractChainedSource<Key, Value, SourceKey, SourceValue> {

	public static final Logger m_logger = Logger.getLogger(SourceChainBox.class);
	private List<Source> m_chain = new ArrayList<>();
	private Source<Key, Value> m_lastSource = null;

	public SourceChainBox(Source<SourceKey, SourceValue> source) {
		super(source);
		m_logger.debug("init end.");
	}

	/**
	 * Adds next Source in the chain to the collection Sources should be added
	 * from most bottom (connected to the backing Source) All calls to the
	 * SourceChainBox will be delegated to the last added Source
	 */
	public void add(Source src) {
		m_logger.debug("add start.");
		m_chain.add(src);
		m_lastSource = src;
		m_logger.debug("add end.");
	}

	@Override
	public void put(Key key, Value val) {
		if(val instanceof byte[]){
			m_logger.debug("put start."
        		+ " key:" + Hex.toHexString((byte[])key) + " val:" + Hex.toHexString((byte[])val));
		}
		else
		{
			m_logger.debug("put start."
	        		+ " key:" + Hex.toHexString((byte[])key) + " val:" + val);			
		}
		m_lastSource.put(key, val);
		m_logger.debug("put end.");
	}

	@Override
	public Value get(Key key) {
		m_logger.debug("get start."
        		+ " key:" + Hex.toHexString((byte[])key));
		return m_lastSource.get(key);
	}

	@Override
	public void delete(Key key) {
		m_logger.debug("delete start."
        		+ " key:" + Hex.toHexString((byte[])key));
		m_lastSource.delete(key);
	}

	// @Override
	// public boolean flush() {
	//// boolean ret = false;
	//// for (int i = chain.size() - 1; i >= 0 ; i--) {
	//// ret |= chain.get(i).flush();
	//// }
	// return lastSource.flush();
	// }

	@Override
	protected boolean flushImpl() {
		m_logger.debug("flushImpl start.");
		return m_lastSource.flush();
	}
}
