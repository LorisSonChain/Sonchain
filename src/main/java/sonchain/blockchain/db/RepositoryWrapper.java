package sonchain.blockchain.db;

import java.math.BigInteger;
import java.util.*;

import javax.annotation.Nullable;

import sonchain.blockchain.accounts.AccountState;
import sonchain.blockchain.core.Block;
import sonchain.blockchain.core.BlockChainImpl;
import sonchain.blockchain.core.Repository;
import sonchain.blockchain.listener.CompositeSonChainListener;
import sonchain.blockchain.listener.SonChainListener;
import sonchain.blockchain.listener.SonChainListenerAdapter;
import sonchain.blockchain.vm.DataWord;

public class RepositoryWrapper implements Repository {

    private BlockChainImpl m_blockchain = null;

    public RepositoryWrapper() {
    	CompositeSonChainListener listener = new CompositeSonChainListener();
    	m_blockchain = new BlockChainImpl(listener);
    }

    @Override
    public Set<byte[]> getAccountsKeys() {
        return m_blockchain.getRepository().getAccountsKeys();
    }
    
    @Override
    public BigInteger addBalance(byte[] addr, BigInteger value) {
        return m_blockchain.getRepository().addBalance(addr, value);
    }

    @Override
    public void addStorageRow(byte[] addr, DataWord key, DataWord value) {
    	m_blockchain.getRepository().addStorageRow(addr, key, value);
    }

    @Override
    public void close() {
    	m_blockchain.getRepository().close();
    }
    
    @Override
    public void commit() {
    	m_blockchain.getRepository().commit();
    }

    @Override
    public AccountState createAccount(byte[] addr) {
        return m_blockchain.getRepository().createAccount(addr);
    }

    @Override
    public void delete(byte[] addr) {
    	m_blockchain.getRepository().delete(addr);
    }

    @Override
    public void dumpState(Block block, long gasUsed, int txNumber, byte[] txHash) {
    	m_blockchain.getRepository().dumpState(block, gasUsed, txNumber, txHash);
    }

    @Override
    public void flush() {
    	m_blockchain.getRepository().flush();
    }

    @Override
    public void flushNoReconnect() {
    	m_blockchain.getRepository().flushNoReconnect();
    }

    @Override
    public AccountState getAccountState(byte[] addr) {
        return m_blockchain.getRepository().getAccountState(addr);
    }

    @Override
    public BigInteger getBalance(byte[] addr) {
        return m_blockchain.getRepository().getBalance(addr);
    }
    
    @Override
    public byte[] getCode(byte[] addr) {
        return m_blockchain.getRepository().getCode(addr);
    }

    @Override
    public byte[] getCodeHash(byte[] addr) {
        return m_blockchain.getRepository().getCodeHash(addr);
    }

    @Override
    public ContractDetails getContractDetails(byte[] addr) {
        return m_blockchain.getRepository().getContractDetails(addr);
    }

    @Override
    public BigInteger getNonce(byte[] addr) {
        return m_blockchain.getRepository().getNonce(addr);
    }

    @Override
    public byte[] getRoot() {
        return m_blockchain.getRepository().getRoot();
    }

    @Override
    public Repository getSnapshotTo(byte[] root) {
        return m_blockchain.getRepository().getSnapshotTo(root);
    }

    @Override
    public Map<DataWord, DataWord> getStorage(byte[] addr, @Nullable Collection<DataWord> keys) {
        return m_blockchain.getRepository().getStorage(addr, keys);
    }

    @Override
    public Set<DataWord> getStorageKeys(byte[] addr) {
        return m_blockchain.getRepository().getStorageKeys(addr);
    }

    @Override
    public int getStorageSize(byte[] addr) {
        return m_blockchain.getRepository().getStorageSize(addr);
    }

    @Override
    public DataWord getStorageValue(byte[] addr, DataWord key) {
        return m_blockchain.getRepository().getStorageValue(addr, key);
    }
    
    @Override
    public boolean hasContractDetails(byte[] addr) {
        return m_blockchain.getRepository().hasContractDetails(addr);
    }

    @Override
    public BigInteger increaseNonce(byte[] addr) {
        return m_blockchain.getRepository().increaseNonce(addr);
    }

    @Override
    public boolean isClosed() {
        return m_blockchain.getRepository().isClosed();
    }

    @Override
    public boolean isExist(byte[] addr) {
        return m_blockchain.getRepository().isExist(addr);
    }

    @Override
    public void loadAccount(byte[] addr, HashMap<ByteArrayWrapper, AccountState> cacheAccounts,
    		HashMap<ByteArrayWrapper, ContractDetails> cacheDetails) {
    	m_blockchain.getRepository().loadAccount(addr, cacheAccounts, cacheDetails);
    }

    @Override
    public void reset() {
    	m_blockchain.getRepository().reset();
    }
    
    @Override
    public void rollback() {
    	m_blockchain.getRepository().rollback();
    }

    @Override
    public void saveCode(byte[] addr, byte[] code) {
    	m_blockchain.getRepository().saveCode(addr, code);
    }
    
    @Override
    public BigInteger setNonce(byte[] addr, BigInteger nonce) {
        return m_blockchain.getRepository().setNonce(addr, nonce);
    }

    @Override
    public Repository startTracking() {
        return m_blockchain.getRepository().startTracking();
    }

    @Override
    public void syncToRoot(byte[] root) {
    	m_blockchain.getRepository().syncToRoot(root);
    }

    @Override
    public void updateBatch(HashMap<ByteArrayWrapper, AccountState> accountStates, 
    		HashMap<ByteArrayWrapper, ContractDetails> contractDetailes) {
    	m_blockchain.getRepository().updateBatch(accountStates, contractDetailes);
    }
}
