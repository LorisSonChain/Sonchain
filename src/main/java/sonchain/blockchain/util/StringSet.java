package sonchain.blockchain.util;

import java.util.*;

import sonchain.blockchain.db.ByteArrayWrapper;
import sonchain.blockchain.db.StringWrapper;

public class StringSet implements Set<String> {
    Set<StringWrapper> m_delegate;

    public StringSet() {
        this(new HashSet<StringWrapper>());
    }

    StringSet(Set<StringWrapper> delegate) {
        this.m_delegate = delegate;
    }

    @Override
    public boolean add(String str) {
        return m_delegate.add(new StringWrapper(str));
    }

    @Override
    public boolean addAll(Collection<? extends String> strs) {
        boolean ret = false;
        for (String str : strs) {
            ret |= add(str);
        }
        return ret;
    }
    
    @Override
    public void clear() {
    	m_delegate.clear();
    }

    @Override
    public boolean contains(Object o) {
        return m_delegate.contains(new ByteArrayWrapper((byte[]) o));
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public boolean equals(Object o) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public int hashCode() {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public boolean isEmpty() {
        return m_delegate.isEmpty();
    }

    @Override
    public Iterator<String> iterator() {
        return new Iterator<String>() {
            Iterator<StringWrapper> it = m_delegate.iterator();
            @Override
            public boolean hasNext() {
                return it.hasNext();
            }

            @Override
            public String next() {
                return it.next().getData();
            }

            @Override
            public void remove() {
                it.remove();
            }
        };
    }

    @Override
    public Object[] toArray() {
        byte[][] ret = new byte[size()][];

        ByteArrayWrapper[] arr = m_delegate.toArray(new ByteArrayWrapper[size()]);
        for (int i = 0; i < arr.length; i++) {
            ret[i] = arr[i].getData();
        }
        return ret;
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return (T[]) toArray();
    }

    @Override
    public boolean remove(Object o) {
        return m_delegate.remove(new ByteArrayWrapper((byte[]) o));
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
