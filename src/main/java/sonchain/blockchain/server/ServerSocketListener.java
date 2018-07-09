package sonchain.blockchain.server;

import owchart.owlib.Sock.SocketListener;

/**
 */
public class ServerSocketListener implements SocketListener
{
    public ServerSocketListener()
    {

    }
    public void CallBack(int socketID, int localSID, byte []str, int len)
    {
        BaseServiceSV.CallBack(socketID, localSID, str, len);
    }
    public void WriteLog(int socketID, int localSID, int state, String log)
    {
        BaseServiceSV.WriteLog(socketID, localSID, state, log);
    }
}