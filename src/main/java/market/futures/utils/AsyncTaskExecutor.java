package market.futures.utils;

/**
 * @author xuejian.sun
 * @date 2019-10-04 10:44
 */
public interface AsyncTaskExecutor<T> {
    /**
     * 添加一个任务
     *
     * @param task task
     */
    void addTask(T task);

    /**
     * 是否已经停止，只是进入停止状态，已存在的任务可能还在进行
     *
     * @return boolean
     */
    boolean isShutdown();

    /**
     * 是否已经终止，Executor将不可用。
     *
     * @return boolean
     */
    boolean isTerminated();

    /**
     * 停止，当有任务存在时会延迟到任务全部处理完毕，在进行终止，期间提交的新任务都将被拒绝。
     */
    void shutdown();

    /**
     * 立刻停止
     */
    void shutdownNow();
}
