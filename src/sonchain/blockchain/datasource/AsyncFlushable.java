package sonchain.blockchain.datasource;

import com.google.common.util.concurrent.ListenableFuture;

public interface AsyncFlushable {

    /**
     * Does async flush, i.e. returns immediately while starts doing flush in a separate thread
     * This call may still block if the previous flush is not complete yet
     *
     * @return Future when the actual flush is complete
     */
    ListenableFuture<Boolean> flushAsync() throws InterruptedException;

    /**
     * Flip the backing storage so the current state will be flushed
     * when call {@link #flushAsync()} and all the newer changes will
     * be collected to a new backing store and will be flushed only on
     * subsequent flush call
     *
     * The method is intended to make consistent flush from several
     * sources. I.e. at some point all the related Sources are flipped
     * synchronously first (this doesn't consume any time normally) and then
     * are flushed asynchronously
     *
     * This call may block until a previous flush is completed (if still in progress)
     *
     * @throws InterruptedException
     */
    void flipStorage() throws InterruptedException;
}
