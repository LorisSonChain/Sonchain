package sonchain.blockchain.util;

import java.util.concurrent.locks.Lock;

public final class CLock implements AutoCloseable {
    private final Lock m_lock;

    public CLock(Lock l) {
    	m_lock = l;
    }

    public final CLock lock() {
    	m_lock.lock();
        return this;
    }

    public final void close() {
    	m_lock.unlock();
    }
}
