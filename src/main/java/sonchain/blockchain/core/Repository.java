package sonchain.blockchain.core;

import java.math.BigInteger;
import java.util.*;

import sonchain.blockchain.accounts.AccountState;
import sonchain.blockchain.db.ByteArrayWrapper;
import sonchain.blockchain.db.ContractDetails;
import sonchain.blockchain.vm.DataWord;

/**
 * Repository
 *
 */
public interface Repository extends sonchain.blockchain.facade.Repository{

    BigInteger addBalance(byte[] addr, BigInteger value);

    void addStorageRow(byte[] addr, DataWord key, DataWord value);

    /**
     * Close the database
     */
    void close();    
    void commit();    
    AccountState createAccount(byte[] addr);
    void delete(byte[] addr);
    void dumpState(Block block, long gasUsed, int txNumber, byte[] txHash);
    AccountState getAccountState(byte[] addr);
    BigInteger getBalance(byte[] addr);
    byte[] getCode(byte[] addr);
    byte[] getCodeHash(byte[] addr);
    ContractDetails getContractDetails(byte[] addr);
    BigInteger getNonce(byte[] addr);
    byte[] getRoot();
    Repository getSnapshotTo(byte[] root);
    DataWord getStorageValue(byte[] addr, DataWord key);
    Set<byte[]> getAccountsKeys();
    void flush();
    void flushNoReconnect();
    boolean hasContractDetails(byte[] addr);
    BigInteger increaseNonce(byte[] addr);
    boolean isClosed();
    boolean isExist(byte[] addr);
    void loadAccount(byte[] addr, HashMap<ByteArrayWrapper, AccountState> cacheAccounts,
                     HashMap<ByteArrayWrapper, ContractDetails> cacheDetails);
    void rollback();
    void saveCode(byte[] addr, byte[] code);
    BigInteger setNonce(byte[] addr, BigInteger nonce);
    Repository startTracking();
    void syncToRoot(byte[] root);
    void reset();
    void updateBatch(HashMap<ByteArrayWrapper, AccountState> accountStates, 
    		HashMap<ByteArrayWrapper, ContractDetails> contractDetailes);
}
