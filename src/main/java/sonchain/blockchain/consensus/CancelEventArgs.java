package sonchain.blockchain.consensus;

public class CancelEventArgs {
	private boolean m_cancel;
	
	public boolean getCancel(){
		return m_cancel;
	}
	public void setCancel(boolean cancel){
		m_cancel = cancel;
	}
	
	public CancelEventArgs(){
		
	}
	public CancelEventArgs(boolean cancel){
		m_cancel = cancel;
	}
}
