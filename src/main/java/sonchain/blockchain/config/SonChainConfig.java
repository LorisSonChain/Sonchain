package sonchain.blockchain.config;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;

import sonchain.blockchain.core.Block;
import sonchain.blockchain.core.Repository;
import sonchain.blockchain.core.Transaction;
import sonchain.blockchain.db.BlockStore;
import sonchain.blockchain.validator.BlockHeaderValidator;

public class SonChainConfig implements BlockChainConfigInterface, BaseChainConfig{
    protected Constants m_constants = null;
    private List<Pair<Long, BlockHeaderValidator>> m_headerValidators = new ArrayList<>();
    
    public SonChainConfig() {
        this(new Constants());
    }

    public SonChainConfig(Constants constants) {
    	m_constants = constants;
    }

    @Override
    public boolean acceptTransactionSignature(Transaction tx) {
    	return tx.getSignature().validateComponents();
    }

    @Override
    public BlockChainConfigInterface getConfigForBlock(long blockNumber) {
        return this;
    }

    @Override
    public Constants getConstants() {
        return m_constants;
    }

    @Override
    public Constants getCommonConstants() {
        return getConstants();
    }

    @Override
    public List<Pair<Long, BlockHeaderValidator>> headerValidators() {
        return m_headerValidators;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }

    @Override
    public String validateTransactionChanges(BlockStore blockStore, Block curBlock, Transaction tx,
                                               Repository repository) {
        return null;
    }
}
