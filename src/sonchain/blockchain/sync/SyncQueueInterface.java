package sonchain.blockchain.sync;

import java.util.Collection;
import java.util.List;

import sonchain.blockchain.core.Block;
import sonchain.blockchain.core.BlockHeaderWrapper;

public interface SyncQueueInterface {

    /**
     * Wanted headers
     */
    interface HeadersRequest {
        int getCount();
        byte[] getHash();
        long getStart();
        int getStep();
        boolean isReverse();
        List<HeadersRequest> split(int maxCount);
    }

    /**
     * Wanted blocks
     */
    interface BlocksRequest {
        List<BlockHeaderWrapper> getBlockHeaders();
        List<BlocksRequest> split(int count);
    }

    /**
     * Adds new received blocks to the queue
     * The blocks need to be verified but can be passed in any order and need not correspond
     * to prior returned block request
     * @return  blocks ready to be imported in the valid import order.
     */
    List<Block> addBlocks(Collection<Block> blocks);

    /**
     * Adds received headers.
     * Headers themselves need to be verified (except parent hash)
     * The list can be in any order and shouldn't correspond to prior headers request
     * @return If this is 'header-only' SyncQueue then the next chain of headers
     * is popped from SyncQueue and returned
     * The reverse implementation should return headers in revers order (N, N-1, ...)
     * If this instance is for headers+blocks downloading then null returned
     */
    List<BlockHeaderWrapper> addHeaders(Collection<BlockHeaderWrapper> headers);

    /**
     * Returns approximate header count waiting for their blocks
     */
    int getHeadersCount();

    /**
     * Returns wanted headers requests
     * @param maxSize Maximum number of headers in a singles request
     * @param maxRequests Maximum number of requests
     * @param maxTotalHeaders The total maximum of cached headers in the implementation
     * @return null if the end of headers reached (e.g. when download is limited with a block number)
     *   empty list if no headers for now (e.g. max allowed number of cached headers reached)
     */
    List<HeadersRequest> requestHeaders(int maxSize, int maxRequests, int maxTotalHeaders);

    /**
     * Returns wanted blocks hashes
     */
    BlocksRequest requestBlocks(int maxSize);
}