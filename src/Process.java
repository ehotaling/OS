import java.util.concurrent.Semaphore;

public abstract class Process implements Runnable{

    // Each process should have a Java Thread and a Semaphore
    public Thread thread = new Thread(this);

    // Start at 0 so it the process doesn't run immediately
    private final Semaphore available = new Semaphore(0);

    public boolean isExpired = false;

   // flag to indicate termination.
    private boolean terminated = false;


    public Process() {
        System.out.println("Process: Process constructor for: " + this.getClass().getSimpleName()); // Debug print
        this.thread.start(); // TODO should this be this.start() or thread.start()????
    }

    // Mark process as terminated.
    public void terminate() {
        terminated = true;
    }

    // sets the boolean indicating that this process’ quantum has expired
    public void requestStop() {
        System.out.println("Process.requestStop: Request stop for process: " + this.getClass().getSimpleName()); // Debug print
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
        return terminated || !thread.isAlive();
    }

    // releases (increments) the semaphore, allowing this thread to run
    public void start() {
        System.out.println("Process.start: Starting process: " + this.getClass().getSimpleName()); // Debug print
        available.release();
        System.out.println("Process.start: Semaphore released for process: " +
                this.getClass().getSimpleName() + ", permits: " + available.availablePermits()); // Debug print

    }


    // acquires (decrements) the semaphore, stopping this thread from running
    public void stop() {
        // System.out.println("Process.stop: Stopping process: " + this.getClass().getSimpleName()); // Debug print
        available.acquireUninterruptibly();
        System.out.println("Process.stop: Semaphore acquired for process: " + this.getClass().getSimpleName() + ", permits: " + available.availablePermits()); // Debug print
    }

    @Override
    public void run() {
        System.out.println("Process.run: Run method invoked for process: " + this.getClass().getSimpleName());
        try {
            available.acquire();
            main();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            System.out.println("Process.run: Run method finished for process: " + this.getClass().getSimpleName());
            terminate(); // Ensure that the process is marked as done.
        }
    }


    // When the process's quantum is expired, cooperate() calls OS.switchProcess()
    public void cooperate() throws InterruptedException {
        if (isExpired) {
            System.out.println("Process.cooperate: Process " + this.getClass().getSimpleName() + " is expired, switching process.");
            isExpired = false;
            OS.switchProcess();
        } else {
            System.out.println("Process.cooperate: Process " + this.getClass().getSimpleName() + " cooperating.");
        }
    }
}