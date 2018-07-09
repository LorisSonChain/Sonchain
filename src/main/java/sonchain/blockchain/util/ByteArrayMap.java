package sonchain.blockchain.util;

import java.util.*;

import sonchain.blockchain.db.ByteArrayWrapper;

public class ByteArrayMap<V> implements Map<byte[], V> {
	private Map<ByteArrayWrapper, V> m_delegate = null;

	public ByteArrayMap() {
		this(new HashMap<ByteArrayWrapper, V>());
	}

	public ByteArrayMap(Map<ByteArrayWrapper, V> delegate) {
		m_delegate = delegate;
	}

	@Override
	public void clear() {
		m_delegate.clear();
	}

	@Override
	public boolean containsKey(Object key) {
		return m_delegate.containsKey(new ByteArrayWrapper((byte[]) key));
	}

	@Override
	public boolean containsValue(Object value) {
		return m_delegate.containsValue(value);
	}

	@Override
	public Set<Entry<byte[], V>> entrySet() {
		return new MapEntrySet(m_delegate.entrySet());
	}

	@Override
	public boolean equals(Object o) {
		return m_delegate.equals(o);
	}

	@Override
	public V get(Object key) {
		return m_delegate.get(new ByteArrayWrapper((byte[]) key));
	}

	@Override
	public int hashCode() {
		return m_delegate.hashCode();
	}

	@Override
	public boolean isEmpty() {
		return m_delegate.isEmpty();
	}

	@Override
	public Set<byte[]> keySet() {
		return new ByteArraySet(new SetAdapter<>(m_delegate));
	}
	
	@Override
	public String toString() {
		return m_delegate.toString();
	}

	@Override
	public V put(byte[] key, V value) {
		return m_delegate.put(new ByteArrayWrapper(key), value);
	}

	@Override
	public void putAll(Map<? extends byte[], ? extends V> m) {
		for (Entry<? extends byte[], ? extends V> entry : m.entrySet()) {
			m_delegate.put(new ByteArrayWrapper(entry.getKey()), entry.getValue());
		}
	}
	
	@Override
	public V remove(Object key) {
		return m_delegate.remove(new ByteArrayWrapper((byte[]) key));
	}

	@Override
	public int size() {
		return m_delegate.size();
	}

	@Override
	public Collection<V> values() {
		return m_delegate.values();
	}

	private class MapEntrySet implements Set<Map.Entry<byte[], V>> {
		
		private final Set<Map.Entry<ByteArrayWrapper, V>> m_delegate;

		private MapEntrySet(Set<Entry<ByteArrayWrapper, V>> delegate) {
			m_delegate = delegate;
		}

		@Override
		public boolean add(Entry<byte[], V> vEntry) {
			throw new RuntimeException("Not implemented");
		}
		
		@Override
		public boolean addAll(Collection<? extends Entry<byte[], V>> c) {
			throw new RuntimeException("Not implemented");
		}

		@Override
		public void clear() {
			throw new RuntimeException("Not implemented");
		}

		@Override
		public boolean contains(Object o) {
			throw new RuntimeException("Not implemented");
		}

		@Override
		public boolean containsAll(Collection<?> c) {
			throw new RuntimeException("Not implemented");
		}

		@Override
		public boolean isEmpty() {
			return m_delegate.isEmpty();
		}

		@Override
		public Iterator<Entry<byte[], V>> iterator() {
			final Iterator<Entry<ByteArrayWrapper, V>> it = m_delegate.iterator();
			return new Iterator<Entry<byte[], V>>() {

				@Override
				public boolean hasNext() {
					return it.hasNext();
				}

				@Override
				public Entry<byte[], V> next() {
					Entry<ByteArrayWrapper, V> next = it.next();
					return new AbstractMap.SimpleImmutableEntry(next.getKey().getData(), next.getValue());
				}

				@Override
				public void remove() {
					it.remove();
				}
			};
		}

		@Override
		public Object[] toArray() {
			throw new RuntimeException("Not implemented");
		}

		@Override
		public <T> T[] toArray(T[] a) {
			throw new RuntimeException("Not implemented");
		}
		
		@Override
		public boolean remove(Object o) {
			throw new RuntimeException("Not implemented");
		}

		@Override
		public boolean removeAll(Collection<?> c) {
			throw new RuntimeException("Not implemented");
		}

		@Override
		public boolean retainAll(Collection<?> c) {
			throw new RuntimeException("Not implemented");
		}

		@Override
		public int size() {
			return m_delegate.size();
		}
	}
}