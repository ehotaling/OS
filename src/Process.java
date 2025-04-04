import jdk.swing.interop.SwingInterOpUtils;

import java.util.concurrent.Semaphore;

public abstract class Process implements Runnable {

    // Thread for running this process. In our OS simulator, each process is represented as a Java Thread.
    public Thread thread;
    // Semaphore used to pause and resume process execution. It helps simulate cooperative multitasking.
    // The process will block (pause) when the semaphore's permit count is 0.
    final Semaphore available = new Semaphore(0);
    // Flag indicating that the process's time quantum has expired.
    // When set to true, the process should yield control.
    public volatile boolean isExpired = false;
    // Flag to mark that the process has exited (finished execution).
    public volatile boolean exited = false;


    // Process constructor.
    // Initializes the thread for the process and prints debug info.
    public Process() {
        System.out.println("Process: Process constructor for: " + this.getClass().getSimpleName());
        this.thread = new Thread(this);
    }

    // Marks the process as exited.
    // This method is called when the process wants to indicate it is done.
    public void exit() {
        System.out.println("Process.exit: Exiting process: " + this.getClass().getSimpleName());
        exited = true;
        // Release a permit so that any blocked acquire (in run() or stop()) can continue.
        available.release();
    }


    // Called when the scheduler’s timer interrupts.
    // Sets the flag indicating that the process’s quantum has expired.
    public void requestStop() {
        isExpired = true;
    }

    // Checks if the process is currently stopped.
    // It returns true when no permits are available on the semaphore.
    public boolean isStopped() {
        return available.availablePermits() == 0;
    }

    // Checks if the process has been marked as exited or the thread is not alive.
    // A process is considered done if it has called exit() and finished its execution.
    public boolean isDone() {
        return exited || !thread.isAlive();
    }


    // Starts the process
    // This method starts the thread and releases a permit so that the process can begin execution.
    // It simulates the OS starting a new userland process.
    public void start() {
        System.out.println("Process.start: Starting process: " + this.getClass().getSimpleName());
        if (!thread.isAlive()) {
            System.out.println("Process.start: Starting thread: " + this.getClass().getSimpleName());
            thread.start();
        }
        System.out.println("Process.start: Semaphore available before calling available.release(): " + available.availablePermits());
        available.release();
        System.out.println("Process.start: Semaphore available after calling available.release(): " + available.availablePermits());
    }


    // Stops (pauses) the process by acquiring a permit from the semaphore.
    // This effectively blocks the process until the semaphore is released again by the scheduler.
    public void stop() {
        try {
            System.out.println("Process.stop: " + this.getClass().getSimpleName() + ": stopping process: " + this.getClass().getSimpleName());
            available.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
//        System.out.println("Process.stop: Process stopped: " + this.getClass().getSimpleName() +
//                ", now has permits: " + available.availablePermits());
    }

    // The run() method is invoked when the process's thread starts.
    // It waits (by acquiring the semaphore) until the process is allowed to run, then calls its main() method.
    @Override
    public void run() {
        System.out.println("Process.run: Run method invoked for process: " + this.getClass().getSimpleName());
        try {
            available.acquire();
            if (exited) {
                // If exit() was called before main() starts, simply return.
                System.out.println("Process.run: Exit process: " + this.getClass().getSimpleName());
                return;
            }
            main();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }


    // Abstract main() method that each subclass must implement.
    // This method represents the process's main logic (like the main() function in traditional programs).
    public abstract void main() throws InterruptedException;

    // Cooperative multitasking method.
    // If the process's quantum is expired, it stops itself and yields control by calling OS.switchProcess().
    // Otherwise, it prints a message indicating it is cooperating (i.e., continuing execution).
    public void cooperate() throws InterruptedException {
        if (isExpired) {
            System.out.println("Process.cooperate: Process " + this.getClass().getSimpleName()
                    + " is expired, switching process.");
            isExpired = false;
            OS.switchProcess();
        }
    }

    public boolean isAlive() {
        return thread.isAlive();
    }

    public void resumeProcess() {
        // This releases a permit regardless of the thread's alive state,
        // ensuring that if the process is blocked, it can continue.
        available.release();
    }


    // Make the thread alive but not running
    public void startThread() {
        if (!thread.isAlive()) {
            System.out.println("Process.startThread: Starting thread: " + this.getClass().getSimpleName());
            thread.start();
        }
    }
}
