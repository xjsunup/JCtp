package market.futures.utils;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.LongAdder;

/** 自定义线程工厂
 *
 * @author xuejian.sun
 * @date 2019-03-21 19:10
 */
public class NamedThreadFactory implements ThreadFactory {

    private LongAdder longAdder = new LongAdder();
    /**
     * 线程池名
     */
    private String threadPoolName;
    /**
     * 是否守护线程
     */
    private boolean isDaemon;

    public NamedThreadFactory(String threadPoolName, boolean isDaemon) {
        this.threadPoolName = threadPoolName;
        this.isDaemon = isDaemon;
    }

    @Override
    public Thread newThread(Runnable runnable) {
        Thread thread = new Thread(runnable);
        thread.setName(threadPoolName + longAdder.intValue());
        thread.setDaemon(isDaemon);
        longAdder.increment();
        return thread;
    }
}
