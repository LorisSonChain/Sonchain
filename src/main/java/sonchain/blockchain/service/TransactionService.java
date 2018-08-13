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
import sonchain.blockchain.util.ByteUtil;

public class TransactionService  implements HttpEasyService {

	public static final Logger m_logger = Logger.getLogger(TransactionService.class);
	
	@Override
	public int OnReceive(HttpData data) {
		try {
			String method = data.m_method.toLowerCase();
			if (method.equals("get")) {
				String function = data.m_parameters.get("function");
				if (function.equalsIgnoreCase("transfer")) {
					String sender = data.m_parameters.get("sender");
					String recipient = data.m_parameters.get("recipient");
					String amount = data.m_parameters.get("amount");
					String memo = data.m_parameters.get("memo");
					
//			        byte[] senderPrivKey = Hex.decode(privateKey);
//			        ECKey senderKey = ECKey.fromPrivate(senderPrivKey);
//		            Transaction tx = createTx(DataCenter.getSonChainImpl().getBlockChain(), senderKey, recipient,
//		            		null, new BigInteger(amount), memo);
//		            Future<Transaction> future = DataCenter.getSonChainImpl().submitTransaction(tx);
//		            Transaction transaction = future.get();
//		            String transactionHash = Hex.toHexString(transaction.getHash());
//		            DataCenter.getSonChainImpl().initTransactionStatue(transactionHash);
//		            m_logger.debug("transfer ret Hash:" + transactionHash);
//					data.m_resStr = transactionHash;
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
                                   byte[] data, BigInteger value, String memo) {
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
