package sonchain.blockchain.db;

import sonchain.blockchain.core.Block;
import sonchain.blockchain.util.Numeric;

/**
 *
 */
public abstract class AbstractBlockstore implements BlockStore {

    @Override
    public byte[] getBlockHashByNumber(long blockNumber, byte[] branchBlockHash) {
        Block branchBlock = getBlockByHash(branchBlockHash);
        if (branchBlock.getBlockNumber() < blockNumber) {
            throw new IllegalArgumentException("Requested block number > branch hash number: " 
            		+ blockNumber + " < " + branchBlock.getBlockNumber());
        }
        while(branchBlock.getBlockNumber() > blockNumber) {
            branchBlock = getBlockByHash(Numeric.hexStringToByteArray(branchBlock.getParentHash()));
        }
        return branchBlock.getHash();
    }
}
