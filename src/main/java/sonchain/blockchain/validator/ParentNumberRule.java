package sonchain.blockchain.validator;

import sonchain.blockchain.core.BlockHeader;

public class ParentNumberRule extends DependentBlockHeaderRule {
    @Override
    public boolean validate(BlockHeader header, BlockHeader parent) {
        m_errors.clear();
        if (header.getNumber() != (parent.getNumber() + 1)) {
        	m_errors.add(String.format("#%d: block number is not parentBlock number + 1", header.getNumber()));
            return false;
        }
        return true;
    }
}
