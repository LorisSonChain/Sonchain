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
        /**
         * Transaction may be dropped due to:
         * - Invalid transaction (invalid nonce, low gas price, insufficient account funds,
         *         invalid signature)
         * - Timeout (when pending transaction is not included to any block for
         *         last [transaction.outdated.threshold] blocks
         * This is the final state
         */
        DROPPED,
        /**
         * The same as PENDING when transaction is just arrived
         * Next state can be either PENDING or INCLUDED
         */
        NEW_PENDING,
        /**
         * State when transaction is not included to any blocks (on the main chain), and
         * was executed on the last best block. The repository state is reflected in the PendingState
         * Next state can be either INCLUDED, DROPPED (due to timeout)
         * or again PENDING when a new block (without this transaction) arrives
         */
        PENDING,
        /**
         * State when the transaction is included to a block.
         * This could be the final state, however next state could also be
         * PENDING: when a fork became the main chain but doesn't include this tx
         * INCLUDED: when a fork became the main chain and tx is included into another
         *           block from the new main chain
         * DROPPED: If switched to a new (long enough) main chain without this Tx
         */
        INCLUDED;
        public boolean isPending() {
            return this == NEW_PENDING || this == PENDING;
        }
    }

    enum SyncState {
        /**
         * When doing fast sync UNSECURE sync means that the full state is downloaded,
         * chain is on the latest block, and blockchain operations may be executed
         * (such as state querying, transaction submission)
         * but the state isn't yet confirmed with  the whole block chain and can't be
         * trusted.
         * At this stage historical blocks and receipts are unavailable yet
         */
        UNSECURE,
        /**
         * When doing fast sync SECURE sync means that the full state is downloaded,
         * chain is on the latest block, and blockchain operations may be executed
         * (such as state querying, transaction submission)
         * The state is now confirmed by the full chain (all block headers are
         * downloaded and verified) and can be trusted
         * At this stage historical blocks and receipts are unavailable yet
         */
        SECURE,
        /**
         * Sync is fully complete. All blocks and receipts are downloaded.
         */
        COMPLETE
    }

    void onBlock(BlockSummary blockSummary);
    void onNoConnections();
    void onPeerDisconnect(String host, long port);
    /**
     * PendingState changes on either new pending transaction or new best block receive
     * When a new transaction arrives it is executed on top of the current pending state
     * When a new best block arrives the PendingState is adjusted to the new Repository state
     * and all transactions which remain pending are executed on top of the new PendingState
     */
    void onPendingStateChanged(PendingState pendingState);
    /**
     * @deprecated use onPendingTransactionUpdate filtering state NEW_PENDING
     * Will be removed in the next release
     */
    void onPendingTransactionsReceived(List<Transaction> transactions);
    /**
     * Is called when PendingTransaction arrives, executed or dropped and included to a block
     *
     * @param txReceipt Receipt of the tx execution on the current PendingState
     * @param state Current state of pending tx
     * @param block The block which the current pending state is based on (for PENDING tx state)
     *              or the block which tx was included to (for INCLUDED state)
     */
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
