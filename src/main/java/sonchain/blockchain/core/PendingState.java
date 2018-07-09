package sonchain.blockchain.core;

import java.util.List;

public interface PendingState extends sonchain.blockchain.facade.PendingState {

    List<Transaction> addPendingTransactions(List<Transaction> transactions);
    void addPendingTransaction(Transaction tx);
    void processBest(Block block, List<TransactionReceipt> receipts);
}
