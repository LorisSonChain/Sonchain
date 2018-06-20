package sonchain.blockchain.net;

import java.util.*;

/**
 * A list of commands for the SonChain network protocol.
 * <br>
 * The codes for these commands are the first byte in every packet.
 *
 */
public enum SonMessageCodes {
	  /* SonChain protocol */

    /**
     * {@code [0x00, [PROTOCOL_VERSION, NETWORK_ID, TD, BEST_HASH, GENESIS_HASH] } <br>
     *
     * Inform a peer of it's current ethereum state. This message should be
     * send after the initial handshake and prior to any ethereum related messages.
     */
    STATUS(0x00),

    /**
     * PV 61 and lower <br>
     * {@code [+0x01, [hash_0: B_32, hash_1: B_32, ...] } <br>
     *
     * PV 62 and upper <br>
     * {@code [+0x01: P, [hash_0: B_32, number_0: P], [hash_1: B_32, number_1: P], ...] } <br>
     *
     * Specify one or more new blocks which have appeared on the network.
     * To be maximally helpful, nodes should inform peers of all blocks that they may not be aware of.
     * Including hashes that the sending peer could reasonably be considered to know
     * (due to the fact they were previously informed of because
     * that node has itself advertised knowledge of the hashes through NewBlockHashes)
     * is considered Bad Form, and may reduce the reputation of the sending node.
     * Including hashes that the sending node later refuses to honour with a proceeding
     * GetBlocks message is considered Bad Form, and may reduce the reputation of the sending node.
     *
     */
    NEW_BLOCK_HASHES(0x01),

    /**
     * {@code [+0x02, [nonce, receiving_address, value, ...], ...] } <br>
     *
     * Specify (a) transaction(s) that the peer should make sure is included
     * on its transaction queue. The items in the list (following the first item 0x12)
     * are transactions in the format described in the main Ethereum specification.
     */
    TRANSACTIONS(0x02),

    /**
     * {@code [+0x03: P, block: { P , B_32 }, maxHeaders: P, skip: P, reverse: P in { 0 , 1 } ] } <br>
     *
     * Replaces GetBlockHashes since PV 62. <br>
     *
     * Require peer to return a BlockHeaders message.
     * Reply must contain a number of block headers,
     * of rising number when reverse is 0, falling when 1, skip blocks apart,
     * beginning at block block (denoted by either number or hash) in the canonical chain,
     * and with at most maxHeaders items.
     */
    GET_BLOCK_HEADERS(0x03),

    /**
     * {@code [+0x04, blockHeader_0, blockHeader_1, ...] } <br>
     *
     * Replaces BLOCK_HASHES since PV 62. <br>
     *
     * Reply to GetBlockHeaders.
     * The items in the list (following the message ID) are
     * block headers in the format described in the main Ethereum specification,
     * previously asked for in a GetBlockHeaders message.
     * This may validly contain no block headers
     * if no block headers were able to be returned for the GetBlockHeaders query.
     */
    BLOCK_HEADERS(0x04),

    /**
     * {@code [+0x05, hash_0: B_32, hash_1: B_32, ...] } <br>
     *
     * Replaces GetBlocks since PV 62. <br>
     *
     * Require peer to return a BlockBodies message.
     * Specify the set of blocks that we're interested in with the hashes.
     */
    GET_BLOCK_BODIES(0x05),

    /**
     * {@code [+0x06, [transactions_0, uncles_0] , ...] } <br>
     *
     * Replaces Blocks since PV 62. <br>
     *
     * Reply to GetBlockBodies.
     * The items in the list (following the message ID) are some of the blocks, minus the header,
     * in the format described in the main Ethereum specification, previously asked for in a GetBlockBodies message.
     * This may validly contain no block headers
     * if no block headers were able to be returned for the GetBlockHeaders query.
     */
    BLOCK_BODIES(0x06),

    /**
     * {@code [+0x07 [blockHeader, transactionList, uncleList], totalDifficulty] } <br>
     *
     * Specify a single block that the peer should know about. The composite item
     * in the list (following the message ID) is a block in the format described
     * in the main Ethereum specification.
     */
    NEW_BLOCK(0x07),

    /**
     * {@code [+0x0d, hash_0: B_32, hash_1: B_32, ...] } <br>
     *
     * Require peer to return a NodeData message. Hint that useful values in it
     * are those which correspond to given hashes.
     */
    GET_NODE_DATA(0x0d),

    /**
     * {@code [+0x0e, value_0: B, value_1: B, ...] } <br>
     *
     * Provide a set of values which correspond to previously asked node data
     * hashes from GetNodeData. Does not need to contain all; best effort is
     * fine. If it contains none, then has no information for previous
     * GetNodeData hashes.
     */
    NODE_DATA(0x0e),

    /**
     * {@code [+0x0f, hash_0: B_32, hash_1: B_32, ...] } <br>
     *
     * Require peer to return a Receipts message. Hint that useful values in it
     * are those which correspond to blocks of the given hashes.
     */
    GET_RECEIPTS(0x0f),

    /**
     * {@code [+0x10, [receipt_0, receipt_1], ...] } <br>
     *
     * Provide a set of receipts which correspond to previously asked in GetReceipts.
     */
    RECEIPTS(0x10);

    private int m_cmd;

    private static final Map<Integer, SonMessageCodes> m_intToTypeMap = new HashMap<>();
    private static SonMessageCodes[] m_versionToValuesMap = null;

    static {
    	m_versionToValuesMap = new SonMessageCodes[]{
                STATUS,
                NEW_BLOCK_HASHES,
                TRANSACTIONS,
                GET_BLOCK_HEADERS,
                BLOCK_HEADERS,
                GET_BLOCK_BODIES,
                BLOCK_BODIES,
                NEW_BLOCK,
                GET_NODE_DATA,
                NODE_DATA,
                GET_RECEIPTS,
                RECEIPTS
        };

        for (SonMessageCodes code : values()) {
        	m_intToTypeMap.put(code.m_cmd, code);
        }
    }

    private SonMessageCodes(int cmd) {
    	m_cmd = cmd;
    }

    public static SonMessageCodes[] values(int i) {
        return m_versionToValuesMap;
    }

    public static SonMessageCodes fromByte(byte i) {
        return m_intToTypeMap.get((int) i);
    }

    public static boolean inRange(byte code) {
    	SonMessageCodes[] codes = values(1);
        return code >= codes[0].asByte() && code <= codes[codes.length - 1].asByte();
    }

    public byte asByte() {
        return (byte) (m_cmd);
    }
}
