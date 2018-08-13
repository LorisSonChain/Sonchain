package sonchain.blockchain.datasource;

import java.util.AbstractList;

import org.bouncycastle.util.encoders.Hex;

import sonchain.blockchain.util.ByteUtil;

public class DataSourceArray<V> extends AbstractList<V> {
    private int m_size = -1;
    private static final String SIZE_KEY = "FFFFFFFFFFFFFFFF";
    private ObjectDataSource<V> m_src = null;

    public DataSourceArray(ObjectDataSource<V> src) {
        this.m_src = src;
    }

    @Override
    public synchronized void add(int index, V element) {
        set(index, element);
    }

    public synchronized boolean flush() {
        return m_src.flush();
    }

    @Override
    public synchronized V get(int idx) {
        if (idx < 0 || idx >= size()){
        	throw new IndexOutOfBoundsException(idx + " > " + m_size);
        }
        return m_src.get(String.valueOf(idx));
    }

    
    @Override
    public synchronized V remove(int index) {
        throw new RuntimeException("Not supported yet.");
    }

    @Override
    public synchronized V set(int idx, V value) {
        if (idx >= size()) {
            setSize(idx + 1);
        }
        m_src.put(String.valueOf(idx), value);
        return value;
    }

    private synchronized void setSize(int newSize) {
    	m_size = newSize;
        m_src.getSource().put(SIZE_KEY, String.valueOf(newSize));
    }
    
    @Override
    public synchronized int size() {
        if (m_size < 0) {
            String sizeBB = m_src.getSource().get(SIZE_KEY);
            m_size = sizeBB == null ? 0 : Integer.valueOf(sizeBB);
        }
        return m_size;
    }
}
