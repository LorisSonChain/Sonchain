package sonchain.blockchain.plugins.wallet;

import java.math.BigInteger;

import org.apache.commons.codec.digest.DigestUtils;

/**
 * WalletAccount
 *
 */
public class WalletAccount {
	public byte[] m_address = null;
	public boolean m_isAlive = false;
	public boolean m_isDefault = false;
	public boolean m_isLock = false;
	public String m_label = "";
	public int m_version = 0;
	
	public String getAddress()
	{
		return DigestUtils.sha256Hex(m_address);
	}
	
	public BigInteger getBalance()
	{
		return BigInteger.ZERO;
	}
}
