package sonchain.blockchain.validator;

import java.util.List;

import sonchain.blockchain.core.BlockHeader;

public class ParentBlockHeaderValidator extends DependentBlockHeaderRule {

    private List<DependentBlockHeaderRule> m_rules;

    public ParentBlockHeaderValidator(List<DependentBlockHeaderRule> rules) {
    	m_rules = rules;
    }

    @Override
    public boolean validate(BlockHeader header, BlockHeader parent) {
        m_errors.clear();
        for (DependentBlockHeaderRule rule : m_rules) {
            if (!rule.validate(header, parent)) {
            	m_errors.addAll(rule.getErrors());
                return false;
            }
        }
        return true;
    }
}
