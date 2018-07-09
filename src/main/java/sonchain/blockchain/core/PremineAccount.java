package sonchain.blockchain.core;

import sonchain.blockchain.accounts.AccountState;

public class PremineAccount {

	public PremineAccount() {
	}

	public PremineAccount(AccountState accountState) {
		this.m_accountState = accountState;
	}
	
	public byte[] m_code = null;
	public AccountState m_accountState = null;
	
	public byte[] getStateRoot() {
		return m_accountState.getStateRoot();
	}
}
