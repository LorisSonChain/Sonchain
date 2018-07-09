package sonchain.blockchain.sync;

import java.util.*;

import sonchain.blockchain.core.Block;
import sonchain.blockchain.core.BlockHeaderWrapper;
import sonchain.blockchain.util.ByteArrayMap;
import sonchain.blockchain.util.FastByteComparisons;
import sonchain.blockchain.util.MinMaxMap;

public class SyncQueueReverseImpl implements SyncQueueInterface {

	private byte[] m_curHeaderHash = null;
//    List<BlockHeaderWrapper> headers = new ArrayList<>();
	private MinMaxMap<BlockHeaderWrapper> m_headers = new MinMaxMap<>();
	private long m_minValidated = -1;
	private ByteArrayMap<Block> m_blocks = new ByteArrayMap<>();
	private boolean m_headersOnly = false;

    public SyncQueueReverseImpl(byte[] startHash) {
    	m_curHeaderHash = startHash;
    }

    public SyncQueueReverseImpl(byte[] startHash, boolean headersOnly) {
    	m_curHeaderHash = startHash;
    	m_headersOnly = headersOnly;
    }

    @Override
    public synchronized List<HeadersRequest> requestHeaders(int maxSize, int maxRequests, int maxTotalHeaders) {
        List<HeadersRequest> ret = new ArrayList<>();
        if (m_minValidated < 0) {
            ret.add(new SyncQueueImpl.HeadersRequestImpl(m_curHeaderHash, maxSize, true, maxSize - 1));
        } else if (m_minValidated == 0) {
            // genesis reached
            return null;
        } else {
            if (m_minValidated - m_headers.getMin() < maxSize * maxSize && m_minValidated > maxSize) {
                ret.add(new SyncQueueImpl.HeadersRequestImpl(
                		m_headers.get(m_headers.getMin()).getHash(), maxSize, true, maxSize - 1));
                maxRequests--;
            }

            Set<Map.Entry<Long, BlockHeaderWrapper>> entries =
            		m_headers.descendingMap().subMap(m_minValidated, true, m_headers.getMin(), true).entrySet();
            Iterator<Map.Entry<Long, BlockHeaderWrapper>> it = entries.iterator();
            BlockHeaderWrapper prevEntry = it.next().getValue();
            while(maxRequests > 0 && it.hasNext()) {
                BlockHeaderWrapper entry = it.next().getValue();
                if (prevEntry.getNumber() - entry.getNumber() > 1) {
                    ret.add(new SyncQueueImpl.HeadersRequestImpl(prevEntry.getHash(), maxSize, true));
                    maxRequests--;
                }
                prevEntry = entry;
            }
            if (maxRequests > 0) {
                ret.add(new SyncQueueImpl.HeadersRequestImpl(prevEntry.getHash(), maxSize, true));
            }
        }

        return ret;
    }

    @Override
    public synchronized List<BlockHeaderWrapper> addHeaders(Collection<BlockHeaderWrapper> newHeaders) {
        if (m_minValidated < 0) {
            // need to fetch initial header
            for (BlockHeaderWrapper header : newHeaders) {
                if (FastByteComparisons.equal(m_curHeaderHash, header.getHash())) {
                	m_minValidated = header.getNumber();
                    m_headers.put(header.getNumber(), header);
                }
            }
        }

        // start header not found or we are already done
        if (m_minValidated <= 0) {
        	return Collections.emptyList();
        }

        for (BlockHeaderWrapper header : newHeaders) {
            if (header.getNumber() < m_minValidated) {
            	m_headers.put(header.getNumber(), header);
            }
        }


        if (m_minValidated == -1) {
        	m_minValidated = m_headers.getMax();
        }
        for (; m_minValidated >= m_headers.getMin() ; m_minValidated--) {
            BlockHeaderWrapper header = m_headers.get(m_minValidated);
            BlockHeaderWrapper parent = m_headers.get(m_minValidated - 1);
            if (parent == null) {
                // Some peers doesn't return 0 block header
                if (m_minValidated == 1){
                	m_minValidated = 0;
                }
                break;
            }
            if (!FastByteComparisons.equal(header.getHeader().getParentHash(), parent.getHash())) {
                // chain is broken here (unlikely) - refetch the rest
            	m_headers.clearAllBefore(header.getNumber());
                break;
            }
        }
        if (m_headersOnly) {
            List<BlockHeaderWrapper> ret = new ArrayList<>();
            for (long i = m_headers.getMax(); i > m_minValidated; i--) {
                ret.add(m_headers.remove(i));
            }
            return ret;
        } else {
            return null;
        }
    }

    @Override
    public synchronized BlocksRequest requestBlocks(int maxSize) {
        List<BlockHeaderWrapper> reqHeaders = new ArrayList<>();
        for (BlockHeaderWrapper header : m_headers.descendingMap().values()) {
            if (maxSize == 0) {
            	break;
            }
            if (m_blocks.get(header.getHash()) == null) {
                reqHeaders.add(header);
                maxSize--;
            }
        }
        return new SyncQueueImpl.BlocksRequestImpl(reqHeaders);
    }

    @Override
    public synchronized List<Block> addBlocks(Collection<Block> newBlocks) {
        for (Block block : newBlocks) {
        	m_blocks.put(block.getHash(), block);
        }
        List<Block> ret = new ArrayList<>();
        for (long i = m_headers.getMax(); i > m_minValidated; i--) {
            Block block = m_blocks.get(m_headers.get(i).getHash());
            if (block == null) break;
            ret.add(block);
            m_blocks.remove(m_headers.get(i).getHash());
            m_headers.remove(i);
        }
        return ret;
    }

    @Override
    public synchronized int getHeadersCount() {
        return m_headers.size();
    }
}

