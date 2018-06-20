package sonchain.blockchain.core;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import sonchain.blockchain.db.ByteArrayWrapper;

public class Chain {

	public static final Logger m_logger = Logger.getLogger(Chain.class);
	private List<Block> m_chain = new ArrayList<>();
	private Map<ByteArrayWrapper, Block> m_index = new HashMap<>();

	public void add(Block block) {
		m_logger.info(String.format("adding block start to alt chain block.hash: [{%s}] ",
				block.getShortHash()));
		m_chain.add(block);
		m_index.put(new ByteArrayWrapper(block.getHash()), block);
		m_logger.info(String.format("adding block end to alt chain block.hash: [{%s}] ",
				block.getShortHash()));
	}

	public boolean tryToConnect(Block block) {
		m_logger.debug("tryToConnect start");
		if (m_chain.isEmpty()) {
			add(block);
			m_logger.debug("tryToConnect end");
			return true;
		}
		Block lastBlock = m_chain.get(m_chain.size() - 1);
		if (lastBlock.isParentOf(block)) {
			add(block);
			m_logger.debug("tryToConnect end");
			return true;
		}
		m_logger.debug("tryToConnect end");
		return false;
	}

	public Block get(int i) {
		m_logger.debug("get start");
		return m_chain.get(i);
	}

	public Block getLast() {
		m_logger.debug("getLast start");
		return m_chain.get(m_chain.size() - 1);
	}

	public long getSize() {
		m_logger.debug("getSize start");
		return m_chain.size();
	}

	public boolean isParentOnTheChain(Block block) {
		m_logger.debug("isParentOnTheChain start");
		return (m_index.get(new ByteArrayWrapper(block.getParentHash())) != null);
	}
}
