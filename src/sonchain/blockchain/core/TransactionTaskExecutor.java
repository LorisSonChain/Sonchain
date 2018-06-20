package sonchain.blockchain.core;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import sonchain.blockchain.net.submit.TransactionTask;

public class TransactionTaskExecutor {

    static {
        instance = new TransactionTaskExecutor();
    }

    public static TransactionTaskExecutor instance;
    private ExecutorService executor = Executors.newFixedThreadPool(1);

    public Future<List<Transaction>> submitTransaction(TransactionTask task) {
        return executor.submit(task);
    }
}
