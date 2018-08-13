package sonchain.blockchain.service;

import java.math.BigInteger;
import java.util.concurrent.Future;

import org.apache.log4j.Logger;
import org.bouncycastle.util.BigIntegers;
import org.bouncycastle.util.encoders.Hex;

import sonchain.blockchain.accounts.keystore.WalletFile;
import sonchain.blockchain.base.HttpData;
import sonchain.blockchain.base.HttpEasyService;
import sonchain.blockchain.core.BlockChain;
import sonchain.blockchain.core.BlockChainImpl;
import sonchain.blockchain.core.Repository;
import sonchain.blockchain.core.Transaction;
import sonchain.blockchain.core.TransactionExecutor;
import sonchain.blockchain.core.TransactionReceipt;
import sonchain.blockchain.crypto.Credentials;
import sonchain.blockchain.crypto.ECKey;
import sonchain.blockchain.crypto.HashUtil;
import sonchain.blockchain.crypto.Keys;
import sonchain.blockchain.data.CredentialData;
import sonchain.blockchain.plugins.wallet.WalletManager;
import sonchain.blockchain.plugins.wallet.WalletUtil;
import sonchain.blockchain.util.ByteUtil;
import sonchain.blockchain.util.Numeric;

public class BlockWalletService implements HttpEasyService {

	public static final Logger m_logger = Logger.getLogger(BlockWalletService.class);
	@Override
	public int OnReceive(HttpData data) {
		try {
			String method = data.m_method.toLowerCase();
			if (method.equals("get")) {
				String function = data.m_parameters.get("function");
				if (function.equalsIgnoreCase("generatefullnewwalletfilebypassword")) {
					String password = data.m_parameters.get("password");
					data.m_resStr = WalletUtil.generateFullNewWallet(password);
				}
				else if (function.equalsIgnoreCase("unlockwallet")) {
					String type = data.m_parameters.get("type");
					String content = data.m_parameters.get("content");
					if(type.equals("0"))
					{
						String password = data.m_parameters.get("password");
						data.m_resStr = WalletUtil.getCredentialDataString(password, content);
					}
					else if(type.equals("1"))
					{
						if(!WalletUtil.isValidPrivateKey(content))
						{
							data.m_resStr = "-1";					
						}
						else
						{
							data.m_resStr = WalletUtil.loadCredentialsString(content);
						}
					}
					else if(type.equals("2"))
					{
						data.m_resStr = WalletUtil.getBalanceString(content);
					}
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
				else if (function.equalsIgnoreCase("transfer")) {
					String privateKey = data.m_parameters.get("privatekey");
					if(privateKey == null || privateKey.length() == 0)
					{
						data.m_resStr = "-1"; 
					}
					else
					{
				        byte[] recieveAddress = Hex.decode(data.m_parameters.get("recieveaddress"));
						String value = data.m_parameters.get("value");
						
				        byte[] senderPrivKey = Hex.decode(privateKey);
				        //byte[] senderPrivKey = HashUtil.sha3(privateKey.getBytes());
				        ECKey senderKey = ECKey.fromPrivate(senderPrivKey);
			            Transaction tx = createTx(DataCenter.getSonChainImpl().getBlockChain(), senderKey, recieveAddress,
			            		null, new BigInteger(value));
			            Future<Transaction> future = DataCenter.getSonChainImpl().submitTransaction(tx);
			            Transaction transaction = future.get();
			            String transactionHash = Hex.toHexString(transaction.getHash());
			            DataCenter.getSonChainImpl().initTransactionStatue(transactionHash);
			            m_logger.debug("transfer ret Hash:" + transactionHash);
						data.m_resStr = transactionHash;
					}	
				}
				else if (function.equalsIgnoreCase("gettransactionstatus")) {
					String transactionsHash = data.m_parameters.get("transactionshash");
					data.m_resStr = String.valueOf(
							DataCenter.getSonChainImpl().getTransactionStatue(transactionsHash));
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

    protected Transaction createTx(BlockChain blockchain, ECKey senderKey, byte[] receiveAddress,
                                   byte[] data, BigInteger value) {
        BigInteger nonce = DataCenter.getSonChainImpl().getNonce(senderKey.getAddress());
        Transaction tx = new Transaction(
                ByteUtil.bigIntegerToBytes(nonce),
                receiveAddress,
                ByteUtil.bigIntegerToBytes(value),
                data);
        //TODO
        //tx.sign(senderKey);
        return tx;
    }
}
