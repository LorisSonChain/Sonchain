package sonchain.blockchain.core;

import java.math.BigInteger;
import java.util.*;

import org.apache.log4j.Logger;

import sonchain.blockchain.accounts.AccountState;
import sonchain.blockchain.db.ByteArrayWrapper;
import sonchain.blockchain.service.DataCenter;
import sonchain.blockchain.util.ByteUtil;

/**
 * Genesis
 *
 */
public class Genesis extends Block {

	public static final Logger m_logger = Logger.getLogger(Genesis.class);
	public static byte[] ZERO_HASH_2048 = new byte[256];
	public static long NUMBER = 0;

	private Map<ByteArrayWrapper, PremineAccount> m_premine = new HashMap<>();

	/**
	 * Instance
	 * @return
	 */
	public static Genesis getInstance() {
		return DataCenter.m_config.getGenesis();
	}

	/**
	 * Constructor
	 * @param parentHash
	 * @param minedBy
	 * @param number
	 * @param timestamp
	 * @param extraData
	 * @param mixHash
	 */
	public Genesis(byte[] parentHash, byte[] minedBy, long number, long timestamp,  byte[] extraData, byte[] mixHash) {
		super(parentHash, minedBy, number, timestamp, extraData, null);
	}

//	public static Genesis getInstance(SystemProperties config) {
//		return config.getGenesis();
//	}
	
	public Map<ByteArrayWrapper, PremineAccount> getPremine() {
		return m_premine;
	}

	public void setPremine(Map<ByteArrayWrapper, PremineAccount> premine) {
		m_premine = premine;
	}

	public void addPremine(ByteArrayWrapper address, AccountState accountState) {
		m_logger.debug("addPremine start");
		m_premine.put(address, new PremineAccount(accountState));
		m_logger.debug("addPremine end");
	}

	public static void populateRepository(Repository repository, Genesis genesis) {
		m_logger.debug("populateRepository start");
		for (ByteArrayWrapper key : genesis.getPremine().keySet()) {
			PremineAccount premineAccount = genesis.getPremine().get(key);
			AccountState accountState = premineAccount.m_accountState;
			repository.createAccount(key.getData());
			//repository.setNonce(key.getData(), accountState.getNonce());
			repository.addBalance(key.getData(), accountState.getBalance());
			if (premineAccount.m_code != null) {
				repository.saveCode(key.getData(), premineAccount.m_code);
			}
		}
		m_logger.debug("populateRepository end");
	}
}
