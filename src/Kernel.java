public class Kernel extends Process  { // Kernel extends Process because it is a running process

    // Creates a private scheduler to manage process switching
    private final Scheduler scheduler = new Scheduler();

    public Kernel() {
    }

    @Override
    public void main() {
        while (true) { // Warning on infinite loop is OK...
            // If there is a pending system call, process it
            if (!OS.parameters.isEmpty()) {

                switch (OS.currentCall) { // get a job from OS, do it
                    case CreateProcess ->  // Note how we get parameters from OS and set the return value
                            OS.retVal = CreateProcess((UserlandProcess) OS.parameters.get(0), (OS.PriorityType) OS.parameters.get(1));
                    case SwitchProcess -> SwitchProcess();
                    case Sleep -> Sleep((int) OS.parameters.get(0));
                    case GetPID -> OS.retVal = GetPid();
                    case Exit -> Exit();
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
            }
            // Now that we have done the work asked of us, start some process then go to sleep.
            if (scheduler.runningProcess != null) {
                scheduler.runningProcess.start(); // calls start() on the next process to run
            }
            this.stop(); // Calls stop() on self, so that only one process is running
        }
    }

    private void SwitchProcess() {
        scheduler.switchProcess();
    }

    // For assignment 1, you can ignore the priority. We will use that in assignment 2

    // Calls the scheduler’s version of CreateProcess
    private int CreateProcess(UserlandProcess up, OS.PriorityType priority) {

        OS.retVal = scheduler.createProcess(up, priority);
        return (int) OS.retVal;
    }

    private void Sleep(int mills) {
        scheduler.sleep(mills);
    }

    // Removes process from the scheduler and chooses another process
    private void Exit() {
        if (scheduler.runningProcess != null) {
            scheduler.removeProcess(scheduler.runningProcess);
            scheduler.runningProcess = null;
            scheduler.switchProcess();
        }
    }

    // This returns the PID of the currently running process
    private int GetPid() {
        OS.retVal = scheduler.runningProcess.pid;
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