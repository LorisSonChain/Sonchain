package sonchain.blockchain.core;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public abstract class FutureAdapter<T, S> implements Future<T> {

	private final Future<S> adaptee;

	private Object result = null;

	private State state = State.NEW;

	private final Object mutex = new Object();

	/**
	 * Constructs a new {@code FutureAdapter} with the given adaptee.
	 * @param adaptee the future to delegate to
	 */
	protected FutureAdapter(Future<S> adaptee) {
		//Assert.notNull(adaptee, "'delegate' must not be null");
		this.adaptee = adaptee;
	}

	@Override
	public boolean cancel(boolean mayInterruptIfRunning) {
		return adaptee.cancel(mayInterruptIfRunning);
	}

	@Override
	public boolean isCancelled() {
		return adaptee.isCancelled();
	}

	@Override
	public boolean isDone() {
		return adaptee.isDone();
	}

	@Override
	public T get() throws InterruptedException, ExecutionException {
		return adaptInternal(adaptee.get());
	}

	@Override
	public T get(long timeout, TimeUnit unit)
			throws InterruptedException, ExecutionException, TimeoutException {
		return adaptInternal(adaptee.get(timeout, unit));
	}

	/**
	 * Returns the adaptee.
	 */
	protected Future<S> adaptee() {
		return adaptee;
	}

	@SuppressWarnings("unchecked")
	final T adaptInternal(S adapteeResult) throws ExecutionException {
		synchronized (mutex) {
			switch (state) {
				case SUCCESS:
					return (T) result;
				case FAILURE:
					if (result instanceof ExecutionException) {
						throw (ExecutionException) result;
					}
					else {
						throw new ExecutionException((Throwable) result);
					}
				case NEW:
					try {
						T adapted = adapt(adapteeResult);
						result = adapted;
						state = State.SUCCESS;
						return adapted;
					} catch (Throwable ex) {
						result = ex;
						state = State.FAILURE;
						throw ex;
					}
				default:
					throw new IllegalStateException();
			}
		}
	}

	/**
	 * Adapts the given adaptee's result into T.
	 * @return the adapted result
	 */
	protected abstract T adapt(S adapteeResult) throws ExecutionException;

	private enum State {NEW, SUCCESS, FAILURE}

}
