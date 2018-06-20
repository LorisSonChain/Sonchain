package sonchain.blockchain.base;

import sonchain.blockchain.client.*;

/** 
CMessage
 
*/
public class CMessage
{
	public CMessage()
	{
	}
	
	public CMessage(int groupID, int serviceID, int functionID, int sessionID, int requestID, int socketID, int state, int compressType, int bodyLength, byte[] body)
	{
		m_groupID = groupID;
		m_serviceID = serviceID;
		m_functionID = functionID;
		m_sessionID = sessionID;
		m_requestID = requestID;
		m_socketID = socketID;
		m_state = state;
		m_compressType = compressType;
		m_bodyLength = bodyLength;
		m_body = body;
	}
	
	public int m_groupID;
	public int m_serviceID;
	public int m_functionID;
	public int m_sessionID;
	public int m_requestID;
	public int m_socketID;
	public int m_state;
	public int m_compressType;
	public int m_bodyLength;
	public byte[] m_body;
	public final void Copy(CMessage message)
	{
		m_groupID = message.m_groupID;
		m_serviceID = message.m_serviceID;
		m_functionID = message.m_functionID;
		m_sessionID = message.m_sessionID;
		m_requestID = message.m_requestID;
		m_socketID = message.m_socketID;
		m_state = message.m_state;
		m_compressType = message.m_compressType;
		m_bodyLength = message.m_bodyLength;
		m_body = message.m_body;
	}
}