package sonchain.blockchain.datasource.inmem;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import sonchain.blockchain.datasource.base.DbSource;
import sonchain.blockchain.util.CLock;
import sonchain.blockchain.util.StringMap;

public class HashMapDB<V> implements DbSource<V> {

    protected Map<String, V> m_storage = null;

    protected ReadWriteLock m_rwLock = new ReentrantReadWriteLock();
    protected CLock m_readLock = new CLock(m_rwLock.readLock());
    protected CLock m_writeLock = new CLock(m_rwLock.writeLock());

    public HashMapDB() {
        this(new StringMap<V>());
    }

    public HashMapDB(StringMap<V> storage) {
    	m_storage = storage;
    }

    @Override
    public void close() {}

    @Override
    public void delete(String key) {
        try (CLock l = m_writeLock.lock()) {
        	m_storage.remove(key);
        }
    }

    @Override
    public boolean flush() {
        return true;
    }

    @Override
    public V get(String key) {
        try (CLock l = m_readLock.lock()) {
            return m_storage.get(key);
        }
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
        try (CLock l = m_readLock.lock()) {
            return getStorage().keySet();
        }
    }

    @Override
    public void put(String key, V val) {
        if (val == null) {
            delete(key);
        } else {
            try (CLock l = m_writeLock.lock()) {
            	m_storage.put(key, val);
            }
        }
    }
    
    @Override
    public void setName(String name) {}

    @Override
    public void updateBatch(Map<String, V> rows) {
        try (CLock l = m_writeLock.lock()) {
            for (Map.Entry<String, V> entry : rows.entrySet()) {
                put(entry.getKey(), entry.getValue());
            }
        }
    }
}
