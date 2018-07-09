package sonchain.blockchain.contract;

import sonchain.blockchain.core.CallTransaction;

public class SonContractFunctionImpl implements SonContractFunction{
	private SonContractImpl m_contract = null;
    private CallTransaction.Function m_abi = null;

    public SonContractFunctionImpl(SonContractImpl contract, CallTransaction.Function abi) {
    	m_contract = contract;
        m_abi = abi;
    }

    @Override
    public SonContractImpl getContract() {
        return m_contract;
    }

    @Override
    public CallTransaction.Function getInterface() {
        return m_abi;
    }
}
