package sonchain.blockchain.config;

import java.math.BigInteger;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;

import sonchain.blockchain.core.Block;
import sonchain.blockchain.core.BlockHeader;
import sonchain.blockchain.core.Repository;
import sonchain.blockchain.core.Transaction;
import sonchain.blockchain.db.BlockStore;
import sonchain.blockchain.validator.BlockHeaderValidator;

public interface BlockChainConfigInterface {
    /**
     * Validates Tx signature
     */
    boolean acceptTransactionSignature(Transaction tx);
	 /**
     * Get blockchain constants
     */
    Constants getConstants();
    List<Pair<Long, BlockHeaderValidator>> headerValidators();
    /**
     * Validates transaction by the changes made by it in the repository
     * @param blockStore
     * @param curBlock The block being imported
     * @param repositoryTrack The repository track changed by transaction
     * @return null if all is fine or String validation error
     */
    String validateTransactionChanges(BlockStore blockStore, Block curBlock, Transaction tx,
                                      Repository repositoryTrack);
}
