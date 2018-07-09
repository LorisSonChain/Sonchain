package sonchain.blockchain.peer;
import owchart.owlib.Sock.SocketListener;

public class PeerSocketListener implements SocketListener
{
    public PeerSocketListener()
    {

    }
    public void CallBack(int socketID, int localSID, byte []str, int len)
    {
        BaseServicePeer.CallBack(socketID, localSID, str, len);
    }
    public void WriteLog(int socketID, int localSID, int state, String log)
    {
        BaseServicePeer.WriteLog(socketID, localSID, state, log);
    }
}
