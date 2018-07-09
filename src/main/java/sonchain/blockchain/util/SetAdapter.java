package sonchain.blockchain.util;

import java.util.*;

public class SetAdapter<E> implements Set<E> {
	
    private static final Object DummyValue = new Object();
    private Map<E, Object> m_delegate;

    public SetAdapter(Map<E, ?> delegate) {
        this.m_delegate = (Map<E, Object>) delegate;
    }

    @Override
    public boolean add(E e) {
        return m_delegate.put(e, DummyValue) == null;
    }

    @Override
    public boolean addAll(Collection<? extends E> c) {
        boolean ret = false;
        for (E e : c) {
            ret |= add(e);
        }
        return ret;
    }

    @Override
    public void clear() {
    	m_delegate.clear();
    }

    @Override
    public boolean contains(Object o) {
        return m_delegate.containsKey(o);
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return m_delegate.keySet().containsAll(c);
    }

    @Override
    public boolean isEmpty() {
        return m_delegate.isEmpty();
    }

    @Override
    public Iterator<E> iterator() {
        return m_delegate.keySet().iterator();
    }

    @Override
    public Object[] toArray() {
        return m_delegate.keySet().toArray();
    }
    
    @Override
    public <T> T[] toArray(T[] a) {
        return m_delegate.keySet().toArray(a);
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        boolean ret = false;
        for (Object e : c) {
            ret |= remove(e);
        }
        return ret;
    }
    
    @Override
    public boolean remove(Object o) {
        return m_delegate.remove(o) != null;
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        throw new RuntimeException("Not implemented"); // TODO add later if required
    }

    @Override
    public int size() {
        return m_delegate.size();
    }
}
