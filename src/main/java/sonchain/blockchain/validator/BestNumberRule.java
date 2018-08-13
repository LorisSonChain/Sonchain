package sonchain.blockchain.validator;

import sonchain.blockchain.core.BlockHeader;
import sonchain.blockchain.service.DataCenter;

/**
 *
 */
public class BestNumberRule extends DependentBlockHeaderRule {

    private final int BEST_NUMBER_DIFF_LIMIT;

    public BestNumberRule() {
        BEST_NUMBER_DIFF_LIMIT = DataCenter.m_config.
                getCommonConstants().getBestNumberDiffLimit();
    }

    @Override
    public boolean validate(BlockHeader header, BlockHeader bestHeader) {
        m_errors.clear();
        long diff = header.getBlockNumber() - bestHeader.getBlockNumber();
        if (diff > -1 * BEST_NUMBER_DIFF_LIMIT) {
        	m_errors.add(String.format(
                    "#%d: (header.getNumber() - bestHeader.getNumber()) <= BEST_NUMBER_DIFF_LIMIT",
                    header.getBlockNumber()
            ));
            return false;
        }
        return true;
    }
}

