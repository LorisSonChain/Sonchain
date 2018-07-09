package sonchain.blockchain.base;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import sonchain.blockchain.script.CFunctionEx;
import sonchain.blockchain.script.CFunctionHttp;
import sonchain.blockchain.service.DataCenter;
import owchart.owlib.Base.CStr;
import owchart.owlib.Base.INativeBase;
import owchart.owlib.Base.NativeBase;
import owchart.owlib.Base.RefObject;
import owchart.owlib.Chart.CFunction;
import owchart.owlib.Chart.CIndicator;

/**
 * NodeService
 */
public class NodeService
{
	class SocketHandler implements Runnable
	{
		public Socket m_socket;

		@Override
		public void run()
		{
			readData(m_socket);
		}
	}

	public static byte[] ReadEnough(InputStream in, int len)
	{
		byte[] byts = new byte[len];
		int rLen = 0;
		while (rLen < len && rLen != -1)
		{
			try
			{
				rLen += in.read(byts, rLen, len - rLen);
				if (in.available() == 0)
				{
					break;
				}
			}
			catch (Exception e)
			{
				break;
			}
		}
		return byts;
	}

	private static void readData(Socket socket)
	{
		NodeService nodeService = DataCenter.GetNodeService();
		int newSocketID = socket.hashCode();
		try
		{
			DataInputStream sReader = new DataInputStream(socket.getInputStream());
			byte[] recvDatas = new byte[102400];
			int len = sReader.read(recvDatas);
			ByteArrayInputStream byteArray = new ByteArrayInputStream(recvDatas);
			BufferedReader reader = new BufferedReader(new InputStreamReader(byteArray));
			HttpData data = new HttpData();
			String requestHeader;
			data.m_remoteIP = socket.getInetAddress().getHostAddress();
			data.m_remotePort = socket.getPort();

			int contentLength = 0;
			String parameters = "";
			while ((requestHeader = reader.readLine()) != null && !requestHeader.isEmpty())
			{
				if (requestHeader.indexOf("GET") == 0)
				{
					int end = requestHeader.indexOf("HTTP/");
					data.m_method = "GET";
					parameters = requestHeader.substring(5, end - 1);
				}
				else if (requestHeader.indexOf("POST") == 0)
				{
					int end = requestHeader.indexOf("HTTP/");
					data.m_method = "POST";
					parameters = requestHeader.substring(5, end - 1);
				}
				else if (requestHeader.indexOf("Accept: ") == 0)
				{
					try
					{
						data.m_contentType = requestHeader.substring(8, requestHeader.indexOf(','));
					}
					catch (Exception ex)
					{
					}
				}
				else if (requestHeader.indexOf("Host:") == 0)
				{
					data.m_url = requestHeader.substring(requestHeader.indexOf(':') + 2);
				}
				else if (requestHeader.indexOf("Content-Length") == 0)
				{
					int begin = requestHeader.indexOf("Content-Length:") + "Content-Length:".length();
					String postParamterLength = requestHeader.substring(begin).trim();
					contentLength = Integer.parseInt(postParamterLength);
				}
			}
			if (contentLength > 0)
			{
				data.m_contentType = "POST";
				data.m_body = new byte[contentLength];
				int idx = 0, ide = 0;
				while (idx < contentLength)
				{
					int recvData = reader.read();
					if (recvData != -1)
					{
						if (recvData != 0)
						{
							ide++;
						}
						idx++;
					}
					else
					{
						break;
					}
				}
				reader.close();
				byteArray.close();
				if (ide == 0)
				{
					sReader.read(data.m_body);
				}
				else
				{
					for (int i = 0; i < contentLength; i++)
					{
						data.m_body[i] = recvDatas[len - contentLength + i];
					}
				}
				data.m_contentLength = contentLength;
			}
			else
			{
				reader.close();
				byteArray.close();
				// memoryStream.Dispose();
			}
			if (data.m_method.length() == 0)
			{
				sReader.close();
				return;
			}
			int cindex = parameters.indexOf('?');
			if (cindex != -1)
			{
				data.m_url = data.m_url + "/" + parameters;
				parameters = parameters.substring(cindex + 1);
				String[] strs = parameters.split("[&]");
				int strsSize = strs.length;
				for (int i = 0; i < strsSize; i++)
				{
					String[] subStrs = strs[i].split("[=]");
					int subSize = subStrs.length;
					if (subSize > 1)
					{
						data.m_parameters.put(subStrs[0].toLowerCase(), URLDecoder.decode(subStrs[1], "UTF-8"));
					}
				}
			}
			else
			{
				data.m_url += "/" + parameters;
			}
			String isGizpBody = data.m_parameters.get("gzip");
			if (isGizpBody != null && isGizpBody.equals("true"))
			{
				data.m_body = CStrA.UnGZip(data.m_body);
			}
			CIndicator indicator = null;
			if (nodeService.UseScript())
			{
				try
				{
					synchronized (nodeService.m_indicators)
					{
						indicator = nodeService.m_indicators.pop();
					}
				}
				catch (java.lang.Exception e)
				{
					indicator = CFunctionEx.CreateIndicator(nodeService.m_script, nodeService.m_native);
				}
				java.util.ArrayList<CFunction> functions = indicator.GetFunctions();
				int functionsSize = functions.size();
				for (int i = 0; i < functionsSize; i++)
				{
					CFunctionHttp function = (CFunctionHttp) ((functions.get(i) instanceof CFunctionHttp)
							? functions.get(i)
							: null);
					if (function != null)
					{
						function.m_data = data;
					}
				}
			}
			data.m_socketID = newSocketID;
			synchronized (nodeService.m_httpDatas)
			{
				nodeService.m_httpDatas.put(newSocketID, data);
			}
			if (indicator != null)
			{
				indicator.CallFunction("ONHTTPREQUEST();");
			}
			if (data.m_close)
			{
				sReader.close();
				return;
			}
			int resContentLength = 0;
			if (data.m_resBytes != null)
			{
				resContentLength = data.m_resBytes.length;
			}
			else
			{
				resContentLength = data.m_resStr.getBytes(Charset.forName("GB2312")).length;
			}
			StringBuilder bld = new StringBuilder();
			bld.append("HTTP/1.0 " + CStr.ConvertIntToStr(data.m_statusCode) + " OK\r\n");
			bld.append(String.format("Content-Length: %d\r\n", resContentLength));
			bld.append("Connection: close\r\n\r\n");
			if (data.m_resBytes != null)
			{
				DataOutputStream writer = new DataOutputStream(socket.getOutputStream());
				writer.write(bld.toString().getBytes());
				writer.write(data.m_resBytes);
				writer.flush();
				writer.close();
			}
			else if (data.m_resStr != null && !data.m_resStr.isEmpty())
			{

				DataOutputStream writer = new DataOutputStream(socket.getOutputStream());
				writer.write(bld.toString().getBytes());
				byte[] resBytes = data.m_resStr.getBytes(Charset.forName("GB2312"));
				writer.write(resBytes);
				writer.flush();
				writer.close();
			}
			if (indicator != null)
			{
				synchronized (nodeService.m_indicators)
				{
					nodeService.m_indicators.push(indicator);
				}
			}
			sReader.close();
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
		finally
		{
			synchronized (nodeService.m_httpDatas)
			{
				nodeService.m_httpDatas.remove(newSocketID);
			}
			try
			{
				socket.close();
			}
			catch (Exception ex)
			{
				ex.printStackTrace();
			}
		}
	}

	private String m_fileName;

	public java.util.HashMap<Integer, HttpData> m_httpDatas = new java.util.HashMap<Integer, HttpData>();

	protected java.util.Stack<CIndicator> m_indicators = new java.util.Stack<CIndicator>();

	protected CIndicator m_indicator;

	protected ServerSocket m_listener;

	protected int m_port = 8080;

	protected String m_script;

	protected INativeBase m_native;

	protected boolean m_useScript;

	public NodeService(String fileName)
	{
		m_fileName = fileName;
	}

	public final void CheckScript()
	{
		String newScript = "";
		RefObject<String> tempRef_newScript = new RefObject<String>(newScript);
		CFileA.Read(m_fileName, tempRef_newScript);
		newScript = tempRef_newScript.argvalue;
		if (!m_script.equals(newScript))
		{
			System.out.println("Script is changed.....");
			m_script = newScript;
			synchronized (m_indicators)
			{
				while (true)
				{
					try
					{
						CIndicator indicator = m_indicators.pop();
						indicator.Clear();
					}
					catch (java.lang.Exception e)
					{
						break;
					}
				}
			}
		}
	}

	public final CIndicator GetIndicator()
	{
		return m_indicator;
	}
	
	public final ServerSocket GetListener()
	{
		return m_listener;
	}
	
	public final INativeBase GetNative()
	{
		return m_native;
	}

	public final int GetPort()
	{
		return m_port;
	}
	
	public final String GetScript()
	{
		return m_script;
	}

	public final void SetPort(int port)
	{
		m_port = port;
	}

	public int Start()
	{
		m_useScript = CFileA.IsFileExist(m_fileName);
		if (m_useScript)
		{
			m_native = new NativeBase();
			RefObject<String> tempRef_m_script = new RefObject<String>(m_script);
			CFileA.Read(m_fileName, tempRef_m_script);
			m_script = tempRef_m_script.argvalue;
			m_indicator = CFunctionEx.CreateIndicator(m_script, m_native);
		}
		try
		{
			if (m_indicator != null)
			{
				m_indicator.CallFunction("ONHTTPSERVERSTARTING('" + m_fileName + "');");
			}
			m_listener = new ServerSocket(m_port);
			if (m_indicator != null)
			{
				m_indicator.CallFunction("ONHTTPSERVERSTART(0,0);");
			}
			ExecutorService service = Executors.newCachedThreadPool();
			while (DataCenter.IsAppAlive())
			{
				Socket socket = m_listener.accept();
				try
				{
					SocketHandler receiver = new SocketHandler();
					receiver.m_socket = socket;
					service.execute(receiver);
				}
				catch (Exception e)
				{
					try
					{
						socket.close();
					}
					catch (Exception e2)
					{
						e2.printStackTrace();
					}
				}
			}
			service.shutdown();
			service.awaitTermination(10, TimeUnit.SECONDS);
			return 1;
		}
		catch (Exception ex)
		{
			if (m_indicator != null)
			{
				m_indicator.CallFunction("ONHTTPSERVERSTARTFAIL('" + ex.getMessage() + "');");
			}
		}
		return -1;
	}
	
	public final boolean UseScript()
	{
		return m_useScript;
	}
}
