package sonchain.blockchain.facade;

import java.math.BigInteger;
import java.net.InetAddress;
import java.util.List;
import java.util.concurrent.Future;

import sonchain.blockchain.consensus.ConsensusService;
import sonchain.blockchain.consensus.NodeManager;
import sonchain.blockchain.consensus.SonChainProducerNode;
import sonchain.blockchain.core.Block;
import sonchain.blockchain.core.BlockChain;
import sonchain.blockchain.core.Transaction;
import sonchain.blockchain.core.TransactionReceipt;
import sonchain.blockchain.listener.SonChainListener;
import sonchain.blockchain.manager.AdminInfo;
import sonchain.blockchain.manager.BlockLoader;

public interface SonChain {

    void addListener(SonChainListener listener);

    List<Transaction> addTransactions(List<Transaction> transactions);
    
    TransactionReceipt callConstant(Transaction tx, Block block);

    void close();
    
    void connect(SonChainProducerNode node);
    
    void connect(InetAddress addr, int port, String remoteId);

    void connect(String ip, int port, String remoteId);
    
    Transaction createTransaction(BigInteger nonce, byte[] receiveAddress, BigInteger value, byte[] data);

    void exitOn(long number);

    BlockChain getBlockChain();

    BlockLoader getBlockLoader();
    
    Integer getChainIdForNextBlock();
    
    Repository getLastRepositorySnapshot();
    
    Repository getPendingState();
    
    List<Transaction> getPendingStateTransactions();
    
    Repository getRepository();
    
    ConsensusService getConsensusService();
    
    Repository getSnapshotTo(byte[] root);
    
    SyncStatus getSyncStatus();
    
    List<Transaction> getWireTransactions();
    
    void initSyncing();

    boolean isConnected();
    
    Future<Transaction> submitTransaction(Transaction transaction);

    void startPeerDiscovery();

    void stopPeerDiscovery();

	AdminInfo getAdminInfo();
}
