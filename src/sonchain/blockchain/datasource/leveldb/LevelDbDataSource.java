package sonchain.blockchain.datasource.leveldb;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.apache.log4j.Logger;

import org.bouncycastle.util.encoders.Hex;
import org.iq80.leveldb.*;

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import sonchain.blockchain.core.FileUtil;
import sonchain.blockchain.datasource.DbSource;
import sonchain.blockchain.service.DataCenter;

import static org.fusesource.leveldbjni.JniDBFactory.factory;

/**
 * Level数据库
 * @author GAIA
 *
 */
public class LevelDbDataSource implements DbSource<byte[]> {

    private boolean m_alive = false;
    private DB m_db = null;
	public static final Logger m_logger = Logger.getLogger(LevelDbDataSource.class);
    private String m_name = "";

    // The native LevelDB insert/update/delete are normally thread-safe
    // However close operation is not thread-safe and may lead to a native crash when
    // accessing a closed DB.
    // The leveldbJNI lib has a protection over accessing closed DB but it is not synchronized
    // This ReadWriteLock still permits concurrent execution of insert/delete/update operations
    // however blocks them on init/close/delete operations
    private ReadWriteLock m_resetDbLock = new ReentrantReadWriteLock();

    /**
     * 初始化
     */
    public LevelDbDataSource() {
    }

    /**
     * 初始化
     * @param name
     */
    public LevelDbDataSource(String name) {
        this.m_name = name;
        m_logger.debug("New LevelDbDataSource: " + name);
    }

    /**
     * 获取数据库名称
     */
    @Override
    public String getName() {
    	m_logger.debug("getName name:" + m_name);
        return m_name;
    }

    /**
     * 设置数据库名称
     */
    @Override
    public void setName(String name) {
    	m_logger.debug("SetName name:" + m_name);
        this.m_name = name;
    }

    /**
     * 关闭
     */
    @Override
    public void close() {
    	m_logger.debug("close start name :" + m_name);
    	m_resetDbLock.writeLock().lock();
        try {
            if (!isAlive()) {
            	return;
            }
            try {
                m_logger.debug(String.format("Close db: {%s}", m_name));
            	m_db.close();

                m_alive = false;
            } catch (IOException e) {
                m_logger.error(String.format("Failed to find the db file on the close: {%s} ", m_name));
            }
        } finally {
        	m_resetDbLock.writeLock().unlock();
        	m_logger.debug("close end name:" + m_name);
        }
    }

    /**
     * 删除关键字
     */
    @Override
    public void delete(byte[] key) {
    	m_logger.debug("close delete start name :" + m_name + "delete key:" + Hex.toHexString(key));
    	m_resetDbLock.readLock().lock();
        try {
            if (m_logger.isTraceEnabled()) {
            	m_logger.trace("~> LevelDbDataSource.delete(): " + m_name + ", key: " + Hex.toHexString(key));
            }
        	m_db.delete(key);
            if (m_logger.isTraceEnabled()){
            	m_logger.trace("<~ LevelDbDataSource.delete(): " + m_name + ", key: " + Hex.toHexString(key));
            }
        } finally {
        	m_resetDbLock.readLock().unlock();
        	m_logger.debug("close delete end name :" + m_name + "delete key:" + Hex.toHexString(key));
        }
    }

    /**
     * 销毁数据库
     * @param fileLocation
     */
    public void destroyDB(File fileLocation) {
    	m_logger.debug("destroyDB start name :" + m_name);
    	m_resetDbLock.writeLock().lock();
        try {
        	m_logger.debug("Destroying existing database: " + fileLocation);
            Options options = new Options();
            try {
                factory.destroy(fileLocation, options);
            } catch (IOException e) {
            	m_logger.error(e.getMessage(), e);
            }
        } finally {
        	m_resetDbLock.writeLock().unlock();
        	m_logger.debug("destroyDB end name :" + m_name);
        }
    }

    /**
     * 
     */
    @Override
    public boolean flush() {
    	m_logger.debug("flush start name :" + m_name);
        return false;
    }

    /**
     * 根据Key获取Value
     */
    @Override
    public byte[] get(byte[] key) {
    	m_logger.debug("get start name :" + m_name + " get key:" + Hex.toHexString(key));
    	m_resetDbLock.readLock().lock();
        try {
            if (m_logger.isTraceEnabled()){
            	m_logger.trace("~> LevelDbDataSource.get(): " + m_name + ", key: " + Hex.toHexString(key));
            }
            try {
                byte[] ret = m_db.get(key);
                if (m_logger.isTraceEnabled()){
                	m_logger.trace("<~ LevelDbDataSource.get(): " + m_name + ", key: " 
                			+ Hex.toHexString(key) + ", " + (ret == null ? "null" : ret.length));
                }
                return ret;
            } catch (DBException e) {
            	m_logger.warn("Exception. Retrying again...", e);
                byte[] ret = m_db.get(key);
                if (m_logger.isTraceEnabled()) {
                	m_logger.trace("<~ LevelDbDataSource.get(): " + m_name + ", key: " 
                			+ Hex.toHexString(key) + ", " + (ret == null ? "null" : ret.length));
                }
                return ret;
            }
        } finally {
        	m_resetDbLock.readLock().unlock();
        	m_logger.debug("get end name :" + m_name + "get key:" + Hex.toHexString(key));
        }
    }

    /**
     * 获取路径
     * @return
     */
    private Path getPath() {
        return Paths.get(DataCenter.m_config.m_datebaseDir, m_name);
    }

    /**
     * 初始化
     */
    @Override
    public void init() {
    	m_logger.debug("init start name :" + m_name);
    	m_resetDbLock.writeLock().lock();
        try {
        	m_logger.debug("~> LevelDbDataSource.init(): " + m_name);
            if (isAlive()) {
            	m_logger.debug("~> LevelDbDataSource.init() is alive return: " + m_name);
            	return;
            }
            if (m_name == null) {
            	throw new NullPointerException("no name set to the db");
            }
            Options options = new Options();
            options.createIfMissing(true);
            options.compressionType(CompressionType.NONE);
            options.blockSize(10 * 1024 * 1024);
            options.writeBufferSize(10 * 1024 * 1024);
            options.cacheSize(0);
            options.paranoidChecks(true);
            options.verifyChecksums(true);
            options.maxOpenFiles(32);
            try {
            	m_logger.debug("Opening database");
                final Path dbPath = getPath();
                if (!Files.isSymbolicLink(dbPath.getParent())) {
                	Files.createDirectories(dbPath.getParent());
                }
                m_logger.debug(String.format("Initializing new or existing database: '{%s}'", m_name));
                try {
                	m_db = factory.open(dbPath.toFile(), options);
                } catch (IOException e) {
                    // database could be corrupted
                    // exception in std out may look:
                    // org.fusesource.leveldbjni.internal.NativeDB$DBException: Corruption: 16 missing files; e.g.: /Users/stan/ethereumj/database-test/block/000026.ldb
                    // org.fusesource.leveldbjni.internal.NativeDB$DBException: Corruption: checksum mismatch
                    if (e.getMessage().contains("Corruption:")) {
                    	m_logger.warn("Problem initializing database.", e);
                    	m_logger.info("LevelDB database must be corrupted. Trying to repair. Could take some time.");
                        factory.repair(dbPath.toFile(), options);
                        m_logger.info("Repair finished. Opening database again.");
                        m_db = factory.open(dbPath.toFile(), options);
                    } else {
                        // must be db lock
                        // org.fusesource.leveldbjni.internal.NativeDB$DBException: IO error: lock /Users/stan/ethereumj/database-test/state/LOCK: Resource temporarily unavailable
                        throw e;
                    }
                }
                m_alive = true;
            } catch (IOException ioe) {
            	m_logger.error(ioe.getMessage(), ioe);
                throw new RuntimeException("Can't initialize database", ioe);
            }
            m_logger.debug("<~ LevelDbDataSource.init(): " + m_name);
        } finally {
        	m_resetDbLock.writeLock().unlock();
        	m_logger.debug("init end name :" + m_name);
        }
    }

    /**
     * 是否存活
     */
    @Override
    public boolean isAlive() {
        return m_alive;
    }

    /**
     * 获取所有的键值
     */
    @Override
    public Set<byte[]> keys() {
    	m_logger.debug("keys start name :" + m_name);
    	m_resetDbLock.readLock().lock();
        try {
            if (m_logger.isTraceEnabled()) {
            	m_logger.trace("~> LevelDbDataSource.keys(): " + m_name);
            }
            try (DBIterator iterator = m_db.iterator()) {
                Set<byte[]> result = new HashSet<>();
                for (iterator.seekToFirst(); iterator.hasNext(); iterator.next()) {
                    result.add(iterator.peekNext().getKey());
                }
                if (m_logger.isTraceEnabled()) {
                	m_logger.trace("<~ LevelDbDataSource.keys(): " + m_name + ", " + result.size());
                }
                return result;
            } catch (IOException e) {
            	m_logger.error("Unexpected", e);
                throw new RuntimeException(e);
            }
        } finally {
        	m_resetDbLock.readLock().unlock();
        	m_logger.debug("keys end name :" + m_name);
        }
    }

    /**
     * 
     */
    @Override
    public void put(byte[] key, byte[] value) {
    	m_logger.debug("put start name :" + m_name + "key:" + Hex.toHexString(key) + "value" + Hex.toHexString(value));
    	m_resetDbLock.readLock().lock();
        try {
            if (m_logger.isTraceEnabled()) {
            	m_logger.trace("~> LevelDbDataSource.put(): " + m_name + ", key: " + Hex.toHexString(key) 
            			+ ", " + (value == null ? "null" : value.length));
            }
        	m_db.put(key, value);
            if (m_logger.isTraceEnabled()) {
            	m_logger.trace("<~ LevelDbDataSource.put(): " + m_name + ", key: " + Hex.toHexString(key) 
            			+ ", " + (value == null ? "null" : value.length));
            }
        } finally {
        	m_resetDbLock.readLock().unlock();
        	m_logger.debug("put end name :" + m_name + "key:" + Hex.toHexString(key) + "value" + Hex.toHexString(value));
        }
    }
    
    /**
     * 重置
     */
    public void reset() {
    	m_logger.debug("reset start name :" + m_name);
        close();
        FileUtil.RecursiveDelete(getPath().toString());
        init();
    	m_logger.debug("reset end name :" + m_name);
    }
    
    /**
     * 批量更新
     * @param rows
     * @throws IOException
     */
    private void updateBatchInternal(Map<byte[], byte[]> rows) throws IOException {
    	m_logger.debug("updateBatchInternal start name :" + m_name);
        try (WriteBatch batch = m_db.createWriteBatch()) {
            for (Map.Entry<byte[], byte[]> entry : rows.entrySet()) {
                if (entry.getValue() == null) {
                	m_logger.debug("updateBatchInternal delete key :" + Hex.toHexString(entry.getKey()));
                    batch.delete(entry.getKey());
                } else {
                	m_logger.debug("updateBatchInternal put key :" + Hex.toHexString(entry.getKey())
                	+ "value:" + Hex.toHexString(entry.getValue()));
                    batch.put(entry.getKey(), entry.getValue());
                }
            }
            m_db.write(batch);
        }
    	m_logger.debug("updateBatchInternal end name :" + m_name);
    }

    /**
     * 批量更新
     */
    @Override
    public void updateBatch(Map<byte[], byte[]> rows) {
    	m_logger.debug("updateBatch start name :" + m_name);
    	m_resetDbLock.readLock().lock();
        try {
            if (m_logger.isTraceEnabled()) {
            	m_logger.trace("~> LevelDbDataSource.updateBatch(): " + m_name + ", " + rows.size());
            }
            try {
                updateBatchInternal(rows);
                if (m_logger.isTraceEnabled()) {
                	m_logger.trace("<~ LevelDbDataSource.updateBatch(): " + m_name + ", " + rows.size());
                }
            } catch (Exception e) {
            	m_logger.error("Error, retrying one more time...", e);
                // try one more time
                try {
                    updateBatchInternal(rows);
                    if (m_logger.isTraceEnabled()) {
                    	m_logger.trace("<~ LevelDbDataSource.updateBatch(): " + m_name + ", " + rows.size());
                    }
                } catch (Exception e1) {
                	m_logger.error("Error", e);
                    throw new RuntimeException(e);
                }
            }
        } finally {
        	m_resetDbLock.readLock().unlock();
        	m_logger.debug("updateBatch end name :" + m_name);
        }
    }
}