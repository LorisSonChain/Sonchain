package sonchain.blockchain.facade;

import java.util.List;

import sonchain.blockchain.core.Transaction;

public interface PendingState {

    /**
     * @return pending state repository
     */
    Repository getRepository();

    /**
     * @return list of pending transactions
     */
    List<Transaction> getPendingTransactions();
}
