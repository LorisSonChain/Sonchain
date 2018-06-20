package sonchain.blockchain.db;

import java.util.List;

import sonchain.blockchain.core.Block;
import sonchain.blockchain.core.BlockHeader;
import sonchain.blockchain.crypto.HashUtil;

public class BlockStoreDummy implements BlockStore {

    @Override
    public void close() {
    	
    }

    @Override
    public Block getBestBlock() {
        return null;
    }

    @Override
    public Block getBlockByHash(byte[] hash) {
        return null;
    }

    @Override
    public byte[] getBlockHashByNumber(long blockNumber) {

        byte[] data = String.valueOf(blockNumber).getBytes();
        return HashUtil.sha3(data);
    }

    @Override
    public byte[] getBlockHashByNumber(long blockNumber, byte[] branchBlockHash) {
        return getBlockHashByNumber(blockNumber);
    }

    @Override
    public Block getChainBlockByNumber(long blockNumber) {
        return null;
    }

    @Override
    public List<byte[]> getListHashesEndWith(byte[] hash, long qty) {
        return null;
    }

    @Override
    public List<BlockHeader> getListHeadersEndWith(byte[] hash, long qty) {
        return null;
    }

    @Override
    public List<Block> getListBlocksEndWith(byte[] hash, long qty) {
        return null;
    }

    @Override
    public long getMaxNumber() {
        return 0;
    }
    
    @Override
    public void flush() {
    }

    @Override
    public boolean isBlockExist(byte[] hash) {
        return false;
    }

    @Override
    public void load() {
    }

    @Override
    public void reBranch(Block forkBlock) {

    }

    @Override
    public void saveBlock(Block block, boolean mainChain) {

    }
}