package sonchain.blockchain.datasource;

import java.util.Arrays;

import org.apache.log4j.Logger;

import sonchain.blockchain.crypto.HashUtil;
import sonchain.blockchain.util.ByteUtil;
import sonchain.blockchain.util.RLP;

public class CountingBytesSource extends AbstractChainedSource<byte[], byte[], byte[], byte[]>
		implements HashedKeySource<byte[], byte[]> {

	public static final Logger m_logger = Logger.getLogger(CountingBytesSource.class);
	private QuotientFilter m_filter;
	private boolean m_dirty = false;
	private byte[] m_filterKey = HashUtil.sha3("countingStateFilter".getBytes());

	public CountingBytesSource(Source<byte[], byte[]> src) {
		this(src, false);
		m_logger.debug("CountingBytesSource init end");
	}

	public CountingBytesSource(Source<byte[], byte[]> src, boolean bloom) {
		super(src);
		byte[] filterBytes = src.get(m_filterKey);
		if (bloom) {
			if (filterBytes != null) {
				m_filter = QuotientFilter.deserialize(filterBytes);
			} else {
				m_filter = QuotientFilter.create(5_000_000, 10_000);
			}
		}
		m_logger.debug("CountingBytesSource init end");
	}

	/**
	 * Extracts value from the backing Source counter + value byte array
	 */
	protected byte[] decodeValue(byte[] srcVal) {
		m_logger.debug("CountingBytesSource decodeValue start ");
		return srcVal == null ? null : Arrays.copyOfRange(srcVal, RLP.decode(srcVal, 0).getPos(), srcVal.length);
	}

	/**
	 * Extracts counter from the backing Source counter + value byte array
	 */
	protected int decodeCount(byte[] srcVal) {
		m_logger.debug("CountingBytesSource decodeCount start");
		return srcVal == null ? 0 : ByteUtil.byteArrayToInt((byte[]) RLP.decode(srcVal, 0).getDecoded());
	}

	@Override
	public void delete(byte[] key) {
		m_logger.debug("CountingBytesSource delete start");
		synchronized (this) {
			int srcCount;
			byte[] srcVal = null;
			if (m_filter == null || m_filter.maybeContains(key)) {
				srcVal = getSource().get(key);
				srcCount = decodeCount(srcVal);
			} else {
				srcCount = 1;
			}
			if (srcCount > 1) {
				getSource().put(key, encodeCount(decodeValue(srcVal), srcCount - 1));
			} else {
				getSource().delete(key);
			}
		}
		m_logger.debug("CountingBytesSource delete end");
	}

	/**
	 * Composes value and counter into backing Source value
	 */
	protected byte[] encodeCount(byte[] val, int count) {
		m_logger.debug("CountingBytesSource encodeCount start");
		return ByteUtil.merge(RLP.encodeInt(count), val);
	}

	@Override
	protected boolean flushImpl() {
		m_logger.debug("CountingBytesSource flushImpl start");
		if (m_filter != null && m_dirty) {
			byte[] filterBytes;
			synchronized (this) {
				filterBytes = m_filter.serialize();
			}
			getSource().put(m_filterKey, filterBytes);
			m_dirty = false;
			m_logger.debug("CountingBytesSource flushImpl success");
			return true;
		} else {
			m_logger.debug("CountingBytesSource flushImpl failed");
			return false;
		}
	}

	@Override
	public byte[] get(byte[] key) {
		m_logger.debug("CountingBytesSource get start");
		return decodeValue(getSource().get(key));
	}

	@Override
	public void put(byte[] key, byte[] val) {
		m_logger.debug("CountingBytesSource put start");
		if (val == null) {
			delete(key);
			m_logger.debug("CountingBytesSource put delete end");
			return;
		}

		synchronized (this) {
			byte[] srcVal = getSource().get(key);
			int srcCount = decodeCount(srcVal);
			if (srcCount >= 1) {
				if (m_filter != null){
					m_filter.insert(key);
				}
				m_dirty = true;
			}
			getSource().put(key, encodeCount(val, srcCount + 1));
		}
		m_logger.debug("CountingBytesSource put end");
	}
}