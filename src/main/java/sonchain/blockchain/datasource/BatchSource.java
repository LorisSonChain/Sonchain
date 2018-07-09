package sonchain.blockchain.datasource;

import java.util.Map;

public interface BatchSource<K, V> extends Source<K, V> {

    void updateBatch(Map<K, V> rows);
}
