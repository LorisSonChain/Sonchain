package sonchain.blockchain.datasource;

public class SourceCodec<Key, Value, SourceKey, SourceValue>
		extends AbstractChainedSource<Key, Value, SourceKey, SourceValue> {

	protected Serializer<Key, SourceKey> m_keySerializer;
	protected Serializer<Value, SourceValue> m_valSerializer;

	/**
	 * Instantiates class
	 * 
	 * @param src
	 *            Backing Source
	 * @param keySerializer
	 *            Key codec Key <=> SourceKey
	 * @param valSerializer
	 *            Value codec Value <=> SourceValue
	 */
	public SourceCodec(Source<SourceKey, SourceValue> src, Serializer<Key, SourceKey> keySerializer,
			Serializer<Value, SourceValue> valSerializer) {
		super(src);
		m_keySerializer = keySerializer;
		m_valSerializer= valSerializer;
		setFlushSource(true);
	}

	@Override
	public void delete(Key key) {
		getSource().delete(m_keySerializer.serialize(key));
	}

	@Override
	public boolean flushImpl() {
		return false;
	}

	@Override
	public Value get(Key key) {
		return m_valSerializer.deserialize(getSource().get(m_keySerializer.serialize(key)));
	}

	@Override
	public void put(Key key, Value val) {
		getSource().put(m_keySerializer.serialize(key), m_valSerializer.serialize(val));
	}

	/**
	 * Shortcut class when only value conversion is required
	 */
	public static class ValueOnly<Key, Value, SourceValue> extends SourceCodec<Key, Value, Key, SourceValue> {
		public ValueOnly(Source<Key, SourceValue> src, Serializer<Value, SourceValue> valSerializer) {
			super(src, new Serializers.Identity<Key>(), valSerializer);
		}
	}

	/**
	 * Shortcut class when only value conversion is required and keys are of
	 * byte[] type
	 */
	public static class BytesKey<Value, SourceValue> extends ValueOnly<byte[], Value, SourceValue> {
		public BytesKey(Source<byte[], SourceValue> src, Serializer<Value, SourceValue> valSerializer) {
			super(src, valSerializer);
		}
	}
}
