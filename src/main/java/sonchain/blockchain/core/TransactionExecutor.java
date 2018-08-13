package sonchain.blockchain.core;

import java.math.BigInteger;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.log4j.Logger;
import org.bouncycastle.util.encoders.Hex;

import sonchain.blockchain.accounts.AccountState;
import sonchain.blockchain.config.BlockChainConfigInterface;
import sonchain.blockchain.config.CommonConfig;
import sonchain.blockchain.db.BlockStore;
import sonchain.blockchain.db.ContractDetails;
import sonchain.blockchain.listener.SonChainListener;
import sonchain.blockchain.listener.SonChainListenerAdapter;
import sonchain.blockchain.service.DataCenter;
import sonchain.blockchain.util.BIUtil;
import sonchain.blockchain.util.ByteArraySet;
import sonchain.blockchain.vm.DataWord;
import sonchain.blockchain.vm.LogInfo;
import sonchain.blockchain.vm.PrecompiledContract;
import sonchain.blockchain.vm.program.ProgramResult;
import sonchain.blockchain.vm.program.invoke.ProgramInvoke;
import sonchain.blockchain.vm.program.invoke.ProgramInvokeFactory;
import static org.apache.commons.lang3.ArrayUtils.getLength;
import static org.apache.commons.lang3.ArrayUtils.isEmpty;
import static sonchain.blockchain.util.BIUtil.*;
import static sonchain.blockchain.util.ByteUtil.EMPTY_BYTE_ARRAY;
import static sonchain.blockchain.util.ByteUtil.toHexString;

public class TransactionExecutor {

	public static final Logger m_logger = Logger.getLogger(TransactionExecutor.class);

	private BlockChainConfigInterface m_blockchainConfig = null;
	private BlockStore m_blockStore = null;
	private Repository m_cacheTrack = null;
	private byte[] m_nodeAddress = null;
	private CommonConfig m_commonConfig = null;
	private Block m_currentBlock = null;
	private String m_execError = "";
	private boolean m_localCall = false;
	private SonChainListener m_listener = null;
	private List<LogInfo> m_logs = null;
	private ProgramInvokeFactory m_programInvokeFactory = null;
	private boolean m_readyToExecute = false;
	private TransactionReceipt m_receipt = null;
	private ProgramResult m_result = new ProgramResult();
	private ByteArraySet m_touchedAccounts = new ByteArraySet();
	private Repository m_track = null;
	private Transaction m_tx = null;
    private PrecompiledContract m_precompiledContract = null;

	public TransactionExecutor(Transaction tx, byte[] nodeAddress, Repository track, BlockStore blockStore,
			ProgramInvokeFactory programInvokeFactory, Block currentBlock) {
		this(tx, nodeAddress, track, blockStore, programInvokeFactory, currentBlock, new SonChainListenerAdapter());
	}

	public TransactionExecutor(Transaction tx, byte[] nodeAddress, Repository track, BlockStore blockStore,
			ProgramInvokeFactory programInvokeFactory, Block currentBlock, SonChainListener listener) {
		m_tx = tx;
		m_nodeAddress = nodeAddress;
		m_track = track;
		m_cacheTrack = track.startTracking();
		m_blockStore = blockStore;
		m_programInvokeFactory = programInvokeFactory;
		m_currentBlock = currentBlock;
		m_listener = listener;
		withCommonConfig(CommonConfig.getDefault());
	}

	private void call() {
		m_logger.debug("call start TransactionInfo:" + m_tx.toString());
		if (!m_readyToExecute) {
			return;
		}
		byte[] targetAddress = m_tx.getReceiveAddress();
		//TODO
		byte[] code = m_track.getCode(targetAddress);
		BigInteger endowment = toBI(m_tx.getValue());
		m_logger.debug("call SenderAddress:" + Hex.toHexString(m_tx.getSenderAddress())
			+ " targetAddress: " + Hex.toHexString(targetAddress) + " Value：" + endowment);
		transfer(m_track, m_tx.getSenderAddress(), targetAddress, endowment);
		m_touchedAccounts.add(targetAddress);
		m_logger.debug("call end");
	}

	private void execError(String err) {
		m_logger.warn(err);
		m_execError = err;
	}

	/**
	 * Do all the basic validation, if the executor will be ready to run the
	 * transaction at the end set readyToExecute = true
	 */
	public void init() {
		m_logger.debug("init start TransactionInfo:" + m_tx.toString());
		if (m_localCall) {
			m_readyToExecute = true;
			return;
		}
		BigInteger reqNonce = m_track.getNonce(m_tx.getSenderAddress());
		BigInteger txNonce = toBI(m_tx.getNonce());
		m_logger.debug("Sender Balance SenderAddress:" + Hex.toHexString(m_tx.getSenderAddress())
		+ " reqNonce: " + reqNonce + " txNonce：" + txNonce);
		if (isNotEqual(reqNonce, txNonce)) {
			execError(String.format("Invalid nonce: required: %s , tx.nonce: %s", reqNonce, txNonce));
			return;
		}

		BigInteger senderBalance = m_track.getBalance(m_tx.getSenderAddress());
		m_logger.debug("Sender Balance SenderAddress:" + Hex.toHexString(m_tx.getSenderAddress())
				+ " Balance:" + senderBalance);
		if (!m_blockchainConfig.acceptTransactionSignature(m_tx)) {
			//TODO
			//execError("Transaction signature not accepted: " + m_tx.getSignature());
			return;
		}
		m_readyToExecute = true;
		m_logger.debug("init end");
	}

	public void execute() {
		m_logger.debug("execute start TransactionInfo:" + m_tx.toString());
		if (!m_readyToExecute) {
			return;
		}
		if (!m_localCall) {
			m_track.increaseNonce(m_tx.getSenderAddress());
		}
		if (m_tx.isContractCreation()) {
			create();
		} else {
			call();
		}
		m_logger.debug("execute end");
	}

	private void create() {
		m_logger.debug("create start TransactionInfo:" + m_tx.toString());
		byte[] newContractAddress = m_tx.getContractAddress();
		// In case of hashing collisions (for TCK tests only), check for any
		// balance before createAccount()
		BigInteger oldBalance = m_track.getBalance(newContractAddress);
		m_cacheTrack.createAccount(m_tx.getContractAddress());
		m_cacheTrack.addBalance(newContractAddress, oldBalance);
		m_cacheTrack.increaseNonce(newContractAddress);
		
		//if(ArrayUtils.isEmpty(m_tx.getData())){
			//m_result.spendGas(basicTxCost);
		//}
		//else{

            ProgramInvoke programInvoke = m_programInvokeFactory.createProgramInvoke(m_tx, m_currentBlock, m_cacheTrack, m_blockStore);
            //m_vm = new VM(config);
            //m_program = new Program(m_tx.getData(), programInvoke, m_tx, config).withCommonConfig(m_commonConfig);
			
		//}
		BigInteger endowment = toBI(m_tx.getValue());
		BIUtil.transfer(m_cacheTrack, m_tx.getSenderAddress(), newContractAddress, endowment);
		m_touchedAccounts.add(newContractAddress);
		m_logger.debug("create end");
	}

	public void go() {
		m_logger.debug("go start" + m_tx.toString());
		if (!m_readyToExecute) {
			m_logger.debug("go end");
			return;
		}
		try {
			String err = DataCenter.m_config.getConfigForBlock(m_currentBlock.getBlockNumber())
					.validateTransactionChanges(m_blockStore, m_currentBlock, m_tx, null);

			m_logger.debug("go start validateTransactionChanges err:" + err);
			if (m_result.getException() != null) {
				m_result.getDeleteAccounts().clear();
				m_result.getLogInfoList().clear();
				m_result.resetFutureRefund();
				throw m_result.getException();
			}
			m_touchedAccounts.addAll(m_result.getTouchedAccounts());
			m_cacheTrack.commit();
		} catch (Throwable e) {
			// TODO: catch whatever they will throw on you !!!
			// https://github.com/ethereum/cpp-ethereum/blob/develop/libethereum/Executive.cpp#L241
			m_cacheTrack.rollback();
			execError(e.getMessage());
		}
		finally
		{
			m_logger.debug("go end");
		}
	}

	public TransactionExecutionSummary finalization() {
		m_logger.debug("finalization start" + m_tx.toString());
		if (!m_readyToExecute) {
			m_logger.debug("finalization end");
			return null;
		}
		try
		{
			TransactionExecutionSummary.Builder summaryBuilder = TransactionExecutionSummary.builderFor(m_tx)
					.logs(m_result.getLogInfoList()).result(m_result.getHReturn());

			m_logger.debug("finalization start TransactionExecutionSummary:" + summaryBuilder.toString());
			if (m_result != null) {
				byte[] addr = m_tx.isContractCreation() ? m_tx.getContractAddress() : m_tx.getReceiveAddress();
	
				//summaryBuilder.deletedAccounts(m_result.getDeleteAccounts())
				//		.internalTransactions(m_result.getInternalTransactions());
	
				ContractDetails contractDetails = m_track.getContractDetails(addr);
				if (contractDetails != null) {
					// TODO
					// summaryBuilder.storageDiff(track.getContractDetails(addr).getStorage());
					//
					// if (program != null) {
					// summaryBuilder.touchedStorage(contractDetails.getStorage(),
					// program.getStorageDiff());
					// }
				}
				if (m_result.getException() != null) {
					summaryBuilder.markAsFailed();
				}
			}
			TransactionExecutionSummary summary = summaryBuilder.build();
			m_logger.debug("finalization start TransactionExecutionSummary11:" + summary.toString());
			//TODO
			//m_touchedAccounts.add(m_nodeAddress);
			m_logger.info(String.format("Pay fees to miner: , feesEarned: [{%s}]", 
					Hex.toHexString(m_nodeAddress)));
			if (m_result != null) {
				m_logs = m_result.getLogInfoList();
				// Traverse list of suicides
				for (DataWord address : m_result.getDeleteAccounts()) {
					m_track.delete(address.getLast20Bytes());
				}
			}
			if(m_listener != null){
				m_listener.onTransactionExecuted(summary);
			}
			return summary;
		}
		finally
		{
			m_logger.debug("finalization end");
		}
	}

	public TransactionExecutor setLocalCall(boolean localCall) {
		m_localCall = localCall;
		return this;
	}

	public TransactionReceipt getReceipt() {
		if (m_receipt == null) {
			m_receipt = new TransactionReceipt();
	        //TODO
			//m_receipt.setTransaction(m_tx);
			m_receipt.setLogInfoList(getVMLogs());
	        //TODO
			//m_receipt.setExecutionResult(getResult().getHReturn());
			m_receipt.setError(m_execError);
			// receipt.setPostTxState(track.getRoot()); // TODO later when
			// RepositoryTrack.getRoot() is implemented
		}
		return m_receipt;
	}

	public ProgramResult getResult() {
		return m_result;
	}

	public List<LogInfo> getVMLogs() {
		return m_logs;
	}

	public TransactionExecutor withCommonConfig(CommonConfig commonConfig) {
		m_logger.debug("withCommonConfig start");
		m_commonConfig = commonConfig;
		m_blockchainConfig = DataCenter.m_config.getConfigForBlock(m_currentBlock.getBlockNumber());
		m_logger.debug("withCommonConfig end");
		return this;
	}
}
