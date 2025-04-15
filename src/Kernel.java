import java.util.HashMap;

public class Kernel extends Process implements Device {

    // The scheduler manages process switching and scheduling.
    private final Scheduler scheduler = new Scheduler();
    // The VFS (Virtual File System) handles device operations (e.g., file I/O) and routing of device calls.
    private final VFS vfs = new VFS();

    public HashMap<Integer, PCB> waitingForMessage = new HashMap<>();

    public boolean[] freeSpace = new boolean[1024]; // array of booleans to track which pages are in use.
    private static final int PAGE_SIZE = 1024; // Define page size constant

    // Constructor for Kernel, initializes memory free space to false.
    public Kernel() {
        System.out.println("Kernel: Kernel constructor called");
        // initialize freeSpace
        for (int i = 0; i < PAGE_SIZE; i++) {
            freeSpace[i] = true;
        }
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
                        SendMessage((KernelMessage) OS.parameters.getFirst());
                        OS.retVal = 1;
                    }
                    case WaitForMessage -> {
                        System.out.println("Kernel.main: System call is WaitForMessage");
                        var retVal = WaitForMessage();
                        if (retVal != null)
                            OS.retVal = retVal;
                    }
                    case GetPIDByName -> {
                        System.out.println("Kernel.main: System call is GetPIDByName");
                        // OS.parameters.get(0) is expected to be a String with the process name.
                        OS.retVal = GetPidByName((String) OS.parameters.getFirst());
                    }
                    case GetMapping -> {
                        System.out.println("Kernel.main: System call is GetMapping");
                        // OS.parameters.get(0) is expected to be a virtual page number
                        GetMapping((Integer) OS.parameters.getFirst());
                        OS.retVal = 1;
                    }
                    case AllocateMemory -> {
                        System.out.println("Kernel.main: System call is AllocateMemory");
                        OS.retVal = AllocateMemory((Integer) OS.parameters.getFirst());
                    }
                    case FreeMemory -> {
                        System.out.println("Kernel.main: System call is FreeMemory");
                        OS.retVal = FreeMemory((int) OS.parameters.get(0), (int) OS.parameters.get(1));
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

    // Paging



    // Messages

    // GetMapping: Placeholder for obtaining memory mapping.
    private void GetMapping(int virtualPageNum) throws InterruptedException {
        // Look up the value inside the currently running processes page table and return it
        PCB currentProcess = scheduler.runningProcess;

        if (currentProcess == null) {
            // Handle error: No running process? Should not happen if called correctly.
            System.out.println("Kernel.GetMapping Error: No running process!");
            return;
        }

        // Get current process page table
        int[] pageTable = currentProcess.pageTable;

        // Check bounds for virtualPageNum
        if (virtualPageNum < 0 || virtualPageNum >= pageTable.length) {
            System.out.println("Kernel.GetMapping: Seg fault. Invalid page number: " + virtualPageNum);
            Exit();
            return;
        }

        int physicalPageNum = pageTable[virtualPageNum];

        // if there is no mapping that is a seg fault
        if (physicalPageNum == -1) {
            System.out.println("Kernel.GetMapping: Seg fault: Unmapped page " + virtualPageNum);
            Exit(); // Will exit current process
            return;
        }

        // Otherwise, update random TLB entry
        int randomIndex = (int) (Math.random() * 2); // for 0 or 1
        Hardware.TLB[randomIndex][0] = virtualPageNum;
        Hardware.TLB[randomIndex][1] = physicalPageNum;

        System.out.println("Kernel.GetMapping: Mapped virtual page " + virtualPageNum + " to physical page " +
                physicalPageNum + " in TLB[" + randomIndex + "]");
    }

    // Finds number of pages to add, adds mapping to the first correctly sized hole in virtual space,
    // marks physical pages as in use, returns start memory address
    private int AllocateMemory(int sizeInBytes) {
        if (sizeInBytes <= 0 || sizeInBytes % PAGE_SIZE != 0) {
            System.err.println("Kernel.AllocateMemory Error: Invalid size " + sizeInBytes + ". Must be a positive multiple of " + PAGE_SIZE);
            return -1; // Indicate failure
        }
        int numberOfPages = sizeInBytes / PAGE_SIZE;
        PCB currentProcess = scheduler.runningProcess;
        if (currentProcess == null) {
            System.err.println("Kernel.AllocateMemory Error: No running process.");
            return -1;
        }

        // Find contiguous physical pages
        int consecutiveFreeCount = 0;
        int startPhysicalPage = -1; // initialize to not found
        for (int i =0; i < freeSpace.length; i++) {
            if (freeSpace[i]) { // if physical page i is free
                consecutiveFreeCount++;
                if (consecutiveFreeCount == numberOfPages) { // Found enough pages
                    startPhysicalPage = i - numberOfPages + 1;
                break;
                }
            } else {
                consecutiveFreeCount = 0; // reset counter if page is not free
            }
        }

        // Check if physical pages were found
        if (startPhysicalPage == -1) {
            System.out.println("Kernel.AllocateMemory: Allocate Memory Failed. Not enough contiguous memory");
            return -1; // Return -1 for failure
        }

        // Find contiguous virtual pages
        int startVirtualPage = -1;
        consecutiveFreeCount = 0;
        for (int i = 0; i < currentProcess.pageTable.length; i++) {
            if (currentProcess.pageTable[i] == -1) {
                consecutiveFreeCount++;
                if (consecutiveFreeCount == numberOfPages) {
                    startVirtualPage = i - numberOfPages + 1; // Found enough pages
                    break;
                }
            } else {
                consecutiveFreeCount = 0; // reset count
            }
        }

        // check if virtual pages were found
        if (startVirtualPage == -1) {
            System.out.println("Kernel.AllocateMemory: Allocate Memory Failed. Not enough contiguous address space for PID " + currentProcess.pid);
            return -1;
        }

        // Map the virtual pages to the physical pages
        System.out.println("Kernel.AllocateMemory: Mapping virtual pages " + startVirtualPage + ".." + (startVirtualPage + numberOfPages - 1) +
                " -> physical pages " + startPhysicalPage + ".." + (startPhysicalPage + numberOfPages - 1) + " for PID " + currentProcess.pid);
        for (int i = 0; i < numberOfPages; i++) {
            int currentVirtualPage = startVirtualPage + i;
            int currentPhysicalPage = startPhysicalPage + i;

            currentProcess.pageTable[currentVirtualPage] = currentPhysicalPage; // Map virtual to physical
            freeSpace[currentPhysicalPage] = false; // Mark as allocated
        }

        int startVirtualAddress = startVirtualPage * PAGE_SIZE;
        System.out.println("Kernel.AllocateMemory: Allocated " + sizeInBytes + " bytes succesfully. Starting Virtual Address: " + startVirtualAddress);
        return startVirtualAddress;
    }

    // Frees a block of memory beginning at the virtual address pointer.
    // Unmaps the corresponding virtual pages in the process's page table
    // Marks the corresponding physical pages as free.
    private boolean FreeMemory(int pointer, int sizeInBytes) {

        if (sizeInBytes <= 0 || sizeInBytes % PAGE_SIZE != 0) { // Validate size
            System.err.println("Kernel.FreeMemory Error: Invalid size " + sizeInBytes + ". Must be a positive multiple of " + PAGE_SIZE);
            return false; // Failure
        }

        if (pointer < 0 || pointer % PAGE_SIZE != 0) { // Validate pointer
            System.err.println("Kernel.FreeMemory Error: Invalid pointer " + pointer);
            return false;
        }
        int numberOfPages = sizeInBytes / PAGE_SIZE;
        int startVirtualPage = pointer / PAGE_SIZE;
        int endVirtualPage = startVirtualPage + numberOfPages - 1; // Calculate last virtual page index

        PCB currentProcess = scheduler.runningProcess;
        if (currentProcess == null) {
            System.err.println("Kernel.FreeMemory Error: No running process.");
            return false;
        }

        if (startVirtualPage < 0 || endVirtualPage >= currentProcess.pageTable.length) {
            System.out.println("Kernel.FreeMemory: Error: Virtual page range out of bounds " + startVirtualPage + ".." + (endVirtualPage));
            return false;
        }

        System.out.println("Kernel.FreeMemory: Attempting to free virtual pages " + startVirtualPage + ".." + endVirtualPage + " for PID " + currentProcess.pid);

        for (int i = startVirtualPage; i <= endVirtualPage; i++) {
            int currentPhysicalPage = currentProcess.pageTable[i];

            // Check to see if page was mapped before freeing.
            if (currentPhysicalPage != -1) {
                // Check if current page index is valid
                if (currentPhysicalPage >= 0 && currentPhysicalPage < freeSpace.length) {
                    freeSpace[currentPhysicalPage] = true; // mark as free
                    System.out.println("Kernel.FreeMemory: Freed physical page " + currentPhysicalPage + " for PID " + currentProcess.pid);
                } else {
                    System.out.println("Kernel.FreeMemory: WARNING: Virtual page " + i + " was mapped to invalid physical page " + currentPhysicalPage + " for PID " + currentProcess.pid);
                }
            } else {
                System.out.println("Kernel.FreeMemory: Virtual page " + i + " was already free for PID " + currentProcess.pid);
            }
            currentProcess.pageTable[i] = -1;
        }
        System.out.println("Kernel.FreeMemory: Completed freeing request for PID " + currentProcess.pid);
        return true;
    }

    // Free all memory associated with a process. Clearing TLB happens in Scheduler.switchProcess()
    private void FreeAllMemory(PCB currentlyRunning) {
        System.out.println("Kernel.FreeAllMemory: Freeing all memory for PID " + currentlyRunning.pid);
        for (int i = 0; i < currentlyRunning.pageTable.length; i++) {
            int currentPhysicalPage = currentlyRunning.pageTable[i];
            if (currentPhysicalPage != -1) {
                currentlyRunning.pageTable[i] = -1;
                freeSpace[currentPhysicalPage] = true;
            }
        }
        System.out.println("Kernel.FreeAllMemory: Finished freeing memory for PID " + currentlyRunning.pid);
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
        if (current == null) throw new InterruptedException();

        // If there is no message then put process into waiting map and switch process. When a message is sent, process
        // will be awoken.
        if (current.messageQueue.isEmpty()) {
            // Add current process to the waiting map if it's not already waiting
            waitingForMessage.put(current.pid, current);
            current.waitingForMessage = true;
            System.out.println("Kernel.WaitForMessage: Process " + current.userlandProcess.getClass().getSimpleName() + " is now waiting for a message.");
            scheduler.switchProcess();
            return null;

        } else {
            // If there is a message return it
            System.out.println("Kernel.WaitForMessage: Process " + current.userlandProcess.getClass().getSimpleName() + " is returning a message.");
            current.waitingForMessage = false;
            return current.messageQueue.removeFirst();
        }
    }

    // Helper method to find a process by its name
    private int GetPidByName(String name) {
        return scheduler.getPidByName(name);
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

    // Marks the current process as exited, removes it from the scheduler, frees its memory and switches to the next process.
    // Clearing TLB happens in Scheduler.switchProcess()
    private void Exit() throws InterruptedException {
        if (scheduler.runningProcess != null) {
            PCB exitingProcess = scheduler.runningProcess;
            exitingProcess.exit(); // Mark as exited.
            exitingProcess.messageQueue.clear();
            scheduler.removeProcess(exitingProcess);
            FreeAllMemory(exitingProcess);
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
