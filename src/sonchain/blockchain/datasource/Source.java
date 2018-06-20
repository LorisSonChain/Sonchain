package sonchain.blockchain.datasource;

/**
 * 所有数据源基类
 * @author GAIA
 *
 * @param <K>
 * @param <V>
 */
public interface Source<K, V> {
    void delete(K key);  
    boolean flush();
    V get(K key); 
    void put(K key, V val); 
}
