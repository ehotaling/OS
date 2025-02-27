import java.util.concurrent.Semaphore;

public abstract class Process implements Runnable{

    // Each process should have a Java Thread and a Semaphore
    public Thread thread = new Thread(this);

    // Start at 0 so it the process doesn't run immediately
    private final Semaphore available = new Semaphore(0);

    public boolean isExpired = false;


    public Process() {
    }

    // sets the boolean indicating that this process’ quantum has expired
    public void requestStop() {
        isExpired = true;
    }

    // will represent the main of our “program”
    public abstract void main() throws InterruptedException;

    // indicates if the semaphore is 0
    public boolean isStopped() {
        return available.availablePermits() == 0;
    }

    // true when the Java thread is not alive
    public boolean isDone() {
        return !thread.isAlive();
    }

    // releases (increments) the semaphore, allowing this thread to run
    public void start() {
        available.release();
    }

    // acquires (decrements) the semaphore, stopping this thread from running
    public void stop() {
        available.acquireUninterruptibly();
    }

    // acquire the semaphore, then call main
    public void run() { // This is called by the Thread - NEVER CALL THIS!!!
        try {
            available.acquire();
            main();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    // if the boolean is true, set the boolean to false and call OS.switchProcess(), reset timeout counter if process cooperates
    public void cooperate() {
        if (isExpired) {
            isExpired = false;
            OS.switchProcess();
        }
    }
}
