public class Kernel extends Process implements Device {
    private final Scheduler scheduler = new Scheduler();

    // private VFS instance to handle device calls
    private final VFS vfs = new VFS();

    public Kernel() {
        System.out.println("Kernel: Kernel constructor called"); // debug print
    }

    @Override
    public void main() throws InterruptedException {
        System.out.println("Kernel.main: Kernel main loop started"); // debug print
        while (true) {
            // check if a system call is pending
            if (OS.currentCall != null) {
                System.out.println("Kernel.main: Processing system call: " + OS.currentCall);
                // process the system call based on its type
                switch (OS.currentCall) {
                    case CreateProcess -> {
                        System.out.println("Kernel.main: System call is CreateProcess");
                        OS.retVal = CreateProcess((UserlandProcess) OS.parameters.get(0), (OS.PriorityType) OS.parameters.get(1));
                        System.out.println("Kernel.main: CreateProcess returned PID: " + OS.retVal);
                    }
                    case SwitchProcess -> {
                        System.out.println("Kernel.main: System call is SwitchProcess");
                        SwitchProcess();
                    }
                    case Sleep -> {
                        System.out.println("Kernel.main: System call is Sleep");
                        Sleep((int) OS.parameters.get(0));
                    }
                    case GetPID -> {
                        System.out.println("Kernel.main: System call is GetPID");
                        OS.retVal = GetPid();
                    }
                    case Exit -> {
                        System.out.println("Kernel.main: System call is Exit");
                        Exit();
                    }

                    // Device system calls
                    case Open -> {
                        System.out.println("Kernel.main: System call is Open");
                        OS.retVal = open((String) OS.parameters.get(0));
                    }
                    case Close -> {
                        System.out.println("Kernel.main: System call is Close");
                        close((int) OS.parameters.get(0));
                    }
                    case Read -> {
                        System.out.println("Kernel.main: System call is Read");
                        OS.retVal = read((int) OS.parameters.get(0), (int) OS.parameters.get(1));
                    }
                    case Seek -> {
                        System.out.println("Kernel.main: System call is Seek");
                        seek((int) OS.parameters.get(0), (int) OS.parameters.get(1));
                    }
                    case Write -> {
                        System.out.println("Kernel.main: System call is Write");
                        OS.retVal = write((int) OS.parameters.get(0), (byte[]) OS.parameters.get(1));
                    }
                }
            }
            // clear system call state so that it is not re-processed
            OS.currentCall = null;
            OS.parameters.clear();
            scheduler.switchProcess();
            this.stop();
        }

    }


    // call scheduler to switch process
    private void SwitchProcess() throws InterruptedException {
        System.out.println("Kernel.SwitchProcess: Switching process via scheduler");
        scheduler.switchProcess();
    }

    // create a new process via scheduler and return its PID
    private int CreateProcess(UserlandProcess up, OS.PriorityType priority) throws InterruptedException {
        return scheduler.createProcess(up, priority);
    }

    // call scheduler to sleep the current process for given milliseconds
    private void Sleep(int mills) {
        scheduler.sleep(mills);
    }

    // exit the current process via scheduler
    private void Exit() throws InterruptedException {
        if (scheduler.runningProcess != null) {
            PCB exitingProcess = scheduler.runningProcess;
            exitingProcess.exit(); // Mark as exited
            scheduler.removeProcess(exitingProcess); // Remove from scheduler
            scheduler.switchProcess(); // Switch to another process
        }
    }




    // get the PID of the running process from the scheduler
    private int GetPid() {
        System.out.println("Kernel.GetPid: Getting PID of running process");
        int pid;
        if (scheduler.runningProcess != null) {
            pid = scheduler.runningProcess.pid;
        } else {
            pid = -1;
        }
        System.out.println("Kernel.GetPid: Returning PID: " + pid);
        return pid;
    }

    // open a device using VFS and store its VFS id in the PCB's openDevices array
    @Override
    public int open(String s) {
        // get the currently running process PCB
        PCB current = scheduler.runningProcess;
        if (current == null) {
            // no running process so return error code
            return -1;
        }
        // loop through the openDevices array to find an empty slot (-1 indicates empty)
        for (int i = 0; i < current.openDevices.length; i++) {
            if (current.openDevices[i] == -1) {
                // call VFS open with the provided string parameter
                int vfsId = vfs.open(s);
                if (vfsId == -1) {
                    // VFS open failed so return error code
                    return -1;
                }
                // store the VFS id in the PCB's openDevices array
                current.openDevices[i] = vfsId;
                return i;
            }
        }
        // no empty slot found so return error code
        return -1;
    }

    // close a device by using the VFS and clear the corresponding PCB openDevices slot
    @Override
    public void close(int id) {
        // get the currently running process PCB
        PCB current = scheduler.runningProcess;
        if (current == null || id < 0 || id >= current.openDevices.length) {
            // invalid process or id so do nothing
            return;
        }
        int vfsId = current.openDevices[id];
        if (vfsId != -1) {
            // call VFS close and clear the slot in the PCB openDevices array
            vfs.close(vfsId);
            current.openDevices[id] = -1;
        }
    }

    // read data from a device via VFS using the PCB mapping
    @Override
    public byte[] read(int id, int size) {
        // get the currently running process PCB
        PCB current = scheduler.runningProcess;
        if (current == null || id < 0 || id >= current.openDevices.length) {
            // invalid process or id so return an empty byte array
            return new byte[0];
        }
        int vfsId = current.openDevices[id];
        if (vfsId == -1) {
            // no device open in that slot so return an empty byte array
            return new byte[0];
        }
        // call VFS read and return the resulting data
        return vfs.read(vfsId, size);
    }

    // seek to a position in a device via VFS using the PCB mapping
    @Override
    public void seek(int id, int to) {
        // get the currently running process PCB
        PCB current = scheduler.runningProcess;
        if (current == null || id < 0 || id >= current.openDevices.length) {
            // invalid process or id so do nothing
            return;
        }
        int vfsId = current.openDevices[id];
        if (vfsId == -1) {
            // no device open in that slot so do nothing
            return;
        }
        // call VFS seek to move the file pointer
        vfs.seek(vfsId, to);
    }

    // write data to a device via VFS using the PCB mapping
    @Override
    public int write(int id, byte[] data) {
        // get the currently running process PCB
        PCB current = scheduler.runningProcess;
        if (current == null || id < 0 || id >= current.openDevices.length) {
            // invalid process or id so return 0 bytes written
            return 0;
        }
        int vfsId = current.openDevices[id];
        if (vfsId == -1) {
            // no device open in that slot so return 0 bytes written
            return 0;
        }
        // call VFS write and return the number of bytes written
        return vfs.write(vfsId, data);
    }

    // send a message to another process
    private void SendMessage() {
        // implementation pending
    }

    // wait for a message from another process
    private KernelMessage WaitForMessage() {
        // implementation pending
        return null;
    }

    // get a process PID by its name
    private int GetPidByName(String name) {
        // implementation pending return error code
        return 0;
    }

    // get memory mapping information
    private void GetMapping(int virtualPage) {
        // implementation pending
    }

    // allocate memory and return a pointer
    private int AllocateMemory(int size) {
        // implementation pending return error code
        return 0;
    }

    // free allocated memory and return status
    private boolean FreeMemory(int pointer, int size) {
        // implementation pending return success
        return true;
    }

    // free all memory allocated to the given process (stub implementation)
    private void FreeAllMemory(PCB currentlyRunning) {
        // implementation pending
    }

    // getter for the scheduler instance
    public Scheduler getScheduler() {
        return scheduler;
    }

    public void forceCloseDevice(PCB process, int deviceSlot) {
        if (process == null || deviceSlot < 0 || deviceSlot >= process.openDevices.length) {
            return; // invalid input so do nothing
        }
        int vfsId = process.openDevices[deviceSlot];
        if (vfsId != -1) {
            vfs.close(vfsId); // close device in VFS
            process.openDevices[deviceSlot] = -1; // mark slot as free
            System.out.println("Kernel.forceCloseDevice: Closed device slot " + deviceSlot + " for PID " + process.pid);
        }
    }


}
