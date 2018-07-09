package sonchain.blockchain.server;

import sonchain.blockchain.base.Binary;
import sonchain.blockchain.base.CMessage;

/**
 * 附近socket服务
 * 
 * @author joel
 *
 */
public class NeighborService extends BaseServiceSV
{
	// 附近socket服务ID
	public static final int NEIGHBORSERVICE_ID = 10001;

	// 获取延迟
	public static final int FUC_NEIGHBORDELY = 2;

	public NeighborService()
	{
		SetServiceID(NEIGHBORSERVICE_ID);
	}

	@Override
	public void OnReceive(CMessage message)
	{
		int fid = message.m_functionID;
		if (fid == FUC_NEIGHBORDELY)
		{
			try
			{
				// 读取客户端传过来的时间
				// 获取当前系统时间
				// 将两个时间返回给客户端
				Binary bi = new Binary();
				bi.Write(message.m_body, message.m_bodyLength);
				String clientTime = bi.ReadString();
				String serverTime = Long.toString(System.currentTimeMillis());
				message.m_body = null;
				message.m_bodyLength = 0;
				bi.Close();
				bi = new Binary();
				bi.WriteString(clientTime);
				bi.WriteString(serverTime);
				byte[] byts = bi.GetBytes();
				int bdLen = byts.length;
				message.m_body = byts;
				message.m_bodyLength = bdLen;
				Send(message);
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
	}
}
