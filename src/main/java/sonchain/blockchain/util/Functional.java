package sonchain.blockchain.util;

public interface Functional {

    interface Consumer<T> {

        void accept(T t);
    }

    interface BiConsumer<T, U> {
        void accept(T t, U u);
    }

    interface Function<T, R> {
        R apply(T t);
    }

    interface Supplier<T> {

        T get();
    }

    interface InvokeWrapper {        
        void invoke();
    }

    interface InvokeWrapperWithResult<R> {
        R invoke();
    }

    interface Predicate<T> {
        boolean test(T t);
    }
}
