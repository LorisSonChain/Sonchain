package sonchain.blockchain.core;

import java.math.BigInteger;
import java.util.*;

import sonchain.blockchain.accounts.AccountState;
import sonchain.blockchain.db.ByteArrayWrapper;
import sonchain.blockchain.db.ContractDetails;
import sonchain.blockchain.vm.DataWord;

/**
 * Repository
 * @author GAIA
 *
 */
public interface Repository extends sonchain.blockchain.facade.Repository{

    /**
     * Add value to the balance of an account
     *
     * @param addr of the account
     * @param value to be added
     * @return new balance of the account
     */
    BigInteger addBalance(byte[] addr, BigInteger value);

    /**
     * Put a value in storage of an account at a given key
     *
     * @param addr of the account
     * @param key of the data to store
     * @param value is the data to store
     */
    void addStorageRow(byte[] addr, DataWord key, DataWord value);

    /**
     * Close the database
     */
    void close();

    /**
     * Store all the temporary changes made
     * to the repository in the actual database
     */
    void commit();
    
	/**
     * Create a new account in the database
     *
     * @param addr of the contract
     * @return newly created account state
     */
    AccountState createAccount(byte[] addr);

    /**
     * Deletes the account
     *
     * @param addr of the account
     */
    void delete(byte[] addr);

    /**
     * Dump the full state of the current repository into a file with JSON format
     * It contains all the contracts/account, their attributes and
     *
     * @param block of the current state
     * @param gasUsed the amount of gas used in the block until that point
     * @param txNumber is the number of the transaction for which the dump has to be made
     * @param txHash is the hash of the given transaction.
     * If null, the block state post coinbase reward is dumped.
     */
    void dumpState(Block block, long gasUsed, int txNumber, byte[] txHash);

    /**
     * Retrieve an account
     *
     * @param addr of the account
     * @return account state as stored in the database
     */
    AccountState getAccountState(byte[] addr);

    /**
     * Retrieve balance of an account
     *
     * @param addr of the account
     * @return balance of the account as a <code>BigInteger</code> value
     */
    BigInteger getBalance(byte[] addr);

    /**
     * Retrieve the code associated with an account
     *
     * @param addr of the account
     * @return code in byte-array format
     */
    byte[] getCode(byte[] addr);

    /**
     * Retrieve the code hash associated with an account
     *
     * @param addr of the account
     * @return code hash
     */
    byte[] getCodeHash(byte[] addr);

    /**
     * Retrieve contract details for a given account from the database
     *
     * @param addr of the account
     * @return new contract details
     */
    ContractDetails getContractDetails(byte[] addr);

    /**
     * Get current nonce of a given account
     *
     * @param addr of the account
     * @return value of the nonce
     */
    BigInteger getNonce(byte[] addr);

    /**
     * Get root
     * @return
     */
    byte[] getRoot();

    /**
     * 获取快照
     * @param root
     * @return
     */
    Repository getSnapshotTo(byte[] root);

    /**
     * Retrieve storage value from an account for a given key
     *
     * @param addr of the account
     * @param key associated with this value
     * @return data in the form of a <code>DataWord</code>
     */
    DataWord getStorageValue(byte[] addr, DataWord key);

    /**
     * @return Returns set of all the account addresses
     */
    Set<byte[]> getAccountsKeys();

    /**
     * 
     */
    void flush();
    
    /**
     * 
     */
    void flushNoReconnect();

    /**
     * 
     * @param addr
     * @return
     */
    boolean hasContractDetails(byte[] addr);

    /**
     * Increase the account nonce of the given account by one
     *
     * @param addr of the account
     * @return new value of the nonce
     */
    BigInteger increaseNonce(byte[] addr);

    /**
     * Check to see if the current repository has an open connection to the database
     *
     * @return <tt>true</tt> if connection to database is open
     */
    boolean isClosed();

    /**
     * @param addr - account to check
     * @return - true if account exist,
     *           false otherwise
     */
    boolean isExist(byte[] addr);

    /**
     * LoadAccount
     * @param addr
     * @param cacheAccounts
     * @param cacheDetails
     */
    void loadAccount(byte[] addr, HashMap<ByteArrayWrapper, AccountState> cacheAccounts,
                     HashMap<ByteArrayWrapper, ContractDetails> cacheDetails);

    /**
     * Undo all the changes made so far
     * to a snapshot of the repository
     */
    void rollback();

    /**
     * Store code associated with an account
     *
     * @param addr for the account
     * @param code that will be associated with this account
     */
    void saveCode(byte[] addr, byte[] code);

    /**
     * Sets the account nonce of the given account
     *
     * @param addr of the account
     * @param nonce new nonce
     * @return new value of the nonce
     */
    BigInteger setNonce(byte[] addr, BigInteger nonce);

    /**
     * Save a snapshot and start tracking future changes
     *
     * @return the tracker repository
     */
    Repository startTracking();

    /**
     * Return to one of the previous snapshots
     * by moving the root.
     *
     * @param root - new root
     */
    void syncToRoot(byte[] root);

    /**
     * Reset
     */
    void reset();

    /**
     * UpdateBatch
     * @param accountStates
     * @param contractDetailes
     */
    void updateBatch(HashMap<ByteArrayWrapper, AccountState> accountStates, 
    		HashMap<ByteArrayWrapper, ContractDetails> contractDetailes);
}
