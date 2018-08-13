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

public class AccountService implements HttpEasyService {
	public static final Logger m_logger = Logger.getLogger(AccountService.class);
	
	@Override
	public int OnReceive(HttpData data) {
		try {
			String method = data.m_method.toLowerCase();
			if (method.equals("get")) {
				String function = data.m_parameters.get("function");
				if (function.equalsIgnoreCase("create")){
					String creator = data.m_parameters.get("creator");
					String name = data.m_parameters.get("name");
					String ownerKey = data.m_parameters.get("ownerkey");
					String activeKey = data.m_parameters.get("activekey");
					int expiration = 30;
					if(data.m_parameters.containsKey("expiration")){
						expiration = Integer.valueOf(data.m_parameters.get("expiration"));
					}
				}
				else if (function.equalsIgnoreCase("open")){
					String walletName = "default";
					if(data.m_parameters.containsKey("name"))
					{
						walletName = data.m_parameters.get("name");
					}
				}
				else if (function.equalsIgnoreCase("lock")){
					String walletName = "default";
					if(data.m_parameters.containsKey("name"))
					{
						walletName = data.m_parameters.get("name");
					}
				}
				else if (function.equalsIgnoreCase("unlock")) {
					String walletName = "default";
					if(data.m_parameters.containsKey("name"))
					{
						walletName = data.m_parameters.get("name");
					}
					String password = data.m_parameters.get("password");
				}
				else if (function.equalsIgnoreCase("importkey")) {
					String walletName = "default";
					if(data.m_parameters.containsKey("name"))
					{
						walletName = data.m_parameters.get("name");
					}
					String key = data.m_parameters.get("key");
				}
				else if (function.equalsIgnoreCase("removekey")) {
					String walletName = "default";
					if(data.m_parameters.containsKey("name"))
					{
						walletName = data.m_parameters.get("name");
					}
					String key = data.m_parameters.get("key");
					String password = data.m_parameters.get("password");
				}
				else if (function.equalsIgnoreCase("createkey")) {
					String walletName = "default";
					if(data.m_parameters.containsKey("name"))
					{
						walletName = data.m_parameters.get("name");
					}
				}
				else if (function.equalsIgnoreCase("listwallet")) {
				}	
				else if (function.equalsIgnoreCase("listkeys")) {
				}	
				else if (function.equalsIgnoreCase("listprivatekeys")) {
					String walletName = "default";
					if(data.m_parameters.containsKey("name"))
					{
						walletName = data.m_parameters.get("name");
					}
					String password = data.m_parameters.get("password");
				}			
				else if (function.equalsIgnoreCase("lockall")) {
				}		
				else if (function.equalsIgnoreCase("sign")) {
					String jsonTransaction = data.m_parameters.get("transaction");
					String privateKey = data.m_parameters.get("privatekey");
				}		
				else if (function.equalsIgnoreCase("getbalance")) {
					String address = data.m_parameters.get("address");
					BigInteger balance = DataCenter.getSonChainImpl().getRepository()
						.getBalance(Numeric.hexStringToByteArray(address));
					data.m_resStr = balance.toString();
				}
				else if (function.equalsIgnoreCase("getprivatekey")) {
					String privatejson = data.m_parameters.get("privatejson");
					String password = data.m_parameters.get("password");
					data.m_resStr = WalletUtil.getPrivateKey(password, privatejson);
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
