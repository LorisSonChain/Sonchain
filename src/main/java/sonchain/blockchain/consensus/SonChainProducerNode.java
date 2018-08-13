package sonchain.blockchain.consensus;

import org.bouncycastle.util.encoders.Hex;

import owchart.owlib.Base.CStr;
import sonchain.blockchain.crypto.ECKey;
import sonchain.blockchain.util.Numeric;
import sonchain.blockchain.util.Utils;

public class SonChainProducerNode {
	private String m_address = "";
	private ECKey m_ecKey = null;
	private String m_host = "";
	private byte[] m_id = null;
	private String m_name = "";
	private int m_no = 0;
	private int m_port = 0;
	private String m_publicKey = "";
	
	public String getAddress(){
		return m_address;
	}
	
	public int getNo() {
		return m_no;
	}

	public void setNo(int m_no) {
		this.m_no = m_no;
	}
	
	public ECKey getECKey(){
		return m_ecKey;
	}

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
		return CStr.ConvertIntToStr(m_no) + ":" + m_name + ":" + m_host + ":" 
				+ CStr.ConvertIntToStr(m_port) + " :" + m_publicKey;
	}

	public byte[] toBytes() {
		String str = toString();
		return str.getBytes();
	}

	public static SonChainProducerNode fromBytes(byte[] bytes) {
		SonChainProducerNode info = new SonChainProducerNode();
		String str = new String(bytes);
		String[] strs = str.split("[:]");
		if (strs.length > 4) {
			info.m_no = Integer.valueOf(strs[0]);
			info.m_name = strs[1];
			info.m_host = strs[2];
			info.m_port = Integer.valueOf(strs[3]);
			info.m_publicKey = strs[4];
			info.m_ecKey = ECKey.fromPublicOnly(Numeric.hexStringToByteArray(info.m_publicKey));
			info.m_address = Hex.toHexString(info.m_ecKey.getAddress());
			info.m_id = bytes;
		}
		return info;
	}

	public static SonChainProducerNode fromString(String str) {
		SonChainProducerNode info = new SonChainProducerNode();
		String[] strs = str.split("[:]");
		if (strs.length > 4) {
			info.m_no = Integer.valueOf(strs[0]);
			info.m_name = strs[1];
			info.m_host = strs[2];
			info.m_port = Integer.valueOf(strs[3]);
			info.m_publicKey = strs[4];
			info.m_ecKey = ECKey.fromPublicOnly(Numeric.hexStringToByteArray(info.m_publicKey));
			info.m_address = Hex.toHexString(info.m_ecKey.getAddress());
			info.m_id = str.getBytes();
		}
		return info;
	}
}
