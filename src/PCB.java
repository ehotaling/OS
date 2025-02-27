// Holds Userland process, holds and assigns PID, is used in queue, methods call Userland process methods

public class PCB { // Process Control Block
    private static int nextPid = 1;
    public final UserlandProcess userlandProcess;
    public int pid;
    private OS.PriorityType priority;
    private int timeoutCount = 0; // tracks consecutive timeouts
    public long wakeupTime = 0;


    // Package private constructor, only kernel should manage PCB's
    PCB(UserlandProcess up, OS.PriorityType priority) {
        pid = nextPid++; // Process gets a pid, next process gets the next pid up
        this.userlandProcess = up;
        this.priority = priority;
        up.thread.start();
    }

    // increments timeout count if process doesn't cooperate
    public void incrementTimeoutCount() {
        timeoutCount++;
        if (timeoutCount > 5) { // if process times out more than 5 times in a row
            demotePriority();
        }
    }

    private void demotePriority() {
        if (priority == OS.PriorityType.realtime) {
            priority = OS.PriorityType.interactive;
        } else if (priority == OS.PriorityType.interactive) {
            priority = OS.PriorityType.background;
        }
    }

    public void resetTimeoutCount() {
        timeoutCount = 0;
    }

    public String getName() {
        return null;
    }

    OS.PriorityType getPriority() {
        return priority;
    }

    public void requestStop() {
        userlandProcess.requestStop();
    }

    // calls userlandProcess’ stop. Loops with Thread.sleep() until ulp.isStopped() is true.
    public void stop() {
        while (!userlandProcess.isStopped()) {
            try {
                userlandProcess.stop();
                Thread.sleep(10); // To avoid clogging up resources
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }

    // calls userlandprocess’ isDone() //
    public boolean isDone() {
        return userlandProcess.isDone();
    }

    // calls userlandprocess’ start() //
    void start() {
        userlandProcess.start();
    }

    public void setPriority(OS.PriorityType newPriority) {
        priority = newPriority;
    }

}
