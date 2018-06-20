package sonchain.blockchain.consensus;

import org.bouncycastle.util.encoders.Hex;

import owchart.owlib.Base.CStr;
import sonchain.blockchain.util.Utils;

public class SonChainPeerNode {
	private byte[] m_id = null;
	private String m_name = "";
	private String m_host = "";
	private int m_port = 0;
	private String m_publicKey = "";

	public byte[] getId() {
		return m_id;
	}

	public String getHexId() {
		return Hex.toHexString(m_id);
	}

	public String getHexIdShort() {
		return Utils.getNodeIdShort(getHexId());
	}

	public void setId(byte[] id) {
		m_id = id;
	}

	public String getHost() {
		return m_host;
	}

	public void setHost(String host) {
		m_host = host;
	}
	
	public String getName(){
		return m_name;
	}
	
	public void SetName(String name){
		m_name = name;
	}

	public int getPort() {
		return m_port;
	}

	public void setPort(int port) {
		m_port = port;
	}

	@Override
	public String toString() {
		return m_name + ":" + m_host + ":" + CStr.ConvertIntToStr(m_port) + " :" + m_publicKey;
	}

	public byte[] toBytes() {
		String str = toString();
		return str.getBytes();
	}

	public static SonChainPeerNode fromBytes(byte[] bytes) {
		SonChainPeerNode info = new SonChainPeerNode();
		String str = new String(bytes);
		String[] strs = str.split("[:]");
		if (strs.length > 3) {
			info.m_name = strs[0];
			info.m_host = strs[1];
			info.m_port = Integer.valueOf(strs[2]);
			info.m_publicKey = strs[3];
			info.m_id = bytes;
		}
		return info;
	}

	public static SonChainPeerNode fromString(String str) {
		SonChainPeerNode info = new SonChainPeerNode();
		String[] strs = str.split("[:]");
		if (strs.length > 3) {
			info.m_name = strs[0];
			info.m_host = strs[1];
			info.m_port = Integer.valueOf(strs[2]);
			info.m_publicKey = strs[3];
			info.m_id = str.getBytes();
		}
		return info;
	}
}
