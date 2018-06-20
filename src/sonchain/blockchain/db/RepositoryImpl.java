package sonchain.blockchain.db;

import java.math.BigInteger;
import java.util.*;

import javax.annotation.Nullable;

import org.apache.log4j.Logger;
import org.bouncycastle.util.encoders.Hex;

import sonchain.blockchain.accounts.AccountState;
import sonchain.blockchain.core.Block;
import sonchain.blockchain.core.Repository;
import sonchain.blockchain.crypto.HashUtil;
import sonchain.blockchain.datasource.CachedSource;
import sonchain.blockchain.datasource.MultiCache;
import sonchain.blockchain.datasource.ReadWriteCache;
import sonchain.blockchain.datasource.Source;
import sonchain.blockchain.datasource.WriteCache;
import sonchain.blockchain.service.DataCenter;
import sonchain.blockchain.util.ByteUtil;
import sonchain.blockchain.util.FastByteComparisons;
import sonchain.blockchain.util.Numeric;
import sonchain.blockchain.vm.DataWord;

/**
 * RepositoryImpl
 * @author GAIA
 *
 */
public class RepositoryImpl implements Repository, sonchain.blockchain.facade.Repository {

	public static final Logger m_logger = Logger.getLogger(RepositoryImpl.class);
    protected RepositoryImpl m_parent = null;
    protected Source<byte[], AccountState> m_accountStateCache = null;
    protected Source<byte[], byte[]> m_codeCache = null;
    protected MultiCache<? extends CachedSource<DataWord, DataWord>> m_storageCache = null;

    /**
     * Init
     */
    protected RepositoryImpl() {
    }

    /**
     * Init
     * @param accountStateCache
     * @param codeCache
     * @param storageCache
     */
    public RepositoryImpl(Source<byte[], AccountState> accountStateCache, Source<byte[], byte[]> codeCache,
                          MultiCache<? extends CachedSource<DataWord, DataWord>> storageCache) {
        init(accountStateCache, codeCache, storageCache);
    }

    /**
     * addBalance
     */
    @Override
    public synchronized BigInteger addBalance(byte[] addr, BigInteger value) {
        m_logger.debug("addBalance start address:" + ByteUtil.toHexString(addr) + "[value:]" + value.toString());
        AccountState accountState = getOrCreateAccountState(addr);
        m_accountStateCache.put(addr, accountState.withBalanceIncrement(value));
        accountState = getOrCreateAccountState(addr);
        BigInteger balance = accountState.getBalance();
        m_logger.debug("addBalance end address:" + ByteUtil.toHexString(addr) + "[balance:]" + balance.toString());
        return balance;
    }

    /**
     * addStorageRow
     */
    @Override
    public synchronized void addStorageRow(byte[] addr, DataWord key, DataWord value) {
        m_logger.debug("addStorageRow start address:" + ByteUtil.toHexString(addr) 
        	+ "[key:]" + ByteUtil.toHexString(key.getData())
        	+ "[value:]" + ByteUtil.toHexString(value.getData()));
    	getOrCreateAccountState(addr);
        Source<DataWord, DataWord> contractStorage = m_storageCache.get(addr);
        contractStorage.put(key, value.isZero() ? null : value);
        m_logger.debug("addStorageRow end address:" + ByteUtil.toHexString(addr) 
        		+ "[key:]" + ByteUtil.toHexString(key.getData())
        		+ "[value:]" + ByteUtil.toHexString(value.getData()));
    }

    /**
     * Close
     */
    @Override
    public void close() {
    }

    /**
     * Commit
     */
    @Override
    public synchronized void commit() {
        Repository parentSync = m_parent == null ? this : m_parent;
        // need to synchronize on parent since between different caches flush
        // the parent repo would not be in consistent state
        // when no parent just take this instance as a mock
        m_logger.debug("commit start ");
        synchronized (parentSync) {
        	m_storageCache.flush();
            m_codeCache.flush();
            m_accountStateCache.flush();
        }
        m_logger.debug("commit end ");
    }

    /**
     * createAccount
     */
    @Override
    public synchronized AccountState createAccount(byte[] addr) {
        m_logger.debug("createAccount start address:" + ByteUtil.toHexString(addr) + "[Balance:]0");
        AccountState state = new AccountState(DataCenter.m_config.getCommonConstants().getInitialNonce(),
                BigInteger.ZERO);
        m_accountStateCache.put(addr, state);
        m_logger.debug("createAccount end address:" + ByteUtil.toHexString(addr) + "[Balance:]0");
        return state;
    }

    /**
     * deleteAccount
     */
    @Override
    public synchronized void delete(byte[] addr) {
        m_logger.debug("delete start address:" + ByteUtil.toHexString(addr));
    	m_accountStateCache.delete(addr);
    	m_storageCache.delete(addr);
        m_logger.debug("delete end address:" + ByteUtil.toHexString(addr));
    }

    /**
     * dumpState
     */
    @Override
    public void dumpState(Block block, long gasUsed, int txNumber, byte[] txHash) {
        throw new RuntimeException("Not supported");
    }

    /**
     * dumpStateTrie
     * @return
     */
    public String dumpStateTrie() {
        throw new RuntimeException("Not supported");
    }

    @Override
    public void flush() {
        throw new RuntimeException("Not supported");
    }


    @Override
    public void flushNoReconnect() {
        throw new RuntimeException("Not supported");
    }

    /**
     * getAccountsKeys
     */
    @Override
    public Set<byte[]> getAccountsKeys() {
        throw new RuntimeException("Not supported");
    }

    /**
     * getAccountState
     */
    @Override
    public synchronized AccountState getAccountState(byte[] addr) {
        return m_accountStateCache.get(addr);
    }

    /**
     * getBalance
     */
    @Override
    public synchronized BigInteger getBalance(byte[] addr) {
        m_logger.debug("getBalance start address:" + ByteUtil.toHexString(addr));
        AccountState accountState = getAccountState(addr);
        BigInteger balance = accountState == null ? BigInteger.ZERO : accountState.getBalance();
        m_logger.debug("getBalance end address:" + ByteUtil.toHexString(addr) + "[balance:]" + balance.toString());
        return balance;
    }

    /**
     * getCode
     */
    @Override
    public synchronized byte[] getCode(byte[] addr) {
        m_logger.debug("getCode start address:" + ByteUtil.toHexString(addr));
        byte[] codeHash = getCodeHash(addr);
        return FastByteComparisons.equal(codeHash, HashUtil.EMPTY_DATA_HASH) ?
                ByteUtil.EMPTY_BYTE_ARRAY : m_codeCache.get(codeHash);
    }

    /**
     * getCodeHash
     */
    @Override
    public byte[] getCodeHash(byte[] addr) {
        m_logger.debug("getCodeHash start address:" + ByteUtil.toHexString(addr));
        AccountState accountState = getAccountState(addr);
        return accountState != null ? accountState.getCodeHash() : HashUtil.EMPTY_DATA_HASH;
    }
    
    /**
     * getContractDetails
     */
	@Override
	public ContractDetails getContractDetails(byte[] addr) {
        m_logger.debug("getContractDetails start address:" + ByteUtil.toHexString(addr));
        return new ContractDetailsImpl(addr);
	}

    /**
     * getNonce
     */
    @Override
    public synchronized BigInteger getNonce(byte[] addr) {
        m_logger.debug("getNonce start address:" + ByteUtil.toHexString(addr));
        AccountState accountState = getAccountState(addr);
        return accountState == null ? DataCenter.m_config.getCommonConstants().getInitialNonce() :
                accountState.getNonce();
    }

    /**
     * getOrCreateAccountState
     * @param addr
     * @return
     */
    synchronized AccountState getOrCreateAccountState(byte[] addr) {
        m_logger.debug("getOrCreateAccountState start address:" + ByteUtil.toHexString(addr));
        AccountState ret = m_accountStateCache.get(addr);
        if (ret == null) {
            m_logger.debug("getOrCreateAccountState not exist create new");
            ret = createAccount(addr);
        }
        return ret;
    }

    /**
     * getRoot
     */
    @Override
    public byte[] getRoot() {
        throw new RuntimeException("Not supported");
    }

    /**
     * getSnapshotTo
     */
    @Override
    public synchronized Repository getSnapshotTo(byte[] root) {
        m_logger.debug("getSnapshotTo start ");
        return m_parent.getSnapshotTo(root);
    }

    /**
     * getStorage
     */
    @Override
    public Map<DataWord, DataWord> getStorage(byte[] addr, @Nullable Collection<DataWord> keys) {
        throw new RuntimeException("Not supported");
    }

    /**
     * getStorageKeys
     */
    @Override
    public Set<DataWord> getStorageKeys(byte[] addr) {
        throw new RuntimeException("Not supported");
    }

    /**
     * getStorageSize
     */
    @Override
    public int getStorageSize(byte[] addr) {
        throw new RuntimeException("Not supported");
    }

    /**
     * getStorageValue
     */
    @Override
    public synchronized DataWord getStorageValue(byte[] addr, DataWord key) {
        m_logger.debug("getStorageValue start address:" + ByteUtil.toHexString(addr));
        AccountState accountState = getAccountState(addr);
        return accountState == null ? null : m_storageCache.get(addr).get(key);
    }

    /**
     * getTrieDump
     * @return
     */
    public synchronized String getTrieDump() {
        m_logger.debug("getTrieDump start ");
        return dumpStateTrie();
    }

    /**
     * hasContractDetails
     */
    @Override
    public synchronized boolean hasContractDetails(byte[] addr) {
        m_logger.debug("hasContractDetails start address:" + ByteUtil.toHexString(addr));
        boolean result = getContractDetails(addr) != null;
        m_logger.debug("hasContractDetails end address:" + ByteUtil.toHexString(addr) + "[result:]" + result);
        return result;
    }
    
    /**
     * increaseNonce
     */
    @Override
    public synchronized BigInteger increaseNonce(byte[] addr) {
        m_logger.debug("increaseNonce start address:" + Hex.toHexString(addr));
        AccountState accountState = getOrCreateAccountState(addr);
        m_accountStateCache.put(addr, accountState.withIncrementedNonce());
        accountState = getOrCreateAccountState(addr);
        BigInteger nonce = accountState.getNonce();
        m_logger.debug("increaseNonce end address:" + Hex.toHexString(addr) + " Nonce:" + nonce);
        return accountState.getNonce();
    }

    /**
     * init
     * @param accountStateCache
     * @param codeCache
     * @param storageCache
     */
    protected void init(Source<byte[], AccountState> accountStateCache, Source<byte[], byte[]> codeCache,
                        MultiCache<? extends CachedSource<DataWord, DataWord>> storageCache) {
        m_logger.debug("init start ");
        m_accountStateCache = accountStateCache;
        m_codeCache = codeCache;
        m_storageCache = storageCache;
        String address = "5c75cb43354c9360865fcc170b6925278964bb2c";
        byte[] add = Numeric.hexStringToByteArray(address);
        BigInteger balance = getBalance(add);
        m_logger.debug("get account address: " + address + " balance:" + balance);
//        if(account == null)
//        {
//            m_logger.debug("get account address: " + address + " account is null:");
//        }
//        else
//        {
//        	m_logger.debug("get account address: " + address + " account :" + account.toString());
//        }
        m_logger.debug("init end ");
        
    }

    @Override
    public boolean isClosed() {
        throw new RuntimeException("Not supported");
    }

    /**
     * isExist
     */
    @Override
    public synchronized boolean isExist(byte[] addr) {
        m_logger.debug("isExist start ");
        return getAccountState(addr) != null;
    }

    /**
     * loadAccount
     */
    @Override
    public void loadAccount(byte[] addr, HashMap<ByteArrayWrapper, AccountState> cacheAccounts, 
    		HashMap<ByteArrayWrapper, ContractDetails> cacheDetails) {
        throw new RuntimeException("Not supported");
    }

    /**
     * rollback
     */
    @Override
    public synchronized void rollback() {
        // nothing to do, will be GCed
    }

    /**
     * saveCode
     */
    @Override
    public synchronized void saveCode(byte[] addr, byte[] code) {
        m_logger.debug("saveCode start ");
        byte[] codeHash = HashUtil.sha3(code);
        m_codeCache.put(codeHash, code);
        AccountState accountState = getOrCreateAccountState(addr);
        m_accountStateCache.put(addr, accountState.withCodeHash(codeHash));
    }

    /**
     * setNonce
     */
    @Override
    public synchronized BigInteger setNonce(byte[] addr, BigInteger nonce) {
        m_logger.debug("setNonce start ");
        AccountState accountState = getOrCreateAccountState(addr);
        m_accountStateCache.put(addr, accountState.withNonce(nonce));
        accountState = getOrCreateAccountState(addr);
        BigInteger retNonce = accountState.getNonce();
        m_logger.debug("increaseNonce end address:" + Hex.toHexString(addr) + " Nonce:" + retNonce);
        return nonce;
    }

    /**
     * startTracking
     */
    @Override
    public synchronized RepositoryImpl startTracking() {
        m_logger.debug("startTracking start ");
        ReadWriteCache<byte[], AccountState> trackAccountStateCache = new ReadWriteCache.BytesKey<>(m_accountStateCache,
                WriteCache.CacheType.SIMPLE);
        Source<byte[], byte[]> trackCodeCache = new WriteCache.BytesKey<>(m_codeCache, WriteCache.CacheType.SIMPLE);
        MultiCache<CachedSource<DataWord, DataWord>> trackStorageCache = new MultiCache(m_storageCache) {
            @Override
            protected CachedSource create(byte[] key, CachedSource srcCache) {
                return new WriteCache<>(srcCache, WriteCache.CacheType.SIMPLE);
            }
        };
        RepositoryImpl ret = new RepositoryImpl(trackAccountStateCache, trackCodeCache, trackStorageCache);
        ret.m_parent = this;
        m_logger.debug("startTracking end ");
        return ret;
    }

    @Override
    public void syncToRoot(byte[] root) {
        throw new RuntimeException("Not supported");
    }
    
    /**
     * reset
     */
    @Override
    public void reset() {
        throw new RuntimeException("Not supported");
    }
    /**
     * updateBatch
     */
    @Override
    public void updateBatch(HashMap<ByteArrayWrapper, AccountState> accountStates, 
    		HashMap<ByteArrayWrapper, ContractDetails> contractDetailes) {
        m_logger.debug("updateBatch start ");
        for (Map.Entry<ByteArrayWrapper, AccountState> entry : accountStates.entrySet()) {
        	m_accountStateCache.put(entry.getKey().getData(), entry.getValue());
        }
        for (Map.Entry<ByteArrayWrapper, ContractDetails> entry : contractDetailes.entrySet()) {
            ContractDetails details = getContractDetails(entry.getKey().getData());
            for (DataWord key : entry.getValue().getStorageKeys()) {
                details.put(key, entry.getValue().get(key));
            }
            byte[] code = entry.getValue().getCode();
            if (code != null && code.length > 0) {
                details.setCode(code);
            }
        }
        m_logger.debug("updateBatch end ");
    }

    class ContractDetailsImpl implements ContractDetails {
        private byte[] address;

        public ContractDetailsImpl(byte[] address) {
            this.address = address;
        }

        @Override
        public void put(DataWord key, DataWord value) {
            RepositoryImpl.this.addStorageRow(address, key, value);
        }

        @Override
        public DataWord get(DataWord key) {
            return RepositoryImpl.this.getStorageValue(address, key);
        }

        @Override
        public byte[] getCode() {
            return RepositoryImpl.this.getCode(address);
        }

        @Override
        public byte[] getCode(byte[] codeHash) {
            throw new RuntimeException("Not supported");
        }

        @Override
        public void setCode(byte[] code) {
            RepositoryImpl.this.saveCode(address, code);
        }

        @Override
        public byte[] getStorageHash() {
            throw new RuntimeException("Not supported");
        }

        @Override
        public void decode(byte[] rlpCode) {
            throw new RuntimeException("Not supported");
        }

        @Override
        public void setDirty(boolean dirty) {
            throw new RuntimeException("Not supported");
        }

        @Override
        public void setDeleted(boolean deleted) {
            RepositoryImpl.this.delete(address);
        }

        @Override
        public boolean isDirty() {
            throw new RuntimeException("Not supported");
        }

        @Override
        public boolean isDeleted() {
            throw new RuntimeException("Not supported");
        }

        @Override
        public byte[] getEncoded() {
            throw new RuntimeException("Not supported");
        }

        @Override
        public int getStorageSize() {
            throw new RuntimeException("Not supported");
        }

        @Override
        public Set<DataWord> getStorageKeys() {
            throw new RuntimeException("Not supported");
        }

        @Override
        public Map<DataWord, DataWord> getStorage(@Nullable Collection<DataWord> keys) {
            throw new RuntimeException("Not supported");
        }

        @Override
        public Map<DataWord, DataWord> getStorage() {
            throw new RuntimeException("Not supported");
        }

        @Override
        public void setStorage(List<DataWord> storageKeys, List<DataWord> storageValues) {
            throw new RuntimeException("Not supported");
        }

        @Override
        public void setStorage(Map<DataWord, DataWord> storage) {
            throw new RuntimeException("Not supported");
        }

        @Override
        public byte[] getAddress() {
            return address;
        }

        @Override
        public void setAddress(byte[] address) {
            throw new RuntimeException("Not supported");
        }

        @Override
        public ContractDetails clone() {
            throw new RuntimeException("Not supported");
        }
        
        @Override
        public ContractDetails getSnapshotTo(byte[] hash) {
            throw new RuntimeException("Not supported");
        }

		@Override
		public String toString() {
			// TODO Auto-generated method stub
			return null;
		}

        @Override
        public void syncStorage() {
            throw new RuntimeException("Not supported");
        }

    }
}
