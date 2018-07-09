package sonchain.blockchain.server;

import java.text.SimpleDateFormat;
import java.util.List;

import org.apache.log4j.Logger;

import sonchain.blockchain.base.Binary;
import sonchain.blockchain.base.CMessage;
import sonchain.blockchain.consensus.ConsensusPayload;
import sonchain.blockchain.core.BlockChain;
import sonchain.blockchain.core.PendingState;
import sonchain.blockchain.core.Transaction;
import sonchain.blockchain.data.TransactionsMessage;
import sonchain.blockchain.service.DataCenter;

/**
 *
 */
public class SonChainServiceSV extends BaseServiceSV
{
	private static SimpleDateFormat TimeStampFormatForParse = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
	public static final Logger m_logger = Logger.getLogger(SonChainServiceSV.class);

	public SonChainServiceSV()
	{
		SetServiceID(SERVICEID_SONCHAIN);
	}

	private final int SERVICEID_SONCHAIN = 10000;
    public final int FUNCTIONID_STATUS = 0;
    public final int FUNCTIONID_NEW_BLOCK_HASHES = 1;
    public final int FUNCTIONID_TRANSACTIONS = 2;
    public final int FUNCTIONID_GET_BLOCK_HEADERS = 3;
    public final int FUNCTIONID_BLOCK_HEADERS = 4;
    public final int FUNCTIONID_GET_BLOCK_BODIES = 5;
    public final int FUNCTIONID_BLOCK_BODIES = 6;
    public final int FUNCTIONID_NEW_BLOCK = 7;
    public final int FUNCTIONID_GET_NODE_DATA = 13;
    public final int FUNCTIONID_NODE_DATA = 14;
    public final int FUNCTIONID_GET_RECEIPTS = 15;
    public final int FUNCTIONID_RECEIPTS = 16;
    public final int FUNCTIONID_CHANGEVIEW = 32;
    public final int FUNCTIONID_PREPAREREQUEST = 33;
    public final int FUNCTIONID_PREPARERESPONSE = 34;
    public final int FUNCTIONID_CONSENSUSPAYLOAD = 35;    
    
    protected PendingState m_pendingState = null;
    protected BlockChain m_blockchain = null;

	private int m_port = 16666;

	public int GetPort()
	{
		return m_port;
	}

	public void SetPort(int port)
	{
		m_port = port;
	}

	@Override
	public void OnReceive(CMessage message)
	{
		super.OnReceive(message);
		switch (message.m_functionID)
		{
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
		case FUNCTIONID_CHANGEVIEW:
			break;
		case FUNCTIONID_PREPAREREQUEST:
			break;
		case FUNCTIONID_PREPARERESPONSE:
			break;
		case FUNCTIONID_CONSENSUSPAYLOAD:
			processConsensusPayload(message);
			break;
		default:
			break;
		}
	}
	private void processConsensusPayload(CMessage message){

		Binary reader = new Binary();
		try
		{
			reader.Write(message.m_body, message.m_bodyLength);
			ConsensusPayload consensusPayLoad = new ConsensusPayload();
			consensusPayLoad.deserialize(reader);
			DataCenter.getSonChainImpl().getConsensusService().localNodeInventoryReceived(consensusPayLoad);
		}
		catch (Exception e)
		{
			e.printStackTrace();
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
        try
        {
        	m_logger.debug("processTransactions start");
        	TransactionsMessage msg = new TransactionsMessage(message.m_body);
            List<Transaction> txSet = msg.getTransactions();
            DataCenter.getSonChainImpl().addTransactions(txSet);
            //if (!newPending.isEmpty()) {
            //    TransactionTask transactionTask = new TransactionTask(newPending);
            //    TransactionTaskExecutor.instance.submitTransaction(transactionTask);    
            //}
        	m_logger.debug("processTransactions end");
        }
        catch(Exception ex)
        {
        	m_logger.error("processTransactions error:" + ex.getMessage());  
        	m_logger.equals(ex);
        }
        finally
        {
        	m_logger.debug("processTransactions end");        	
        }
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

	public int SendMsg(CMessage message)
	{
		SendToListener(message);
		return 1;
	}
}
