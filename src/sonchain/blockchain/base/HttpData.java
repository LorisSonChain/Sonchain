//LORD_USERMANAGERSERVER_JAVA_S1:LORD100%;
//****************************************************************************\
//*                                                                             *
//* HttpData.java -    HttpData functions, types, and definitions               *
//*                                                                             *
//*               Version 1.00 ★★★★★                                                                                                                        *
//*                                                                             *
//*               Copyright (c) 2016-2016, Client. All rights reserved.         *
//*               Created by Lord.                                              *
//*                                                                             *
//******************************************************************************

package sonchain.blockchain.base;

import java.util.*;

public class HttpData
{
	public boolean m_close;
	public int m_contentLength;
	public String m_contentType = "";
	public byte[] m_body;
	public String m_method = "";
	public HashMap<String, String> m_parameters = new HashMap<String, String>();
	public byte[] m_resBytes;
	public String m_resStr = "";
	public String m_remoteIP = "";
	public int m_remotePort;
	public int m_socketID;
	public int m_statusCode = 200;
	public String m_url = "";
}
