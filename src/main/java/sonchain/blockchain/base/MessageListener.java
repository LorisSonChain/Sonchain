package sonchain.blockchain.base;

import sonchain.blockchain.peer.*;
import sonchain.blockchain.base.*;

public class MessageListener
{
	public MessageListener()
	{
	}

	protected void finalize() throws Throwable
	{
		Clear();
	}

	private java.util.ArrayList<ListenerMessageCallBack> m_callBacks = new java.util.ArrayList<ListenerMessageCallBack>();

	public final void Add(ListenerMessageCallBack callBack)
	{
		m_callBacks.add(callBack);
	}
	
	public final void CallBack(CMessage message)
	{
		int callBackSize = m_callBacks.size();
		for (int i = 0; i < callBackSize; i++)
		{
			m_callBacks.get(i).CallListenerMessageEvent(message);
		}
	}
	
	public final void Clear()
	{
		m_callBacks.clear();
	}
	
	public final void Remove(ListenerMessageCallBack callBack)
	{
		m_callBacks.remove(callBack);
	}
}