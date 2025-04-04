import java.util.*;
import java.time.Clock;
import java.util.HashMap;

public class Scheduler {
    // Separate queues for each priority type.
    private LinkedList<PCB> realTimeQueue = new LinkedList<>();
    private LinkedList<PCB> interactiveQueue = new LinkedList<>();
    private LinkedList<PCB> backgroundQueue = new LinkedList<>();

    // Priority queue for sleeping processes ordered by wakeup time.
    private PriorityQueue<SleepingProcesses> sleepingProcesses;

    // HashMap to map pid to PCB for fast lookup
    private HashMap<Integer,PCB> processMap = new HashMap<>();

    private Clock clock = Clock.systemUTC();
    private Timer timer = new Timer();

    // Reference to the currently running process.
    public PCB runningProcess = null;

    // Reference to kernel for device cleanup calls.
    private Kernel kernel;

    public PCB getCurrentlyRunning() {
        return runningProcess;
    }

    public void removeProcess(PCB p) {
        OS.PriorityType priority = p.getPriority();
        switch (priority) {
            case realtime -> realTimeQueue.remove(p);
            case interactive -> interactiveQueue.remove(p);
            case background -> backgroundQueue.remove(p);
        }
        // Remove process from the process map if not waiting on a message
        if (!p.waitingForMessage) {
            processMap.remove(p.pid);
        }

    }
    public PCB getPCB(int pid) {
        return processMap.get(pid);
    }

    public void switchAndStartProcess() {
        switchProcess();
        runningProcess.userlandProcess.start();
    }


    // Private class to track sleeping processes with wakeup time.
    private static class SleepingProcesses {
        PCB process;
        long wakeUpTime;
        SleepingProcesses(PCB process, long wakeUpTime) {
            this.process = process;
            this.wakeUpTime = wakeUpTime;
        }
    }

    // Scheduler constructor.
    public Scheduler() {
        System.out.println("Scheduler: Scheduler constructor called");
        sleepingProcesses = new PriorityQueue<>(Comparator.comparingLong(s -> s.wakeUpTime));
        // Set up a periodic interrupt every 250ms.
        TimerTask interrupt = new TimerTask() {
            public void run() {
                if (runningProcess != null) {
                    runningProcess.incrementTimeoutCount();
                    runningProcess.requestStop();
                }
            }
        };
        timer.schedule(interrupt, 250, 250);
        System.out.println("Scheduler: Timer task scheduled");
    }

    // Setter for kernel reference.
    public void setKernel(Kernel kernel) {
        this.kernel = kernel;
    }

    // Create a process and add it to the proper queue.
    public int createProcess(UserlandProcess up, OS.PriorityType p) throws InterruptedException {
        System.out.println("Scheduler.createProcess: Creating process " + up.getClass().getSimpleName() + " with priority " + p);

        PCB newProcess = new PCB(up, p);

        // Add new process to the process map
        processMap.put(newProcess.pid, newProcess);

        // If no process is currently running, start the created process otherwise it is added to the queue.
        if (runningProcess == null) {
            System.out.println("Scheduler.createProcess: No running process, " + up.getClass().getSimpleName() + " will run next");
            runningProcess = newProcess;
        } else {
            addProcessToQueue(newProcess, p);
            newProcess.startThread(); // The thread is now alive but won't run immediately.
        }
        return newProcess.pid;
    }

    public void switchProcess() {

        // If there is a currently running process, check if it should be re-queued.
        if (runningProcess != null && !runningProcess.isDone() && !runningProcess.waitingForMessage) {
            if (!runningProcess.userlandProcess.isDone()) {
                addProcessToQueue(runningProcess);
            }
        }

        // Select the next process to run.
        runningProcess = selectProcess();

        // If the process was waiting for a message reset the flag and return the Kernel Message to the process.
        if (runningProcess.waitingForMessage) {
            runningProcess.waitingForMessage = false;
            OS.retVal = runningProcess.messageQueue.removeFirst();
        }

        if (runningProcess == null) {
            // Fallback: attempt to poll any process from the background queue.
            System.out.println("Scheduler.switchProcess: No process selected by selectProcess(), attempting fallback.");
            runningProcess = pollNext(backgroundQueue);
        }
        if (runningProcess != null) {
            System.out.println("Scheduler.switchProcess: Selected " + runningProcess.userlandProcess.getClass().getSimpleName());
        } else {
            System.out.println("Scheduler.switchProcess: No process available to run!");
        }
    }

    // Helper to add a process to a queue based on its priority.
    private void addProcessToQueue(PCB process, OS.PriorityType p) {
        switch (p) {
            case realtime -> realTimeQueue.add(process);
            case interactive -> interactiveQueue.add(process);
            case background -> backgroundQueue.add(process);
        }
        System.out.println("Scheduler.createProcess: Adding process "
                + process.userlandProcess.getClass().getSimpleName() + " to queue: " + p);
    }

    // Helper to add process to queue using its current priority.
    private void addProcessToQueue(PCB process) {
        switch (process.getPriority()) {
            case realtime -> realTimeQueue.add(process);
            case interactive -> interactiveQueue.add(process);
            case background -> backgroundQueue.add(process);
        }
        System.out.println("Scheduler.createProcess: Adding process "
                + process.userlandProcess.getClass().getSimpleName() + " to queue: " + process.getPriority());
    }

    // Select next process using a probabilistic model.
    private PCB selectProcess() {
        wakeupProcesses(); // wake any sleeping processes first
        Random random = new Random();
        double randomDouble = random.nextDouble();

        // If there is at least one non-idle process available...
        if (!realTimeQueue.isEmpty() || !interactiveQueue.isEmpty() || hasNonIdleBackground()) {
            // Prefer real-time processes (60% chance)
            if (!realTimeQueue.isEmpty() && randomDouble < 0.6) {
                return pollNext(realTimeQueue);
            }
            // Otherwise, try interactive processes (next 30%)
            else if (!interactiveQueue.isEmpty() && randomDouble < 0.9) {
                return pollNext(interactiveQueue);
            }
            // Otherwise, check background—but only non-idle processes!
            else {
                PCB pcb = pollNonIdleBackground();
                if (pcb != null) {
                    return pcb;
                }
                // Fallback: if background had no non-idle, try real-time or interactive if available.
                if (!realTimeQueue.isEmpty()) {
                    return pollNext(realTimeQueue);
                }
                if (!interactiveQueue.isEmpty()) {
                    return pollNext(interactiveQueue);
                }
            }
        }
        // No non-idle process exists in any queue, so allow IdleProcess.
        if (!realTimeQueue.isEmpty()) return pollNext(realTimeQueue);
        if (!interactiveQueue.isEmpty()) return pollNext(interactiveQueue);
        if (!backgroundQueue.isEmpty()) return pollNext(backgroundQueue);
        return null;
    }

    // Helper method: checks if backgroundQueue has any non-idle processes.
    private boolean hasNonIdleBackground() {
        for (PCB pcb : backgroundQueue) {
            if (!(pcb.userlandProcess instanceof IdleProcess) && !pcb.isDone()) {
                return true;
            }
        }
        return false;
    }

    // Helper method: polls the first non-idle process from the background queue.
    private PCB pollNonIdleBackground() {
        for (Iterator<PCB> it = backgroundQueue.iterator(); it.hasNext(); ) {
            PCB pcb = it.next();
            if (!(pcb.userlandProcess instanceof IdleProcess) && !pcb.isDone()) {
                it.remove();
                return pcb;
            }
        }
        return null;
    }

    // Poll a queue until a process that is not done is found.
    private PCB pollNext(LinkedList<PCB> queue) {
        while (!queue.isEmpty()) {
            PCB pcb = queue.poll();
            if (!pcb.isDone()) {
                return pcb;
            } else {
                System.out.println("Scheduler.pollNext: Discarding finished process "
                        + pcb.userlandProcess.getClass().getSimpleName() + " PID " + pcb.pid);
            }
        }
        return null;
    }

    private void wakeupProcesses() {
        long currentTime = clock.millis();
        // Temporary list for processes not ready yet.
        List<SleepingProcesses> notReady = new ArrayList<>();

        // Iterate over all sleeping processes.
        for (SleepingProcesses sp : sleepingProcesses) {
            if (sp.wakeUpTime <= currentTime) {
                addProcessToQueue(sp.process);
            } else {
                notReady.add(sp);
            }
        }

        // Clear the priority queue and re-add only the processes that are not ready.
        sleepingProcesses.clear();
        sleepingProcesses.addAll(notReady);
    }

    // Wakes up process that are waiting for messages
    public void wakeUpProcess(PCB process) {
        addProcessToQueue(process);
        System.out.println("Scheduler: Process " + process.pid + " has been woken up from message waiting.");
    }



    // Put the running process to sleep.
    public void sleep(int mills) {
        if (runningProcess != null) {
            runningProcess.wakeupTime = clock.millis() + mills;
            sleepingProcesses.add(new SleepingProcesses(runningProcess, runningProcess.wakeupTime));
            runningProcess = selectProcess();
        }
    }

    // Helper method to find a process by its name
    public int getPidByName(String name) {
        for (PCB pcb : processMap.values()) {
            if (pcb.name.equals(name)) {
                return pcb.pid;
            }
        }
        return -1; // if not found
    }
}
