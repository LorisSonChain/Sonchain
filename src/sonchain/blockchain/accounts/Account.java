package sonchain.blockchain.accounts;

import java.math.BigInteger;
import java.util.*;

import sonchain.blockchain.core.Repository;
import sonchain.blockchain.core.Transaction;
import sonchain.blockchain.crypto.ECKey;
import sonchain.blockchain.util.Utils;

/**
 * Account
 * @author GAIA
 *
 */
public class Account {

	public Account() {
	}

	private byte[] m_address;
	
	private ECKey m_ecKey;
	
	private Set<Transaction> m_pendingTransactions = Collections.synchronizedSet(new HashSet<Transaction>());
	
    private Repository m_repository = null;

	public byte[] getAddress() {
		return m_address;
	}

	public void setAddress(byte[] address) {
		this.m_address = address;
	}
	
	public ECKey getEcKey() {
		return m_ecKey;
	}
	
	public Set<Transaction> getPendingTransactions() {
		return m_pendingTransactions;
	}

	public void addPendingTransaction(Transaction transaction) {
		synchronized (m_pendingTransactions) {
			m_pendingTransactions.add(transaction);
		}
	}
	
	public void clearAllPendingTransactions() {
		synchronized (m_pendingTransactions) {
			m_pendingTransactions.clear();
		}
	}
	
	public BigInteger getBalance() {
		BigInteger balance = m_repository.getBalance(getAddress());
		synchronized (m_pendingTransactions) {
			if (!m_pendingTransactions.isEmpty()) {
				for (Transaction tx : m_pendingTransactions) {
					if (Arrays.equals(getAddress(), tx.getSender())) {
						balance = balance.subtract(new BigInteger(1, tx.getValue()));
					}
					if (Arrays.equals(getAddress(), tx.getReceiveAddress())) {
						balance = balance.add(new BigInteger(1, tx.getValue()));
					}
				}
				// todo: calculate the fee for pending
			}
		}
		return balance;
	}
	
	public BigInteger getNonce() {
		return m_repository.getNonce(getAddress());
	}


	public void init() {
		m_ecKey = new ECKey(Utils.getRandom());
		m_address = m_ecKey.getAddress();
	}
	
	public void init(ECKey ecKey) {
		m_ecKey = ecKey;
		m_address = m_ecKey.getAddress();
	}
}
