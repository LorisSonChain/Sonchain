package sonchain.blockchain.validator;

import sonchain.blockchain.core.BlockHeader;

public class DependentBlockHeaderRuleAdapter extends DependentBlockHeaderRule {

    @Override
    public boolean validate(BlockHeader header, BlockHeader dependency) {
        return true;
    }
}
