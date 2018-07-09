package sonchain.blockchain.core;

import java.math.BigInteger;
import java.util.List;

import org.bouncycastle.math.ec.ECPoint;

import sonchain.blockchain.consensus.SonChainPeerNode;
import sonchain.blockchain.db.BlockStore;
import sonchain.blockchain.db.PeerSource;
import sonchain.blockchain.db.TransactionStore;
import sonchain.blockchain.vm.program.invoke.ProgramInvokeFactory;

/**
 * BlockChain
 *
 */
public interface BlockChain {

    BlockSummary add(Block block);
    void close();
    byte[] calcReceiptsTrie(List<TransactionReceipt> receipts);
    byte[] calcTxTrie(List<Transaction> transactions);    
    boolean containsBlock(byte[] hash);
    boolean containsTransaction(byte[] blockHash, byte[] hash);
    Block createNewBlock(Block parent, List<Transaction> transactions);
    List<Chain> getAltChains();
    Block getBestBlock();    
    byte[] getBestBlockHash();
    Block getBlockByHash(byte[] hash);
    Block getBlockByNumber(long number);
    BlockStore getBlockStore();
    List<byte[]> getListOfBodiesByHashes(List<byte[]> hashes);
    List<byte[]> getListOfHashesStartFrom(byte[] hash, int qty);
    List<BlockHeader> getListOfHeadersStartFrom(BlockIdentifier identifier, int skip, int limit, boolean reverse);
    List<byte[]> getListOfHashesStartFromBlock(long blockNumber, int qty);
    byte[] getNodeAddress();
    PeerSource getPeerSource();
    ProgramInvokeFactory getProgramInvokeFactory();
    Repository getRepository();
    Repository getRepositorySnapshot();
    long getSize();
    TransactionInfo getTransactionInfo(byte[] hash);
    TransactionStore getTransactionStore();
    SonChainPeerNode[] getValidators();
    void flush();
    boolean hasParentOnTheChain(Block block);
    boolean isBlockExist(byte[] hash);
    void setBestBlock(Block block);
    void setExitOn(long exitOn);
    void storeBlock(Block block, List<TransactionReceipt> receipts);
    ImportResult tryToConnect(Block block);
    
}
