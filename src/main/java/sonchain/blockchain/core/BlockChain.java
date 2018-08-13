package sonchain.blockchain.core;

import java.math.BigInteger;
import java.util.List;

import org.bouncycastle.math.ec.ECPoint;

import sonchain.blockchain.consensus.SonChainProducerNode;
import sonchain.blockchain.db.BlockStore;
import sonchain.blockchain.db.PeerSource;
import sonchain.blockchain.db.TransactionStore;
import sonchain.blockchain.vm.program.invoke.ProgramInvokeFactory;

/**
 * BlockChain
 * @author GAIA
 *
 */
public interface BlockChain {

    BlockSummary add(Block block);
    void close();
    byte[] calcReceiptsTrie(List<TransactionReceipt> receipts);
    byte[] calcTxTrie(List<TransactionReceipt> transactions);    
    boolean containsBlock(byte[] hash);
    boolean containsTransaction(byte[] blockHash, byte[] hash);
    Block createNewBlock(Block parent, List<TransactionReceipt> transactions);
    List<Chain> getAltChains();
    /**
     * @return - last added block from blockchain
     */
    Block getBestBlock();
    
    byte[] getBestBlockHash();
    /**
     * Get block by hash
     * @param hash - hash of the block
     * @return - bloc by that hash
     */
    Block getBlockByHash(byte[] hash);
	
	/**
     * Get block by number from the best chain
     * @param number - number of the block
     * @return block by that number
     */
    Block getBlockByNumber(long number);

    /**
     * Get the underlying BlockStore
     * @return Blockstore
     */
    BlockStore getBlockStore();
    List<byte[]> getListOfBodiesByHashes(List<byte[]> hashes);
    List<byte[]> getListOfHashesStartFrom(byte[] hash, int qty);
    List<BlockHeader> getListOfHeadersStartFrom(BlockIdentifier identifier, int skip, int limit, boolean reverse);
    List<byte[]> getListOfHashesStartFromBlock(long blockNumber, int qty);
    String getNodeAddress();
    PeerSource getPeerSource();
    ProgramInvokeFactory getProgramInvokeFactory();
    Repository getRepository();
    Repository getRepositorySnapshot();
    long getSize();
    TransactionInfo getTransactionInfo(byte[] hash);
    TransactionStore getTransactionStore();
    /**
     * 获取下一个区块的记账人列表
     * @return 返回一组节点列表，表示下一个区块的记账人列表
     */
    SonChainProducerNode[] getValidators();
    /**
     * Flush the content of local storage objects to disk
     */
    void flush();
    boolean hasParentOnTheChain(Block block);
    boolean isBlockExist(byte[] hash);
    void setBestBlock(Block block);
    void setExitOn(long exitOn);
    void storeBlock(Block block, List<TransactionReceipt> receipts);
    ImportResult tryToConnect(Block block);
    
}
