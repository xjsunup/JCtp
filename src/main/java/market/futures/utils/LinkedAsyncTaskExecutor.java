package market.futures.utils;

import lombok.extern.slf4j.Slf4j;

import java.util.Objects;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.LongAdder;

/**
 * @author xuejian.sun
 * @date 2019/8/21 14:13
 */
@Slf4j
public class LinkedAsyncTaskExecutor<T> implements AsyncTaskExecutor<T> {

    private LinkedBlockingQueue<T> tasks;

    private ExecutorService singleThread;

    private AtomicBoolean running;

    private AsyncTaskCallback<T> taskCallback;

    private LongAdder longAdder;

    private AtomicBoolean shutdown;

    private String executorName = "AsyncTaskExecutor";

    public LinkedAsyncTaskExecutor(AsyncTaskCallback<T> taskCallback) {
        this(null, taskCallback);
    }

    public LinkedAsyncTaskExecutor(String executorName, AsyncTaskCallback<T> taskCallback) {
        this(Integer.MAX_VALUE, executorName, taskCallback);
    }

    public LinkedAsyncTaskExecutor(int initSize, String executorName, AsyncTaskCallback<T> taskCallback) {
        this.taskCallback = Objects.requireNonNull(taskCallback, "AsyncTaskCallback cannot must be null!");
        if(initSize == 0) {
            throw new IllegalArgumentException("task queue length is at least 1 !");
        }
        this.longAdder = new LongAdder();
        if(executorName != null && !executorName.isEmpty()) {
            this.executorName = executorName;
        }
        this.tasks = new LinkedBlockingQueue<>(initSize);
        this.singleThread = new ThreadPoolExecutor(1, 1, 0, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(1), new NamedThreadFactory(this.executorName, true));
        this.running = new AtomicBoolean(true);
        this.shutdown = new AtomicBoolean(false);
        pollerMsg();
    }

    /**
     * 添加任务
     *
     * @param task 任务
     */
    @Override
    public void addTask(T task) {
        if(Objects.isNull(task)) {
            return;
        }
        if(isShutdown()) {
            throw new RejectedExecutionException("LinkedAsyncTaskExecutor is shutdown!");
        }
        try {
            tasks.put(task);
        } catch (InterruptedException e) {
            log.error("put task failure", e);
        }
    }

    private void pollerMsg() {
        singleThread.execute(() -> {
            while(running.get() && !Thread.interrupted()) {
                try {
                    T task = tasks.poll(100, TimeUnit.MILLISECONDS);
                    if(Objects.isNull(task)) {
                        continue;
                    }
                    longAdder.increment();
                    taskCallback.call(task);
                } catch (Exception e) {
                    log.error("process task failure", e);
                }
            }
        });
    }

    /**
     * 已经处理结束的任务
     *
     * @return task size
     */
    public long getTerminatedTasks() {
        return longAdder.longValue();
    }

    public long getRunningTaskSize() {
        return tasks.size();
    }

    @Override
    public boolean isShutdown() {
        return shutdown.get();
    }

    /**
     * 中止任务提交，等待队列中所有任务结束
     */
    @Override
    public void shutdown() {
        if(singleThread != null && !singleThread.isShutdown()) {
            singleThread.shutdown();
        }
        shutdown.compareAndSet(false, true);
        while(tasks.size() != 0) {
            // Wait for all tasks in the task queue to end
        }
        boolean stop = this.running.compareAndSet(true, false);
        if(stop) {
            log.info("all task is done!");
        }
    }

    @Override
    public void shutdownNow() {
        if(singleThread != null && !singleThread.isShutdown()) {
            singleThread.shutdownNow();
        }
        shutdown.compareAndSet(false, true);
    }

    @Override
    public boolean isTerminated() {
        return singleThread == null || singleThread.isTerminated();
    }
}
