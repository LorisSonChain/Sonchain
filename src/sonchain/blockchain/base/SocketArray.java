package sonchain.blockchain.base;

public class SocketArray
{
	private java.util.ArrayList<Integer> m_sockets = new java.util.ArrayList<Integer>();

	public final void AddSocket(int socketID)
	{
		int socketsSize = m_sockets.size();
		for (int i = 0; i < socketsSize; i++)
		{
			if (m_sockets.get(i) == socketID)
			{
				return;
			}
		}
		m_sockets.add(socketID);
	}

	public final void GetSocketList(java.util.ArrayList<Integer> socketList)
	{
		int socketsSize = m_sockets.size();
		for (int i = 0; i < socketsSize; i++)
		{
			socketList.add(m_sockets.get(i));
		}
	}
	
	public final void RemoveSocket(int socketID)
	{
		m_sockets.remove(socketID);
	}
}