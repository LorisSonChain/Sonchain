package sonchain.blockchain.client;
import owchart.owlib.Sock.SocketListener;

public class ClientSocketListener implements SocketListener
{
    public ClientSocketListener()
    {

    }
    public void CallBack(int socketID, int localSID, byte []str, int len)
    {
        BaseServiceCT.CallBack(socketID, localSID, str, len);
    }
    public void WriteLog(int socketID, int localSID, int state, String log)
    {
        BaseServiceCT.WriteLog(socketID, localSID, state, log);
    }
}
