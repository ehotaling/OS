// Kernel.java
// The Kernel class is a core part of our OS simulator. It extends Process and implements the Device interface,
// handling both system calls (e.g., process creation, switching, sleep, exit) and device I/O operations.
// This file is essential as many later assignments depend on a properly working kernel.
public class Kernel extends Process implements Device {

    // The scheduler manages process switching and scheduling.
    private final Scheduler scheduler = new Scheduler();
    // The VFS (Virtual File System) handles device operations (e.g., file I/O) and routing of device calls.
    private final VFS vfs = new VFS();

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
                System.out.println("Kernel.main: Processing system call: " + OS.currentCall);
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
                    }
                    case Sleep -> {
                        System.out.println("Kernel.main: System call is Sleep");
                        // Pause the current process for a specified duration.
                        Sleep((int) OS.parameters.get(0));
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
                        close((int) OS.parameters.get(0));
                    }
                    case Read -> {
                        System.out.println("Kernel.main: System call is Read");
                        // Read data from a device via the VFS.
                        OS.retVal = read((int) OS.parameters.get(0), (int) OS.parameters.get(1));
                    }
                    case Seek -> {
                        System.out.println("Kernel.main: System call is Seek");
                        // Change the read/write position for a device.
                        seek((int) OS.parameters.get(0), (int) OS.parameters.get(1));
                    }
                    case Write -> {
                        System.out.println("Kernel.main: System call is Write");
                        // Write data to a device and return the number of bytes written.
                        OS.retVal = write((int) OS.parameters.get(0), (byte[]) OS.parameters.get(1));
                    }
                    default -> {
                        // Handle any unrecognized system call.
                        System.out.println("Kernel.main: Unhandled system call: " + OS.currentCall);
                    }
                }
            }
            if (OS.currentCall != null) {
                System.out.println("Kernel: Done with " + OS.currentCall + " call");
            }
            // Reset the current system call and clear the parameters for the next call.
            OS.currentCall = null;
            OS.parameters.clear();
        }
    }

    // Scheduler-related helper methods:

    // SwitchProcess: Delegates process switching to the scheduler.
    private void SwitchProcess() throws InterruptedException {
        System.out.println("Kernel.SwitchProcess: Switching process via scheduler");
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
            scheduler.removeProcess(exitingProcess);
            scheduler.switchProcess();
        }
    }

    // GetPid: Returns the PID of the currently running process.
    private int GetPid() {
        System.out.println("Kernel.GetPid: Getting PID of running process");
        int pid = (scheduler.runningProcess != null) ? scheduler.runningProcess.pid : -1;
        System.out.println("Kernel.GetPid: Returning PID: " + pid);
        return pid;
    }

    // Device system calls using VFS:

    // open: Opens a device (e.g., file, random device) based on the provided string.
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

    // Stub implementations for additional system calls (pending implementation):

    // SendMessage: Placeholder for sending a kernel message.
    private void SendMessage() { /* pending */ }

    // WaitForMessage: Placeholder for waiting for a kernel message.
    private KernelMessage WaitForMessage() {
        return null;
    }

    // GetPidByName: Placeholder for retrieving a PID by process name.
    private int GetPidByName(String name) {
        return 0;
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
    // This is useful for cleaning up open device entries when a process terminates.
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
