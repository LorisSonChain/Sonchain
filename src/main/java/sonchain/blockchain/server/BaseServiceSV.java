
package sonchain.blockchain.server;

import sonchain.blockchain.base.*;
import owchart.owlib.Sock.*;

import java.util.*;

public class BaseServiceSV {
	public BaseServiceSV() {
	}

	protected void finalize() throws Throwable {
		synchronized (m_listeners) {
			m_listeners.clear();
		}
		synchronized (m_waitMessages) {
			m_waitMessages.clear();
		}
	}

	public static int COMPRESSTYPE_NONE = 0;
	public static int COMPRESSTYPE_GZIP = 1;

	protected HashMap<Integer, Integer> m_compressTypes = new HashMap<Integer, Integer>();
	private java.util.HashMap<Integer, MessageListener> m_listeners = new java.util.HashMap<Integer, MessageListener>();

	private static int m_requestID = 10000;
	private static java.util.HashMap<Integer, BaseServiceSV> m_services = new java.util.HashMap<Integer, BaseServiceSV>();
	private java.util.HashMap<Integer, CMessage> m_waitMessages = new java.util.HashMap<Integer, CMessage>();

	private int m_compressType = COMPRESSTYPE_GZIP;

	public final int GetCompressType() {
		return m_compressType;
	}

	public final void SetCompressType(int value) {
		m_compressType = value;
	}

	protected static long m_downFlow;

	public static long GetDownFlow() {
		return BaseServiceSV.m_downFlow;
	}

	public static void SetDownFlow(long value) {
		BaseServiceSV.m_downFlow = value;
	}

	private int m_groupID = 0;

	public final int GetGroupID() {
		return m_groupID;
	}

	public final void SetGroupID(int value) {
		m_groupID = value;
	}

	private boolean m_isDisposed = false;

	public final boolean IsDisposed() {
		return m_isDisposed;
	}

	private int m_serviceID = 0;

	public final int GetServiceID() {
		return m_serviceID;
	}

	public final void SetServiceID(int value) {
		m_serviceID = value;
	}

	private int m_sessionID = 0;

	public final int GetSessionID() {
		return m_sessionID;
	}

	public final void SetSessionID(int value) {
		m_sessionID = value;
	}

	private int m_socketID = 0;

	public final int GetSocketID() {
		return m_socketID;
	}

	public final void SetSocketID(int socketID) {
		m_socketID = socketID;
	}

	protected static long m_upFlow;

	public static long GetUpFlow() {
		return m_upFlow;
	}

	public static void GetUpFlow(long value) {
		m_upFlow = value;
	}

	public static int CloseServer(int socketID) {
		return Servers.Close(socketID);
	}

	public static int SendByServer(int socketID, int localSID, byte[] str, int len) {
		return Servers.Send(socketID, localSID, str, len);
	}

	public static int StartServer(int port) {
		Servers.SetListener(new ServerSocketListener());
		return Servers.Start(0, port);
	}

	public static void AddService(BaseServiceSV service) {
		m_services.put(service.GetServiceID(), service);
	}

	public static void CallBack(int socketID, int localSID, byte[] str, int len) {
		m_downFlow += len;
		try {
			if (len > 4) {
				Binary br = new Binary();
				br.Write(str, len);
				int head = br.ReadInt();
				// int groupID = br.ReadShort();
				int serviceID = br.ReadShort();
				BaseServiceSV service = null;
				if (m_services.containsKey(serviceID)) {
					m_services.get(serviceID).OnCallBack(br, socketID, localSID, len);
				}
				br.Close();
			}
		} catch (Exception ex) {
			System.out.println(ex.getMessage() + "\r\n" + ex.getStackTrace());
		}
	}

	public void Dispose() {
		if (!m_isDisposed) {
			m_listeners.clear();
			m_isDisposed = true;
		}
	}

	public static String GetHttpWebRequest(String url) {
		return "";
	}

	public static int GetRequestID() {
		return m_requestID++;
	}

	public static void GetServices(java.util.ArrayList<BaseServiceSV> services) {
		for (BaseServiceSV service : m_services.values()) {
			services.add(service);
		}
	}

	public int KeepAlive(int socketID) {
		int ret = -1;
		try {
			Binary bw = new Binary();
			bw.WriteInt((int) 4);
			ret = SendByServer(socketID, m_socketID, bw.GetBytes(), 4);
			bw.Close();
		} catch (Exception ex) {

		}
		return ret;
	}

	public void OnCallBack(Binary br, int socketID, int localSID, int len) {
		try {
			int headSize = 4 * 4 + 2 * 2 + 1 * 2;
			int functionID = br.ReadShort();
			int sessionID = br.ReadInt();
			int requestID = br.ReadInt();
			int state = (int) br.ReadChar();
			int compressType = (int) br.ReadChar();
			int bodyLength = br.ReadInt();
			byte[] body = new byte[len - headSize];
			br.ReadBytes(body);
			CMessage message = null;
			byte[] unzipBody = null;
			if (compressType == COMPRESSTYPE_GZIP) {
				unzipBody = CStrA.UnGZip(body);
				bodyLength = unzipBody.length;
				message = new CMessage(GetGroupID(), GetServiceID(), functionID, sessionID, requestID, socketID, state,
						compressType, bodyLength, unzipBody);
				// TODO...
			} else {
				message = new CMessage(GetGroupID(), GetServiceID(), functionID, sessionID, requestID, socketID, state,
						compressType, bodyLength, body);
			}

			OnReceive(message);
			OnWaitMessageHandle(message);
			body = null;
			unzipBody = null;
		} catch (Exception ex) {

		}
	}

	public void OnClientClose(int socketID, int localSID) {
		if (m_compressTypes.containsKey(socketID)) {
			m_compressTypes.remove(socketID);
		}
	}

	public void OnClientConnect(int socketID, int localSID, String ip) {

	}

	public void OnReceive(CMessage message) {
		synchronized (m_compressTypes) {
			m_compressTypes.put(message.m_socketID, message.m_compressType);
		}
	}

	public void OnWaitMessageHandle(CMessage message) {
		if (m_waitMessages.size() > 0) {
			synchronized (m_waitMessages) {
				if (m_waitMessages.containsKey(message.m_requestID)) {
					CMessage waitMessage = m_waitMessages.get(message.m_requestID);
					waitMessage.Copy(message);
				}
			}
		}
	}

	public void RegisterListener(int requestID, ListenerMessageCallBack callBack) {
		synchronized (m_listeners) {
			MessageListener listener = null;
			if (!m_listeners.containsKey(requestID)) {
				listener = new MessageListener();
				m_listeners.put(requestID, listener);
			} else {
				listener = m_listeners.get(requestID);
			}
			listener.Add(callBack);
		}
	}

	public void RegisterWait(int requestID, CMessage message) {
		synchronized (m_waitMessages) {
			m_waitMessages.put(requestID, message);
		}
	}

	public int Send(CMessage message) {
		int ret = -1;
		try {
			Binary bw = new Binary();
			byte[] body = message.m_body;
			int bodyLength = message.m_bodyLength;
			int uncBodyLength = bodyLength;
			synchronized (m_compressTypes) {
				if (m_compressTypes.containsKey(message.m_socketID)) {
					message.m_compressType = m_compressTypes.get(message.m_socketID);
				}
			}
			byte[] zipBody = null;
			if (message.m_compressType == COMPRESSTYPE_GZIP) {
				if (uncBodyLength < 128) {
					message.m_compressType = COMPRESSTYPE_NONE;
				} else {
					zipBody = CStrA.GZip(body);
					bodyLength = zipBody.length;
				}
			}
			int len = 4 * 4 + bodyLength + 2 * 2 + 1 * 2;
			bw.WriteInt(len);
			// bw.WriteShort((short) message.m_groupID);
			bw.WriteShort((short) message.m_serviceID);
			bw.WriteShort((short) message.m_functionID);
			bw.WriteInt(message.m_sessionID);
			bw.WriteInt(message.m_requestID);
			bw.WriteChar((char) message.m_state);
			bw.WriteChar((char) message.m_compressType);
			bw.WriteInt(uncBodyLength);
			if (message.m_compressType == COMPRESSTYPE_GZIP) {
				bw.WriteBytes(zipBody);
			} else {
				bw.WriteBytes(body);
			}
			byte[] bytes = bw.GetBytes();
			ret = SendByServer(message.m_socketID, m_socketID, bytes, bytes.length);
			bw.Close();
		} catch (Exception ex) {

		}
		return ret;
	}

	public void SendToListener(CMessage message) {
		MessageListener listener = null;
		synchronized (m_listeners) {
			if (m_listeners.containsKey(message.m_requestID)) {
				listener = m_listeners.get(message.m_requestID);
			}
		}
		if (listener != null) {
			listener.CallBack(message);
		}
	}

	public void UnRegisterListener(int requestID) {
		synchronized (m_listeners) {
			m_listeners.remove(requestID);
		}
	}

	public void UnRegisterListener(int requestID, ListenerMessageCallBack callBack) {
		synchronized (m_listeners) {
			if (m_listeners.containsKey(requestID)) {
				m_listeners.get(requestID).Remove(callBack);
			}
		}
	}

	public void UnRegisterWait(int requestID) {
		synchronized (m_waitMessages) {
			if (m_waitMessages.containsKey(requestID)) {
				m_waitMessages.remove(requestID);
			}
		}
	}

	public int WaitMessage(int requestID, int timeout) {
		int state = 0;
		try {
			while (timeout > 0) {
				synchronized (m_waitMessages) {
					if (m_waitMessages.containsKey(requestID)) {
						if (m_waitMessages.get(requestID).m_bodyLength > 0) {
							state = 1;
							break;
						}
					} else {
						break;
					}
				}
				timeout -= 10;
				Thread.sleep(10);
			}
			UnRegisterWait(requestID);
		} catch (Exception ex) {
		}
		return state;
	}

	public static void WriteLog(int socketID, int localSID, int state, String log) {
		if (state == 2) {
			for (BaseServiceSV service : m_services.values()) {
				service.OnClientClose(socketID, localSID);
			}
		} else if (state == 1) {
			for (BaseServiceSV service : m_services.values()) {
				service.OnClientConnect(socketID, localSID, log);
			}
		}
	}
}