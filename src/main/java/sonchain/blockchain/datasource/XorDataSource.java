package sonchain.blockchain.datasource;

import org.apache.log4j.Logger;
import org.bouncycastle.util.encoders.Hex;

import sonchain.blockchain.datasource.base.AbstractSource;
import sonchain.blockchain.datasource.base.Source;
import sonchain.blockchain.util.ByteUtil;

public class XorDataSource<V> extends AbstractSource<String, V, String, V> {
    private String m_subKey;

	public static final Logger m_logger = Logger.getLogger(XorDataSource.class);
    /**
     * Creates instance with a value all keys are XORed with
     */
    public XorDataSource(Source<String, V> source, String subKey, String name) {
        super(source);
        setFlushSource(true);
        m_name = name;
        m_subKey = subKey;
        m_logger.debug("XorDataSource init end name:" + m_name);
    }

    private String convertKey(String key) {
        m_logger.debug("XorDataSource convertKey start name:" + m_name);
        key = m_subKey + key;
        return key;
        //return ByteUtil.xorAlignRight(key, m_subKey);
    }

    @Override
    public void delete(String key) {
        m_logger.debug("XorDataSource delete start name:" + m_name + " key:" + key.toString());
        getSource().delete(convertKey(key));
        m_logger.debug("XorDataSource delete end name:" + m_name + " key:" + key.toString());
    }

    @Override
    protected boolean flushImpl() {
        m_logger.debug("XorDataSource flushImpl start name:" + m_name);
        return false;
    }

    @Override
    public V get(String key) {
        m_logger.debug("XorDataSource get start name:" + m_name + " key:" + key.toString());
        V v = getSource().get(convertKey(key));
        if(v instanceof byte[]){
            m_logger.debug("XorDataSource get start name:" + m_name + " value:" + Hex.toHexString((byte[])v));
        }
        return v;
    }

    @Override
    public void put(String key, V value) {
        m_logger.debug("XorDataSource put start name:" + m_name + " key:" + key.toString());
        if(value instanceof byte[]){
            m_logger.debug("XorDataSource put start name:" + m_name + " value:" + Hex.toHexString((byte[])value));
        }
        getSource().put(convertKey(key), value);
    }
}
