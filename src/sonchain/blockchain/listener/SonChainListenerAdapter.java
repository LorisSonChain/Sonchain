package sonchain.blockchain.listener;

import java.util.List;

import sonchain.blockchain.core.Block;
import sonchain.blockchain.core.BlockSummary;
import sonchain.blockchain.core.PendingState;
import sonchain.blockchain.core.Transaction;
import sonchain.blockchain.core.TransactionExecutionSummary;
import sonchain.blockchain.core.TransactionReceipt;
import sonchain.blockchain.data.BaseMessage;

public class SonChainListenerAdapter implements SonChainListener {

    public void onBlock(Block block, List<TransactionReceipt> receipts) {
    }

    @Override
    public void onBlock(BlockSummary blockSummary) {
        onBlock(blockSummary.getBlock(), blockSummary.getReceipts());
    }

    @Override
    public void onNoConnections() {

    }

    @Override
    public void onPeerDisconnect(String host, long port) {
    }

    @Override
    public void onPendingStateChanged(PendingState pendingState) {
    }

    @Override
    //public void onRecvMessage(Channel channel, BaseMessage message) {
    public void onRecvMessage(BaseMessage message) {
    }

    @Override
    //public void onSendMessage(Channel channel, BaseMessage message) {
    public void onSendMessage(BaseMessage message) {
    }

    @Override
    public void onSyncDone(SyncState state) {

    }
    

    @Override
    public void onPendingTransactionUpdate(TransactionReceipt txReceipt, PendingTransactionState state, Block block) {

    }

    @Override
    public void onVMTraceCreated(String transactionHash, String trace) {

    }

    @Override
    public void trace(String output) {
    }

	@Override
	public void onPendingTransactionsReceived(List<Transaction> transactions) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onTransactionExecuted(TransactionExecutionSummary summary) {
		// TODO Auto-generated method stub
		
	}
}
