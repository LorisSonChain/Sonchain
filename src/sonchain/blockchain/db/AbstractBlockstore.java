package sonchain.blockchain.db;

import sonchain.blockchain.core.Block;

/**
 * 区块存贮抽象类
 * @author GAIA
 *
 */
public abstract class AbstractBlockstore implements BlockStore {

	/**
	 * 根据Number获取区块的Hash值
	 */
    @Override
    public byte[] getBlockHashByNumber(long blockNumber, byte[] branchBlockHash) {
        Block branchBlock = getBlockByHash(branchBlockHash);
        if (branchBlock.getNumber() < blockNumber) {
            throw new IllegalArgumentException("Requested block number > branch hash number: " 
            		+ blockNumber + " < " + branchBlock.getNumber());
        }
        while(branchBlock.getNumber() > blockNumber) {
            branchBlock = getBlockByHash(branchBlock.getParentHash());
        }
        return branchBlock.getHash();
    }
}
