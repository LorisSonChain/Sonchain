package sonchain.blockchain.datasource;

import java.util.Collection;

import org.apache.log4j.Logger;
import org.bouncycastle.util.encoders.Hex;

import com.googlecode.concurentlocks.ReadWriteUpdateLock;
import com.googlecode.concurentlocks.ReentrantReadWriteUpdateLock;

import sonchain.blockchain.util.CLock;
import sonchain.blockchain.datasource.base.CachedSource;
import sonchain.blockchain.datasource.base.Source;
import sonchain.blockchain.util.ByteArrayMap;

public class AccountStateCache {
	public static final Logger m_logger = Logger.getLogger(AccountStateCache.class);
	
    private Source<byte[], byte[]> m_source = null;
    private CachedSource.BytesKey<byte[]> m_cache = null;

    public AccountStateCache(Source<byte[], byte[]> source, CachedSource.BytesKey<byte[]> cache){
    	m_source = source;
    	m_cache = cache;
    }

    public Source<byte[], byte[]> getSource() {
    	m_logger.debug("getSource start");
        return m_source;
    }
    
    public CachedSource.BytesKey<byte[]> getCache()
    {
    	return m_cache;
    }

    public void delete(byte[] key) {
        m_logger.debug("ReadCache delete start key:" + Hex.toHexString(key));
    	m_cache.delete(key);
        //getSource().delete(key);
        m_logger.debug("ReadCache delete end key:" + Hex.toHexString(key));
    }

    public byte[] get(byte[] key) {
        m_logger.debug("ReadCache get start key:" + Hex.toHexString(key));
    	byte[] value = m_cache.get(key);
        if (value == null) {
        	value = getSource() == null ? null : getSource().get(key);
        	m_cache.put(key, value);
        }
        m_logger.debug("ReadCache get end result:" + Hex.toHexString(value));
        return value;
    }
    
    public boolean flush() {
        m_logger.debug("flush start.");
        boolean result =  m_cache.flush();
        m_logger.debug("flush end result:" + result);
        return result;
    }
    
    public Collection<byte[]> getModified() {
        m_logger.debug("getModified start.");
        return m_cache.getModified();
    }
    
    public void put(byte[] key, byte[] val) {
        m_logger.debug("ReadCache put start key:" + Hex.toHexString(key) 
        	+ " val:" + Hex.toHexString(val));
        if (val == null) {
            delete(key);
            m_logger.debug("ReadCache put delete key:" + Hex.toHexString(key) 
        		+ " val:" + Hex.toHexString(val));
        } else {
        	m_cache.put(key, val);
            //getSource().put(key, val);
        }
        m_logger.debug("ReadCache put end key:" + Hex.toHexString(key) 
        		+ " val:" + Hex.toHexString(val));
    }
}
