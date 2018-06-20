/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sonchain.blockchain.data;

import java.nio.charset.Charset;

import owchart.owlib.Base.CStr;

/**
 *
 * @author GAIA_Todd
 */
public class SonChainHostInfo {
	/*
	 * IP地址
	 */
	public String m_ip;

	/*
	 * 服务端端口
	 */
	public int m_serverPort;

	/*
	 * 上线或下线
	 */
	public int m_type;
	

	@Override
	public String toString() {
		return m_ip + ":" + CStr.ConvertIntToStr(m_serverPort);
	}
	
	public byte[] toBytes()
	{
		String str = toString();
		return str.getBytes();
	}
	
	public static SonChainHostInfo fromBytes(byte[] bytes)
	{
		SonChainHostInfo info = new SonChainHostInfo();
		String str = new String(bytes);
		String[] strs = str.split("[:]");
		if(strs.length > 1)
		{
			info.m_ip = strs[0];
			info.m_serverPort = Integer.valueOf(strs[1]);
		}
		return info;
	}
}
