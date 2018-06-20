package sonchain.blockchain.datasource;

public interface Serializer<T, S> {
    /**
     * Converts S ==> T
     * Should correctly handle null parameter
     */
    T deserialize(S stream);
    /**
     * Converts T ==> S
     * Should correctly handle null parameter
     */
    S serialize(T object);
}
