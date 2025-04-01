import java.util.HashMap;

public class Kernel extends Process implements Device {

    // The scheduler manages process switching and scheduling.
    private final Scheduler scheduler = new Scheduler();
    // The VFS (Virtual File System) handles device operations (e.g., file I/O) and routing of device calls.
    private final VFS vfs = new VFS();

    public HashMap<Integer, PCB> waitingForMessage = new HashMap<>();

    // Constructor for Kernel; prints a message for debugging purposes.
    public Kernel() {
        System.out.println("Kernel: Kernel constructor called");
    }

    // The main method for the kernel.
    // It continuously processes system calls submitted via OS.currentCall,
    // simulating a soft interrupt mechanism where userland calls are handled in privileged mode.
    @Override
    public void main() throws InterruptedException {
        System.out.println("Kernel.main: Kernel main loop started");
        while (true) {
            // Check if there is a pending system call from the OS.
            if (OS.currentCall != null) {
                // Process the system call based on its type.
                switch (OS.currentCall) {
                    case CreateProcess -> {
                        System.out.println("Kernel.main: System call is CreateProcess");
                        // Delegate process creation to the scheduler and return the new PID.
                        OS.retVal = CreateProcess((UserlandProcess) OS.parameters.get(0), (OS.PriorityType) OS.parameters.get(1));
                        System.out.println("Kernel.main: CreateProcess returned PID: " + OS.retVal);
                    }
                    case SwitchProcess -> {
                        System.out.println("Kernel.main: System call is SwitchProcess");
                        // Switch to the next process using the scheduler.
                        SwitchProcess();
                        OS.retVal = 1;
                    }
                    case Sleep -> {
                        System.out.println("Kernel.main: System call is Sleep");
                        // Pause the current process for a specified duration.
                        Sleep((int) OS.parameters.get(0));
                        OS.retVal = 1;
                    }
                    case GetPID -> {
                        System.out.println("Kernel.main: System call is GetPID");
                        // Retrieve the PID of the currently running process.
                        OS.retVal = GetPid();
                    }
                    case Exit -> {
                        System.out.println("Kernel.main: System call is Exit");
                        // Handle process exit: mark the process as done, remove it from the scheduler, and switch.
                        Exit();
                        OS.retVal = 1;
                    }
                    // Device system calls:
                    case Open -> {
                        System.out.println("Kernel.main: System call is Open");
                        // Open a device (e.g., file or random device) using the VFS.
                        OS.retVal = open((String) OS.parameters.get(0));
                    }
                    case Close -> {
                        System.out.println("Kernel.main: System call is Close");
                        // Close a device and free its slot in the current process.
                        close((int) OS.parameters.getFirst());
                        OS.retVal = 1;
                    }
                    case Read -> {
                        System.out.println("Kernel.main: System call is Read");
                        // Read data from a device via the VFS.
                        OS.retVal = read((int) OS.parameters.getFirst(), (int) OS.parameters.get(1));
                    }
                    case Seek -> {
                        System.out.println("Kernel.main: System call is Seek");
                        // Change the read/write position for a device.
                        seek((int) OS.parameters.get(0), (int) OS.parameters.get(1));
                        OS.retVal = 1;
                    }
                    case Write -> {
                        System.out.println("Kernel.main: System call is Write");
                        // Write data to a device and return the number of bytes written.
                        OS.retVal = write((int) OS.parameters.get(0), (byte[]) OS.parameters.get(1));
                    }

                    case SendMessage -> {
                        System.out.println("Kernel.main: System call is SendMessage");
                        // OS.parameters.get(0) is expected to be a KernelMessage.
                        SendMessage((KernelMessage) OS.parameters.get(0));
                        OS.retVal = 1;
                    }
                    case WaitForMessage -> {
                        System.out.println("Kernel.main: System call is WaitForMessage");
                        OS.retVal = WaitForMessage();
                    }
                    case GetPIDByName -> {
                        System.out.println("Kernel.main: System call is GetPIDByName");
                        // OS.parameters.get(0) is expected to be a String with the process name.
                        OS.retVal = GetPidByName((String) OS.parameters.get(0));
                    }
                }
            }
            // Reset the current system call and clear the parameters for the next call.
            OS.currentCall = null;
            OS.parameters.clear();
            // Once the OS call is processed, hand over control:
            // If a process is scheduled to run, start it.
            if (scheduler.getCurrentlyRunning() != null) {
                System.out.println("Kernel: Starting " + scheduler.getCurrentlyRunning().userlandProcess.getClass().getSimpleName());
                scheduler.getCurrentlyRunning().userlandProcess.start();
            }
            this.stop();
        }
    }

    // Scheduler-related helper methods:

    // SwitchProcess: Delegates process switching to the scheduler.
    private void SwitchProcess() throws InterruptedException {
        scheduler.switchProcess();
    }

    // CreateProcess: Delegates creation of a new process to the scheduler and returns its PID.
    private int CreateProcess(UserlandProcess up, OS.PriorityType priority) throws InterruptedException {
        return scheduler.createProcess(up, priority);
    }

    // Sleep: Delegates sleep functionality to the scheduler to pause the current process.
    private void Sleep(int mills) {
        scheduler.sleep(mills);
    }

    // Exit: Handles process exit.
    // Marks the current process as exited, removes it from the scheduler, and switches to the next process.
    private void Exit() throws InterruptedException {
        if (scheduler.runningProcess != null) {
            PCB exitingProcess = scheduler.runningProcess;
            exitingProcess.exit(); // Mark as exited.
            exitingProcess.messageQueue.clear();
            scheduler.removeProcess(exitingProcess);
            scheduler.switchProcess();
        }
    }

    // GetPid: Returns the PID of the currently running process.
    private int GetPid() {
        int pid = (scheduler.runningProcess != null) ? scheduler.runningProcess.pid : -1;
        return pid;
    }

    // Device system calls using VFS:

    // open: Opens a device like a file or random device based on the provided string.
    // It finds an available slot in the current process's openDevices array.
    @Override
    public int open(String s) {
        PCB current = scheduler.runningProcess;
        if (current == null) return -1;
        for (int i = 0; i < current.openDevices.length; i++) {
            if (current.openDevices[i] == -1) {
                int vfsId = vfs.open(s);
                if (vfsId == -1) return -1;
                current.openDevices[i] = vfsId;
                return i;
            }
        }
        return -1;
    }

    // close: Closes a device using the VFS.
    // It checks the device id and clears the corresponding entry in the current process's openDevices array.
    @Override
    public void close(int id) {
        PCB current = scheduler.runningProcess;
        if (current == null || id < 0 || id >= current.openDevices.length) return;
        int vfsId = current.openDevices[id];
        if (vfsId != -1) {
            vfs.close(vfsId);
            current.openDevices[id] = -1;
        }
    }

    // read: Reads data from a device via the VFS.
    // Validates the device id and returns the data as a byte array.
    @Override
    public byte[] read(int id, int size) {
        PCB current = scheduler.runningProcess;
        if (current == null || id < 0 || id >= current.openDevices.length) return new byte[0];
        int vfsId = current.openDevices[id];
        if (vfsId == -1) return new byte[0];
        return vfs.read(vfsId, size);
    }

    // seek: Adjusts the read/write position for a device through the VFS.
    @Override
    public void seek(int id, int to) {
        PCB current = scheduler.runningProcess;
        if (current == null || id < 0 || id >= current.openDevices.length) return;
        int vfsId = current.openDevices[id];
        if (vfsId == -1) return;
        vfs.seek(vfsId, to);
    }

    // write: Writes data to a device using the VFS.
    // Returns the number of bytes written.
    @Override
    public int write(int id, byte[] data) {
        PCB current = scheduler.runningProcess;
        if (current == null || id < 0 || id >= current.openDevices.length) return 0;
        int vfsId = current.openDevices[id];
        if (vfsId == -1) return 0;
        return vfs.write(vfsId, data);
    }

    // Copies the provided KernelMessage, sets the sender Pid, and delivers it to the target process
    private void SendMessage(KernelMessage km) {
        // Set sender pid
        int senderPid = GetPid();
        km.setSenderPid(senderPid);

        // Create copy of message so recipient gets its own instance (shared memory would allow overwriting)
        KernelMessage messageCopy = new KernelMessage(km);

        // Get target pid from the message and locate receiver pcb
        int targetPid = messageCopy.getReceiverPid();
        PCB receiver = scheduler.getPCB(targetPid);

        if (receiver == null) {
            System.out.println("SendMessage: Recipient with pid " + targetPid + " not found.");
            return;
        }

        // Add message copy to receivers message queue
        receiver.messageQueue.add(messageCopy);
        System.out.println("Kernel.SendMessage: Message sent from pid " + senderPid + " to pid " + targetPid);

        // If receiver is waiting for a message, remove it from the waiting map and requeue into runnable queue
        if (waitingForMessage.containsKey(targetPid)) {
            PCB waitingProcess = waitingForMessage.remove(targetPid);
            scheduler.wakeUpProcess(waitingProcess); // re-add process to running queue
            System.out.println("SendMessage: Woke up process with pid " + targetPid);
        }

    }

    // Checks the running process's running message queue and if it's empty it's marked as waiting and the
    // scheduler is invoked to switch process
    private Object WaitForMessage() throws InterruptedException {
        System.out.println("Kernel.WaitForMessage Entered");
        PCB current = scheduler.runningProcess;
        if (current == null) return null;

        // If there is no message then put process into waiting map and switch process. When a message is sent, process
        // will be awoken.
        while (current.messageQueue.isEmpty()) {
            // Add current process to the waiting map if it's not already waiting
            if (!waitingForMessage.containsKey(current.pid)) {
                waitingForMessage.put(current.pid, current);
                current.waitingForMessage = true;
                System.out.println("Kernel.WaitForMessage: Process " + current.userlandProcess.getClass().getSimpleName() + " is now waiting for a message.");
                scheduler.switchAndStartProcess();
            }
        }
        // When there is a message return it
        System.out.println("Kernel.WaitForMessage: Process " + current.userlandProcess.getClass().getSimpleName() + " is returning a message.");
        waitingForMessage.remove(current.pid);
        current.waitingForMessage = false;
        return current.messageQueue.removeFirst();
    }

    // Helper method to find a process by its name
    private int GetPidByName(String name) {
        return scheduler.getPidByName(name);
    }

    // GetMapping: Placeholder for obtaining memory mapping.
    private void GetMapping(int virtualPage) {
    }

    // AllocateMemory: Placeholder for memory allocation.
    private int AllocateMemory(int size) {
        return 0;
    }

    // FreeMemory: Placeholder for freeing allocated memory.
    private boolean FreeMemory(int pointer, int size) {
        return true;
    }

    // FreeAllMemory: Placeholder for freeing all memory associated with a process.
    private void FreeAllMemory(PCB currentlyRunning) {
    }

    // Returns the scheduler instance.
    public Scheduler getScheduler() {
        return scheduler;
    }

    // forceCloseDevice: Forces closure of a device for a given process and device slot.
    // for cleaning up open device entries when a process terminates.
    public void forceCloseDevice(PCB process, int deviceSlot) {
        if (process == null || deviceSlot < 0 || deviceSlot >= process.openDevices.length) return;
        int vfsId = process.openDevices[deviceSlot];
        if (vfsId != -1) {
            vfs.close(vfsId);
            process.openDevices[deviceSlot] = -1;
            System.out.println("Kernel.forceCloseDevice: Closed device slot " + deviceSlot + " for PID " + process.pid);
        }
    }
}
