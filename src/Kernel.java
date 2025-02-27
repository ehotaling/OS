public class Kernel extends Process  { // Kernel extends Process because it is a running process

    // Creates a private scheduler to manage process switching
    private final Scheduler scheduler = new Scheduler();

    public Kernel() {
        System.out.println("Kernel: Kernel constructor called."); // Debug print
    }

    @Override
    public void main() {
        System.out.println("Kernel.main: Kernel main loop started."); // Debug print
        while (true) {
            // Process system calls
            if (!OS.parameters.isEmpty()) {
                System.out.println("Kernel.main: Processing system call: " + OS.currentCall); // Debug print
                switch (OS.currentCall) { // get a job from OS, do it
                    case CreateProcess -> { // Note how we get parameters from OS and set the return value
                        System.out.println("Kernel.main: System call is CreateProcess."); // Debug print
                        OS.retVal = CreateProcess((UserlandProcess) OS.parameters.get(0), (OS.PriorityType) OS.parameters.get(1));
                        System.out.println("Kernel.main: CreateProcess call finished, retVal set: " + OS.retVal); // Debug print
                    }

                    case SwitchProcess -> {
                        System.out.println("Kernel.main: System call is SwitchProcess."); // Debug print
                        SwitchProcess();
                        System.out.println("Kernel.main: SwitchProcess call finished."); // Debug print
                    }
                    case Sleep -> {
                        System.out.println("Kernel.main: System call is Sleep."); // Debug print
                        Sleep((int) OS.parameters.get(0));
                        System.out.println("Kernel.main: Sleep call finished."); // Debug print
                    }
                    case GetPID -> {
                        System.out.println("Kernel.main: System call is GetPID."); // Debug print
                        OS.retVal = GetPid();
                        System.out.println("Kernel.main: GetPID call finished, retVal set: " + OS.retVal); // Debug print
                    }
                    case Exit -> {
                        System.out.println("Kernel.main: System call is Exit."); // Debug print
                        Exit();
                        System.out.println("Kernel.main: Exit call finished."); // Debug print
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
                OS.parameters.clear();
                OS.currentCall = null;
                System.out.println("Kernel.main: System call processed, parameters cleared, currentCall reset."); // Debug print

            }
            // Always switch to the next process before starting it
            System.out.println("Kernel.main: Calling scheduler.switchProcess()."); // Debug print
            scheduler.switchProcess();
            // Now that we have done the work asked of us, start some process then go to sleep.
            while (scheduler.runningProcess == null) {
                System.out.println("Kernel.main: No running process found, switching again..."); // Debug print
                scheduler.switchProcess(); // Keep switching until valid process is found
            }

            // Start the scheduled process
            System.out.println("Kernel.main: Starting scheduled process: " + scheduler.runningProcess.userlandProcess.getClass().getSimpleName() + ", PID: " + scheduler.runningProcess.pid); // Debug print
            scheduler.runningProcess.start();
            this.stop();
        }
    }

    private void SwitchProcess() {
        System.out.println("Kernel.SwitchProcess: Switching process via scheduler."); // Debug print
        scheduler.switchProcess();
    }


    // Calls the scheduler’s version of CreateProcess
    private int CreateProcess(UserlandProcess up, OS.PriorityType priority) {
        System.out.println("Kernel.CreateProcess: Creating process " + up.getClass().getSimpleName() + " with priority " + priority); // Debug print
        OS.retVal = scheduler.createProcess(up, priority);
        System.out.println("Kernel.CreateProcess: Process created, returning PID: " + OS.retVal); // Debug print
        return (int) OS.retVal;
    }

    private void Sleep(int mills) {
        scheduler.sleep(mills);
    }

    // Removes process from the scheduler and chooses another process
    private void Exit() {
        System.out.println("Kernel.Exit: Exiting current process."); // Debug print
        if (scheduler.runningProcess != null) {
            scheduler.removeProcess(scheduler.runningProcess);
            scheduler.runningProcess = null;
            scheduler.switchProcess();
        }
        System.out.println("Kernel.Exit: Exit process finished."); // Debug print
    }

    // This returns the PID of the currently running process
    private int GetPid() {
        System.out.println("Kernel.GetPid: Getting PID of running process."); // Debug print
        OS.retVal = scheduler.runningProcess.pid;
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