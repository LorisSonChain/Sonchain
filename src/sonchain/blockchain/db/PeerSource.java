package sonchain.blockchain.db;

import java.math.BigInteger;

import org.apache.commons.lang3.tuple.Pair;

import sonchain.blockchain.consensus.SonChainPeerNode;
import sonchain.blockchain.datasource.DataSourceArray;
import sonchain.blockchain.datasource.ObjectDataSource;
import sonchain.blockchain.datasource.Serializer;
import sonchain.blockchain.datasource.Source;
import sonchain.blockchain.datasource.leveldb.LevelDbDataSource;
import sonchain.blockchain.util.RLP;
import sonchain.blockchain.util.RLPList;

public class PeerSource {
    // for debug purposes
    public static PeerSource m_instance = null;
    private DataSourceArray<Pair<SonChainPeerNode, Integer>> m_nodes = null;
    private Source<byte[], byte[]> m_source = null;

    public static final Serializer<Pair<SonChainPeerNode, Integer>, byte[]> NODE_SERIALIZER 
    		= new Serializer<Pair<SonChainPeerNode, Integer>, byte[]>(){
        @Override
        public byte[] serialize(Pair<SonChainPeerNode, Integer> value) {
            return value.getLeft().toBytes();
        }
        @Override
        public Pair<SonChainPeerNode, Integer> deserialize(byte[] bytes) {
            if (bytes == null) {
            	return null;
            }
            SonChainPeerNode node = SonChainPeerNode.fromBytes(bytes);
            return Pair.of(node, 1);
        }
    };

    public PeerSource(Source<byte[], byte[]> src) {
    	m_source = src;
        m_instance = this;
        m_nodes = new DataSourceArray<>(
                new ObjectDataSource<>(src, NODE_SERIALIZER, 512));
    }
    
    public void clear() {
        if (m_source instanceof LevelDbDataSource) {
            ((LevelDbDataSource) m_source).reset();
            m_nodes = new DataSourceArray<>(
                    new ObjectDataSource<>(m_source, NODE_SERIALIZER, 512));
        } else {
            throw new RuntimeException("Not supported");
        }
    }
    
    public DataSourceArray<Pair<SonChainPeerNode, Integer>> getNodes() {
        return m_nodes;
    }
}
