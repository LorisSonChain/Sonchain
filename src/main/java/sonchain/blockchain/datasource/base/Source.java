package sonchain.blockchain.datasource.base;

/**BaseSource
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
