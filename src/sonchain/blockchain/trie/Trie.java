package sonchain.blockchain.trie;

import sonchain.blockchain.datasource.Source;

/**
 * Trie
 * @author GAIA
 *
 * @param <V>
 */
public interface Trie<V> extends Source<byte[], V> {	  
    void clear();
    byte[] getRootHash();
    void setRoot(byte[] root);  
}