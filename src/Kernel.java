// Kernel.java
public class Kernel extends Process implements Device {

    private final Scheduler scheduler = new Scheduler();
    private final VFS vfs = new VFS();

    public Kernel() {
        System.out.println("Kernel: Kernel constructor called");
    }

    @Override
    public void main() throws InterruptedException {
        System.out.println("Kernel.main: Kernel main loop started");
        while (true) {
            if (OS.currentCall != null) {
                System.out.println("Kernel.main: Processing system call: " + OS.currentCall);
                switch (OS.currentCall) {
                    case CreateProcess -> {
                        System.out.println("Kernel.main: System call is CreateProcess");
                        int pid = CreateProcess((UserlandProcess) OS.parameters.get(0),
                                (OS.PriorityType) OS.parameters.get(1));
                        System.out.println("Kernel.main: CreateProcess returned PID: " + pid);
                        synchronized(OS.sysCallLock) {
                            if (OS.currentSysCallResult != null) {
                                OS.currentSysCallResult.value = pid;
                                OS.currentSysCallResult.latch.countDown();
                            }
                        }
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
                        int pid = GetPid();
                        synchronized(OS.sysCallLock) {
                            if (OS.currentSysCallResult != null) {
                                OS.currentSysCallResult.value = pid;
                                OS.currentSysCallResult.latch.countDown();
                            }
                        }
                    }
                    case Exit -> {
                        System.out.println("Kernel.main: System call is Exit");
                        Exit();
                    }
                    // Device system calls
                    case Open -> {
                        System.out.println("Kernel.main: System call is Open");
                        int ret = open((String) OS.parameters.get(0));
                        synchronized(OS.sysCallLock) {
                            if (OS.currentSysCallResult != null) {
                                OS.currentSysCallResult.value = ret;
                                OS.currentSysCallResult.latch.countDown();
                            }
                        }
                    }
                    case Close -> {
                        System.out.println("Kernel.main: System call is Close");
                        close((int) OS.parameters.get(0));
                    }
                    case Read -> {
                        System.out.println("Kernel.main: System call is Read");
                        byte[] data = read((int) OS.parameters.get(0), (int) OS.parameters.get(1));
                        synchronized(OS.sysCallLock) {
                            if (OS.currentSysCallResult != null) {
                                OS.currentSysCallResult.value = data;
                                OS.currentSysCallResult.latch.countDown();
                            }
                        }
                    }
                    case Seek -> {
                        System.out.println("Kernel.main: System call is Seek");
                        seek((int) OS.parameters.get(0), (int) OS.parameters.get(1));
                    }
                    case Write -> {
                        System.out.println("Kernel.main: System call is Write");
                        int written = write((int) OS.parameters.get(0), (byte[]) OS.parameters.get(1));
                        synchronized(OS.sysCallLock) {
                            if (OS.currentSysCallResult != null) {
                                OS.currentSysCallResult.value = written;
                                OS.currentSysCallResult.latch.countDown();
                            }
                        }
                    }
                    default -> {
                        System.out.println("Kernel.main: Unhandled system call: " + OS.currentCall);
                    }
                }
            }
            System.out.println("Kernel: Done with " + OS.currentCall + " call");
            OS.currentCall = null;
            OS.parameters.clear();
            this.stop();
        }
    }

    // Calls to the scheduler (and helper methods):

    private void SwitchProcess() throws InterruptedException {
        System.out.println("Kernel.SwitchProcess: Switching process via scheduler");
        scheduler.switchProcess();
    }

    private int CreateProcess(UserlandProcess up, OS.PriorityType priority) throws InterruptedException {
        return scheduler.createProcess(up, priority);
    }

    private void Sleep(int mills) {
        scheduler.sleep(mills);
    }

    private void Exit() throws InterruptedException {
        if (scheduler.runningProcess != null) {
            PCB exitingProcess = scheduler.runningProcess;
            exitingProcess.exit(); // Mark as exited
            scheduler.removeProcess(exitingProcess);
            scheduler.switchProcess();
        }
    }

    private int GetPid() {
        System.out.println("Kernel.GetPid: Getting PID of running process");
        int pid = (scheduler.runningProcess != null) ? scheduler.runningProcess.pid : -1;
        System.out.println("Kernel.GetPid: Returning PID: " + pid);
        return pid;
    }

    // Device system calls implementation using VFS:

    @Override
    public int open(String s) {
        PCB current = scheduler.runningProcess;
        if (current == null) {
            return -1;
        }
        for (int i = 0; i < current.openDevices.length; i++) {
            if (current.openDevices[i] == -1) {
                int vfsId = vfs.open(s);
                if (vfsId == -1) {
                    return -1;
                }
                current.openDevices[i] = vfsId;
                return i;
            }
        }
        return -1;
    }

    @Override
    public void close(int id) {
        PCB current = scheduler.runningProcess;
        if (current == null || id < 0 || id >= current.openDevices.length) {
            return;
        }
        int vfsId = current.openDevices[id];
        if (vfsId != -1) {
            vfs.close(vfsId);
            current.openDevices[id] = -1;
        }
    }

    @Override
    public byte[] read(int id, int size) {
        PCB current = scheduler.runningProcess;
        if (current == null || id < 0 || id >= current.openDevices.length) {
            return new byte[0];
        }
        int vfsId = current.openDevices[id];
        if (vfsId == -1) {
            return new byte[0];
        }
        return vfs.read(vfsId, size);
    }

    @Override
    public void seek(int id, int to) {
        PCB current = scheduler.runningProcess;
        if (current == null || id < 0 || id >= current.openDevices.length) {
            return;
        }
        int vfsId = current.openDevices[id];
        if (vfsId == -1) {
            return;
        }
        vfs.seek(vfsId, to);
    }

    @Override
    public int write(int id, byte[] data) {
        PCB current = scheduler.runningProcess;
        if (current == null || id < 0 || id >= current.openDevices.length) {
            return 0;
        }
        int vfsId = current.openDevices[id];
        if (vfsId == -1) {
            return 0;
        }
        return vfs.write(vfsId, data);
    }

    // Stub implementations for additional system calls:
    private void SendMessage() {
        // implementation pending
    }

    private KernelMessage WaitForMessage() {
        return null;
    }

    private int GetPidByName(String name) {
        return 0;
    }

    private void GetMapping(int virtualPage) {
    }

    private int AllocateMemory(int size) {
        return 0;
    }

    private boolean FreeMemory(int pointer, int size) {
        return true;
    }

    private void FreeAllMemory(PCB currentlyRunning) {
    }

    public Scheduler getScheduler() {
        return scheduler;
    }

    public void forceCloseDevice(PCB process, int deviceSlot) {
        if (process == null || deviceSlot < 0 || deviceSlot >= process.openDevices.length) {
            return;
        }
        int vfsId = process.openDevices[deviceSlot];
        if (vfsId != -1) {
            vfs.close(vfsId);
            process.openDevices[deviceSlot] = -1;
            System.out.println("Kernel.forceCloseDevice: Closed device slot " + deviceSlot + " for PID " + process.pid);
        }
    }
}
