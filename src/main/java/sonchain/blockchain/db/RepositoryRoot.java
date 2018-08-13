package sonchain.blockchain.db;

import org.apache.log4j.Logger;
import org.bouncycastle.util.encoders.Hex;

import sonchain.blockchain.accounts.AccountState;
import sonchain.blockchain.core.Repository;
import sonchain.blockchain.datasource.MultiCache;
import sonchain.blockchain.datasource.ReadWriteCache;
import sonchain.blockchain.datasource.Serializers;
import sonchain.blockchain.datasource.SourceCodec;
import sonchain.blockchain.datasource.WriteCache;
import sonchain.blockchain.datasource.base.CachedSource;
import sonchain.blockchain.datasource.base.Source;
import sonchain.blockchain.trie.SecureTrie;
import sonchain.blockchain.trie.Trie;
import sonchain.blockchain.trie.TrieImpl;
import sonchain.blockchain.vm.DataWord;

/**
 * 
 *
 */
public class RepositoryRoot extends RepositoryImpl {

	public static final Logger m_logger = Logger.getLogger(RepositoryRoot.class);
	/**
	 *
	 */
    private static class StorageCache extends ReadWriteCache<DataWord, DataWord> {    	
    	private Trie<byte[]> m_trie;
    	/**
    	 * Init
    	 * @param trie
    	 */
        public StorageCache(Trie<byte[]> trie) {
            super(new SourceCodec<>(trie, Serializers.StorageKeySerializer, 
            		Serializers.StorageValueSerializer), WriteCache.CacheType.SIMPLE);
            m_trie = trie;
        }
    }

    /**
     *
     */
    private class MultiStorageCache extends MultiCache<StorageCache> {
        public MultiStorageCache() {
            super(null);
        }
        @Override
        protected synchronized StorageCache create(String key, StorageCache srcCache) {
            //TODO
        	return null;
//            AccountState accountState = m_accountStateCache.get(key);
//            TrieImpl storageTrie = createTrie(m_trieCache, accountState == null ? 
//            		null : accountState.getStateRoot());
//            return new StorageCache(storageTrie);
        }

        @Override
        protected synchronized boolean flushChild(String key, StorageCache childCache) {
            if (super.flushChild(key, childCache)) {
                if (childCache != null) {
                    AccountState storageOwnerAcct = m_accountStateCache.get(key);
                    // need to update account storage root
                    childCache.m_trie.flush();
                    byte[] rootHash = childCache.m_trie.getRootHash();
                    m_accountStateCache.put(key, storageOwnerAcct.withStateRoot(rootHash));
                    return true;
                } else {
                    // account was deleted
                    return true;
                }
            } else {
                // no storage changes
                return false;
            }
        }
    }

    private Source<byte[], byte[]> m_stateDS = null;
    private Trie<byte[]> m_stateTrie = null;
    private CachedSource.BytesKey<byte[]> m_trieCache = null;

    /**
     * Init
     * @param stateDS
     */
    public RepositoryRoot(Source<byte[], byte[]> stateDS) {
        this(stateDS, null);
    }

    /**
     * Building the following structure for snapshot Repository:
     * @param stateDS
     * @param root
     */
    public RepositoryRoot(final Source<byte[], byte[]> stateDS, byte[] root) {
    	m_logger.debug("RepositoryRoot init start.");
        m_stateDS = stateDS;
        m_trieCache = new WriteCache.BytesKey<>(stateDS, WriteCache.CacheType.COUNTING);
        //TODO
        //m_stateTrie = new SecureTrie(m_trieCache, root);

        SourceCodec.BytesKey<AccountState, byte[]> accountStateCodec 
        	= new SourceCodec.BytesKey<>(m_stateTrie, Serializers.AccountStateSerializer);
        ReadWriteCache.BytesKey<AccountState> accountStateCache 
        	= new ReadWriteCache.BytesKey<AccountState>(accountStateCodec, WriteCache.CacheType.SIMPLE);

        MultiCache<StorageCache> storageCache = new MultiStorageCache();
        // counting as there can be 2 contracts with the same code, 1 can suicide
        Source<byte[], byte[]> codeCache = new WriteCache.BytesKey<>(stateDS, WriteCache.CacheType.COUNTING);
        //TODO
        //init(accountStateCache, codeCache, storageCache);
    	m_logger.debug("RepositoryRoot init end.");
    }

    /**
     * commit
     */
    @Override
    public synchronized void commit() {
    	m_logger.debug("commit start.");
        super.commit();
        m_stateTrie.flush();
        m_trieCache.flush();
    	m_logger.debug("commit end.");
    }

    /**
     * createTrie
     * @param trieCache
     * @param root
     * @return
     */
    protected TrieImpl createTrie(CachedSource.BytesKey<String> trieCache, byte[] root) {
    	m_logger.debug("createTrie start. root:" + Hex.toHexString(root));

        //TODO
    	//return new SecureTrie(trieCache, root);
    	return null;
    }

    /**
     * dumpStateTrie
     */
    @Override
    public synchronized String dumpStateTrie() {
    	m_logger.debug("dumpStateTrie start.");
        return ((TrieImpl) m_stateTrie).DumpTrie();
    }

    /**
     * flush
     */
    @Override
    public synchronized void flush() {
    	m_logger.debug("flush start.");
        commit();
    }

    /**
     * getRoot
     */
    @Override
    public synchronized byte[] getRoot() {
    	m_logger.debug("getRoot start.");
        m_storageCache.flush();
        m_accountStateCache.flush();
        byte[] root = m_stateTrie.getRootHash();
    	m_logger.debug("getRoot end. root:" + Hex.toHexString(root));
    	return root;
    }

    /**
     * getSnapshotTo
     */
    @Override
    public Repository getSnapshotTo(byte[] root) {
    	m_logger.debug("getSnapshotTo start. root:" + Hex.toHexString(root));
        return new RepositoryRoot(m_stateDS, root);
    }

    /**
     * syncToRoot
     */
    @Override
    public synchronized void syncToRoot(byte[] root) {
    	m_logger.debug("syncToRoot start. root:" + Hex.toHexString(root));
    	m_stateTrie.setRoot(root);
    }
}