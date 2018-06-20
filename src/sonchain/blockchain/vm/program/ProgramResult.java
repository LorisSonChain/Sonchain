package sonchain.blockchain.vm.program;

import sonchain.blockchain.core.InternalTransaction;
import sonchain.blockchain.util.ByteArraySet;
import sonchain.blockchain.util.ByteUtil;
import sonchain.blockchain.vm.DataWord;
import sonchain.blockchain.vm.LogInfo;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.collections4.CollectionUtils;

public class ProgramResult {
	private byte[] m_hReturn = ByteUtil.EMPTY_BYTE_ARRAY;
	private RuntimeException m_exception = null;

	private Set<DataWord> m_deleteAccounts = null;
	private ByteArraySet m_touchedAccounts = new ByteArraySet();
	private List<InternalTransaction> m_internalTransactions = null;
	private List<LogInfo> m_logInfoList = null;
	private long futureRefund = 0;	

	public void setHReturn(byte[] hReturn) {
		m_hReturn = hReturn;
	}

	public byte[] getHReturn() {
		return m_hReturn;
	}

	public RuntimeException getException() {
		return m_exception;
	}

	public void setException(RuntimeException exception) {
		m_exception = exception;
	}

	public Set<DataWord> getDeleteAccounts() {
		if (m_deleteAccounts == null) {
			m_deleteAccounts = new HashSet<>();
		}
		return m_deleteAccounts;
	}

	public void addDeleteAccount(DataWord address) {
		getDeleteAccounts().add(address);
	}

	public void addDeleteAccounts(Set<DataWord> accounts) {
		if (!CollectionUtils.isEmpty(accounts)) {
			getDeleteAccounts().addAll(accounts);
		}
	}

	public void addTouchAccount(byte[] addr) {
		m_touchedAccounts.add(addr);
	}

	public Set<byte[]> getTouchedAccounts() {
		return m_touchedAccounts;
	}

	public void addTouchAccounts(Set<byte[]> accounts) {
		if (!CollectionUtils.isEmpty(accounts)) {
			getTouchedAccounts().addAll(accounts);
		}
	}

	public List<LogInfo> getLogInfoList() {
		if (m_logInfoList == null) {
			m_logInfoList = new ArrayList<>();
		}
		return m_logInfoList;
	}

	public void addLogInfo(LogInfo logInfo) {
		getLogInfoList().add(logInfo);
	}

	public void addLogInfos(List<LogInfo> logInfos) {
		if (!CollectionUtils.isEmpty(logInfos)) {
			getLogInfoList().addAll(logInfos);
		}
	}

//	public List<CallCreate> getCallCreateList() {
//		if (callCreateList == null) {
//			callCreateList = new ArrayList<>();
//		}
//		return callCreateList;
//	}
//
//	public void addCallCreate(byte[] data, byte[] destination, byte[] gasLimit,
//			byte[] value) {
//		getCallCreateList().add(
//				new CallCreate(data, destination, gasLimit, value));
//	}

	public List<InternalTransaction> getInternalTransactions() {
		if (m_internalTransactions == null) {
			m_internalTransactions = new ArrayList<>();
		}
		return m_internalTransactions;
	}

	public InternalTransaction addInternalTransaction(byte[] parentHash,
			int deep, byte[] nonce, DataWord gasPrice, DataWord gasLimit,
			byte[] senderAddress, byte[] receiveAddress, byte[] value,
			byte[] data, String note) {
		InternalTransaction transaction = new InternalTransaction(parentHash,
				deep, CollectionUtils.size(m_internalTransactions), nonce,
				senderAddress, receiveAddress, value, data, note);
		getInternalTransactions().add(transaction);
		return transaction;
	}

	public void addInternalTransactions(
			List<InternalTransaction> internalTransactions) {
		getInternalTransactions().addAll(internalTransactions);
	}

	public void rejectInternalTransactions() {
		for (InternalTransaction internalTx : getInternalTransactions()) {
			internalTx.reject();
		}
	}

	public long getFutureRefund() {
		return futureRefund;
	}

	public void resetFutureRefund() {
		futureRefund = 0;
	}

	public void merge(ProgramResult another) {
		addInternalTransactions(another.getInternalTransactions());
		if (another.getException() == null) {
			addDeleteAccounts(another.getDeleteAccounts());
			addLogInfos(another.getLogInfoList());
			addTouchAccounts(another.getTouchedAccounts());
		}
	}

	public static ProgramResult empty() {
		ProgramResult result = new ProgramResult();
		result.setHReturn(ByteUtil.EMPTY_BYTE_ARRAY);
		return result;
	}
}
