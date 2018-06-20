/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sonchain.blockchain.server;

import java.io.IOException;
import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.codec.digest.DigestUtils;

import sonchain.blockchain.base.Binary;
import sonchain.blockchain.base.CMessage;
import sonchain.blockchain.client.BaseServiceCT;
import sonchain.blockchain.client.SonChainServiceCT;
import sonchain.blockchain.core.Block;
import sonchain.blockchain.core.BlockHeader;
import sonchain.blockchain.core.BlockStatus;
import sonchain.blockchain.core.Transaction;
import sonchain.blockchain.core.TransactionType;
import sonchain.blockchain.data.SonChainData;
import sonchain.blockchain.data.SonChainHostInfo;
import sonchain.blockchain.pow.ProofOfWork;
import sonchain.blockchain.redis.RedisSeq;
import sonchain.blockchain.service.DataCenter;
import owchart.owlib.Base.CStr;
import owchart.owlib.Base.RefObject;

/**
 *
 * @author GAIA_Todd
 */
public class SonChainServiceSV extends BaseServiceSV
{
	private static SimpleDateFormat TimeStampFormatForParse = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

	public SonChainServiceSV()
	{
		SetServiceID(SERVICEID_sonchain);
	}

	private final int SERVICEID_sonchain = 10000;

	public final int FUNCTIONID_GETHOSTS = 1;

	public final int FUNCTIONID_sonchain_SENDALL = 2;

	public final int FUNCTIONID_sonchain_SEND = 4;

	public final int FUNCTIONID_sonchain_ENTER = 6;

	public final int FUNCTIONID_sonchain_PING = 7;

	/**
	 * 接收交易开始计算POW消息
	 */
	public final int FUNCTIONID_RECEIVE_CALCULATE_POW = 11;
	/**
	 * 计算POW结束消息
	 */
	public final int FUNCTIONID_CALCULATE_POW_COMPLETE = 12;
	/**
	 * 计算POW结果确认消息
	 */
	public final int FUNCTIONID_CALCULATE_POW_COMPLETE_CONFIRM = 13;
	/**
	 * 同步区块链请求
	 */
	public final int FUNCTIONID_SYNC_BLOCK_CHAIN_REQ = 14;
	/**
	 * 同步区块链响应
	 */
	public final int FUNCTIONID_SYNC_BLOCK_CHAIN_RTN = 15;
	
    public final int FUNCTIONID_STATUS = 20;
    public final int FUNCTIONID_NEW_BLOCK_HASHES = 21;
    public final int FUNCTIONID_TRANSACTIONS = 22;
    public final int FUNCTIONID_GET_BLOCK_HEADERS = 23;
    public final int FUNCTIONID_BLOCK_HEADERS = 24;
    public final int FUNCTIONID_GET_BLOCK_BODIES = 25;
    public final int FUNCTIONID_BLOCK_BODIES = 26;
    public final int FUNCTIONID_NEW_BLOCK = 27;

	private ArrayList<SonChainHostInfo> m_serverHosts = new ArrayList<SonChainHostInfo>();

	public HashMap<Integer, SonChainHostInfo> m_socketIDs = new HashMap<Integer, SonChainHostInfo>();

	private int m_port = 16666;

	public int GetPort()
	{
		return m_port;
	}

	public void SetPort(int port)
	{
		m_port = port;
	}

	public void AddServerHosts(SonChainHostInfo hostInfo)
	{
		synchronized (m_serverHosts)
		{
			int serverHostsSize = m_serverHosts.size();
			boolean contains = false;
			for (int i = 0; i < serverHostsSize; i++)
			{
				SonChainHostInfo oldHostInfo = m_serverHosts.get(i);
				if (oldHostInfo.m_ip.equals(hostInfo.m_ip) && oldHostInfo.m_serverPort == hostInfo.m_serverPort)
				{
					contains = true;
					break;
				}
			}
			if (!contains)
			{
				m_serverHosts.add(hostInfo);
			}
		}
	}

	public int Enter(CMessage message)
	{
		int rtnSocketID = message.m_socketID;
		int port = 0, type = 0;
		String ip = "";
		try
		{
			Binary br = new Binary();
			br.Write(message.m_body, message.m_bodyLength);
			port = br.ReadInt();
			type = br.ReadInt();
			br.Close();
		}
		catch (Exception ex)
		{

		}
		ArrayList<Integer> sendSocketIDs = new ArrayList<Integer>();
		ArrayList<SonChainHostInfo> hostInfos = new ArrayList<SonChainHostInfo>();
		synchronized (m_socketIDs)
		{
			if (m_socketIDs.containsKey(rtnSocketID))
			{
				ip = m_socketIDs.get(rtnSocketID).m_ip;
				m_socketIDs.get(rtnSocketID).m_serverPort = port;
				m_socketIDs.get(rtnSocketID).m_type = type;
				hostInfos.add(m_socketIDs.get(message.m_socketID));
				for (int socketID : m_socketIDs.keySet())
				{
					if (socketID != rtnSocketID)
					{
						SonChainHostInfo gs = m_socketIDs.get(socketID);
						if (gs.m_type == 0)
						{
							sendSocketIDs.add(socketID);
						}
						else if (gs.m_type == 1)
						{
							if (type == 1)
							{
								sendSocketIDs.add(socketID);
							}
						}
					}
				}
			}
		}
		int sendSocketIDsSize = sendSocketIDs.size();
		if (sendSocketIDsSize > 0)
		{
			SendHostInfos(sendSocketIDs, 1, hostInfos);
		}
		HashMap<String, SonChainHostInfo> allHostInfos = new HashMap<String, SonChainHostInfo>();
		synchronized (m_socketIDs)
		{
			for (int sid : m_socketIDs.keySet())
			{
				if (sid != rtnSocketID)
				{
					allHostInfos.put(m_socketIDs.get(sid).toString(), m_socketIDs.get(sid));
				}
			}
		}
		// 发送本地IP地址
		if (DataCenter.m_config.m_localHost.length() > 0)
		{
			SonChainHostInfo localHostInfo = new SonChainHostInfo();
			localHostInfo.m_ip = DataCenter.m_config.m_localHost;
			localHostInfo.m_serverPort = DataCenter.m_config.m_localPort;
			localHostInfo.m_type = 1;
			allHostInfos.put(localHostInfo.toString(), localHostInfo);
		}
		synchronized (m_serverHosts)
		{
			for (SonChainHostInfo serverHost : m_serverHosts)
			{
				allHostInfos.put(serverHost.toString(), serverHost);
			}
		}
		ArrayList<Integer> rtnSocketIDs = new ArrayList<Integer>();
		rtnSocketIDs.add(rtnSocketID);
		ArrayList<SonChainHostInfo> sendAllHosts = new ArrayList<SonChainHostInfo>();
		for (String key : allHostInfos.keySet())
		{
			sendAllHosts.add(allHostInfos.get(key));
		}
		SendHostInfos(rtnSocketIDs, 0, sendAllHosts);
		sendAllHosts.clear();
		rtnSocketIDs.clear();
		hostInfos.clear();
		sendSocketIDs.clear();
		if (DataCenter.IsFull() && type == 1)
		{
			if (DataCenter.m_clientsonchainServices.size() == 0)
			{
				int socketID = BaseServiceCT.Connect(ip, port);
				if (socketID != -1)
				{
					String key = ip + ":" + CStr.ConvertIntToStr(port);
					SonChainServiceCT clientsonchainService = new SonChainServiceCT();
					DataCenter.m_clientsonchainServices.put(key, clientsonchainService);
					BaseServiceCT.AddService(clientsonchainService);
					clientsonchainService.SetToServer(true);
					clientsonchainService.SetConnected(true);
					clientsonchainService.SetSocketID(socketID);
					clientsonchainService.Enter();
				}
			}
		}
		return 0;
	}

	public int GetsonchainDatas(ArrayList<SonChainData> datas, RefObject<String> ips, byte[] body, int bodyLength)
	{
		try
		{
			Binary br = new Binary();
			br.Write(body, bodyLength);
			ips.argvalue = br.ReadString();
			int sonchainSize = br.ReadInt();
			for (int i = 0; i < sonchainSize; i++)
			{
				SonChainData data = new SonChainData();
				data.m_type = br.ReadInt();
				if (data.m_type == 0)
				{
					data.m_text = br.ReadString();
				}
				else if (data.m_type == 1)
				{
					int len = br.ReadInt();
					List<Transaction> trans = new ArrayList<Transaction>();
					for (int j = 0; j < len; j++)
					{
						Transaction tran = new Transaction();
						//tran.m_type = TransactionType.ContractCreation;
						//tran.m_value = BigInteger.valueOf((long)br.ReadDouble());
						//tran.m_fee = BigInteger.valueOf((long)br.ReadDouble());
						//tran.m_remark = br.ReadString();
						//tran.SetHash(tran.GetTxHash());
						//System.out.println(" receive transaction remark :" + tran.m_remark);
						trans.add(tran);
					}
					data.m_trans = trans;
				}
				else
				{
					data.m_preBlockHash = br.ReadString();
					data.m_blockJson = br.ReadString();
				}
				datas.add(data);
			}
			br.Close();
		}
		catch (Exception ex)
		{

		}
		return 1;
	}

	/**
	 * 挖矿结束处理
	 * 
	 * @param message
	 * @return
	 */
	public int MineBlockOver(CMessage message, String preBlockHash, String blockJson)
	{
		try
		{
			System.out
					.println("MineBlockOver begin:" + TimeStampFormatForParse.format(Calendar.getInstance().getTime()));
			Block block = Block.toData(blockJson);
			//ProofOfWork pow = ProofOfWork.newProofOfWork((BlockHeader) block);
//			boolean isValidate = pow.validate();
//			System.out.println("Pow valid: " + pow.validate() + "\n");
//			if (isValidate)
//			{
//				BlockStatus status = DataCenter.m_blockCache.GetBlockStatus(preBlockHash);
//				System.out.println("Block status: " + status.toString() + "\n");
//				// 保存区块和状态
//				if (status == BlockStatus.Mining)
//				{
//					System.out.println("收到挖矿结束消息处理 保存");
//					System.out.println("preBlockHash:" + preBlockHash);
//					System.out.println("blockJson:" + blockJson);
//					DataCenter.m_blockCache.SetBlockStatus(preBlockHash, BlockStatus.Mined);
//					DataCenter.GetBlockChain().AddBlock(block);
//					RedisSeq.SetBlockChain(preBlockHash, blockJson);
//					System.out.println("The size of block chian :" + DataCenter.GetBlockChain().GetSize());
//					sonchainData data = new sonchainData();
//					data.m_type = 3;
//					data.m_preBlockHash = preBlockHash;
//					data.m_blockJson = blockJson;
//
//					MineBlockConfirm(message, data);
//					// 通知所有节点
//					// SendAll(data);
//				}
//			}
		}
		catch (Exception ex)
		{

		}
		return 1;
	}

	/**
	 * 处理同步区块链请求
	 * 
	 * @param message
	 * @return
	 */
	public int MineBlockConfirm(CMessage message, SonChainData data)
	{
		try
		{
			Binary bw = new Binary();
			bw.WriteInt(1);
			bw.WriteInt(data.m_type);
			if (data.m_type == 0)
			{
				bw.WriteString(data.m_text);
			}
			else if (data.m_type == 1)
			{
				List<Transaction> trans = data.m_trans;
				int len = trans.size();
				bw.WriteInt(len);
				for (int j = 0; j < len; j++)
				{
					Transaction tran = new Transaction();
					//bw.WriteDouble(tran.m_value.doubleValue());
					//bw.WriteDouble(tran.m_fee.doubleValue());
					//bw.WriteString(tran.m_remark);
				}
			}
			else
			{
				bw.WriteString(data.m_preBlockHash);
				bw.WriteString(data.m_blockJson);
			}
			byte[] bytes = bw.GetBytes();
			int ret = 0;
			CMessage newMessage = new CMessage(GetGroupID(), GetServiceID(), FUNCTIONID_CALCULATE_POW_COMPLETE_CONFIRM,
					message.m_sessionID, message.m_requestID, DataCenter.m_socketID, 0, GetCompressType(), bytes.length,
					bytes);
			for (SonChainServiceCT ct : DataCenter.m_clientsonchainServices.values())
			{
				ret = ct.Send(newMessage);
			}

			synchronized (m_socketIDs)
			{
				for (int sockID : m_socketIDs.keySet())
				{
					newMessage.m_socketID = sockID;
					Send(newMessage);
				}
			}
			bw.Close();
			return ret;
		}
		catch (Exception ex)
		{
			return 0;
		}
	}

	/**
	 * 挖矿确认
	 * 
	 * @param preBlockHash
	 * @param blockJson
	 * @return
	 */
	public int MineBlockConfirm(String preBlockHash, String blockJson)
	{
		try
		{
			Block block = Block.toData(blockJson);
			System.out.println("挖矿确认成功 保存");
			//DataCenter.GetBlockChain().AddBlock(block);
			RedisSeq.SetBlockChain(preBlockHash, blockJson);
			//System.out.println("The size of block chian :" + DataCenter.GetBlockChain().GetSize());
		}
		catch (Exception ex)
		{

		}
		return 1;
	}

	/**
	 * 挖矿结束处理
	 * 
	 * @param message
	 * @return
	 */
	public int MineBlockOver(CMessage message)
	{
		try
		{
			SendAll(message);
		}
		catch (Exception ex)
		{

		}
		return 1;
	}

	@Override
	public void OnClientClose(int socketID, int localSID)
	{
		super.OnClientClose(socketID, localSID);
		ArrayList<SonChainHostInfo> removeHostInfos = new ArrayList<SonChainHostInfo>();
		ArrayList<Integer> sendSocketIDs = new ArrayList<Integer>();
		synchronized (m_socketIDs)
		{
			ArrayList<Integer> removeSocketIDs = new ArrayList<Integer>();
			for (int sid : m_socketIDs.keySet())
			{
				if (sid == socketID)
				{
					removeHostInfos.add(m_socketIDs.get(sid));
					removeSocketIDs.add(sid);
				}
			}
			int removeSocketIDsSize = removeSocketIDs.size();
			for (int i = 0; i < removeSocketIDsSize; i++)
			{
				m_socketIDs.remove(removeSocketIDs.get(i));
			}
			for (int sid : m_socketIDs.keySet())
			{
				sendSocketIDs.add(sid);
			}
			removeSocketIDs.clear();
		}
		int sendSocketIDsSize = sendSocketIDs.size();
		if (sendSocketIDsSize > 0)
		{
			SendHostInfos(sendSocketIDs, 2, removeHostInfos);
		}
		sendSocketIDs.clear();
		removeHostInfos.clear();
	}

	@Override
	public void OnClientConnect(int socketID, int localSID, String ip)
	{
		super.OnClientConnect(socketID, localSID, ip);
		synchronized (m_socketIDs)
		{
			if (!m_socketIDs.containsKey(socketID))
			{
				m_socketIDs.put(socketID, new SonChainHostInfo());
				String strIPPort = ip.replace("accept:", "");
				String[] strs = strIPPort.split("[:]");
				m_socketIDs.get(socketID).m_ip = strs[0];
			}
		}
		CMessage mes = new CMessage();
		mes.m_functionID = 7;
		mes.m_requestID = BaseServiceSV.GetRequestID();
		mes.m_socketID = socketID;
		Binary bi = new Binary();
		try
		{
			bi.WriteDouble(System.currentTimeMillis());
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		byte[] byts = bi.GetBytes();
		mes.m_body = byts;
		mes.m_bodyLength = byts.length;
		Send(mes);
	}

	@Override
	public void OnReceive(CMessage message)
	{
		super.OnReceive(message);
		switch (message.m_functionID)
		{
		case FUNCTIONID_sonchain_SEND:
			SendMsg(message);
			break;
		case FUNCTIONID_sonchain_SENDALL:
			SendAll(message);
			break;
		case FUNCTIONID_sonchain_ENTER:
			Enter(message);
			break;
		case FUNCTIONID_RECEIVE_CALCULATE_POW:
			ReceiveTransaction(message);
			break;
		case FUNCTIONID_CALCULATE_POW_COMPLETE:
			MineBlockOver(message);
			break;
		case FUNCTIONID_CALCULATE_POW_COMPLETE_CONFIRM:
			MineBlockConfirm(message);
			break;
		case FUNCTIONID_SYNC_BLOCK_CHAIN_REQ:
			SyncBlockChainReq(message);
			break;
		case FUNCTIONID_SYNC_BLOCK_CHAIN_RTN:
			SyncBlockChainRtn(message);
			break;
		case FUNCTIONID_sonchain_PING:
			PingPong(message);
			break;
	        //New Version
		case FUNCTIONID_STATUS:
			processStatus(message);
			break;
		case FUNCTIONID_NEW_BLOCK_HASHES:
			processNewBlockHashes(message);
			break;
		case FUNCTIONID_TRANSACTIONS:
			processTransactions(message);
			break;
		case FUNCTIONID_GET_BLOCK_HEADERS:
			processGetBlockHeaders(message);
			break;
		case FUNCTIONID_BLOCK_HEADERS:
			processBlockHeaders(message);
			break;
		case FUNCTIONID_GET_BLOCK_BODIES:
			processGetBlockBodies(message);
			break;
		case FUNCTIONID_BLOCK_BODIES:
			processBlockBodies(message);
			break;
		case FUNCTIONID_NEW_BLOCK:
			processNewBlock(message);
			break;
		default:
			break;
		}
	}

	private void PingPong(CMessage message)
	{
		Binary bi = new Binary();
		try
		{
			bi.Write(message.m_body, message.m_bodyLength);
			double clientMs = bi.ReadDouble();
			bi.Close();
			bi = null;
			long curMS = System.currentTimeMillis();
			double ms = curMS - clientMs;
			System.out.println(String.format("连接 :%d, 到服务端的延迟为: %s ms", message.m_socketID, ms));
			bi = new Binary();
			curMS = System.currentTimeMillis();
			bi.WriteDouble(curMS);
			byte[] bys = bi.GetBytes();
			int bLen = bys.length;
			message.m_body = bys;
			message.m_bodyLength = bLen;
			Send(message);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
    public void processStatus(CMessage message){
    	
    }
    
    public void processNewBlockHashes(CMessage message){
    	
    }
    
    public void processTransactions(CMessage message){
    	
    }
    
    public void processGetBlockHeaders(CMessage message){
    	
    }
    
    public void processGetBlockBodies(CMessage message){
    	
    }
    
    public void processBlockBodies(CMessage message){
    	
    }
    
    public void processNewBlock(CMessage message){
    	
    }
    
    public void processBlockHeaders(CMessage message){
    	
    }

	public void MineBlockConfirm(CMessage message)
	{
		try
		{
			Binary br = new Binary();
			br.Write(message.m_body, message.m_bodyLength);
			int sonchainSize = br.ReadInt();
			SonChainData data = new SonChainData();
			data.m_type = br.ReadInt();
			if (data.m_type == 0)
			{
				data.m_text = br.ReadString();
			}
			else if (data.m_type == 1)
			{
				int len = br.ReadInt();
				List<Transaction> trans = new ArrayList<Transaction>();
				for (int j = 0; j < len; j++)
				{
					Transaction tran = new Transaction();
					//tran.m_type = TransactionType.ContractCreation;
					//tran.m_value = BigInteger.valueOf((long) br.ReadDouble());
					//tran.m_fee = BigInteger.valueOf((long) br.ReadDouble());
					//tran.m_remark = br.ReadString();
					//tran.SetHash(tran.GetTxHash());
					//System.out.println(" receive transaction remark :" + tran.m_remark);
					trans.add(tran);
				}
				data.m_trans = trans;
			}
			else
			{
				data.m_preBlockHash = br.ReadString();
				data.m_blockJson = br.ReadString();
			}
			Block block = Block.toData(data.m_blockJson);
			System.out.println("挖矿确认成功 保存");
			System.out.println("preBlockHash:" + data.m_preBlockHash);
			System.out.println("blockJson:" + data.m_blockJson);
			//DataCenter.GetBlockChain().AddBlock(block);
			RedisSeq.SetBlockChain(data.m_preBlockHash, data.m_blockJson);
			//System.out.println("The size of block chian :" + DataCenter.GetBlockChain().GetSize());
		}
		catch (Exception ex)
		{
		}
	}

	/**
	 * 计算区块
	 * 
	 * @param trans
	 */
	public void MineBlock(List<Transaction> trans)
	{
		System.out.println("MineBlock begin");
		//Block lastBlock = DataCenter.GetBlockChain().GetLastBlock();
		//String preBlockHash = DigestUtils.sha256Hex(lastBlock.m_hash.getValue().toByteArray());
		//DataCenter.m_blockCache.SetBlockStatus(preBlockHash, BlockStatus.Mining);

		Thread thread = new Thread()
		{
			@Override
			public void run()
			{

				//Block block = DataCenter.GetBlockChain().MineBlock(lastBlock, trans);
				System.out.println(
						"Miner over time :" + TimeStampFormatForParse.format(Calendar.getInstance().getTime()));
//				ProofOfWork pow = ProofOfWork.newProofOfWork((BlockHeader) block);
//				boolean isValidate = pow.validate();
//				System.out.println("Pow valid: " + pow.validate() + "\n");
//				if (isValidate)
//				{
//					BlockStatus status = DataCenter.m_blockCache.GetBlockStatus(preBlockHash);
//					// 保存区块和状态
//					if (status == BlockStatus.Mining)
//					{
//						DataCenter.m_blockCache.SetBlockStatus(preBlockHash, BlockStatus.Mined);
//						DataCenter.m_blockCache.SetBlock(preBlockHash, block);
//						// 广播区块完成消息
//						SendMinedMessage(preBlockHash, block);
//					}
//					else
//					{
//						System.out.println("Miner 失效");
//					}
//					// DataCenter.GetBlockChain().AddBlock(block);
//					// String jsonBlock = block.ToJson();
//					// RedisSeq.SetBlockChain(DigestUtils.sha256Hex(block.m_prevBlockHash),
//					// jsonBlock);
//				}
			}
		};
		thread.start();
	}

	/**
	 * 接收交易信息
	 * 
	 * @param message
	 */
	public void ReceiveTransaction(CMessage message)
	{
		try
		{
			System.out.println("ReceiveTransaction begin");
			Binary br = new Binary();
			br.Write(message.m_body, message.m_bodyLength);
			int len = br.ReadInt();
			List<Transaction> trans = new ArrayList<Transaction>();
			for (int i = 0; i < len; i++)
			{
				Transaction tran = new Transaction();
				tran.m_type = TransactionType.ContractCreation;
				//tran.m_value = BigInteger.valueOf((long) br.ReadDouble());
				//tran.m_fee = BigInteger.valueOf((long) br.ReadDouble());
				//tran.m_remark = br.ReadString();
				//tran.SetHash(tran.GetTxHash());
				//System.out.println(" receive transaction remark :" + tran.m_remark);
				trans.add(tran);
			}
			br.Close();
			SonChainData data = new SonChainData();
			data.m_type = 1;
			data.m_trans.addAll(trans);
			// 通知所有节点交易列表
			SendAll(data);
			// 计算区块
			MineBlock(trans);
		}
		catch (Exception e)
		{
		}
	}

	/**
	 * 发送挖矿完成消息
	 * 
	 * @param preBlockHash
	 * @param block
	 * @return
	 */
	public int SendMinedMessage(String preBlockHash, Block block)
	{
		try
		{
			System.out.println(
					"SendMinedMessage begin :" + TimeStampFormatForParse.format(Calendar.getInstance().getTime()));
			SonChainData data = new SonChainData();
			data.m_type = 2;
			data.m_preBlockHash = preBlockHash;
			data.m_blockJson = block.toJson();
			// 通知所有节点
			SendAll(data);
			return 0;
		}
		catch (Exception ex)
		{

		}
		return -1;
	}

	/**
	 * 发送消息
	 * 
	 * @param message
	 * @param ips
	 * @param datas
	 * @return
	 */
	public int Send(CMessage message, String ips, List<SonChainData> datas)
	{
		try
		{
			Binary bw = new Binary();
			int sonchainSize = datas.size();
			if (DataCenter.IsFull())
			{
				String key = DataCenter.m_config.m_localHost + ":"
						+ CStr.ConvertIntToStr(DataCenter.m_config.m_localPort);
				if (ips.indexOf(key) != -1)
				{
					return 1;
				}
				else
				{
					ips += key + ";";
				}
			}
			bw.WriteString(ips);
			bw.WriteInt(sonchainSize);
			for (int i = 0; i < sonchainSize; i++)
			{
				SonChainData data = datas.get(i);
				bw.WriteInt(data.m_type);
				if (data.m_type == 0)
				{
					bw.WriteString(data.m_text);
				}
				else if (data.m_type == 1)
				{
					List<Transaction> trans = data.m_trans;
					int len = trans.size();
					bw.WriteInt(len);
					for (int j = 0; j < len; j++)
					{
						Transaction tran = new Transaction();
						//bw.WriteDouble(tran.m_value.doubleValue());
						//bw.WriteDouble(tran.m_fee.doubleValue());
						//bw.WriteString(tran.m_remark);
					}
				}
				else
				{
					bw.WriteString(data.m_preBlockHash);
					bw.WriteString(data.m_blockJson);
				}
			}
			byte[] bytes = bw.GetBytes();
			message.m_body = bytes;
			message.m_bodyLength = bytes.length;
			int ret = Send(message);
			bw.Close();
			return ret;
		}
		catch (Exception ex)
		{

		}
		return -1;
	}

	/**
	 * 广播消息
	 * 
	 * @param message
	 * @return
	 */
	public int SendAll(CMessage message)
	{
		int rtnSocketID = message.m_socketID;
		ArrayList<SonChainData> datas = new ArrayList<SonChainData>();
		String ips = "";
		RefObject<String> refIPs = new RefObject<String>(ips);
		GetsonchainDatas(datas, refIPs, message.m_body, message.m_bodyLength);
		int size = datas.size();
		if (size > 0)
		{
			SonChainData data = datas.get(0);
			if (data.m_type == 1)
			{
				MineBlock(data.m_trans);
			}
			else if (data.m_type == 2)
			{
				MineBlockOver(message, data.m_preBlockHash, data.m_blockJson);
			}
			else if (data.m_type == 3)
			{
				MineBlockConfirm(data.m_preBlockHash, data.m_blockJson);
			}
			else if (data.m_type == 4)
			{
			}
		}
		ips = refIPs.argvalue;
		synchronized (m_socketIDs)
		{
			for (int socketID : m_socketIDs.keySet())
			{
				// if (rtnSocketID != socketID) {
				String copyIPs = ips;
				message.m_socketID = socketID;
				int ret = Send(message, copyIPs, datas);
				// }
			}
		}
		datas.clear();
		return 1;
	}

	/**
	 * 广播消息
	 * 
	 * @param data
	 * @return
	 */
	public int SendAll(SonChainData data)
	{
		try
		{
			Binary bw = new Binary();
			String key = DataCenter.m_config.m_localHost + ":" + CStr.ConvertIntToStr(DataCenter.m_config.m_localPort);
			bw.WriteString(key);
			bw.WriteInt(1);
			bw.WriteInt(data.m_type);
			if (data.m_type == 0)
			{
				bw.WriteString(data.m_text);
			}
			else if (data.m_type == 1)
			{
				List<Transaction> trans = data.m_trans;
				int len = trans.size();
				bw.WriteInt(len);
				for (int i = 0; i < len; i++)
				{
					Transaction tran = trans.get(i);
					//bw.WriteDouble(tran.m_value.doubleValue());
					//bw.WriteDouble(tran.m_fee.doubleValue());
					//bw.WriteString(tran.m_remark);
				}
			}
			else
			{
				bw.WriteString(data.m_preBlockHash);
				bw.WriteString(data.m_blockJson);
			}

			byte[] bytes = bw.GetBytes();
			CMessage message = new CMessage(GetGroupID(), GetServiceID(), FUNCTIONID_sonchain_SENDALL, GetSessionID(),
					BaseServiceSV.GetRequestID(), 0, 0, GetCompressType(), bytes.length, bytes);
			synchronized (m_socketIDs)
			{
				for (int sockID : m_socketIDs.keySet())
				{
					message.m_socketID = sockID;
					Send(message);
				}
			}
			for (SonChainServiceCT ct : DataCenter.m_clientsonchainServices.values())
			{
				message = new CMessage(GetGroupID(), GetServiceID(), FUNCTIONID_sonchain_SENDALL, message.m_sessionID,
						message.m_requestID, DataCenter.m_socketID, 0, GetCompressType(), bytes.length, bytes);
				ct.Send(message);
			}
			bw.Close();
			return 1;
		}
		catch (Exception ex)
		{
			return 0;
		}
	}

	public int SendHostInfos(ArrayList<Integer> socketIDs, int type, List<SonChainHostInfo> hostInfos)
	{
		try
		{
			int hostInfosSize = hostInfos.size();
			Binary bw = new Binary();
			bw.WriteInt(hostInfosSize);
			bw.WriteInt(type);
			for (int i = 0; i < hostInfosSize; i++)
			{
				SonChainHostInfo hostInfo = hostInfos.get(i);
				bw.WriteString(hostInfo.m_ip);
				bw.WriteInt(hostInfo.m_serverPort);
				bw.WriteInt(hostInfo.m_type);
			}
			byte[] bytes = bw.GetBytes();
			CMessage message = new CMessage(GetGroupID(), GetServiceID(), FUNCTIONID_GETHOSTS, GetSessionID(),
					DataCenter.GetsonchainRequestID(), 0, 0, GetCompressType(), bytes.length, bytes);
			for (int socketID : socketIDs)
			{
				message.m_socketID = socketID;
				Send(message);
			}
			bw.Close();
		}
		catch (Exception ex)
		{

		}
		return 1;
	}

	public int SendMsg(CMessage message)
	{
		SendToListener(message);
		return 1;
	}

	/**
	 * 同步区块链
	 */
	public int SyncBlockChain()
	{
		try
		{
			//BlockChain1 blockChain = DataCenter.GetBlockChain();
			//Block block = blockChain.GetLastBlock();
			String preHashKey = "";
			//if (block != null)
			{
				//preHashKey = DigestUtils.sha256Hex(block.m_hash.getValue().toByteArray());
			}
			Binary bw = new Binary();
			bw.WriteString(preHashKey);
			byte[] bytes = bw.GetBytes();
			int ret = Send(new CMessage(GetGroupID(), GetServiceID(), FUNCTIONID_SYNC_BLOCK_CHAIN_REQ, GetSessionID(),
					BaseServiceCT.GetRequestID(), GetSocketID(), 0, GetCompressType(), bytes.length, bytes));
			bw.Close();
			return ret;
		}
		catch (Exception ex)
		{
			return 0;
		}
	}

	/**
	 * 处理同步区块链请求
	 * 
	 * @param message
	 * @return
	 */
	public int SyncBlockChainReq(CMessage message)
	{
		return 1;
//		try
//		{
//			Binary br = new Binary();
//			br.Write(message.m_body, message.m_bodyLength);
//			String preHashKey = br.ReadString();
//			List<String> lstHashs = BlockChain1.GetBlocks(preHashKey);
//			br.Close();
//			int size = lstHashs.size();
//			Binary bw = new Binary();
//			bw.WriteInt(size);
//			for (String block : lstHashs)
//			{
//				bw.WriteString(block);
//			}
//			byte[] bytes = bw.GetBytes();
//			int ret = Send(
//					new CMessage(GetGroupID(), GetServiceID(), FUNCTIONID_SYNC_BLOCK_CHAIN_RTN, message.m_sessionID,
//							message.m_requestID, message.m_socketID, 0, GetCompressType(), bytes.length, bytes));
//			bw.Close();
//			return ret;
//		}
//		catch (Exception ex)
//		{
//			return 0;
//		}
	}

	/**
	 * 处理同步区块链响应
	 * 
	 * @param message
	 * @return
	 */
	public int SyncBlockChainRtn(CMessage message)
	{
		return 1;
//		try
//		{
//			Binary br = new Binary();
//			br.Write(message.m_body, message.m_bodyLength);
//			int size = br.ReadInt();
//			List<String> lstHashs = new ArrayList<String>();
//			for (int i = 0; i < size; i++)
//			{
//				lstHashs.add(br.ReadString());
//			}
//			br.Close();
//			return BlockChain1.SyncBlocks(lstHashs);
//		}
//		catch (Exception ex)
//		{
//			return 0;
//		}
	}
}
