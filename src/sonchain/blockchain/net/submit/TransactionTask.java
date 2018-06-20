package sonchain.blockchain.net.submit;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;

import org.apache.log4j.Logger;

import sonchain.blockchain.core.PendingStateImpl;
import sonchain.blockchain.core.Transaction;

public class TransactionTask implements Callable<List<Transaction>> {

	public static final Logger m_logger = Logger.getLogger(TransactionTask.class);

    private List<Transaction> m_tx = null;
    //private final ChannelManager channelManager;
    //private final Channel receivedFrom;

    public TransactionTask(Transaction tx) {
        this(Collections.singletonList(tx));
    }

    public TransactionTask(List<Transaction> tx) {
    	m_tx = tx;
    }

//    public TransactionTask(Transaction tx, ChannelManager channelManager) {
//        this(Collections.singletonList(tx), channelManager);
//    }
//
//    public TransactionTask(List<Transaction> tx, ChannelManager channelManager) {
//        this(tx, channelManager, null);
//    }
//
//    public TransactionTask(List<Transaction> tx, ChannelManager channelManager, Channel receivedFrom) {
//        this.tx = tx;
//        //this.channelManager = channelManager;
//        //this.receivedFrom = receivedFrom;
//    }

    @Override
    public List<Transaction> call() throws Exception {

        try {
        	m_logger.debug("submit tx: {}" + m_tx.toString());
            //channelManager.sendTransaction(tx, receivedFrom);
            return m_tx;

        } catch (Throwable th) {
        	m_logger.warn("Exception caught: {}" + th);
        }
        return null;
    }
}