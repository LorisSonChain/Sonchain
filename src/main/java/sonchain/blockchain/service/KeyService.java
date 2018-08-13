package sonchain.blockchain.service;

import java.math.BigInteger;
import java.util.concurrent.Future;

import org.apache.log4j.Logger;
import org.bouncycastle.util.encoders.Hex;

import sonchain.blockchain.base.HttpData;
import sonchain.blockchain.base.HttpEasyService;
import sonchain.blockchain.core.BlockChain;
import sonchain.blockchain.core.Transaction;
import sonchain.blockchain.crypto.ECKey;
import sonchain.blockchain.plugins.wallet.WalletManager;
import sonchain.blockchain.plugins.wallet.WalletUtil;
import sonchain.blockchain.util.ByteUtil;
import sonchain.blockchain.util.Numeric;

public class KeyService implements HttpEasyService {
	public static final Logger m_logger = Logger.getLogger(KeyService.class);
	
	@Override
	public int OnReceive(HttpData data) {
		try {
			String method = data.m_method.toLowerCase();
			if (method.equals("get")) {
				String function = data.m_parameters.get("function");
				if (function.equalsIgnoreCase("create")){
				}
			} else {
				
			}
			return 1;
		} catch (Exception e) {
			m_logger.error(e);
			data.m_resStr = "-1";
			e.printStackTrace();
			return -1;
		}
	}
}
