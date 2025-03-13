import java.util.concurrent.CountDownLatch;

public class SysCallResult {
    public final CountDownLatch latch = new CountDownLatch(1);
    public Object value;
}
