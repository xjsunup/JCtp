import java.util.concurrent.CountDownLatch;

/**
 * @author xuejian.sun
 * @date 2019/8/30 11:13
 */
public class CountWatch {

    /**
     * 测试结果跟踪器，计数器不是线程安全的，仅在单线程的 consumer 测试中使用；
     *
     * @author haiq
     */
    private long startTicks;
    private long endTicks;
    private long count = 0;
    private boolean end = false;
    private final long expectedCount;
    private CountDownLatch latch = new CountDownLatch(1);

    public CountWatch(long expectedCount) {
        this.expectedCount = expectedCount;
    }

    public void start() {
        startTicks = System.currentTimeMillis();
        end = false;
    }

    public long getMilliTimeSpan() {
        return endTicks - startTicks;
    }

    public boolean count() {
        if(end) {
            return end;
        }
        count++;
        end = count >= expectedCount;
        if(end) {
            endTicks = System.currentTimeMillis();
            latch.countDown();
        }
        return end;
    }

    public void waitForReached() throws InterruptedException {
        latch.await();
    }
}
