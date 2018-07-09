package sonchain.blockchain.contract;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import org.bouncycastle.util.encoders.Hex;

import sonchain.blockchain.core.Block;
import sonchain.blockchain.core.CallTransaction;
import sonchain.blockchain.core.Repository;
import sonchain.blockchain.core.Transaction;
import sonchain.blockchain.core.TransactionExecutor;
import sonchain.blockchain.service.DataCenter;
import sonchain.blockchain.util.ByteUtil;

public class SonContractImpl implements SonContract {
	private byte[] m_address;
    public CompilationResult.ContractMetadata m_compiled = null;
    public CallTransaction.Contract m_contract = null;
    public List<CallTransaction.Contract> m_relatedContracts = new ArrayList<>();

    public SonContractImpl(String abi) {
    	m_contract = new CallTransaction.Contract(abi);
    }
    public SonContractImpl(CompilationResult.ContractMetadata result) {
        this(result.m_abi);
        m_compiled = result;
    }

    public void addRelatedContract(String abi) {
        CallTransaction.Contract c = new CallTransaction.Contract(abi);
        m_relatedContracts.add(c);
    }

    void setAddress(byte[] address) {
    	m_address = address;
    }

    @Override
    public byte[] getAddress() {
        if (m_address == null) {
            throw new RuntimeException("Contract address will be assigned only after block inclusion. Call createBlock() first.");
        }
        return m_address;
    }

    @Override
    public SonContractCallResult callFunction(String functionName, Object... args) {
        return callFunction(0, functionName, args);
    }

    @Override
    public SonContractCallResult callFunction(long value, String functionName, Object... args) {
        CallTransaction.Function function = m_contract.getByName(functionName);
        byte[] data = function.encode(convertArgs(args));
        SonContractCallResult res = new SonContractCallResultImpl(this, function);
        //TODO
        //submitNewTx(new PendingTx(null, BigInteger.valueOf(value), data, null, this, res));
        return res;
    }

    @Override
    public Object[] callConstFunction(String functionName, Object... args) {
        return callConstFunction(DataCenter.getSonChainImpl().getBlockChain().getBestBlock(), functionName, args);
    }

    @Override
    public Object[] callConstFunction(Block callBlock, String functionName, Object... args) {

        CallTransaction.Function func = m_contract.getByName(functionName);
        if (func == null) {
        	throw new RuntimeException("No function with name '" + functionName + "'");
        }
        Transaction tx = CallTransaction.createCallTransaction(0, 0, 100000000000000L,
                Hex.toHexString(getAddress()), 0, func, convertArgs(args));
        tx.sign(new byte[32]);
        Repository repository = DataCenter.getSonChainImpl().
        		getBlockChain().getRepository().getSnapshotTo(callBlock.getStateRoot()).startTracking();
        try {
            TransactionExecutor executor = new TransactionExecutor
                    (tx, callBlock.getMinedBy(), repository, DataCenter.getSonChainImpl().getBlockChain().getBlockStore(),
                    		DataCenter.getSonChainImpl().getBlockChain().getProgramInvokeFactory(), callBlock)
                    .setLocalCall(true);
            executor.init();
            executor.execute();
            executor.go();
            executor.finalization();
            return func.decodeResult(executor.getResult().getHReturn());
        } finally {
            repository.rollback();
        }
    }

    private Object[] convertArgs(Object[] args) {
        Object[] ret = new Object[args.length];
        for (int i = 0; i < args.length; i++) {
            if (args[i] instanceof SonContractFunction) {
            	SonContractFunction f = (SonContractFunction) args[i];
                ret[i] = ByteUtil.merge(f.getContract().getAddress(), f.getInterface().encodeSignature());
            } else {
                ret[i] = args[i];
            }
        }
        return ret;
    }

    @Override
    public SonContractStorage getStorage() {
        return new SonContractStorageImpl(getAddress());
    }

    @Override
    public String getABI() {
        return m_compiled.m_abi;
    }

    @Override
    public String getBinary() {
        return m_compiled.m_bin;
    }

    @Override
    public void call(byte[] callData) {
        // for this we need cleaner separation of EasyBlockchain to
        // Abstract and Solidity specific
        throw new UnsupportedOperationException();
    }

    @Override
    public SonContractFunction getFunction(String name) {
        return new SonContractFunctionImpl(this, m_contract.getByName(name));
    }
}
