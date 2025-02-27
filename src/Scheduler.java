import java.util.*;

// Has priority queues of processes, a timer and tracks the currently running process
public class Scheduler {

    // Separate queues for each priority
    private LinkedList<PCB> realTimeQueue = new LinkedList<>();
    private LinkedList<PCB> interactiveQueue = new LinkedList<>();
    private LinkedList<PCB> backgroundQueue = new LinkedList<>();
    private PriorityQueue<SleepingProcesses> sleepingProcesses;
    private final PCB idlePCB;

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
        System.out.println("Scheduler: Scheduler constructor called."); // Debug print
        idlePCB = new PCB(idleProcess, OS.PriorityType.background);
        backgroundQueue.add(idlePCB);
        sleepingProcesses = new PriorityQueue<>(Comparator.comparingLong(s -> s.wakeUpTime));
        // Schedule (using the timer) the interrupt for every 250ms.
        TimerTask interrupt = new TimerTask() {
            public void run() {
                if (runningProcess == idlePCB) {
                    return;
                }
                if (runningProcess != null) {
                    // If the running process is the idle process, do nothing.
                    if (runningProcess == idlePCB) {
                        return;
                    }
                    if (!runningProcess.userlandProcess.isExpired) {
                        runningProcess.incrementTimeoutCount();
                    } else {
                        runningProcess.resetTimeoutCount();
                    }
                    runningProcess.requestStop();
                }
            }
        };
        timer.schedule(interrupt, 0, 250);
        System.out.println("Scheduler: Timer task scheduled."); // Debug print

    }


    // Adds process to list, calls switchProcess if necessary, returns pid of userland process
    public int createProcess(UserlandProcess up, OS.PriorityType p) {
        // create a PCB for the userland process
        PCB userlandProcess = new PCB(up, p);
        System.out.println("Scheduler.createProcess: Creating process of type: " + up.getClass().getSimpleName() + ", priority: " + p); // Debug print
        switch (p) {
            case realtime -> this.realTimeQueue.add(userlandProcess);
            case interactive -> this.interactiveQueue.add(userlandProcess);
            case background -> this.backgroundQueue.add(userlandProcess);
        }
        System.out.println("Scheduler.createProcess: Process " + up.getClass().getSimpleName() + " created with PID: " + userlandProcess.pid); // Debug print
        // If nothing else is running, call switchProcess() to get it started.
        if (runningProcess == null) {
            System.out.println("Scheduler.createProcess: No running process, calling switchProcess."); // Debug print
            switchProcess();
        }
        return userlandProcess.pid;
    }

    // Put running process into its queue if it's still running,
    // Move sleeping processes whose wakeup time has been reached back into their priority queue.
    // Choose next process to run using probability.
    public void switchProcess() {
        System.out.println("Scheduler.switchProcess: Starting process switch."); // Debug print
        // Check sleeping processes, move eligible ones back into their correct priority queue
        WakeupProcesses();
        // If there is a process currently running, and it's not done, add it to the end of its priority queue.
        if (runningProcess != null && !runningProcess.isDone()) {
            System.out.println("Scheduler.switchProcess: Adding running process back to queue: " + runningProcess.userlandProcess.getClass().getSimpleName() + ", Priority: " + runningProcess.getPriority()); // Debug print
            switch (runningProcess.getPriority()) {
                case realtime -> realTimeQueue.add(runningProcess);
                case interactive -> interactiveQueue.add(runningProcess);
                case background -> backgroundQueue.add(runningProcess);
            }
        }

        // Move sleeping processes whose wakeup time has been reached back into their priority queue.
        WakeupProcesses();

        // Select next process based on probability function
        System.out.println("Scheduler.switchProcess: Selecting next process."); // Debug print
        runningProcess = selectProcess();
        System.out.println("Scheduler.switchProcess: Selected process: " + (runningProcess != null ? runningProcess.userlandProcess.getClass().getSimpleName() : "null")); // Debug print
        if (runningProcess == idlePCB) {
            System.out.println("Scheduler.switchProcess: Selected idle process, adding back to background queue."); // Debug print
            backgroundQueue.add(idlePCB);
        }
        System.out.println("Scheduler.switchProcess: Process switch complete."); // Debug print
    }


    // Uses probability to select next process to ensure fairness.
    private PCB selectProcess() {
        boolean realTimeProcessExists = !realTimeQueue.isEmpty();
        boolean interactiveProcessExists = !interactiveQueue.isEmpty();
        boolean backgroundProcessExists = !backgroundQueue.isEmpty();
        Random random = new Random();
        double randomDouble = random.nextDouble();

        if (realTimeProcessExists) {
            System.out.println("Scheduler.selectProcess: Realtime queue not empty."); // Debug print
            if (randomDouble < 0.6) {
                PCB selectedProcess = realTimeQueue.poll();
                System.out.println("Scheduler.selectProcess: Selected realtime process: " + selectedProcess.userlandProcess.getClass().getSimpleName()); // Debug print
                return selectedProcess;
            } else if (interactiveProcessExists && randomDouble < 0.9) {
                PCB selectedProcess = interactiveQueue.poll();
                System.out.println("Scheduler.selectProcess: Selected interactive process: " + selectedProcess.userlandProcess.getClass().getSimpleName()); // Debug print
                return selectedProcess;
            } else if (backgroundProcessExists) {
                PCB selectedProcess = backgroundQueue.poll();
                System.out.println("Scheduler.selectProcess: Selected background process: " + selectedProcess.userlandProcess.getClass().getSimpleName()); // Debug print
                return selectedProcess;
            } else {
                PCB selectedProcess = realTimeQueue.poll();
                System.out.println("Scheduler.selectProcess: Selected realtime process (default): " + selectedProcess.userlandProcess.getClass().getSimpleName()); // Debug print
                return selectedProcess; // Default to realtime if nothing else available
            }
        }

        if (interactiveProcessExists) {
            System.out.println("Scheduler.selectProcess: Interactive queue not empty."); // Debug print
            if (randomDouble < 0.75) {
                PCB selectedProcess = interactiveQueue.poll();
                System.out.println("Scheduler.selectProcess: Selected interactive process: " + selectedProcess.userlandProcess.getClass().getSimpleName()); // Debug print
                return selectedProcess;
            } else if (backgroundProcessExists) {
                PCB selectedProcess = backgroundQueue.poll();
                System.out.println("Scheduler.selectProcess: Selected background process: " + selectedProcess.userlandProcess.getClass().getSimpleName()); // Debug print
                return selectedProcess;
            } else {
                PCB selectedProcess = interactiveQueue.poll();
                System.out.println("Scheduler.selectProcess: Selected interactive process (default): " + selectedProcess.userlandProcess.getClass().getSimpleName()); // Debug print
                return selectedProcess; // Default to interactive if nothing else
            }
        }

        if (backgroundProcessExists) {
            System.out.println("Scheduler.selectProcess: Background queue not empty."); // Debug print
            PCB selectedProcess = backgroundQueue.poll();
            System.out.println("Scheduler.selectProcess: Selected background process: " + selectedProcess.userlandProcess.getClass().getSimpleName()); // Debug print
            return selectedProcess;
        }

        // If all queues are empty, return idlePCB
        // Make sure idlePCB is always added back to backgroundQueue after use
        System.out.println("Scheduler.selectProcess: All queues empty, selecting idle process."); // Debug print
        return idlePCB;
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