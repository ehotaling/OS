public class Kernel extends Process  { // Kernel extends Process because it is a running process

    // Creates a private scheduler to manage process switching
    private final Scheduler scheduler = new Scheduler();

    public Kernel() {
        System.out.println("Kernel: Kernel constructor called."); // Debug print
    }

    @Override
    public void main() {
        System.out.println("Kernel.main: Kernel main loop started.");

        while (true) {
            // Process system calls
            if (!OS.parameters.isEmpty()) {
                System.out.println("Kernel.main: Processing system call: " + OS.currentCall);

                switch (OS.currentCall) {
                    case CreateProcess -> {
                        System.out.println("Kernel.main: System call is CreateProcess.");
                        OS.retVal = CreateProcess((UserlandProcess) OS.parameters.get(0), (OS.PriorityType) OS.parameters.get(1));
                        System.out.println("Kernel.main: CreateProcess returned PID: " + OS.retVal);
                    }
                    case SwitchProcess -> {
                        System.out.println("Kernel.main: System call is SwitchProcess.");
                        SwitchProcess();
                    }
                    case Sleep -> {
                        System.out.println("Kernel.main: System call is Sleep.");
                        Sleep((int) OS.parameters.get(0));
                    }
                    case GetPID -> {
                        System.out.println("Kernel.main: System call is GetPID.");
                        OS.retVal = GetPid();
                    }
                    case Exit -> {
                        System.out.println("Kernel.main: System call is Exit.");
                        Exit();
                    }

                     /*
                    // Devices
                    case Open ->
                    case Close ->
                    case Read ->
                    case Seek ->
                    case Write ->
                    // Messages
                    case GetPIDByName ->
                    case SendMessage ->
                    case WaitForMessage ->
                    // Memory
                    case GetMapping ->
                    case AllocateMemory ->
                    case FreeMemory ->
                     */

                }
                OS.parameters.clear(); // Clear parameters after processing
                OS.currentCall = null; // Reset current call
            }

            this.stop();
        }
    }


    private void SwitchProcess() {
        System.out.println("Kernel.SwitchProcess: Switching process via scheduler."); // Debug print
        scheduler.switchProcess();

        // scheduler.runningProcess.start(); // don't think we need this here, we handle this in Scheduler.switchProcess
    }


    // Calls the scheduler’s version of CreateProcess
    private int CreateProcess(UserlandProcess up, OS.PriorityType priority) {
        System.out.println("Kernel.CreateProcess: Creating process " + up.getClass().getSimpleName() + " with priority " + priority); // Existing print

        // Added debug prints START
        System.out.println("Kernel.CreateProcess: Calling scheduler.createProcess for process: " + up.getClass().getSimpleName());
        int pid = scheduler.createProcess(up, priority);
        System.out.println("Kernel.CreateProcess: scheduler.createProcess returned PID: " + pid + " for process type: " + up.getClass().getSimpleName());

        System.out.println("Kernel.CreateProcess: Setting OS.retVal to PID: " + pid + " for process type: " + up.getClass().getSimpleName());
        OS.retVal = pid;
        System.out.println("Kernel.CreateProcess: OS.retVal set to: " + OS.retVal + " for process type: " + up.getClass().getSimpleName());
        // Added debug prints END

        System.out.println("Kernel.CreateProcess: Process created, returning PID: " + OS.retVal); // Existing print (this might be redundant now, but let's keep it for now)
        return (int) OS.retVal;
    }

    private void Sleep(int mills) {
        scheduler.sleep(mills);
    }

    // Removes process from the scheduler and chooses another process
    private void Exit() {
        System.out.println("Kernel.Exit: Exiting current process.");
        if (scheduler.runningProcess != null) {
            // Mark the process as terminated.
            scheduler.runningProcess.userlandProcess.terminate();
            while (!scheduler.runningProcess.isDone()) {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException _) {}
            }
            // Remove it from the scheduler.
            scheduler.removeProcess(scheduler.runningProcess);
            scheduler.runningProcess = null;
            scheduler.switchProcess();
        }
        OS.retVal = 0;  // Signal completion so any waiting loop can exit.
        System.out.println("Kernel.Exit: Exit process finished.");
    }



    // This returns the PID of the currently running process
    private int GetPid() {
        System.out.println("Kernel.GetPid: Getting PID of running process."); // Debug print
        if (scheduler.runningProcess != null) {
            OS.retVal = scheduler.runningProcess.pid;
        } else {
            OS.retVal = -1;
        }
        System.out.println("Kernel.GetPid: Returning PID: " + OS.retVal); // Debug print
        return (int) OS.retVal;
    }

    private int Open(String s) {
        return 0; // change this
    }

    private void Close(int id) {
    }

    private byte[] Read(int id, int size) {
        return null; // change this
    }

    private void Seek(int id, int to) {
    }

    private int Write(int id, byte[] data) {
        return 0; // change this
    }

    private void SendMessage(/*KernelMessage km*/) {
    }

    private KernelMessage WaitForMessage() {
        return null;
    }

    private int GetPidByName(String name) {
        return 0; // change this
    }

    private void GetMapping(int virtualPage) {
    }

    private int AllocateMemory(int size) {
        return 0; // change this
    }

    private boolean FreeMemory(int pointer, int size) {
        return true;
    }

    private void FreeAllMemory(PCB currentlyRunning) {
    }

    public Scheduler getScheduler() {
        return scheduler;
    }

}