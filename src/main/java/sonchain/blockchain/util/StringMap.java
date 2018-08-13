package sonchain.blockchain.util;

import java.util.AbstractMap;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import sonchain.blockchain.db.StringWrapper;

public class StringMap<V> implements Map<String, V> {
	
	private Map<StringWrapper, V> m_delegate = null;

	public StringMap() {
		this(new HashMap<StringWrapper, V>());
	}

	public StringMap(Map<StringWrapper, V> delegate) {
		m_delegate = delegate;
	}

	@Override
	public void clear() {
		m_delegate.clear();
	}

	@Override
	public boolean containsKey(Object key) {
		return m_delegate.containsKey(new StringWrapper((String)key));
	}

	@Override
	public boolean containsValue(Object value) {
		return m_delegate.containsValue(value);
	}

	@Override
	public Set<Entry<String, V>> entrySet() {
		return new MapEntrySet(m_delegate.entrySet());
	}

	@Override
	public boolean equals(Object o) {
		return m_delegate.equals(o);
	}

	@Override
	public V get(Object key) {
		return m_delegate.get(new StringWrapper((String) key));
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
	public Set<String> keySet() {
		return new StringSet(new SetAdapter<>(m_delegate));
	}
	
	@Override
	public String toString() {
		return m_delegate.toString();
	}

	@Override
	public V put(String key, V value) {
		return m_delegate.put(new StringWrapper(key), value);
	}

	@Override
	public void putAll(Map<? extends String, ? extends V> m) {
		for (Entry<? extends String, ? extends V> entry : m.entrySet()) {
			m_delegate.put(new StringWrapper(entry.getKey()), entry.getValue());
		}
	}
	
	@Override
	public V remove(Object key) {
		return m_delegate.remove(new StringWrapper((String) key));
	}

	@Override
	public int size() {
		return m_delegate.size();
	}

	@Override
	public Collection<V> values() {
		return m_delegate.values();
	}

	private class MapEntrySet implements Set<Map.Entry<String, V>> {
		
		private final Set<Map.Entry<StringWrapper, V>> m_delegate;

		private MapEntrySet(Set<Entry<StringWrapper, V>> delegate) {
			m_delegate = delegate;
		}

		@Override
		public boolean add(Entry<String, V> vEntry) {
			throw new RuntimeException("Not implemented");
		}
		
		@Override
		public boolean addAll(Collection<? extends Entry<String, V>> c) {
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
		public Iterator<Entry<String, V>> iterator() {
			final Iterator<Entry<StringWrapper, V>> it = m_delegate.iterator();
			return new Iterator<Entry<String, V>>() {

				@Override
				public boolean hasNext() {
					return it.hasNext();
				}

				@Override
				public Entry<String, V> next() {
					Entry<StringWrapper, V> next = it.next();
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
