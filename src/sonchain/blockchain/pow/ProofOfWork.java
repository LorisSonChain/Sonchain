package sonchain.blockchain.pow;

import java.math.BigInteger;
import org.apache.commons.codec.digest.DigestUtils;

import sonchain.blockchain.core.BlockHeader;
import sonchain.blockchain.util.ByteUtil;

public class ProofOfWork {

    /**
     * 区块
     */
    public BlockHeader m_block;

    /**
     * 初始化
     * @param block
     */
    private ProofOfWork(BlockHeader block) {
    	m_block = block;
    }

    /**
     * 创建新的工作量证明，设定难度目标值
     *
     * @param block
     * @return
     */
    public static ProofOfWork newProofOfWork(BlockHeader block) {
        return new ProofOfWork(block);
    }

    /**
     * 运行工作量证明，开始挖矿，找到小于难度目标值的Hash
     *
     * @return
     */
    public PowResult run() {
    	return null;
//        byte[] nonce = m_block.m_nonce;
//        String shaHex = "";
//        byte[] data = null;
//        //System.out.printf("Mining the block containing：%s \n", this.getBlock().getData());
//        long startTime = System.currentTimeMillis();
//        while (nonce < Integer.MAX_VALUE) {
//            data = this.prepareData(BigInteger.valueOf(nonce));
//            shaHex = DigestUtils.sha256Hex(data);
//            if (new BigInteger(shaHex, 16).compareTo(m_block.m_nBits) == -1) {
//                System.out.printf("Elapsed Time: %s seconds \n",
//                		(float) (System.currentTimeMillis() - startTime) / 1000);
//                System.out.printf("correct nonce: %s \n\n", nonce);
//                System.out.printf("shaHex: %s \n\n", shaHex);
//                break;
//            } else {
//                nonce++;
//            }
//        }
//        return new PowResult(data, BigInteger.valueOf(nonce));
    }

    /**
     * 验证区块是否有效
     *
     * @return
     */
    public boolean validate() {
    	return false;
        //byte[] data = this.prepareData(m_block.m_nonce);
        //return new BigInteger(DigestUtils.sha256Hex(data), 16).compareTo(m_block.m_nBits) == -1;
    }

    /**
     * 准备数据
     * 注意：在准备区块数据时，一定要从原始数据类型转化为byte[]，不能直接从字符串进行转换
     *
     * @param nonce
     * @return
     */
    private byte[] prepareData(BigInteger nonce) {
    	return null;
//    	byte[] prevBlockHash;
//    	byte[] merkleRoot;
//    	if(m_block.m_prevBlockHash != null){
//    		prevBlockHash = m_block.m_prevBlockHash;
//    	}
//    	else{
//    		prevBlockHash = Uint256.DEFAULT;    		
//    	}
//    	if(m_block.m_merkleRoot != null){
//    		merkleRoot = m_block.m_merkleRoot;
//    	}
//    	else{
//    		merkleRoot = Uint256.DEFAULT;    		
//    	}
//        return ByteUtil.merge(
//        		prevBlockHash.getValue().toByteArray(),
//        		merkleRoot.getValue().toByteArray(),
//                ByteUtil.toBytes(m_block.m_timestamp),
//                m_block.m_nBits.toByteArray(),
//                nonce.toByteArray()
//        );
    }
}
