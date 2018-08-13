package sonchain.blockchain.datasource;

import sonchain.blockchain.accounts.AccountState;
import sonchain.blockchain.core.BlockHeader;
import sonchain.blockchain.datasource.base.Serializer;
import sonchain.blockchain.util.RLP;
import sonchain.blockchain.util.Value;
import sonchain.blockchain.vm.DataWord;

public class Serializers {

    /**
     *  No conversion
     */
    public static class Identity<T> implements Serializer<T, T> {
        @Override
        public T serialize(T object) {
            return object;
        }
        @Override
        public T deserialize(T stream) {
            return stream;
        }
    }

    /**
     * Serializes/Deserializes AccountState instances from the State Trie (part of Ethereum spec)
     */
    public final static Serializer<AccountState, byte[]> AccountStateSerializer = new Serializer<AccountState, byte[]>() {
        @Override
        public byte[] serialize(AccountState object) {
            return object.getEncoded();
        }

        @Override
        public AccountState deserialize(byte[] stream) {
            return stream == null || stream.length == 0 ? null : new AccountState(stream);
        }
    };

    /**
     * Contract storage key serializer
     */
    public final static Serializer<DataWord, byte[]> StorageKeySerializer = new Serializer<DataWord, byte[]>() {
        @Override
        public byte[] serialize(DataWord object) {
            return object.getData();
        }

        @Override
        public DataWord deserialize(byte[] stream) {
            return new DataWord(stream);
        }
    };

    /**
     * Contract storage value serializer (part of Ethereum spec)
     */
    public final static Serializer<DataWord, byte[]> StorageValueSerializer = new Serializer<DataWord, byte[]>() {
        @Override
        public byte[] serialize(DataWord object) {
            return RLP.encodeElement(object.getNoLeadZeroesData());
        }

        @Override
        public DataWord deserialize(byte[] stream) {
            if (stream == null || stream.length == 0) return null;
            byte[] dataDecoded = RLP.decode2(stream).get(0).getRLPData();
            return new DataWord(dataDecoded);
        }
    };

    /**
     * Trie node serializer (part of Ethereum spec)
     */
    public final static Serializer<Value, byte[]> TrieNodeSerializer = new Serializer<Value, byte[]>() {
        @Override
        public byte[] serialize(Value object) {
            return object.encode();
        }

        @Override
        public Value deserialize(byte[] stream) {
            return Value.fromRlpEncoded(stream);
        }
    };

    /**
     * Trie node serializer (part of Ethereum spec)
     */
    public final static Serializer<BlockHeader, String> BlockHeaderSerializer = new Serializer<BlockHeader, String>() {
        @Override
        public String serialize(BlockHeader object) {
            return object == null ? null : object.toJson();
        }

        @Override
        public BlockHeader deserialize(String stream) {
        	if(stream == null){
        		return null;
        	}
        	else
        	{
        		BlockHeader header = new BlockHeader();
        		header.jsonParse(stream);
        		return header;
        	}
        }
    };
}