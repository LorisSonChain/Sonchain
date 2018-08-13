package sonchain.blockchain.db;

import java.math.BigInteger;

import org.apache.commons.lang3.tuple.Pair;

import sonchain.blockchain.consensus.SonChainProducerNode;
import sonchain.blockchain.datasource.DataSourceArray;
import sonchain.blockchain.datasource.ObjectDataSource;
import sonchain.blockchain.datasource.base.Serializer;
import sonchain.blockchain.datasource.base.Source;
import sonchain.blockchain.datasource.leveldb.LevelDbDataSource;
import sonchain.blockchain.util.RLP;
import sonchain.blockchain.util.RLPList;

public class PeerSource {
    // for debug purposes
    public static PeerSource m_instance = null;
    private DataSourceArray<Pair<SonChainProducerNode, Integer>> m_nodes = null;
    private Source<String, String> m_source = null;

    public static final Serializer<Pair<SonChainProducerNode, Integer>, String> NODE_SERIALIZER 
    		= new Serializer<Pair<SonChainProducerNode, Integer>, String>(){
        @Override
        public String serialize(Pair<SonChainProducerNode, Integer> value) {
            return value.getLeft().toString();
        }
        @Override
        public Pair<SonChainProducerNode, Integer> deserialize(String bytes) {
            if (bytes == null) {
            	return null;
            }
            SonChainProducerNode node = SonChainProducerNode.fromBytes(bytes.getBytes());
            return Pair.of(node, 1);
        }
    };

    public PeerSource(Source<String, String> src) {
    	m_source = src;
        m_instance = this;
        //TODO
        //m_nodes = new DataSourceArray<>(
        //        new ObjectDataSource<>(src, NODE_SERIALIZER, 512));
    }
    
    public void clear() {
        if (m_source instanceof LevelDbDataSource) {
            ((LevelDbDataSource) m_source).reset();
            //TODO
            //m_nodes = new DataSourceArray<>(
            //        new ObjectDataSource<>(m_source, NODE_SERIALIZER, 512));
        } else {
            throw new RuntimeException("Not supported");
        }
    }
    
    public DataSourceArray<Pair<SonChainProducerNode, Integer>> getNodes() {
        return m_nodes;
    }
}
