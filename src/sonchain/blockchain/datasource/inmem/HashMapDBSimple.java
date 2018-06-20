package sonchain.blockchain.datasource.inmem;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import sonchain.blockchain.datasource.DbSource;
import sonchain.blockchain.util.ByteArrayMap;

public class HashMapDBSimple<V> implements DbSource<V> {

    protected final Map<byte[], V> m_storage;

    public HashMapDBSimple() {
        this(new ByteArrayMap<V>());
    }

    public HashMapDBSimple(ByteArrayMap<V> storage) {
        this.m_storage = storage;
    }

    @Override
    public void close() {}

    @Override
    public void delete(byte[] key) {
    	m_storage.remove(key);
    }

    @Override
    public boolean flush() {
        return true;
    }

    @Override
    public V get(byte[] key) {
        return m_storage.get(key);
    }

    @Override
    public String getName() {
        return "in-memory";
    }

    public Map<byte[], V> getStorage() {
        return m_storage;
    }

    @Override
    public void init() {}

    @Override
    public boolean isAlive() {
        return true;
    }

    @Override
    public Set<byte[]> keys() {
        return getStorage().keySet();
    }

    @Override
    public void put(byte[] key, V val) {
        if (val == null) {
            delete(key);
        } else {
        	m_storage.put(key, val);
        }
    }

    @Override
    public void setName(String name) {}

    @Override
    public void updateBatch(Map<byte[], V> rows) {
        for (Map.Entry<byte[], V> entry : rows.entrySet()) {
            put(entry.getKey(), entry.getValue());
        }
    }
}
