package sonchain.blockchain.listener;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import sonchain.blockchain.core.Block;
import sonchain.blockchain.core.BlockSummary;
import sonchain.blockchain.core.EventDispatchThread;
import sonchain.blockchain.core.PendingState;
import sonchain.blockchain.core.Transaction;
import sonchain.blockchain.core.TransactionExecutionSummary;
import sonchain.blockchain.core.TransactionReceipt;
import sonchain.blockchain.data.BaseMessage;

public class CompositeSonChainListener implements SonChainListener {

    private static abstract class RunnableInfo implements Runnable {
        private SonChainListener m_listener;
        private String m_info;

        public RunnableInfo(SonChainListener listener, String info) {
        	m_listener = listener;
        	m_info = info;
        }

        @Override
        public String toString() {
            return "RunnableInfo: " + m_info + " [listener: " + m_listener.getClass() + "]";
        }
    }

    EventDispatchThread m_eventDispatchThread = EventDispatchThread.getDefault();    
    List<SonChainListener> m_listeners = new CopyOnWriteArrayList<>();

    public void addListener(SonChainListener listener) {
    	m_listeners.add(listener);
    }
    public void removeListener(SonChainListener listener) {
    	m_listeners.remove(listener);
    }

    @Override
    public void onBlock(final BlockSummary blockSummary) {
        for (final SonChainListener listener : m_listeners) {
        	m_eventDispatchThread.invokeLater(new RunnableInfo(listener, "onBlock") {
                @Override
                public void run() {
                    listener.onBlock(blockSummary);
                }
            });
        }
    }

    @Override
    public void onNoConnections() {
        for (final SonChainListener listener : m_listeners) {
        	m_eventDispatchThread.invokeLater(new RunnableInfo(listener, "onNoConnections") {
                @Override
                public void run() {
                    listener.onNoConnections();
                }
            });
        }
    }

    @Override
    public void onPeerDisconnect(final String host, final long port) {
        for (final SonChainListener listener : m_listeners) {
        	m_eventDispatchThread.invokeLater(new RunnableInfo(listener, "onPeerDisconnect") {
                @Override
                public void run() {
                    listener.onPeerDisconnect(host, port);
                }
            });
        }
    }

    @Override
    public void onPendingStateChanged(final PendingState pendingState) {
        for (final SonChainListener listener : m_listeners) {
        	m_eventDispatchThread.invokeLater(new RunnableInfo(listener, "onPendingStateChanged") {
                @Override
                public void run() {
                    listener.onPendingStateChanged(pendingState);
                }
            });
        }
    }

    @Override
    public void onSyncDone(final SyncState state) {
        for (final SonChainListener listener : m_listeners) {
        	m_eventDispatchThread.invokeLater(new RunnableInfo(listener, "onSyncDone") {
                @Override
                public void run() {
                    listener.onSyncDone(state);
                }
            });
        }
    }
    
    @Override
    public void onPendingTransactionUpdate(final TransactionReceipt txReceipt, final PendingTransactionState state,
                                           final Block block) {
        for (final SonChainListener listener : m_listeners) {
        	m_eventDispatchThread.invokeLater(new RunnableInfo(listener, "onPendingTransactionUpdate") {
                @Override
                public void run() {
                    listener.onPendingTransactionUpdate(txReceipt, state, block);
                }
            });
        }
    }

    @Override
    public void onVMTraceCreated(final String transactionHash, final String trace) {
        for (final SonChainListener listener : m_listeners) {
        	m_eventDispatchThread.invokeLater(new RunnableInfo(listener, "onVMTraceCreated") {
                @Override
                public void run() {
                    listener.onVMTraceCreated(transactionHash, trace);
                }
            });
        }
    }

    @Override
    public void trace(final String output) {
        for (final SonChainListener listener : m_listeners) {
        	m_eventDispatchThread.invokeLater(new RunnableInfo(listener, "trace") {
                @Override
                public void run() {
                    listener.trace(output);
                }
            });
        }
    }
	@Override
	public void onPendingTransactionsReceived(List<Transaction> transactions) {
        for (final SonChainListener listener : m_listeners) {
        	m_eventDispatchThread.invokeLater(new RunnableInfo(listener, "onPendingTransactionsReceived") {
                @Override
                public void run() {
                    listener.onPendingTransactionsReceived(transactions);
                }
            });
        }
		
	}
	@Override
	public void onTransactionExecuted(TransactionExecutionSummary summary) {
        for (final SonChainListener listener : m_listeners) {
        	m_eventDispatchThread.invokeLater(new RunnableInfo(listener, "onTransactionExecuted") {
                @Override
                public void run() {
                    listener.onTransactionExecuted(summary);
                }
            });
        }		
	}
	@Override
	public void onRecvMessage(BaseMessage message) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void onSendMessage(BaseMessage message) {
		// TODO Auto-generated method stub
		
	}
}
