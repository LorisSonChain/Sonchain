package sonchain.blockchain.data;

import sonchain.blockchain.net.SonMessageCodes;

public abstract class BaseMessage {

    protected byte m_code = 0;
    protected byte[] m_encoded = null;
    protected boolean m_parsed = false;
    
	public BaseMessage() {
	
	}
	
	public BaseMessage(byte[] encoded) {
		m_encoded = encoded;
		m_parsed = false;
	}
	
    abstract public SonMessageCodes getCommand();

    abstract public byte[] getEncoded();
}
