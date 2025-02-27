import java.util.*;

// The Scheduler class manages processes based on priority, tracks sleeping processes, and switches tasks using a probabilistic model.
public class Scheduler {

    // Separate queues for each priority type
    private LinkedList<PCB> realTimeQueue = new LinkedList<>();
    private LinkedList<PCB> interactiveQueue = new LinkedList<>();
    private LinkedList<PCB> backgroundQueue = new LinkedList<>();
    private PriorityQueue<SleepingProcesses> sleepingProcesses;

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

    // Scheduler constructor initializes sleeping process queue and sets up a periodic timer interrupt.
    public Scheduler() {
        System.out.println("Scheduler: Scheduler constructor called.");
        sleepingProcesses = new PriorityQueue<>(Comparator.comparingLong(s -> s.wakeUpTime));

        // Set up a periodic interrupt every 250ms to simulate time slices for running processes.
        TimerTask interrupt = new TimerTask() {
            public void run() {
                if (runningProcess == null) {
                    System.out.println("Scheduler.interrupt: No process available.");
                } else {
                    // If a process is running, update its timeout tracking and stop it if necessary.
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
        System.out.println("Scheduler: Timer task scheduled.");
    }

    // Creates a new process and places it in the correct priority queue.
    // If no process is currently running, it triggers a switch.
    public int createProcess(UserlandProcess up, OS.PriorityType p) {
        PCB userlandProcess = new PCB(up, p);
        System.out.println("Scheduler.createProcess: Creating process of type: "
                + up.toString() + ", priority: " + p); // Existing print

        // Added debug prints START
        System.out.println("Scheduler.createProcess: Adding process of type: " + up.toString() + " to queue: " + p);
        switch (p) {
            case realtime -> realTimeQueue.add(userlandProcess);
            case interactive -> interactiveQueue.add(userlandProcess);
            case background -> backgroundQueue.add(userlandProcess);
        }
        System.out.println("Scheduler.createProcess: Process added to queue, process type: " + up.getClass().getSimpleName() + ", queue: " + p);
        // Added debug prints END


        System.out.println("Scheduler.createProcess: Process "
                + up.getClass().getSimpleName() + " created with PID: " + userlandProcess.pid); // Existing print

        if (runningProcess == null) {
            System.out.println("Scheduler.createProcess: No running process, calling switchProcess.");
            switchProcess();
        }

        System.out.println("Scheduler.createProcess: Returning PID: " + userlandProcess.pid); // Existing print
        return userlandProcess.pid;
    }

    // Handles process switching by moving running processes to their respective queues and selecting the next one.
    public void switchProcess() {
        System.out.println("Scheduler.switchProcess: Starting process switch.");

        // Check if any sleeping processes are ready to be reactivated.
        WakeupProcesses();

        // If the running process is still active, move it back to its queue.
        if (runningProcess != null && !runningProcess.isDone()) {
            System.out.println("Scheduler.switchProcess: Adding running process back to queue: "
                    + runningProcess.userlandProcess.getClass().getSimpleName()
                    + ", Priority: " + runningProcess.getPriority());
            switch (runningProcess.getPriority()) {
                case realtime -> realTimeQueue.add(runningProcess);
                case interactive -> interactiveQueue.add(runningProcess);
                case background -> backgroundQueue.add(runningProcess);
            }
        }

        // Select the next process to run based on the probabilistic model.
        System.out.println("Scheduler.switchProcess: Selecting next process.");
        runningProcess = selectProcess();

        System.out.println("Scheduler.switchProcess: Selected process: "
                + (runningProcess != null ? runningProcess.userlandProcess.getClass().getSimpleName() : "null"));

        System.out.println("Scheduler.switchProcess: Process switch complete.");
    }

    private PCB selectProcess() {
        Random random = new Random();
        double randomDouble = random.nextDouble();
        PCB selectedPCB = null; // Initialize selectedPCB to null

        // realtime queue first
        if (!realTimeQueue.isEmpty()) {
            if (randomDouble < 0.6) {
                selectedPCB = pollNext(realTimeQueue);
                if (selectedPCB != null) return selectedPCB; // Return if process found
            }
            if (!interactiveQueue.isEmpty() && randomDouble < 0.9) {
                selectedPCB = pollNext(interactiveQueue);
                if (selectedPCB != null) return selectedPCB; // Return if process found
            }
            if (!backgroundQueue.isEmpty()) {
                selectedPCB = pollNext(backgroundQueue);
                if (selectedPCB != null) return selectedPCB; // Return if process found
            }
            selectedPCB = pollNext(realTimeQueue); // Fallback to realtime if available
            if (selectedPCB != null) return selectedPCB; // Return if process found in fallback
        }

        //  try interactive queue
        if (!interactiveQueue.isEmpty()) {
            if (randomDouble < 0.75) {
                selectedPCB = pollNext(interactiveQueue);
                if (selectedPCB != null) return selectedPCB; // Return if process found
            }
            if (!backgroundQueue.isEmpty()) {
                selectedPCB = pollNext(backgroundQueue);
                if (selectedPCB != null) return selectedPCB; // Return if process found
            }
            selectedPCB = pollNext(interactiveQueue); // Fallback to interactive if available
            if (selectedPCB != null) return selectedPCB; // Return if process found in fallback
        }

        // try background queue
        if (!backgroundQueue.isEmpty()) {
            selectedPCB = pollNext(backgroundQueue);
            if (selectedPCB != null) return selectedPCB; // Return if process found
        }

        System.out.println("Scheduler.selectProcess: All queues empty, no process available.");
        return null; // Return null explicitly if no process was selected in any queue
    }


    // Polls the next PCB from the provided queue, discarding any PCB whose process is finished.
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



    // Moves sleeping processes back into their appropriate queues when their wakeup time is reached.
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

    // Returns whether there is currently a process running.
    public boolean hasRunningProcess() {
        return runningProcess != null;
    }

    // Puts the running process to sleep for the specified duration.
    // Removes it from its queue and adds it to the sleeping list.
    public void sleep(int mills) {
        setWakeupTime(System.currentTimeMillis() + mills);
        if (runningProcess != null) {
            switch (runningProcess.getPriority()) {
                case realtime -> realTimeQueue.remove(runningProcess);
                case interactive -> interactiveQueue.remove(runningProcess);
                case background -> backgroundQueue.remove(runningProcess);
            }
            sleepingProcesses.add(new SleepingProcesses(runningProcess, getWakeupTime()));
        }
    }

    // Sets the wakeup time for the currently running process.
    public void setWakeupTime(long wakeupTime) {
        runningProcess.wakeupTime = wakeupTime;
    }

    // Retrieves the wakeup time for the currently running process.
    public long getWakeupTime() {
        return runningProcess.wakeupTime;
    }

    // Removes a process from all queues permanently.
    public void removeProcess(PCB runningProcess) {
        realTimeQueue.remove(runningProcess);
        interactiveQueue.remove(runningProcess);
        backgroundQueue.remove(runningProcess);
        sleepingProcesses.remove(runningProcess);
    }
}
