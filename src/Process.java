import java.util.concurrent.Semaphore;

public abstract class Process implements Runnable{

    // Each process should have a Java Thread and a Semaphore
    public Thread thread = new Thread(this);

    // Start at 0 so it the process doesn't run immediately
    final Semaphore available = new Semaphore(0);

    public boolean isExpired = false;

   // flag to indicate termination.
    private boolean exited = false;




    public Process() {
        System.out.println("Process: Process constructor for: " + this.getClass().getSimpleName()); // Debug print
        this.thread = new Thread(this);
    }

    public void exit() {
        exited = true;

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
        return exited;
    }

    // releases (increments) the semaphore, allowing this thread to run
    public void start() {
        if (!thread.isAlive()) {
            thread.start();
        }
        available.release();
    }


    // acquires (decrements) the semaphore, stopping this thread from running
    public void stop() {
        System.out.println("Process.stop: Stopping process: " + this.getClass().getSimpleName()); // Debug print
        try {
            available.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        System.out.println("Process.stop: Process stopped: " + this.getClass().getSimpleName() + ", now has permits: " + available.availablePermits());
    }

    @Override
    public void run() {
        System.out.println("Process.run: Run method invoked for process: " + this.getClass().getSimpleName());
        try {
            available.acquire();
            main();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }


    // When the process's quantum is expired, cooperate() calls OS.switchProcess()
    public void cooperate() throws InterruptedException {
        if (isExpired) {
            System.out.println("Process.cooperate: Process " + this.getClass().getSimpleName()
                    + " is expired, switching process.");
            isExpired = false;
            this.stop();
            OS.switchProcess();

        } else {
            System.out.println("Process.cooperate: Process " + this.getClass().getSimpleName()
                    + " cooperating.");
        }
    }
}