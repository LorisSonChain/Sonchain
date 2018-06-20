package sonchain.blockchain.util;

public interface Functional {

    /**
     * 表示接受单个输入参数并没有返回结果的操作。 
     * 与大多数其他功能接口不同，{@code Consumer}预计将通过副作用操作。
     *
     * @param <T> 操作类型
     */
    interface Consumer<T> {

        /**
         * 根据输入参数，执行指定的操作
         *
         * @param t 输入参数
         */
        void accept(T t);
    }

    /**
     * Represents an operation that accepts two input arguments and returns no
     * result.  This is the two-arity specialization of {@link java.util.function.Consumer}.
     * Unlike most other functional interfaces, {@code BiConsumer} is expected
     * to operate via side-effects.
     *
     * @param <T> the type of the first argument to the operation
     * @param <U> the type of the second argument to the operation
     *
     * @see org.ethereum.util.Functional.Consumer
     */
    interface BiConsumer<T, U> {

        /**
         * Performs this operation on the given arguments.
         *
         * @param t the first input argument
         * @param u the second input argument
         */
        void accept(T t, U u);
    }


    /**
     * 表示一个方法，接收一个输入，返回一个输出
     *
     * @param <T> 方法输入的类型
     * @param <R> t方法输出的类型
     */
    interface Function<T, R> {

        /**
         * 执行
         *
         * @param t 输入参数
         * @return 方法结果
         */
        R apply(T t);
    }

    interface Supplier<T> {

        /**
         * Gets a result.
         *
         * @return a result
         */
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
