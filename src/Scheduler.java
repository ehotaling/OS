import java.util.*;

// The Scheduler class manages processes based on priority, tracks sleeping processes,
// and switches tasks using a probabilistic model.
public class Scheduler {

    // Separate queues for each priority type
    private LinkedList<PCB> realTimeQueue = new LinkedList<>();
    private LinkedList<PCB> interactiveQueue = new LinkedList<>();
    private LinkedList<PCB> backgroundQueue = new LinkedList<>();

    // PriorityQueue for sleeping processes, ordered by wakeup time
    private PriorityQueue<Scheduler.SleepingProcesses> sleepingProcesses;

    // Private class to track sleeping processes and their wakeup time
    private static class SleepingProcesses {
        PCB process;
        long wakeUpTime;

        SleepingProcesses(PCB process, long wakeUpTime) {
            this.process = process;
            this.wakeUpTime = wakeUpTime;
        }
    }

    // Timer instance to periodically check for expired processes
    private Timer timer = new Timer();

    // Reference to the currently running process
    public PCB runningProcess = null;

    // Scheduler constructor
    public Scheduler() {
        System.out.println("Scheduler: Scheduler constructor called.");
        sleepingProcesses = new PriorityQueue<>(Comparator.comparingLong(s -> s.wakeUpTime));

        // Set up a periodic interrupt every 250ms to simulate time slices for running processes.
        TimerTask interrupt = new TimerTask() {
            public void run() {
                // TODO deal with this quantum start time , process quantum needs to kept track of by the scheduler not by process
                long elapsed = System.currentTimeMillis() - runningProcess.userlandProcess.quantumStartTime;
                if (elapsed >= 250) {
                    runningProcess.incrementTimeoutCount();
                    // TODO move to switch process of scheduler
                    System.out.println("Scheduler.interrupt: Elapsed time = " + elapsed
                            + " ms, timeout count = " + runningProcess.getTimeoutCount()
                            + " for PID " + runningProcess.pid + ", process: " + runningProcess.userlandProcess.getClass().getSimpleName());
                    if (runningProcess.getTimeoutCount() > 5) {
                        demote(runningProcess);
                    }
                    runningProcess.requestStop();
                }
            }
        };
        timer.schedule(interrupt, 0, 250);
        System.out.println("Scheduler: Timer task scheduled.");
    }

    // Creates a new process and places it in the correct priority queue.
    // If no process is currently running, trigger a switch immediately.
    public int createProcess(UserlandProcess up, OS.PriorityType p) {
        PCB userlandProcess = new PCB(up, p);
        System.out.println("Scheduler.createProcess: Creating process of type: "
                + up.toString() + ", priority: " + p);
        // TODO make into own function
        addProcessToQueue(userlandProcess, p);
        System.out.println("Scheduler.createProcess: Process "
                + up.getClass().getSimpleName() + " created with PID: "
                + userlandProcess.pid + " and added to scheduler.");

        if (runningProcess == null) {
            switchProcess();
        }

        return userlandProcess.pid;
    }

    // Process switching
    public void switchProcess() {
        System.out.println("Scheduler.switchProcess: Starting process switch.");


        // If the currently running process is an InitProcess and the system call is CreateProcess,
        // delay switching only for CreateProcess calls until initialization is complete.
        if (runningProcess != null && runningProcess.userlandProcess instanceof InitProcess) {
            if (OS.currentCall == OS.CallType.CreateProcess && !OS.isInitProcessFinished()) {
                System.out.println("Scheduler.switchProcess: InitProcess is still running, delaying CreateProcess system call.");
                return; // Delay switching for CreateProcess calls during initialization.
            }
        }


        // Check if any sleeping processes are ready to be awakened
        WakeupProcesses();

        // If the running process is still active, place it back on its queue
        if (runningProcess != null && !runningProcess.isDone()) {
            addProcessToQueue(runningProcess);
        }

        // Pick the next process using the probabilistic model
        runningProcess = selectProcess();

        if (runningProcess != null) {
            System.out.println("Scheduler.switchProcess: Next process: "
                    + runningProcess.userlandProcess.getClass().getSimpleName()
                    + " (PID " + runningProcess.pid + ")");
        } else {
            System.out.println("Scheduler.switchProcess: No process selected (all queues empty).");
        }

        System.out.println("Scheduler.switchProcess: Process switch complete.");
    }

    // Helper method to add a process to a queue
    private void addProcessToQueue(PCB process) {
        switch (runningProcess.getPriority()) {
            case realtime -> realTimeQueue.add(process);
            case interactive -> interactiveQueue.add(process);
            case background -> backgroundQueue.add(process);
        }
    }

    private void addProcessToQueue(PCB process, OS.PriorityType p) {
        switch (p) {
            case realtime -> realTimeQueue.add(process);
            case interactive -> interactiveQueue.add(process);
            case background -> backgroundQueue.add(process);
        }
    }

    private PCB selectProcess() {
        System.out.println("Scheduler.selectProcess: Selecting next process using probabilistic model.");
        Random random = new Random();
        double randomDouble = random.nextDouble();

        // If there is at least one real-time process:
        if (!realTimeQueue.isEmpty()) {
            // 60% chance to choose a real-time process
            if (randomDouble < 0.6) {
                PCB p = pollNext(realTimeQueue);
                if (p != null) return p;
            }
            // 30% chance for an interactive process (if available)
            if (!interactiveQueue.isEmpty() && randomDouble < 0.9) {
                PCB p = pollNext(interactiveQueue);
                if (p != null) return p;
            }
            // Otherwise, choose from background, but try to avoid IdleProcess
            PCB p = pollNextBackground();
            if (p != null) return p;
            // Fallback to real-time if available.
            return pollNext(realTimeQueue);
        }

        // If no real-time processes, but interactive exist:
        if (!interactiveQueue.isEmpty()) {
            if (randomDouble < 0.75) {
                PCB p = pollNext(interactiveQueue);
                if (p != null) return p;
            }
            PCB p = pollNextBackground();
            if (p != null) return p;
            return pollNext(interactiveQueue);
        }

        // Only background processes available:
        return pollNextBackground();
    }


    private PCB pollNextBackground() {
        // Try to find a background PCB whose process is not an IdleProcess.
        for (Iterator<PCB> it = backgroundQueue.iterator(); it.hasNext(); ) {
            PCB pcb = it.next();
            if (!(pcb.userlandProcess instanceof IdleProcess) && !pcb.isDone()) {
                it.remove();
                return pcb;
            }
        }
        // If none found, fall back to polling the backgroundQueue normally.
        return pollNext(backgroundQueue);
    }


    // Poll the queue until we find a process that isn't done
    private PCB pollNext(LinkedList<PCB> queue) {
        while (!queue.isEmpty()) {
            PCB pcb = queue.poll();
            if (!pcb.isDone()) {
                return pcb;
            } else {
                System.out.println("Scheduler.pollNext: Discarding finished process: "
                        + pcb.userlandProcess.getClass().getSimpleName() + ", PID: " + pcb.pid);
            }
        }
        return null;
    }

    // Wake up processes whose wakeUpTime is reached
    private void WakeupProcesses() {
        long currentTime = System.currentTimeMillis();
        while (!sleepingProcesses.isEmpty() && sleepingProcesses.peek().wakeUpTime <= currentTime) {
            PCB process = sleepingProcesses.poll().process;
            addProcessToQueue(process);
        }
    }

    // Check if we have a currently running process
    public boolean hasRunningProcess() {
        return runningProcess != null;
    }

    // Put the running process to sleep
    public void sleep(int mills) {
        setWakeupTime(System.currentTimeMillis() + mills);
        if (runningProcess != null) {
            // Place in sleeping list
            sleepingProcesses.add(new SleepingProcesses(runningProcess, getWakeupTime()));
        }
    }

    public void setWakeupTime(long wakeupTime) {
        runningProcess.wakeupTime = wakeupTime;
    }

    public long getWakeupTime() {
        return runningProcess.wakeupTime;
    }

    // Remove a process from all queues permanently
    public void removeProcess(PCB runningProcess) {
        realTimeQueue.remove(runningProcess);
        interactiveQueue.remove(runningProcess);
        backgroundQueue.remove(runningProcess);
        sleepingProcesses.removeIf(sp -> sp.process == runningProcess);
    }


    private void demote(PCB pcb) {
        OS.PriorityType oldPriority = pcb.getPriority();
        OS.PriorityType newPriority = oldPriority;
        if (oldPriority == OS.PriorityType.realtime) {
            newPriority = OS.PriorityType.interactive;
        } else if (oldPriority == OS.PriorityType.interactive) {
            newPriority = OS.PriorityType.background;
        }
        if (oldPriority != newPriority) {
            pcb.setPriority(newPriority);
            pcb.resetTimeoutCount();
            System.out.println("Scheduler.demote: Process " + pcb.pid
                    + " demoted from " + oldPriority + " to " + newPriority);
        } else {
            System.out.println("Scheduler.demote: Process " + pcb.pid
                    + " already at lowest priority (" + oldPriority + ")");
        }
    }

}
