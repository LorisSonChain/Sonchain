package sonchain.blockchain.contract;

import sonchain.blockchain.core.CallTransaction;

public interface SonContractFunction {

	SonContract getContract();
    CallTransaction.Function getInterface();
}
