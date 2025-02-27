import java.util.*;

// Has list of processes, a timer and tracks the currently running process
public class Scheduler {

    // hold the list of process that the scheduler knows about
    private LinkedList<PCB> pcbs = new LinkedList<>();
    // Separate queues for each priority
    private LinkedList<PCB> realTimeQueue = new LinkedList<>();
    private LinkedList<PCB> interactiveQueue = new LinkedList<>();
    private LinkedList<PCB> backgroundQueue = new LinkedList<>();
    private PriorityQueue<SleepingProcesses> sleepingProcesses;

    private static class SleepingProcesses {
        PCB process;
        long wakeUpTime;

        SleepingProcesses(PCB process, long wakeUpTime) {
            this.process = process;
            this.wakeUpTime = wakeUpTime;
        }
    }

    // Private instance of the Timer class
    private Timer timer = new Timer();

    // Reusable idle process
    private final IdleProcess idleProcess = new IdleProcess();

    public PCB runningProcess = null;


    public Scheduler() {
        sleepingProcesses = new PriorityQueue<>(Comparator.comparingLong(s -> s.wakeUpTime));
        // Schedule (using the timer) the interrupt for every 250ms.
        TimerTask interrupt = new TimerTask() {
            public void run() {
                if (runningProcess != null) {
                    // If running process did not cooperate and ran until timer ran out increment the timeout counter
                    if (!runningProcess.userlandProcess.isExpired) {
                        runningProcess.incrementTimeoutCount();
                    } else {
                        runningProcess.resetTimeoutCount(); // Process cooperated, so reset timeout counter
                    }
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
        switch (p) {
            case realtime -> this.realTimeQueue.add(userlandProcess);
            case interactive -> this.interactiveQueue.add(userlandProcess);
            case background -> this.backgroundQueue.add(userlandProcess);
        }
        // If nothing else is running, call switchProcess() to get it started.
        if (runningProcess == null) {
            switchProcess();
        }
        return userlandProcess.pid;
    }

    // Put running process into its queue if it's still running,
    // Move sleeping processes whose wakeup time has been reached back into their priority queue.
    // Choose next process to run using probability.
    public void switchProcess() {
        // Check sleeping processes, move eligible ones back into their correct priority queue
        WakeupProcesses();
        // If there is a process currently running, and it's not done, add it to the end of its priority queue.
        if (runningProcess != null && !runningProcess.isDone()) {
            switch (runningProcess.getPriority()) {
                case realtime -> realTimeQueue.add(runningProcess);
                case interactive -> interactiveQueue.add(runningProcess);
                case background -> backgroundQueue.add(runningProcess);
            }
        }

        // Move sleeping processes whose wakeup time has been reached back into their priority queue.
        WakeupProcesses();

        // Select next process based on probability function
        runningProcess = selectProcess();
    }

    // Uses probability to select next process to ensure fairness.
    private PCB selectProcess() {
        boolean realTimeProcessExists = !realTimeQueue.isEmpty();
        boolean interactiveProcessExists = !interactiveQueue.isEmpty();
        Random random = new Random();
        double randomDouble = random.nextDouble();

        if (realTimeProcessExists) {
            if (randomDouble < 0.6) {
                return realTimeQueue.poll();
            } else if (interactiveProcessExists) {
                    if (randomDouble < 0.9) {
                        return interactiveQueue.poll();
                    } else return backgroundQueue.poll();
            }
        }

        if (interactiveProcessExists) {
            if (randomDouble < 0.75) {
                return interactiveQueue.poll();
            } else return backgroundQueue.poll();
        }

        return backgroundQueue.poll();
    }

    // If processes wakeup time has arrived, remove from sleeping processes queue and return them to correct priority queue.
    private void WakeupProcesses() {
        long currentTime = System.currentTimeMillis();
        while (!sleepingProcesses.isEmpty() && sleepingProcesses.peek().wakeUpTime <= currentTime) {
            PCB process = sleepingProcesses.poll().process;
            switch (process.getPriority()) {
                case realtime -> realTimeQueue.add(process);
                case interactive -> interactiveQueue.add(process);
                case background -> backgroundQueue.add(process);
            }
        }
    }


    // flag to determine if there is a running process.
    public boolean hasRunningProcess() {
        return runningProcess != null;
    }

    // set wakeup time for process, remove it from its priority queue, and add it to list of sleeping processes
    public void sleep(int mills) {
        setWakeupTime(System.currentTimeMillis() + mills);
        if (runningProcess != null) {
            switch (runningProcess.getPriority()) {
                case realtime -> realTimeQueue.remove(runningProcess);
                case interactive -> interactiveQueue.remove(runningProcess);
                case background -> backgroundQueue.remove(runningProcess);
            }
            sleepingProcesses.add(new SleepingProcesses(runningProcess,getWakeupTime())); // add running process to sleep list
        }
    }

    public void setWakeupTime(long wakeupTime) {
        runningProcess.wakeupTime = wakeupTime;
    }

    public long getWakeupTime() {
        return runningProcess.wakeupTime;
    }

    // Remove process from its queue permanently.
    public void removeProcess(PCB runningProcess) {
        if (runningProcess != null) {
            switch (runningProcess.getPriority()) {
                case realtime -> realTimeQueue.remove(runningProcess);
                case interactive -> interactiveQueue.remove(runningProcess);
                case background -> backgroundQueue.remove(runningProcess);
            }
        }
    }
}
