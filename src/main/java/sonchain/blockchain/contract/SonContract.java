package sonchain.blockchain.contract;

import sonchain.blockchain.core.Block;

public interface SonContract extends Contract {

    /**
     * Submits the transaction which invokes the specified contract function
     * with corresponding arguments
     *
     * TODO: either return pending transaction execution result
     * or return Future which is available upon block including trnasaction
     * or combine both approaches
     */
	SonContractCallResult callFunction(String functionName, Object ... args);

    /**
     * Submits the transaction which invokes the specified contract function
     * with corresponding arguments and sends the specified value to the contract
     */
	SonContractCallResult callFunction(long value, String functionName, Object ... args);

    /**
     * Call the function without submitting a transaction and without
     * modifying the contract state.
     * Synchronously returns function execution result
     * (see output argument mapping in class doc)
     */
    Object[] callConstFunction(String functionName, Object ... args);

    /**
     * Call the function without submitting a transaction and without
     * modifying the contract state. The function is executed with the
     * contract state actual after including the specified block.
     *
     * Synchronously returns function execution result
     * (see output argument mapping in class doc)
     */
    Object[] callConstFunction(Block callBlock, String functionName, Object... args);

    /**
     * Gets the contract function. This object can be passed as a call argument for another
     * function with a function type parameter
     */
    SonContractFunction getFunction(String name);

    /**
     * Returns the JSON ABI (Application Binary Interface)
     */
    String getABI();
}
