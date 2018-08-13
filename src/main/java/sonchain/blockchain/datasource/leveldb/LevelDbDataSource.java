package sonchain.blockchain.datasource.leveldb;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.apache.log4j.Logger;
import org.iq80.leveldb.*;

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import sonchain.blockchain.core.FileUtil;
import sonchain.blockchain.datasource.base.DbSource;
import sonchain.blockchain.service.DataCenter;

import static org.fusesource.leveldbjni.JniDBFactory.factory;

/**
 *
 */
public class LevelDbDataSource implements DbSource<String> {

    private boolean m_alive = false;
    private DB m_db = null;
	public static final Logger m_logger = Logger.getLogger(LevelDbDataSource.class);
    private String m_name = "";
    private Charset m_charset = Charset.forName("utf-8");

    // The native LevelDB insert/update/delete are normally thread-safe
    // However close operation is not thread-safe and may lead to a native crash when
    // accessing a closed DB.
    // The leveldbJNI lib has a protection over accessing closed DB but it is not synchronized
    // This ReadWriteLock still permits concurrent execution of insert/delete/update operations
    // however blocks them on init/close/delete operations
    private ReadWriteLock m_resetDbLock = new ReentrantReadWriteLock();

    /**
     * Init
     */
    public LevelDbDataSource() {
    }

    /**
     * Init
     * @param name
     */
    public LevelDbDataSource(String name) {
        this.m_name = name;
        m_logger.debug("New LevelDbDataSource: " + name);
    }

    /**
     * getName
     */
    @Override
    public String getName() {
    	m_logger.debug("getName name:" + m_name);
        return m_name;
    }

    /**
     * setName
     */
    @Override
    public void setName(String name) {
    	m_logger.debug("SetName name:" + m_name);
        this.m_name = name;
    }

    /**
     * close
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
     * delete
     */
    @Override
    public void delete(String key) {
    	m_logger.debug("close delete start name :" + m_name + "delete key:" + key);
    	m_resetDbLock.readLock().lock();
        try {
            if (m_logger.isTraceEnabled()) {
            	m_logger.trace("~> LevelDbDataSource.delete(): " + m_name + ", key: " + key);
            }
        	m_db.delete(key.getBytes(m_charset));
            if (m_logger.isTraceEnabled()){
            	m_logger.trace("<~ LevelDbDataSource.delete(): " + m_name + ", key: " + key);
            }
        } finally {
        	m_resetDbLock.readLock().unlock();
        	m_logger.debug("close delete end name :" + m_name + "delete key:" + key);
        }
    }

    /**
     * destroyDB
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
     * get
     */
    @Override
    public String get(String key) {
    	m_logger.debug("get start name :" + m_name + " get key:" + key);
    	m_resetDbLock.readLock().lock();
        try {
            if (m_logger.isTraceEnabled()){
            	m_logger.trace("~> LevelDbDataSource.get(): " + m_name + ", key: " + key);
            }
            try {
                byte[] ret = m_db.get(key.getBytes(m_charset));
                if (m_logger.isTraceEnabled()){
                	m_logger.trace("<~ LevelDbDataSource.get(): " + m_name + ", key: " 
                			+ key + ", " + (ret == null ? "null" : ret.length));
                }
                return new String(ret, m_charset);
            } catch (DBException e) {
            	m_logger.warn("Exception. Retrying again...", e);
                byte[] ret = m_db.get(key.getBytes(m_charset));
                if (m_logger.isTraceEnabled()) {
                	m_logger.trace("<~ LevelDbDataSource.get(): " + m_name + ", key: " 
                			+ key + ", " + (ret == null ? "null" : ret.length));
                }
                return new String(ret, m_charset);
            }
        } finally {
        	m_resetDbLock.readLock().unlock();
        	m_logger.debug("get end name :" + m_name + "get key:" + key);
        }
    }

    /**
     * @return
     */
    private Path getPath() {
        return Paths.get(DataCenter.m_config.m_datebaseDir, m_name);
    }

    /**
     * init
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
     * isAlive
     */
    @Override
    public boolean isAlive() {
        return m_alive;
    }

    /**
     * keys
     */
    @Override
    public Set<String> keys() {
    	m_logger.debug("keys start name :" + m_name);
    	m_resetDbLock.readLock().lock();
        try {
            if (m_logger.isTraceEnabled()) {
            	m_logger.trace("~> LevelDbDataSource.keys(): " + m_name);
            }
            try (DBIterator iterator = m_db.iterator()) {
                Set<String> result = new HashSet<>();
                for (iterator.seekToFirst(); iterator.hasNext(); iterator.next()) {
                    result.add(new String(iterator.peekNext().getKey(), m_charset));
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
    public void put(String key, String value) {
    	m_logger.debug("put start name :" + m_name + "key:" + key + "value" + value);
    	m_resetDbLock.readLock().lock();
        try {
            if (m_logger.isTraceEnabled()) {
            	m_logger.trace("~> LevelDbDataSource.put(): " + m_name + ", key: " + key 
            			+ ", " + (value == null ? "null" : value.length()));
            }
        	m_db.put(key.getBytes(m_charset), value.getBytes(m_charset));
            if (m_logger.isTraceEnabled()) {
            	m_logger.trace("<~ LevelDbDataSource.put(): " + m_name + ", key: " + key 
            			+ ", " + (value == null ? "null" : value.length()));
            }
        } finally {
        	m_resetDbLock.readLock().unlock();
        	m_logger.debug("put end name :" + m_name + "key:" + key + "value" + value);
        }
    }
    
    /**
     * reset
     */
    public void reset() {
    	m_logger.debug("reset start name :" + m_name);
        close();
        FileUtil.RecursiveDelete(getPath().toString());
        init();
    	m_logger.debug("reset end name :" + m_name);
    }
    
    /**
     * updateBatchInternal
     * @param rows
     * @throws IOException
     */
    private void updateBatchInternal(Map<String, String>  rows) throws IOException {
    	m_logger.debug("updateBatchInternal start name :" + m_name);
        try (WriteBatch batch = m_db.createWriteBatch()) {
            for (Map.Entry<String, String> entry : rows.entrySet()) {
                if (entry.getValue() == null) {
                	m_logger.debug("updateBatchInternal delete key :" + entry.getKey());
                    batch.delete(entry.getKey().getBytes(m_charset));
                } else {
                	m_logger.debug("updateBatchInternal put key :" + entry.getKey()
                	+ "value:" + entry.getValue());
                    batch.put(entry.getKey().getBytes(m_charset), entry.getValue().getBytes(m_charset));
                }
            }
            m_db.write(batch);
        }
    	m_logger.debug("updateBatchInternal end name :" + m_name);
    }

    /**
     * updateBatch
     */
    @Override
    public void updateBatch(Map<String, String> rows) {
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