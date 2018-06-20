package sonchain.blockchain.facade;

public class SyncStatus {
	public enum SyncStage {
        /**
         * Fast sync: looking for a Pivot block.
         * Normally we need several peers to select the block but
         * the block can be selected from existing peers due to timeout
         */
        PivotBlock,
        /**
         * Fast sync: downloading state trie nodes and importing blocks
         */
        StateNodes,
        /**
         * Fast sync: downloading headers for securing the latest state
         */
        Headers,
        /**
         * Fast sync: downloading blocks
         */
        BlockBodies,
        /**
         * Fast sync: downloading receipts
         */
        Receipts,
        /**
         * Regular sync is in progress
         */
        Regular,
        /**
         * Sync is complete:
         * Fast sync: the state is secure, all blocks and receipt are downloaded
         * Regular sync: all blocks are imported up to the blockchain head
         */
        Complete,
        /**
         * Syncing is turned off
         */
        Off;

        /**
         * Indicates if this state represents ongoing FastSync
         */
        public boolean isFastSync() {
            return this == PivotBlock || this == StateNodes || this == Headers || this == BlockBodies || this == Receipts;
        }

        /**
         * Indicates the current state is secure
         *
         * When doing fast sync UNSECURE sync means that the full state is downloaded,
         * chain is on the latest block, and blockchain operations may be executed
         * (such as state querying, transaction submission)
         * but the state isn't yet confirmed with  the whole block chain and can't be
         * trusted.
         * At this stage historical blocks and receipts are unavailable yet
         *
         * SECURE sync means that the full state is downloaded,
         * chain is on the latest block, and blockchain operations may be executed
         * (such as state querying, transaction submission)
         * The state is now confirmed by the full chain (all block headers are
         * downloaded and verified) and can be trusted
         * At this stage historical blocks and receipts are unavailable yet
         */
        public boolean isSecure() {
            return this != PivotBlock || this != StateNodes && this != Headers;
        }

        /**
         * Indicates the blockchain state is up-to-date
         * Warning: the state could still be non-secure
         */
        public boolean hasLatestState() {
            return this == Headers || this == BlockBodies || this == Receipts || this == Complete;
        }
    }

    private long m_blockBestKnown = 0;
    private long m_blockLastImported = 0;
    private long m_curCnt = 0;
    private long m_knownCnt = 0;
    private SyncStage m_stage;

    public SyncStatus(SyncStatus state, long blockLastImported, long blockBestKnown) {
        this(state.getStage(), state.getCurCnt(), state.getKnownCnt(), blockLastImported, blockBestKnown);
    }

    public SyncStatus(SyncStage stage, long curCnt, long knownCnt, long blockLastImported, long blockBestKnown) {
    	m_stage = stage;
    	m_curCnt = curCnt;
    	m_knownCnt = knownCnt;
    	m_blockLastImported = blockLastImported;
        m_blockBestKnown = blockBestKnown;
    }

    public SyncStatus(SyncStage stage, long curCnt, long knownCnt) {
        this(stage, curCnt, knownCnt, 0, 0);
    }

    /**
     * Gets the current stage of syncing
     */
    public SyncStage getStage() {
        return m_stage;
    }

    /**
     * Gets the current count of items processed for this syncing stage :
     * PivotBlock: number of seconds pivot block is searching for
     *          ( this number can be greater than getKnownCnt() if no peers found)
     * StateNodes: number of trie nodes downloaded
     * Headers: number of headers downloaded
     * BlockBodies: number of block bodies downloaded
     * Receipts: number of blocks receipts are downloaded for
     */
    public long getCurCnt() {
        return m_curCnt;
    }

    /**
     * Gets the known count of items for this syncing stage :
     * PivotBlock: number of seconds pivot is forced to be selected
     * StateNodes: number of currently known trie nodes. This number is not constant as new nodes
     *             are discovered when their parent is downloaded
     * Headers: number of headers to be downloaded
     * BlockBodies: number of block bodies to be downloaded
     * Receipts: number of blocks receipts are to be downloaded for
     */
    public long getKnownCnt() {
        return m_knownCnt;
    }

    /**
     * Reflects the blockchain state: the latest imported block
     * Blocks importing can run in parallel with other sync stages
     * (like header/blocks/receipts downloading)
     */
    public long getBlockLastImported() {
        return m_blockLastImported;
    }

    /**
     * Return the best known block from other peers
     */
    public long getBlockBestKnown() {
        return m_blockBestKnown;
    }

    @Override
    public String toString() {
        return m_stage +
                (m_stage != SyncStage.Off && m_stage != SyncStage.Complete ? " (" + getCurCnt() + " of " + getKnownCnt() + ")" : "") +
                ", last block #" + getBlockLastImported() + ", best known #" + getBlockBestKnown();
    }
}