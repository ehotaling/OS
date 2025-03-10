public class PCB { // Process Control Block
    private static int nextPid = 1;
    public final UserlandProcess userlandProcess;
    public int pid;
    private OS.PriorityType priority;
    private int timeoutCount; // tracks consecutive timeouts
    public long wakeupTime;

    // Only kernel should manage PCB's
    PCB(UserlandProcess up, OS.PriorityType priority) {
        System.out.println("PCB: Creating PCB for process: "
                + up.getClass().getSimpleName()
                + ", priority: " + priority
                + ", PID: " + nextPid);

        pid = nextPid++; // Process gets a pid, next process gets the next pid up
        this.userlandProcess = up;
        this.priority = priority;
        this.timeoutCount = 0;

//        if (!up.thread.isAlive()) {
//            System.out.println("PCB: Process thread is not alive, starting thread for: "
//                    + up.getClass().getSimpleName() + ", PID: " + pid);
//            up.thread.start();
//            System.out.println("PCB: Thread started for: "
//                    + up.getClass().getSimpleName() + ", PID: " + pid);
//        } else {
//            System.out.println("PCB: Process thread is already alive for: "
//                    + up.getClass().getSimpleName() + ", PID: " + pid);
//        }

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

    // Provide an accessor so Scheduler can see if we crossed the demotion threshold
    public int getTimeoutCount() {
        return timeoutCount;
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

    // calls userlandProcess’ stop. Loops with Thread.sleep() until ulp.isStopped() is true.
    public void stop() {
        System.out.println("PCB.stop: Stopping process: "
                + userlandProcess.getClass().getSimpleName()
                + ", PID: " + pid);

        while (!userlandProcess.isStopped()) {
            if (userlandProcess.isDone()) {  // If finished, break immediately.
                break;
            }
            try {
                userlandProcess.stop();
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        System.out.println("PCB.stop: Process stopped: "
                + userlandProcess.getClass().getSimpleName()
                + ", PID: " + pid);
    }

    // calls userlandprocess’ isDone()
    public boolean isDone() {
        return userlandProcess.isDone();
    }

    // calls userlandprocess’ start()
    void start() {
        System.out.println("PCB.start: Starting process: "
                + userlandProcess.getClass().getSimpleName()
                + ", PID: " + pid);
        userlandProcess.start();
    }

    private void demotePriority() {
        OS.PriorityType oldPriority = priority;
        switch (priority) {
            case realtime -> priority = OS.PriorityType.interactive;
            case interactive -> priority = OS.PriorityType.background;
        }
        resetTimeoutCount();
        System.out.println("Scheduler.demote: Process " + pid
                + " demoted from " + oldPriority + " to " + priority);
    }
}
