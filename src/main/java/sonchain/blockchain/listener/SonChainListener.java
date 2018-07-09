package sonchain.blockchain.listener;

import java.util.List;

import sonchain.blockchain.core.Block;
import sonchain.blockchain.core.BlockSummary;
import sonchain.blockchain.core.PendingState;
import sonchain.blockchain.core.Transaction;
import sonchain.blockchain.core.TransactionExecutionSummary;
import sonchain.blockchain.core.TransactionReceipt;
import sonchain.blockchain.data.BaseMessage;

public interface SonChainListener {

    enum PendingTransactionState {
        DROPPED,
        NEW_PENDING,
        PENDING,
        INCLUDED;
        public boolean isPending() {
            return this == NEW_PENDING || this == PENDING;
        }
    }

    enum SyncState {
        UNSECURE,
        SECURE,
        COMPLETE
    }

    void onBlock(BlockSummary blockSummary);
    void onNoConnections();
    void onPeerDisconnect(String host, long port);
    void onPendingStateChanged(PendingState pendingState);
    void onPendingTransactionsReceived(List<Transaction> transactions);
    void onPendingTransactionUpdate(TransactionReceipt txReceipt, PendingTransactionState state, Block block);

    //void onRecvMessage(Channel channel, BaseMessage message);
    void onRecvMessage(BaseMessage message);

    //void onSendMessage(Channel channel, BaseMessage message);
    void onSendMessage(BaseMessage message);
    
    void onTransactionExecuted(TransactionExecutionSummary summary);
    void onSyncDone(SyncState state);
    void onVMTraceCreated(String transactionHash, String trace);
    void trace(String output);
}
