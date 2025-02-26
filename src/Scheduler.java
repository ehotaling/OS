import java.util.LinkedList;
import java.util.Timer;
import java.util.TimerTask;

// Has list of processes, a timer and tracks the currently running process
public class Scheduler {

    // hold the list of process that the scheduler knows about
    private LinkedList<PCB> pcbs = new LinkedList<>();

    // Private instance of the Timer class
    private Timer timer = new Timer();

    // Reusable idle process
    private final IdleProcess idleProcess = new IdleProcess();

    public PCB runningProcess = null;


    public Scheduler() {
        // Schedule (using the timer) the interrupt for every 250ms.
        TimerTask interrupt = new TimerTask() {
            public void run() {
                if (runningProcess != null) {
                    runningProcess.requestStop();
                }
            }
        };
        timer.schedule(interrupt, 0, 250);
    }
    

    // Adds process to list, calls switchProcess if necessary, returns pid of userland process
    public int createProcess(UserlandProcess up, OS.PriorityType p) {
        // create a PCB for the userland process
        PCB userlandProcess = new PCB(up, p);
        this.pcbs.add(userlandProcess);
        // If nothing else is running, call switchProcess() to get it started.
        if (runningProcess == null) {
            switchProcess();
        }
        return userlandProcess.pid;
    }

    // Gets first process from the queue, runs the process
    public void switchProcess() {
        // If there is a process currently running, and it's not done, add it to the end of the list.
        if (runningProcess != null && !runningProcess.isDone()) {
            pcbs.add(runningProcess);
        }
        // Since OS.Startup enqueues the idle process, the PCB list should never be empty.
        // Simply select the next process from the list.
        runningProcess = pcbs.remove();
    }


    // flag to determine if there is a running process.
    public boolean hasRunningProcess() {
        return runningProcess != null;
    }
}
