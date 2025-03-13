import java.util.*;
import java.time.Clock;

public class Scheduler {
    // separate queues for each priority type
    private LinkedList<PCB> realTimeQueue = new LinkedList<>();
    private LinkedList<PCB> interactiveQueue = new LinkedList<>();
    private LinkedList<PCB> backgroundQueue = new LinkedList<>();

    // priority queue for sleeping processes ordered by wakeup time
    private PriorityQueue<SleepingProcesses> sleepingProcesses;

    private Clock clock = Clock.systemUTC();
    private Timer timer = new Timer();

    // reference to the currently running process
    public PCB runningProcess = null;

    // reference to kernel for device cleanup calls
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
    }



    // private class to track sleeping processes with wakeup time
    private static class SleepingProcesses {
        PCB process;
        long wakeUpTime;
        SleepingProcesses(PCB process, long wakeUpTime) {
            this.process = process;
            this.wakeUpTime = wakeUpTime;
        }
    }

    // scheduler constructor
    public Scheduler() {
        System.out.println("Scheduler: Scheduler constructor called");
        sleepingProcesses = new PriorityQueue<>(Comparator.comparingLong(s -> s.wakeUpTime));
        // set up a periodic interrupt every 250ms
        TimerTask interrupt = new TimerTask() {
            public void run() {
                if (runningProcess != null) {
                    runningProcess.incrementTimeoutCount();
                    runningProcess.requestStop();
                }
                wakeupProcesses();
            }
        };
        timer.schedule(interrupt, 250, 250);
        System.out.println("Scheduler: Timer task scheduled");
    }

    // setter for kernel reference
    public void setKernel(Kernel kernel) {
        this.kernel = kernel;
    }

    public int createProcess(UserlandProcess up, OS.PriorityType p) throws InterruptedException {
        System.out.println("Scheduler.createProcess: Creating process " + up.getClass().getSimpleName() + " with priority " + p);

        PCB newProcess = new PCB(up, p);
        addProcessToQueue(newProcess, p);

        System.out.println("Scheduler.createProcess: Process " + up.getClass().getSimpleName()
                + " created with PID " + newProcess.pid);

        // If no process is currently running, start the first created process
        if (runningProcess == null) {
            System.out.println("Scheduler.createProcess: No running process, switching process");
            switchProcess();
        }

        return newProcess.pid;
    }


    public void switchProcess() throws InterruptedException {
        wakeupProcesses();
        System.out.println("Scheduler.switchProcess: Starting process switch");

        // If there is a currently running process, check if it should be requeued
        if (runningProcess != null && !runningProcess.isDone()) {
            if (!runningProcess.userlandProcess.isDone()) {
                addProcessToQueue(runningProcess);
            }
        }

        // Select the next process to run
        runningProcess = selectProcess();
        System.out.println("Scheduler.switchProcess: Selected " + runningProcess.userlandProcess.getClass().getSimpleName());

        // If no process was selected, use an IdleProcess
        if (runningProcess == null) {
            System.out.println("Scheduler.switchProcess: No process found, selecting IdleProcess");
            runningProcess = new PCB(new IdleProcess(), OS.PriorityType.background);
        }

        // Start the new running process
        if (runningProcess != null) {
            runningProcess.userlandProcess.start();
            System.out.println("Scheduler.switchProcess: Running process " + runningProcess.userlandProcess.getClass().getSimpleName());
        } else {
            System.out.println("Scheduler.switchProcess: No process could be started");
        }
    }


    // helper to add a process to a queue based on its priority
    private void addProcessToQueue(PCB process, OS.PriorityType p) {
        switch (p) {
            case realtime -> realTimeQueue.add(process);
            case interactive -> interactiveQueue.add(process);
            case background -> backgroundQueue.add(process);
        }
        System.out.println("Scheduler.createProcess: Adding process " + process.userlandProcess.getClass().getSimpleName() + " to queue: " + p);
    }

    // helper to add process to queue using its current priority
    private void addProcessToQueue(PCB process) {
        switch (process.getPriority()) {
            case realtime -> realTimeQueue.add(process);
            case interactive -> interactiveQueue.add(process);
            case background -> backgroundQueue.add(process);
        }
        System.out.println("Scheduler.createProcess: Adding process " + process.getClass().getSimpleName() + " to queue: " + process.getPriority());
    }

    // Select next process using probabilistic model
    private PCB selectProcess() {
        System.out.println("Scheduler.selectProcess: Selecting next process using probabilistic model");
        Random random = new Random();
        double randomDouble = random.nextDouble();

        if (!realTimeQueue.isEmpty()) {
            if (randomDouble < 0.6) return pollNext(realTimeQueue); // 60% chance for real-time
            if (!interactiveQueue.isEmpty() && randomDouble < 0.9) return pollNext(interactiveQueue); // 30% for interactive
            return pollNextBackground(); // 10% for background
        }

        if (!interactiveQueue.isEmpty()) {
            if (randomDouble < 0.75) {
                return pollNext(interactiveQueue); // Prefer interactive
            } else {
                PCB process = pollNextBackground();
                // Fallback to interactive if no background process is available
                return process != null ? process : pollNext(interactiveQueue);
            }
        }


        return pollNextBackground(); // Only background processes left
    }

    // Poll next background process avoiding IdleProcess if possible
    private PCB pollNextBackground() {
        for (Iterator<PCB> it = backgroundQueue.iterator(); it.hasNext(); ) {
            PCB pcb = it.next();
            if (!(pcb.userlandProcess instanceof IdleProcess) && !pcb.isDone()) {
                it.remove();
                return pcb;
            }
        }
        return pollNext(backgroundQueue);
    }

    // Poll queue until a process that is not done is found
    private PCB pollNext(LinkedList<PCB> queue) {
        while (!queue.isEmpty()) {
            PCB pcb = queue.poll();
            if (!pcb.isDone()) {
                return pcb;
            } else {
                System.out.println("Scheduler.pollNext: Discarding finished process " + pcb.userlandProcess.getClass().getSimpleName() + " PID " + pcb.pid);
            }
        }
        return null;
    }


    // wake up processes whose wakeup time has passed
    private void wakeupProcesses() {
        long currentTime = clock.millis();
        while (!sleepingProcesses.isEmpty() && sleepingProcesses.peek().wakeUpTime <= currentTime) {
            PCB process = sleepingProcesses.poll().process;
            addProcessToQueue(process);
        }
    }

    // put the running process to sleep
    public void sleep(int mills) {
        if (runningProcess != null) {
            runningProcess.wakeupTime = clock.millis() + mills;
            sleepingProcesses.add(new SleepingProcesses(runningProcess, runningProcess.wakeupTime));
        }
    }
}
