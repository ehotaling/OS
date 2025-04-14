// OS.java
// This file serves as the gateway between the userland thread and the kernel thread.
// It implements system call interfaces for process management and device I/O.
// - Devices can be diverse (disks, video, sound, virtual devices, etc.) and must be managed uniformly
//   through a standardized interface (open, close, read, seek, write).
// - The OS must track open devices per process and ensure that on process termination,
//   all device resources are released.
// - Kernel calls need to safely cross the kernel–userland boundary.
// - The kernel, PCB modifications, and Virtual File System (VFS) must correctly map user calls
//   to device-specific implementations.

import java.util.ArrayList;
import java.util.List;

public class OS {

    // The one and only instance of the kernel.
    private static Kernel ki;

    // List used to pass system call parameters between userland and kernel.
    // (This list is reused for each call, so it must be cleared before adding new parameters.)
    public static List<Object> parameters = new ArrayList<>();


    // Shared return value for system calls. Kernel writes to this and userland waits for it.
    public static Object retVal;

    public static void getMapping(int virtualPageNum) {
    }

    // Enum defining the types of system calls. Includes process management calls and device I/O operations.
    public enum CallType {
        SwitchProcess, SendMessage, Open, Close, Read, Seek, Write,
        GetMapping, CreateProcess, Sleep, GetPID, AllocateMemory,
        FreeMemory, GetPIDByName, WaitForMessage, Exit
    }
    // Indicates the current system call type being processed.
    public static CallType currentCall;

    // Priority types for process creation; used to determine scheduling behavior.
    public enum PriorityType { realtime, interactive, background }

    // Starts the kernel thread if it is not null.
    // This is invoked before making any system call to ensure the kernel is running.
    private static void startTheKernel() throws InterruptedException {
        // Start the kernel thread if it is not already running.
        if (!ki.isAlive()) {
            ki.start();
        } else {
            // Kernel is already alive but is blocked, so unblock it.
            ki.resumeProcess();
        }

        // Yield control to the kernel:
        // If the scheduler has a currently running process, stop that process.
        if (ki.getScheduler() != null && ki.getScheduler().getCurrentlyRunning() != null) {
            System.out.println("OS.startTheKernel: Stopping " + ki.getScheduler().getCurrentlyRunning().userlandProcess.getClass().getSimpleName() + " for a system call: " + OS.currentCall);
            ki.getScheduler().getCurrentlyRunning().stop();
        }
        waitForRetVal();
    }

    private static void waitForRetVal() {
        // wait until the kernel completes its task and sets retVal.
        while (retVal == null) {
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    // Startup method to initialize the kernel and create the initial process.
    // This corresponds to the OS bootstrapping and must be done before any other process operations.
    // 'init' is the initial userland process to be created.
    public static void Startup(UserlandProcess init) throws InterruptedException {
        System.out.println("OS.Startup: Initializing Kernel");
        ki = new Kernel();
        // Pass kernel reference to scheduler, which will later manage the PCB (including tracking open device ids)
        ki.getScheduler().setKernel(ki);
        System.out.println("OS.Startup: Kernel initialized");
        System.out.println("OS.Startup: Creating IdleProcess.");
        CreateProcess(new IdleProcess(), PriorityType.background);
        System.out.println("OS.Startup: Creating InitProcess");
        CreateProcess(init, PriorityType.interactive);
        System.out.println("OS.Startup: InitProcess created.");
    }

    // CreateProcess without explicit priority; defaults to an interactive process.
    // 'up' is the userland process instance.
    // Returns the process ID (PID) assigned.
    public static int CreateProcess(UserlandProcess up) throws InterruptedException {
        return CreateProcess(up, PriorityType.interactive);
    }

    // CreateProcess with an explicit priority.
    // Clears previous parameters, adds the userland process and its priority to the parameters list,
    // sets the current system call type to CreateProcess, and starts the kernel thread if needed.
    // Waits for a return value (PID) from the kernel.
    public static int CreateProcess(UserlandProcess up, PriorityType priority) throws InterruptedException {
        parameters.clear();
        parameters.add(up);
        parameters.add(priority);
        currentCall = CallType.CreateProcess;
        startTheKernel();
        int pid = (int) retVal;
        retVal = null;
        return pid;
    }

    // switchProcess: Requests a process switch.
    // Debug prints are used for tracing; clears parameters and sets the system call type.
    public static void switchProcess() throws InterruptedException {
        parameters.clear();
        currentCall = CallType.SwitchProcess;
        startTheKernel();
    }

    // GetPID: Retrieves the current process ID.
    // Sets the system call type, starts the kernel, and waits until the kernel returns the PID.
    // Returns the current process's PID.
    public static int GetPID() throws InterruptedException {
        parameters.clear();
        currentCall = CallType.GetPID;
        startTheKernel();
        int pid = (int) retVal;
        retVal = null;
        return pid;
    }

    // Exit: Processes the exit system call.
    // On process termination, the kernel must ensure that all open devices are closed as per the assignment instructions.
    public static void Exit() throws InterruptedException {
        parameters.clear();
        currentCall = CallType.Exit;
        startTheKernel();
    }

    // Sleep: Pauses process execution for the specified number of milliseconds.
    // The sleep duration is passed to the kernel as a parameter.
    public static void Sleep(int mills) throws InterruptedException {
        parameters.clear();
        parameters.add(mills);
        currentCall = CallType.Sleep;
        startTheKernel();
    }

    // ***** Device Calls *****
    // These methods implement the standard device I/O system calls.
    // Each device (e.g., RandomDevice, FakeFileSystem) implements the Device interface with open(), close(), read(), seek(), and write().
    // The OS methods below pass the call through to the kernel, which uses the Virtual File System (VFS) to route the request.

    // Open: Performs a device open system call.
    // Accepts a String parameter which may be a filename or a seed (for RandomDevice),
    // and calls the kernel, which delegates to the VFS.
    // Returns the device id (or VFS index) returned by the kernel.
    public static int Open(String s) throws InterruptedException {
        parameters.clear();
        parameters.add(s);
        currentCall = CallType.Open;
        startTheKernel();
        int deviceId = (int) retVal;
        retVal = null;
        return deviceId;
    }

    // Close: Performs a device close system call.
    // Clears parameters, adds the device id, sets the system call type,
    // and calls the kernel to handle the closure (including cleaning up PCB entries).
    public static void Close(int id) throws InterruptedException {
        parameters.clear();
        parameters.add(id);
        currentCall = CallType.Close;
        startTheKernel();
    }

    // Read: Performs a device read system call.
    // Sends the device id and the number of bytes to read to the kernel,
    // which then delegates the call to the appropriate device via the VFS.
    // Returns a byte array containing the data read.
    public static byte[] Read(int id, int size) throws InterruptedException {
        parameters.clear();
        parameters.add(id);
        parameters.add(size);
        currentCall = CallType.Read;
        startTheKernel();
        byte[] dataRead = (byte[]) retVal;
        retVal = null;
        return dataRead;
    }

    // Seek: Performs a device seek system call.
    // Instructs the device to adjust its internal pointer to a specified position.
    public static void Seek(int id, int to) throws InterruptedException {
        parameters.clear();
        parameters.add(id);
        parameters.add(to);
        currentCall = CallType.Seek;
        startTheKernel();
    }

    // Write: Performs a device write system call.
    // Sends data to the device and waits for a return value indicating the number of bytes written.
    // Returns the number of bytes written.
    public static int Write(int id, byte[] data) throws InterruptedException {
        parameters.clear();
        parameters.add(id);
        parameters.add(data);
        currentCall = CallType.Write;
        startTheKernel();
        int bytesWritten = (int) retVal;
        retVal = null;
        return bytesWritten;
    }

    // ***** Message Calls  *****

    // Sends a kernel message by forwarding the message to the kernel
    public static void SendMessage(KernelMessage km) throws InterruptedException {
        parameters.clear();
        parameters.add(km);
        currentCall = CallType.SendMessage;
        System.out.println("OS.SendMessage: " + km);
        startTheKernel();
        retVal = null;
    }

    // Waits for a kernel message to arrive and returns it
    public static KernelMessage WaitForMessage() throws InterruptedException {
        parameters.clear();
        currentCall = CallType.WaitForMessage;
        startTheKernel();
        KernelMessage km = (KernelMessage) retVal;
        retVal = null;
        return km;
    }

    // Returns the pid of a process given its name
    public static int GetPidByName(String name) throws InterruptedException {
        parameters.clear();
        parameters.add(name);
        currentCall = CallType.GetPIDByName;
        startTheKernel();
        int pid = (int) retVal;
        retVal = null;
        return pid;
    }

    // ***** Memory Calls (Stubs) *****
    // Placeholders for future memory management features such as mapping, allocation, and freeing of memory.

    // GetMapping: Stub for obtaining the mapping for a virtual page.
    public static void GetMapping(int virtualPage) {
        // implementation pending
    }

    // AllocateMemory: Stub for memory allocation.
    // Returns an identifier for the allocated memory (currently 0 as implementation is pending).
    public static int AllocateMemory(int size) {
        return 0; // implementation pending
    }

    // FreeMemory: Stub for freeing allocated memory.
    // Returns true if the memory was freed successfully, false otherwise.
    public static boolean FreeMemory(int pointer, int size) {
        return false; // implementation pending
    }
}
