import java.util.LinkedList;

public class PCB { // Process Control Block
    private static int nextPid = 1;
    public final UserlandProcess userlandProcess;
    public int pid;
    public boolean waitingForMessage; // flag to track if process is waiting on a message
    private OS.PriorityType priority;
    private int timeoutCount; // tracks consecutive timeouts
    public long wakeupTime;
    public int[] openDevices; // array to track open device VFS ids; -1 means empty
    final String name; // process name for name based lookup
    public LinkedList<KernelMessage> messageQueue = new LinkedList<>();
    public int[] pageTable = new int[100];

    // Only kernel should manage PCB's
    PCB(UserlandProcess up, OS.PriorityType priority) {
        System.out.println("PCB: Creating PCB for process: "
                + up.getClass().getSimpleName()
                + ", priority: " + priority
                + ", PID: " + nextPid);

        pid = nextPid++; // Process gets a pid, next process gets the next pid up
        this.userlandProcess = up;
        this.name = up.getClass().getSimpleName();
        this.priority = priority;
        this.timeoutCount = 0;
        this.waitingForMessage = false;

        // Initialize page table for PCB
        for (int i = 0; i < 100; i++) {
            pageTable[i] = -1;
        }

        // allocate openDevices array with 10 slots and initialize each slot to -1
        openDevices = new int[10];
        for (int i = 0; i < openDevices.length; i++) {
            openDevices[i] = -1;
        }

        System.out.println("PCB: PCB created for: "
                + up.getClass().getSimpleName() + ", PID: " + pid);
    }


    // increments the consecutive timeouts counter
    public void incrementTimeoutCount() {
        timeoutCount++;
        if (timeoutCount > 5) {
            demotePriority();
            timeoutCount = 0;
        }
    }


    // resets the consecutive timeouts counter
    public void resetTimeoutCount() {
        timeoutCount = 0;
    }

    // Accessor for priority
    OS.PriorityType getPriority() {
        return priority;
    }

    // Mutator for priority
    public void setPriority(OS.PriorityType newPriority) {
        this.priority = newPriority;
    }

    public void requestStop() {
        userlandProcess.requestStop();
    }

    public void stop() throws InterruptedException {
        userlandProcess.stop();
    }

    // calls userlandprocess’ isDone()
    public boolean isDone() {
        return userlandProcess.isDone();
    }

    // calls userlandprocess’ start()
    void start() {
        userlandProcess.start();
    }

    public void exit() {
        userlandProcess.exit();
    }

    private void demotePriority() {
        OS.PriorityType oldPriority = priority;
        switch (priority) {
            case realtime -> priority = OS.PriorityType.interactive;
            case interactive -> priority = OS.PriorityType.background;
        }
        resetTimeoutCount();
        System.out.println("PCB.demotePriority: Process " + this.userlandProcess.getClass().getSimpleName()
                + " demoted from " + oldPriority + " to " + priority);
    }

    public void startThread() {
        userlandProcess.startThread();
    }
}
