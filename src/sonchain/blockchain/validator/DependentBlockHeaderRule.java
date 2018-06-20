package sonchain.blockchain.validator;

import java.util.*;

import org.apache.log4j.Logger;
import org.apache.log4j.Priority;

import sonchain.blockchain.core.BlockHeader;

public abstract class DependentBlockHeaderRule extends AbstractValidationRule {

    protected List<String> m_errors = new LinkedList<>();

    public List<String> getErrors() {
        return m_errors;
    }

    public void logErrors(Logger logger) {
        if (logger.isEnabledFor(Priority.ERROR))
            for (String msg : m_errors) {
                logger.warn(String.format("{%s} invalid: {%s}", getEntityClass().getSimpleName(), msg));
            }
    }

    @Override
    public Class getEntityClass() {
        return BlockHeader.class;
    }

    /**
     * Runs header validation and returns its result
     *
     * @param header block's header
     * @param dependency header of the dependency block
     * @return true if validation passed, false otherwise
     */
    abstract public boolean validate(BlockHeader header, BlockHeader dependency);

}
