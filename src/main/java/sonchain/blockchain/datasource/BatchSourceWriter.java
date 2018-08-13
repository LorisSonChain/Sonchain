package sonchain.blockchain.datasource;

import java.util.*;

import sonchain.blockchain.datasource.base.AbstractSource;
import sonchain.blockchain.datasource.base.BatchSource;

public class BatchSourceWriter<Key, Value> extends AbstractSource<Key, Value, Key, Value> {

    private Map<Key, Value> m_buf = new HashMap<>();

    public BatchSourceWriter(BatchSource<Key, Value> src) {
        super(src);
    }

    private BatchSource<Key, Value> getBatchSource() {
        return (BatchSource<Key, Value>) getSource();
    }

    @Override
    public synchronized void delete(Key key) {
    	m_buf.put(key, null);
    }

    @Override
    public Value get(Key key) {
        return getSource().get(key);
    }

    @Override
    public synchronized boolean flushImpl() {
        if (!m_buf.isEmpty()) {
            getBatchSource().updateBatch(m_buf);
            m_buf.clear();
            return true;
        } else {
            return false;
        }
    }

    @Override
    public synchronized void put(Key key, Value val) {
    	m_buf.put(key, val);
    }
}
