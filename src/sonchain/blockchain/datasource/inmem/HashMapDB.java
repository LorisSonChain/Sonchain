package sonchain.blockchain.datasource.inmem;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import sonchain.blockchain.datasource.DbSource;
import sonchain.blockchain.util.CLock;
import sonchain.blockchain.util.ByteArrayMap;


public class HashMapDB<V> implements DbSource<V> {

    protected Map<byte[], V> m_storage = null;

    protected ReadWriteLock m_rwLock = new ReentrantReadWriteLock();
    protected CLock m_readLock = new CLock(m_rwLock.readLock());
    protected CLock m_writeLock = new CLock(m_rwLock.writeLock());

    public HashMapDB() {
        this(new ByteArrayMap<V>());
    }

    public HashMapDB(ByteArrayMap<V> storage) {
    	m_storage = storage;
    }

    @Override
    public void close() {}

    @Override
    public void delete(byte[] key) {
        try (CLock l = m_writeLock.lock()) {
        	m_storage.remove(key);
        }
    }

    @Override
    public boolean flush() {
        return true;
    }

    @Override
    public V get(byte[] key) {
        try (CLock l = m_readLock.lock()) {
            return m_storage.get(key);
        }
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
        try (CLock l = m_readLock.lock()) {
            return getStorage().keySet();
        }
    }

    @Override
    public void put(byte[] key, V val) {
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
    public void updateBatch(Map<byte[], V> rows) {
        try (CLock l = m_writeLock.lock()) {
            for (Map.Entry<byte[], V> entry : rows.entrySet()) {
                put(entry.getKey(), entry.getValue());
            }
        }
    }
}
