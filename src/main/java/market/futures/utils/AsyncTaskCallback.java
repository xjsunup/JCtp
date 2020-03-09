package market.futures.utils;

/**
 * @author xuejian.sun
 * @date 2019/8/21 14:32
 */
@FunctionalInterface
public interface AsyncTaskCallback<T> {
    /**
     * msg callback
     * @return t
     */
    void call(T t);
}
