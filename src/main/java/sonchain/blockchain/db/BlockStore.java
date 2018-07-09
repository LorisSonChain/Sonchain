package sonchain.blockchain.db;

import java.util.List;

import sonchain.blockchain.core.Block;
import sonchain.blockchain.core.BlockHeader;

public interface BlockStore {

    void close();
    Block getBestBlock();
    Block getBlockByHash(byte[] hash);
	byte[] getBlockHashByNumber(long blockNumber);
    byte[] getBlockHashByNumber(long blockNumber, byte[] branchBlockHash);
    Block getChainBlockByNumber(long blockNumber);
    List<byte[]> getListHashesEndWith(byte[] hash, long qty);
    List<BlockHeader> getListHeadersEndWith(byte[] hash, long qty);
    List<Block> getListBlocksEndWith(byte[] hash, long qty);
    long getMaxNumber();
    void flush();
    boolean isBlockExist(byte[] hash);
    void load();
    void reBranch(Block forkBlock);
    void saveBlock(Block block, boolean mainChain);
}
