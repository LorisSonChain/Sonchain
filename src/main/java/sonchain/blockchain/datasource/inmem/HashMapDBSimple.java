package sonchain.blockchain.datasource.inmem;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import sonchain.blockchain.datasource.base.DbSource;
import sonchain.blockchain.util.ByteArrayMap;
import sonchain.blockchain.util.StringMap;

public class HashMapDBSimple<V> implements DbSource<V> {

    protected final Map<String, V> m_storage;

    public HashMapDBSimple() {
        this(new StringMap<V>());
    }

    public HashMapDBSimple(StringMap<V> storage) {
        this.m_storage = storage;
    }

    @Override
    public void close() {}

    @Override
    public void delete(String key) {
    	m_storage.remove(key);
    }

    @Override
    public boolean flush() {
        return true;
    }

    @Override
    public V get(String key) {
        return m_storage.get(key);
    }

    @Override
    public String getName() {
        return "in-memory";
    }

    public Map<String, V> getStorage() {
        return m_storage;
    }

    @Override
    public void init() {}

    @Override
    public boolean isAlive() {
        return true;
    }

    @Override
    public Set<String> keys() {
        return getStorage().keySet();
    }

    @Override
    public void put(String key, V val) {
        if (val == null) {
            delete(key);
        } else {
        	m_storage.put(key, val);
        }
    }

    @Override
    public void setName(String name) {}

    @Override
    public void updateBatch(Map<String, V> rows) {
        for (Map.Entry<String, V> entry : rows.entrySet()) {
            put(entry.getKey(), entry.getValue());
        }
    }
}
