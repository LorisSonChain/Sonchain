package sonchain.blockchain.datasource;

import java.util.Set;

public interface DbSource<V> extends BatchSource<byte[], V> {

    /**
     * Closes the DB table/connection
     */
    void close();
    
    /**
     * @return DB name
     */
    String getName();

    /**
     * Initializes DB (open table, connection, etc)
     */
    void init();
    
    /**
     * @return true if DB connection is alive
     */
    boolean isAlive();

    /**
     * @return DB keys if this option is available
     * @throws RuntimeException if the method is not supported
     */
    Set<byte[]> keys() throws RuntimeException;

    /**
     * Sets the DB name.
     * This could be the underlying DB table/dir name
     */
    void setName(String name);
}

