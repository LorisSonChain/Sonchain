package sonchain.blockchain.contract;

/**
 * Abstract contract
 *
 */
public interface Contract {
	  
	/**
     * Address of the contract. If the contract creation transaction is
     * still in pending state (not included to a block) the address can be missed
     */
    byte[] getAddress();

    /**
     * Submits contract invocation transaction
     */
    void call(byte[] callData);

    /**
     * Returns the interface for accessing contract storage
     */
    SonContractStorage getStorage();

    /**
     * Returns the contract code binary Hex encoded
     */
    String getBinary();
}
