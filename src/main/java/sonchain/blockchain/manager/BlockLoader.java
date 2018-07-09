package sonchain.blockchain.manager;

import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Scanner;

import org.apache.log4j.Logger;
import org.bouncycastle.util.encoders.Hex;

import sonchain.blockchain.core.Block;
import sonchain.blockchain.core.BlockChainImpl;
import sonchain.blockchain.core.BlockHeader;
import sonchain.blockchain.core.ImportResult;
import sonchain.blockchain.core.Transaction;
import sonchain.blockchain.core.genesis.GenesisLoader;
import sonchain.blockchain.service.DataCenter;
import sonchain.blockchain.util.ExecutorPipeline;
import sonchain.blockchain.util.Functional;
import sonchain.blockchain.util.RLP;
import sonchain.blockchain.util.RLPElement;
import sonchain.blockchain.util.RLPList;
import sonchain.blockchain.validator.BlockHeaderValidator;

/**
 *
 */
public class BlockLoader {
    private DateFormat m_dfHHmmssSSSS = new SimpleDateFormat("HH:mm:ss.SSSS");
    ExecutorPipeline<Block, Block> m_executorPipeline1 = null;
    ExecutorPipeline<Block, ?> m_executorPipeline2 = null;
    private BlockHeaderValidator m_headerValidator = new BlockHeaderValidator();
    private Scanner m_scanner = null;
	public static final Logger m_logger = Logger.getLogger(BlockLoader.class);

    private void BlockWork(Block block) {
        if (block.getNumber() >= DataCenter.getSonChainImpl().getBlockChain().getBlockStore().getBestBlock().getNumber() 
        		|| DataCenter.getSonChainImpl().getBlockChain().getBlockStore().getBlockByHash(block.getHash()) == null) {
            if (block.getNumber() > 0 && !IsValid(block.getHeader())) {
                throw new RuntimeException();
            }

            long s = System.currentTimeMillis();
            ImportResult result = DataCenter.getSonChainImpl().getBlockChain().tryToConnect(block);

            if (block.getNumber() % 10 == 0) {
                System.out.println(m_dfHHmmssSSSS.format(new Date()) + " Imported block " + block.getShortDescr() 
                		+ ": " + result + " (prework: "
                        + m_executorPipeline1.GetQueue().size() + ", work: " + m_executorPipeline2.GetQueue().size()
                        + ", blocks: " + m_executorPipeline1.GetOrderMap().size() + ") in " +
                        (System.currentTimeMillis() - s) + " ms");
            }
        } else {
            if (block.getNumber() % 10000 == 0)
                System.out.println("Skipping block #" + block.getNumber());
        }
    }
    
    private boolean IsValid(BlockHeader header) {
        return m_headerValidator.validateAndLog(header);
    }

    public void LoadBlocks() {
    	m_executorPipeline1 = new ExecutorPipeline(8, 1000, true,
    			new Functional.Function<Block, Block>() {
            @Override
            public Block apply(Block b) {
                if (b.getNumber() >= DataCenter.getSonChainImpl().getBlockChain().getBlockStore().getBestBlock().getNumber()) {
                    for (Transaction tx : b.getTransactionsList()) {
                        tx.getSender();
                    }
                }
                return b;
            }
        }, new Functional.Consumer<Throwable>() {
            @Override
            public void accept(Throwable throwable) {
                m_logger.error("Unhandled exception: ", throwable);
            }
        });

    	m_executorPipeline2 = m_executorPipeline1.Add(1, 1000, new Functional.Consumer<Block>() {
            @Override
            public void accept(Block block) {
                try {
                	BlockWork(block);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        String fileSrc = DataCenter.m_config.m_blocksLoader;
        try {
            String blocksFormat = DataCenter.m_config.m_blocksFormat;
            m_logger.debug("Loading blocks: " + fileSrc + ", format: " + blocksFormat);

            if ("rlp".equalsIgnoreCase(blocksFormat)) {
                Path path = Paths.get(fileSrc);
                byte[] data = Files.readAllBytes(path);
                RLPList list = RLP.decode2(data);
                for (RLPElement item : list) {
                    Block block = new Block(item.getRLPData());
                    m_executorPipeline1.Push(block);
                }
            } else {                                        // hex string
                FileInputStream inputStream = new FileInputStream(fileSrc);
                m_scanner = new Scanner(inputStream, "UTF-8");
                while (m_scanner.hasNextLine()) {
                    byte[] blockRLPBytes = Hex.decode(m_scanner.nextLine());
                    Block block = new Block(blockRLPBytes);
                    m_executorPipeline1.Push(block);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }

        try {
        	m_executorPipeline1.Join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        //dbFlushManager.flushSync();
        m_logger.debug(" * Done * ");
        System.exit(0);
    }
}
