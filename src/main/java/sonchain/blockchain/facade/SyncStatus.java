package sonchain.blockchain.facade;

public class SyncStatus {
	public enum SyncStage {
        PivotBlock,
        StateNodes,
        Headers,
        BlockBodies,
        Receipts,
        Regular,
        Complete,
        Off;

        /**
         * Indicates if this state represents ongoing FastSync
         */
        public boolean isFastSync() {
            return this == PivotBlock || this == StateNodes || this == Headers || this == BlockBodies || this == Receipts;
        }
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
    
    public SyncStage getStage() {
        return m_stage;
    }

    public long getCurCnt() {
        return m_curCnt;
    }
    public long getKnownCnt() {
        return m_knownCnt;
    }
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