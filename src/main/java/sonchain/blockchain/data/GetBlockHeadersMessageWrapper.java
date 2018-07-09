package sonchain.blockchain.data;

import java.util.List;

import com.google.common.util.concurrent.SettableFuture;

import sonchain.blockchain.core.BlockHeader;

public class GetBlockHeadersMessageWrapper {
	
	private GetBlockHeadersMessage m_message = null;
	private boolean m_newHashesHandling = false;
	private boolean m_sent = false;
	private SettableFuture<List<BlockHeader>> m_futureHeaders = SettableFuture.create();

	public GetBlockHeadersMessageWrapper(GetBlockHeadersMessage message) {
		m_message = message;
	}

	public GetBlockHeadersMessageWrapper(GetBlockHeadersMessage message, boolean newHashesHandling) {
		m_message = message;
		m_newHashesHandling = newHashesHandling;
	}

	public GetBlockHeadersMessage getMessage() {
		return m_message;
	}

	public boolean isNewHashesHandling() {
		return m_newHashesHandling;
	}

	public boolean isSent() {
		return m_sent;
	}

	public void send() {
		m_sent = true;
	}

	public SettableFuture<List<BlockHeader>> getFutureHeaders() {
		return m_futureHeaders;
	}
}
