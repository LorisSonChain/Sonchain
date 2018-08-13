package sonchain.blockchain.service;

import sonchain.blockchain.base.CFileA;
import sonchain.blockchain.base.CStrA;
import sonchain.blockchain.base.HttpEasyService;
import sonchain.blockchain.base.NodeService;

import java.io.File;
import java.math.BigInteger;
import java.security.SignatureException;
import java.util.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.log4j.Logger;
import org.bouncycastle.math.ec.ECPoint;
import org.bouncycastle.util.BigIntegers;
import org.bouncycastle.util.encoders.Hex;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import sonchain.blockchain.config.BlockChainConfig;
import sonchain.blockchain.consensus.NodeManager;
import sonchain.blockchain.consensus.SonChainProducerNode;
import sonchain.blockchain.core.BlockChainImpl;
import sonchain.blockchain.core.Genesis;
import sonchain.blockchain.core.Transaction;
import sonchain.blockchain.crypto.ECKey;
import sonchain.blockchain.crypto.ECKey.ECDSASignature;
import sonchain.blockchain.crypto.HashUtil;
import sonchain.blockchain.data.SonChainHostInfo;
import sonchain.blockchain.facade.SonChainImpl;
import sonchain.blockchain.manager.BlockLoader;
import sonchain.blockchain.peer.*;
import sonchain.blockchain.plugins.wallet.WalletManager;
import owchart.owlib.Base.CStr;
import owchart.owlib.Base.RefObject;

/**
 *
 */
public class DataCenter
{	
	public static final Logger m_logger = Logger.getLogger(DataCenter.class);
	public static BlockChainConfig m_config = new BlockChainConfig();

	private static HashMap<String, HttpEasyService> m_httpEasyServices = new HashMap<String, HttpEasyService>();
	public static HashMap<String, HttpEasyService> GetHttpGetServices() {
		return m_httpEasyServices;
	}
	
    private static NodeManager m_nodeManager = new NodeManager();    
    public static NodeManager getNodeManager(){
    	return m_nodeManager;
    }

	private static boolean m_isAppAlive = true;
	public static boolean IsAppAlive() {
		return DataCenter.m_isAppAlive;
	}

	public static void SetAppAlive(boolean value) {
		DataCenter.m_isAppAlive = value;
	}

	public static int GetsonchainRequestID()
	{
		return 9999;
	}

	private static boolean m_isFull = false;
	public static boolean IsFull()
	{
		return m_isFull;
	}

	public static String GetAppPath()
	{
		return System.getProperty("user.dir");
	}

	public static String GetUserPath()
	{
		return System.getProperty("user.dir");
	}

	//private static NeighborService m_neighborService;

	private static NodeService m_nodeService;
	public static NodeService GetNodeService() {
		return DataCenter.m_nodeService;
	}
	
	private static SonChainImpl m_sonChainImpl;
	public static SonChainImpl getSonChainImpl()
	{
		return m_sonChainImpl;
	}
	
	private static SonChainProducerNode[] m_standbyPeerNodes = null; 
	public static SonChainProducerNode[] GetStandbyPeerNodes()
	{
		return m_standbyPeerNodes;
	}
	
	private static List<SonChainProducerNode> m_remotePeerNodes = null; 
	public static List<SonChainProducerNode> GetRemotePeerNodes()
	{
		return m_remotePeerNodes;
	}
	
	protected static void LoadSonChainConfig()
	{
		try
		{
			String filePath = GetAppPath() + "//SonChainConfig.xml";
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document xmldoc = (Document) builder.parse(new File(filePath));
			Element root = xmldoc.getDocumentElement();
			NodeList nodeList = root.getChildNodes();
			int length = nodeList.getLength();
			for (int i = 0; i < length; i++)
			{

				Node node = nodeList.item(i);
				String nodeName = node.getNodeName();
				if (nodeName.equals("clearcache")){
					m_config.m_clearCache = CStrA.ConvertStrToBoolean(node.getTextContent());
				}
				else if(nodeName.equals("m_databaseVersion")){
					m_config.m_databaseVersion = node.getTextContent();
				}
				else if(nodeName.equals("m_projectVersion")){
					m_config.m_projectVersion = node.getTextContent();
				}
				else if(nodeName.equals("m_datebaseDir")){
					m_config.m_datebaseDir = node.getTextContent();
				}
				else if(nodeName.equals("m_nodewalletaddress")){
					m_config.m_nodeWalletAddress = node.getTextContent();
				}
				else if(nodeName.equals("m_genesisfilepath")){
					m_config.m_genesisFilePath = node.getTextContent();
				}
				else if(nodeName.equals("m_blockchainconfigname")){
					m_config.m_blockchainConfigName = node.getTextContent();
				}
				else if(nodeName.equals("m_roothashstart")){
					m_config.m_rootHashStart = node.getTextContent();
				}
				else if (nodeName.equals("m_transactionapprovetimeout")){
					m_config.m_transactionApproveTimeout = CStrA
							.ConvertStrToInt(node.getTextContent());
				}
				else if(nodeName.equals("m_cryptoprovidername")){
					m_config.m_cryptoProviderName = node.getTextContent();
				}
				else if(nodeName.equals("m_cryptohashalg256")){
					m_config.m_cryptoHashAlg256 = node.getTextContent();
				}
				else if(nodeName.equals("m_cryptohashalg512")){
					m_config.m_cryptoHashAlg512 = node.getTextContent();
				}
				else if(nodeName.equals("m_keyvalueDatasource")){
					m_config.m_keyvalueDatasource = node.getTextContent();
				}
				else if (nodeName.equals("m_cachemaxstatebloomsize")){
					m_config.m_cacheMaxStateBloomSize = CStrA
							.ConvertStrToInt(node.getTextContent());
				}
				else if (nodeName.equals("m_recordblocks")){
					m_config.m_recordBlocks = CStrA.ConvertStrToBoolean(node.getTextContent());
				}
				else if(nodeName.equals("m_dumpdir")){
					m_config.m_dumpDir = node.getTextContent();
				}
				else if (nodeName.equals("m_transactionoutdatedthreshold")){
					m_config.m_transactionOutdatedThreshold = CStrA
							.ConvertStrToInt(node.getTextContent());
				}
				else if (nodeName.equals("m_cacheflushwritecachesize")){
					m_config.m_cacheFlushWriteCacheSize = CStrA
							.ConvertStrToInt(node.getTextContent());
				}
				else if (nodeName.equals("m_cacheFlushBlocks")){
					m_config.m_cacheFlushBlocks = CStrA
							.ConvertStrToInt(node.getTextContent());
				}
				else if (nodeName.equals("m_cacheflushshortsyncflush")){
					m_config.m_cacheFlushShortSyncFlush 
						= CStrA.ConvertStrToBoolean(node.getTextContent());
				}
				else if (nodeName.equals("m_cachestatecachesize")){
					m_config.m_cacheStateCacheSize = CStrA
							.ConvertStrToInt(node.getTextContent());
				}
				else if (nodeName.equals("m_cachestatecachesize")){
					m_config.m_cacheStateCacheSize = CStrA
							.ConvertStrToInt(node.getTextContent());
				}
				else if (nodeName.equals("m_blocksloader")){
					m_config.m_blocksLoader = node.getTextContent();
				}
				else if (nodeName.equals("m_blocksformat")){
					m_config.m_blocksFormat = node.getTextContent();
				}
				else if (nodeName.equals("m_nodewalletaddress")){
					m_config.m_nodeWalletAddress = node.getTextContent();
				}
				else if (nodeName.equals("m_databasereset")){
					m_config.m_databaseReset 
						= CStrA.ConvertStrToBoolean(node.getTextContent());
				}
				else if (nodeName.equals("m_databaseresetblock")){
					m_config.m_databaseResetBlock = CStrA
							.ConvertStrToInt(node.getTextContent());
				}
				else if (nodeName.equals("m_cacheblockqueuesize")){
					m_config.m_cacheBlockQueueSize = CStrA
							.ConvertStrToInt(node.getTextContent());
				}
				else if (nodeName.equals("m_syncfastenabled")){
					m_config.m_syncFastEnabled 
						= CStrA.ConvertStrToBoolean(node.getTextContent());
				}
				else if (nodeName.equals("m_syncenabled")){
					m_config.m_syncEnabled 
						= CStrA.ConvertStrToBoolean(node.getTextContent());
				}
				else if (nodeName.equals("m_syncfastpivotblockhash")){
					m_config.m_syncFastPivotBlockHash = node.getTextContent();
				}
				else if (nodeName.equals("m_startExecutorDate")){
					m_config.m_startExecutorDate = node.getTextContent();
				}
				else if (nodeName.equals("m_localhost")){
					m_config.m_localHost = node.getTextContent();
				}
				else if (nodeName.equals("m_localport")){
					m_config.m_localPort = CStrA.ConvertStrToInt(node.getTextContent());
				}
				else if (nodeName.equals("m_localnodeprivatekey")){
					m_config.m_localNodePrivateKey = node.getTextContent();
				}
				else if (nodeName.equals("m_maxtransactionperblock")){
					m_config.m_maxTransactionsPerBlock = CStrA.ConvertStrToInt(node.getTextContent());
				}
				else if (nodeName.equals("m_system_account_name")){
					m_config.m_systemAccountName = node.getTextContent();
				}
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	protected static void LoadStandbyPeerInfos(){

		try
		{
			String filePath = GetAppPath() + "//StandbyPeerInfos.txt";
			RefObject<String> refContent = new RefObject<String>("");
			String content = "";
			CFileA.Read(filePath, refContent);
			content = refContent.argvalue;
			if(content == null || content.length() == 0){
				return;
			}
			ArrayList<String> retLines = new ArrayList<String> ();
			CStrA.Split(retLines, content, "\r\n", false);
			if(retLines == null || retLines.size() == 0){
				return;
			}
			int size = retLines.size();
			m_standbyPeerNodes = new SonChainProducerNode[size];
			for(int i = 0; i < size; i ++){
				SonChainProducerNode peerInfo = SonChainProducerNode.fromString(retLines.get(i));
				m_standbyPeerNodes[i] = peerInfo;
				if(!peerInfo.getHost().equals(m_config.m_localHost)){
					m_remotePeerNodes.add(peerInfo);
				}					
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	/**
	 * 启动服务
	 * @param fileName
	 */
	public static void StartService(String fileName)
	{
		try {
			LoadSonChainConfig();
			LoadStandbyPeerInfos();		
			m_logger.debug("sonchain version:" + m_config.m_projectVersion);
			/**
			//BlockChain1.LoadBlockChain();
			String content = "";
			RefObject<String> refContent = new RefObject<String>(content);
			if (m_config.m_clearCache)
			{
				CFileA.RemoveFile(DataCenter.GetUserPath() + "\\fullservers.txt");
			}
			Random rd = new Random();
			m_isFull = m_config.m_isFull;
			if (!m_isFull)
			{
				m_serversonchainService.SetPort(10000 + rd.nextInt() % 10000);
			}
			m_httpEasyServices.put("blockwalletservice", new BlockWalletService());
			BaseServiceSV.AddService(m_serversonchainService);
			int socketIDServer = BaseServiceSV.StartServer(m_serversonchainService.GetPort());
			m_serversonchainService.SetSocketID(socketIDServer);
			m_neighborService = new NeighborService();
			m_neighborService.SetSocketID(socketIDServer);
			String fullServersPath = DataCenter.GetAppPath() + "\\fullservers.txt";
			ArrayList<SonChainHostInfo> hostInfos = new ArrayList<SonChainHostInfo>();
			if (CFileA.IsFileExist(fullServersPath))
			{
				CFileA.Read(fullServersPath, refContent);
				if (content.length() > 0)
				{
					String[] subStrs = content.split("[;]");
					int subStrsSize = subStrs.length;
					for (int s = 0; s < subStrsSize; s++)
					{
						SonChainHostInfo gsi = new SonChainHostInfo();
						String[] sunStrs = subStrs[s].split("[:]");
						gsi.m_ip = sunStrs[0];
						gsi.m_serverPort = CStr.ConvertStrToInt(sunStrs[1]);
						gsi.m_type = 1;
						hostInfos.add(gsi);
					}
				}
			}
			else
			{
				if (DataCenter.m_config.m_defaultHost.length() > 0)
				{
					SonChainHostInfo defaultHostInfo = new SonChainHostInfo();
					defaultHostInfo.m_ip = DataCenter.m_config.m_defaultHost;
					defaultHostInfo.m_serverPort = DataCenter.m_config.m_defaultPort;
					hostInfos.add(defaultHostInfo);
				}
			}
			int hostInfosSize = hostInfos.size();
			if (DataCenter.IsFull() && hostInfosSize == 0)
			{
				SonChainHostInfo defaultHostInfo = new SonChainHostInfo();
				defaultHostInfo.m_ip = "127.0.0.1";
				defaultHostInfo.m_serverPort = 16666;
				hostInfos.add(defaultHostInfo);
			}
			if (hostInfosSize > 0)
			{
				while (true)
				{
					SonChainHostInfo hostInfo = hostInfos.get(rd.nextInt() % hostInfosSize);
					m_socketID = BaseServiceCT.Connect(hostInfo.m_ip, hostInfo.m_serverPort);
					if (m_socketID != -1)
					{
						String key = hostInfo.toString();
						SonChainServiceCT clientsonchainService = new SonChainServiceCT();
						clientsonchainService.setSonChainHostInfo(hostInfo);
						DataCenter.m_clientsonchainServices.put(key, clientsonchainService);
						BaseServiceCT.AddService(clientsonchainService);
						clientsonchainService.SetToServer(true);
						clientsonchainService.SetConnected(true);
						clientsonchainService.SetSocketID(m_socketID);
						clientsonchainService.Enter();
						clientsonchainService.SyncBlockChain();
						return;
					}
				}
			}**/
			m_nodeService = new NodeService(fileName);
			m_nodeService.SetPort(m_config.m_localPort);
			m_sonChainImpl = new SonChainImpl();
			m_nodeService.Start();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
}
