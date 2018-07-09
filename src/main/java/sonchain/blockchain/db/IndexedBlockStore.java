package sonchain.blockchain.db;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.bouncycastle.util.Arrays;
import org.bouncycastle.util.encoders.Hex;

import sonchain.blockchain.core.Block;
import sonchain.blockchain.core.BlockHeader;
import sonchain.blockchain.crypto.HashUtil;
import sonchain.blockchain.datasource.DataSourceArray;
import sonchain.blockchain.datasource.ObjectDataSource;
import sonchain.blockchain.datasource.Serializer;
import sonchain.blockchain.datasource.Source;
import sonchain.blockchain.util.ByteUtil;
import sonchain.blockchain.util.FastByteComparisons;
import sonchain.blockchain.util.RLP;
import sonchain.blockchain.util.RLPElement;
import sonchain.blockchain.util.RLPList;

/**
 *
 */
public class IndexedBlockStore extends AbstractBlockstore{
	public static final Logger m_logger = Logger.getLogger(IndexedBlockStore.class);

    private Source<byte[], byte[]> m_blocksDS = null;
    private ObjectDataSource<Block> m_blocks = null;
    private Source<byte[], byte[]> m_indexDS = null;
    private DataSourceArray<List<BlockInfo>> m_index = null;

    /**
     * Init
     */
    public IndexedBlockStore(){
    }

    /**
     * addInternalBlock
     * @param block
     * @param mainChain
     */
    private void addInternalBlock(Block block, boolean mainChain){

    	m_logger.debug("addInternalBlock start: blockInfo:" + block.toString());
        List<BlockInfo> blockInfos = block.getNumber() >= m_index.size() ?  
        		null : m_index.get((int) block.getNumber());
        blockInfos = blockInfos == null ? new ArrayList<BlockInfo>() : blockInfos;
        BlockInfo blockInfo = new BlockInfo();
        blockInfo.setHash(block.getHash());
        blockInfo.setMainChain(mainChain); 
        putBlockInfo(blockInfos, blockInfo);
        m_index.set((int) block.getNumber(), blockInfos);
        m_blocks.put(block.getHash(), block);
    }

    /**
     * Close
     */
    @Override
    public synchronized void close() {
//        logger.info("Closing IndexedBlockStore...");
//        try {
//            indexDS.close();
//        } catch (Exception e) {
//            logger.warn("Problems closing indexDS", e);
//        }
//        try {
//            blocksDS.close();
//        } catch (Exception e) {
//            logger.warn("Problems closing blocksDS", e);
//        }
    }

    /**
     * flush
     */
    @Override
    public synchronized void flush(){
    	m_blocks.flush();
        m_index.flush();
        m_blocksDS.flush();
        m_indexDS.flush();
    }

    /**
     * getBestBlock
     */
    public synchronized Block getBestBlock(){

        Long maxLevel = getMaxNumber();
        if (maxLevel < 0) {
        	return null;
        }

        Block bestBlock = getChainBlockByNumber(maxLevel);
        if (bestBlock != null) {
        	return  bestBlock;
        }

        // That scenario can happen
        // if there is a fork branch that is
        // higher than main branch but has
        // less TD than the main branch TD
        while (bestBlock == null){
            --maxLevel;
            bestBlock = getChainBlockByNumber(maxLevel);
        }
        return bestBlock;
    }

    /**
     * getChainBlockByNumber
     */
    @Override
    public synchronized Block getChainBlockByNumber(long number){
        if (number >= m_index.size()){
            return null;
        }

        List<BlockInfo> blockInfos = m_index.get((int) number);
        if (blockInfos == null) {
            return null;
        }
        for (BlockInfo blockInfo : blockInfos){
            if (blockInfo.isMainChain()){
                byte[] hash = blockInfo.getHash();
                m_logger.debug("getChainBlockByNumber number:" + number + " hash:" + Hex.toHexString(hash));
                Block block = m_blocks.get(hash);
                m_logger.debug("getChainBlockByNumber number:" + number + " blockInfo:" + block.toString());
                return block;
            }
        }
        return null;
    }

    /**
     * getBlockByHash
     */
    @Override
    public synchronized Block getBlockByHash(byte[] hash) {
        return m_blocks.get(hash);
    }

    /**
     * getBlockHashByNumber
     */
    public synchronized byte[] getBlockHashByNumber(long blockNumber){
        Block chainBlock = getChainBlockByNumber(blockNumber);
        return chainBlock == null ? null : chainBlock.getHash(); 
        // FIXME: can be improved by accessing the hash directly in the index
    }

    /**
     * getBlockInfoForLevel
     * @param level
     * @return
     */
    private synchronized List<BlockInfo> getBlockInfoForLevel(long level){
        return m_index.get((int) level);
    }

    /***
     * getBlockInfoForHash
     * @param blocks
     * @param hash
     * @return
     */
    private static BlockInfo getBlockInfoForHash(List<BlockInfo> blocks, byte[] hash){
        for (BlockInfo blockInfo : blocks){
            if (Arrays.areEqual(hash, blockInfo.getHash())) return blockInfo;
        }
        return null;
    }

    /**
     * getBlocksByNumber
     * @param number
     * @return
     */
    public synchronized List<Block> getBlocksByNumber(long number){

        List<Block> result = new ArrayList<>();
        if (number >= m_index.size()) {
            return result;
        }
        List<BlockInfo> blockInfos = m_index.get((int) number);
        if (blockInfos == null) {
            return result;
        }
        for (BlockInfo blockInfo : blockInfos){
            byte[] hash = blockInfo.getHash();
            Block block = m_blocks.get(hash);
            result.add(block);
        }
        return result;
    }

    /**
     * getListHashesStartWith
     * @param number
     * @param maxBlocks
     * @return
     */
    public synchronized List<byte[]> getListHashesStartWith(long number, long maxBlocks){

        List<byte[]> result = new ArrayList<>();
        int i = 0;
        for (; i < maxBlocks; ++i){
            List<BlockInfo> blockInfos =  m_index.get((int) number);
            if (blockInfos == null) {
            	break;
            }
            for (BlockInfo blockInfo : blockInfos){
               if (blockInfo.isMainChain()){
                   result.add(blockInfo.getHash());
                   break;
               }
            }
            ++number;
        }
        maxBlocks -= i;
        return result;
    }

    /**
     * getListHashesEndWith
     */
    @Override
    public synchronized List<byte[]> getListHashesEndWith(byte[] hash, long number){
        List<Block> blocks = getListBlocksEndWith(hash, number);
        List<byte[]> hashes = new ArrayList<>(blocks.size());
        for (Block b : blocks) {
            hashes.add(b.getHash());
        }
        return hashes;
    }

    /**
     * getListHeadersEndWith
     */
    @Override
    public synchronized List<BlockHeader> getListHeadersEndWith(byte[] hash, long qty) {

        List<Block> blocks = getListBlocksEndWith(hash, qty);
        List<BlockHeader> headers = new ArrayList<>(blocks.size());
        for (Block b : blocks) {
            headers.add(b.getHeader());
        }
        return headers;
    }

    /**
     * getListBlocksEndWith
     */
    @Override
    public synchronized List<Block> getListBlocksEndWith(byte[] hash, long qty) {
        return getListBlocksEndWithInner(hash, qty);
    }

    /**
     * getListBlocksEndWithInner
     * @param hash
     * @param qty
     * @return
     */
    private List<Block> getListBlocksEndWithInner(byte[] hash, long qty) {

        Block block = m_blocks.get(hash);
        if (block == null) {
        	return new ArrayList<>();
        }
        List<Block> blocks = new ArrayList<>((int) qty);
        for (int i = 0; i < qty; ++i) {
            blocks.add(block);
            block = m_blocks.get(block.getParentHash());
            if (block == null) {
            	break;
            }
        }
        return blocks;
    }

    @Override
    public synchronized long getMaxNumber(){
        Long bestIndex = 0L;
        if (m_index.size() > 0){
            bestIndex = (long) m_index.size();
        }
        return bestIndex - 1L;
    }

    /**
     * Init
     * @param index
     * @param blocks
     */
    public void Init(Source<byte[], byte[]> index, Source<byte[], byte[]> blocks) {
    	m_indexDS = index;
    	m_index = new DataSourceArray<>(new ObjectDataSource<>(index, BLOCK_INFO_SERIALIZER, 512));
    	m_blocksDS = blocks;
    	m_blocks = new ObjectDataSource<>(blocks, new Serializer<Block, byte[]>() {
            @Override
            public byte[] serialize(Block block) {
                byte[] bytes = block.getEncoded();
                m_logger.debug(" number:" + block.getNumber() + " block Info:" + block.toString());
                m_logger.debug(" serialize:" + Hex.toHexString(bytes));
                return bytes;
            }

            @Override
            public Block deserialize(byte[] bytes) {
                Block block = bytes == null ? null : new Block(bytes);
                m_logger.debug("deserialize:" + Hex.toHexString(bytes));
                m_logger.debug(" number:" + block.getNumber() + " block Info:" + block.toString());
                return block;
            }
        }, 512);
    }

    /**
     * isBlockExist
     */
    @Override
    public synchronized boolean isBlockExist(byte[] hash) {
        return m_blocks.get(hash) != null;
    }

    /**
     * load
     */
    @Override
    public synchronized void load() {
    }

    /**
     * printChain
     */
    public synchronized void printChain(){
        Long number = getMaxNumber();

        for (int i = 0; i < number; ++i){
            List<BlockInfo> levelInfos = m_index.get(i);
            if (levelInfos != null) {
                System.out.print(i);
                for (BlockInfo blockInfo : levelInfos){
                    if (blockInfo.isMainChain())
                        System.out.print(" [" + HashUtil.shortHash(blockInfo.getHash()) + "] ");
                    else
                        System.out.print(" " + HashUtil.shortHash(blockInfo.getHash()) + " ");
                }
                System.out.println();
            }
        }
    }

    /**
     * putBlockInfo
     * @param blockInfos
     * @param blockInfo
     */
    private void putBlockInfo(List<BlockInfo> blockInfos, BlockInfo blockInfo) {
        for (int i = 0; i < blockInfos.size(); i++) {
            BlockInfo curBlockInfo = blockInfos.get(i);
            if (FastByteComparisons.equal(curBlockInfo.getHash(), blockInfo.getHash())) {
                blockInfos.set(i, blockInfo);
                return;
            }
        }
        blockInfos.add(blockInfo);
    }

    /**
     * reBranch
     */
    @Override
    public synchronized void reBranch(Block forkBlock){

        Block bestBlock = getBestBlock();
        long maxLevel = Math.max(bestBlock.getNumber(), forkBlock.getNumber());

        // 1. First ensure that you are one the save level
        long currentLevel = maxLevel;
        Block forkLine = forkBlock;
        if (forkBlock.getNumber() > bestBlock.getNumber()){
            while(currentLevel > bestBlock.getNumber()){
                List<BlockInfo> blocks =  getBlockInfoForLevel(currentLevel);
                BlockInfo blockInfo = getBlockInfoForHash(blocks, forkLine.getHash());
                if (blockInfo != null)  {
                    blockInfo.setMainChain(true);
                    setBlockInfoForLevel(currentLevel, blocks);
                }
                forkLine = getBlockByHash(forkLine.getParentHash());
                --currentLevel;
            }
        }

        Block bestLine = bestBlock;
        if (bestBlock.getNumber() > forkBlock.getNumber()){
            while(currentLevel > forkBlock.getNumber()){
                List<BlockInfo> blocks =  getBlockInfoForLevel(currentLevel);
                BlockInfo blockInfo = getBlockInfoForHash(blocks, bestLine.getHash());
                if (blockInfo != null)  {
                    blockInfo.setMainChain(false);
                    setBlockInfoForLevel(currentLevel, blocks);
                }
                bestLine = getBlockByHash(bestLine.getParentHash());
                --currentLevel;
            }
        }

        // 2. Loop back on each level until common block
        while( !bestLine.isEqual(forkLine) ) {
            List<BlockInfo> levelBlocks = getBlockInfoForLevel(currentLevel);
            BlockInfo bestInfo = getBlockInfoForHash(levelBlocks, bestLine.getHash());
            if (bestInfo != null) {
                bestInfo.setMainChain(false);
                setBlockInfoForLevel(currentLevel, levelBlocks);
            }
            BlockInfo forkInfo = getBlockInfoForHash(levelBlocks, forkLine.getHash());
            if (forkInfo != null) {
                forkInfo.setMainChain(true);
                setBlockInfoForLevel(currentLevel, levelBlocks);
            }
            bestLine = getBlockByHash(bestLine.getParentHash());
            forkLine = getBlockByHash(forkLine.getParentHash());
            --currentLevel;
        }
    }

    /**
     * saveBlock
     */
    @Override
    public synchronized void saveBlock(Block block, boolean mainChain){
    	addInternalBlock(block, mainChain);
    }

    /**
     */
    public static final Serializer<List<BlockInfo>, byte[]> BLOCK_INFO_SERIALIZER 
    		= new Serializer<List<BlockInfo>, byte[]>(){
        @Override
        public byte[] serialize(List<BlockInfo> value) {
                List<byte[]> rlpBlockInfoList = new ArrayList<>();
                for (BlockInfo blockInfo : value) {
                    byte[] hash = RLP.encodeElement(blockInfo.getHash());
                    byte[] isMainChain = RLP.encodeInt(blockInfo.isMainChain() ? 1 : 0);
                    rlpBlockInfoList.add(RLP.encodeList(hash, isMainChain));
                }
                byte[][] elements = rlpBlockInfoList.toArray(new byte[rlpBlockInfoList.size()][]);
                return RLP.encodeList(elements);
        }

        @Override
        public List<BlockInfo> deserialize(byte[] bytes) {
            if (bytes == null) {
            	return null;
            }

            List<BlockInfo> blockInfoList = new ArrayList<>();
            RLPList list = (RLPList) RLP.decode2(bytes).get(0);
            for (RLPElement element : list) {
                RLPList rlpBlock = (RLPList) element;
                BlockInfo blockInfo = new BlockInfo();
                byte[] rlpHash = rlpBlock.get(0).getRLPData();
                blockInfo.setHash(rlpHash == null ? new byte[0] : rlpHash);
                blockInfo.setMainChain(ByteUtil.byteArrayToInt(rlpBlock.get(1).getRLPData()) == 1);
                blockInfoList.add(blockInfo);
            }
            return blockInfoList;
        }
    };

    /**
     * setBlockInfoForLevel
     * @param level
     * @param infos
     */
    private synchronized void setBlockInfoForLevel(long level, List<BlockInfo> infos){
    	m_index.set((int) level, infos);
    }
}