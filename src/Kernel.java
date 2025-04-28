import java.util.*;

public class Kernel extends Process implements Device {

    // The scheduler manages process switching and scheduling.
    private final Scheduler scheduler = new Scheduler();
    // The VFS (Virtual File System) handles device operations (e.g., file I/O) and routing of device calls.
    private final VFS vfs = new VFS();

    public HashMap<Integer, PCB> waitingForMessage = new HashMap<>();

    public boolean[] freeSpace = new boolean[1024]; // array of booleans to track which pages are in use.
    private static final int PAGE_SIZE = 1024; // Define page size constant

    // VFS file descriptor for the opened swap file. It is initialized to -1 to indicate the file is not open yet
    private int swapFileId = -1;

    /*
     * Tracks next available page index in the swap file.
     * Each time a page is swapped out it is written to this disk page number and this counter is incremented.
     * Swap space is not reused.
     */
    private int nextSwapPageNumber = 0;

    private final Random random = new Random();

    // Constructor for Kernel, initializes memory free space to true.
    public Kernel() {
        System.out.println("Kernel: Kernel constructor called");
        // Initialize freeSpace, true means free
        Arrays.fill(freeSpace, true);
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

    ////////////////////////////// Memory ///////////////////////////////////////

    /*
     * Open swap file using VFS and FakeFileSystem
     * Stores the file descriptor ID and is called during OS.Startup.
     * Uses single VFS/FFS instance
     * Requires name for the swap file to be passed in
     * Returns true if the file was opened successfully and false otherwise.
     */
    boolean openSwapFile(String filename) {
        // Use existing VFS instance
        // Prefix with "file" for FakeFileSystem as per my implementation
        swapFileId = vfs.open("file " + filename);

        if (swapFileId < 0) {
            System.err.println("Kernel.openSwapFile: Could not open swap file " + filename);
            return false;
        }
        System.out.println("Kernel.openSwapFile: Opened swap file " + filename + " with VFS ID " + swapFileId);
        this.nextSwapPageNumber = 0; // Initialize counter for disk page
        return true;
    }

    /*
     * Handles mapping of a virtual to physical page, including page faults
     * Called by Hardware when there is a TLB miss
     * Finds VirtualToPhysical Mapping for a given virtual page
     * If mapping doesn't exist (null) that is a seg fault
     * If mapping exists and page is valid (!= -1), update TLB and return
     * If mapping exists but physical page is invalid (-1) that is a Page Fault:
     * Find physical data frame, if no free frame perform page swap to get one
     * Assign physical frame number to the mapping
     * Load data from swap file or zero fill the physical frame
     * Then update TLB with new mapping
     */
    private void GetMapping(int virtualPageNum) throws InterruptedException {
        // Look up the value inside the currently running processes page table and return it
        PCB currentProcess = scheduler.runningProcess;

        if (currentProcess == null) {
            // Handle error: No running process? Should not happen if called correctly.
            System.out.println("Kernel.GetMapping Error: No running process!");
            return;
        }

        // Get current process page table
        VirtualToPhysicalMapping[] pageTable = currentProcess.pageTable;

        // Check bounds for virtualPageNum
        if (virtualPageNum < 0 || virtualPageNum >= pageTable.length) {
            System.out.println("Kernel.GetMapping: Seg fault. Invalid page number: " + virtualPageNum);
            Exit();
            return;
        }

        VirtualToPhysicalMapping mapping = pageTable[virtualPageNum];

        // if there is no mapping that is a seg fault
        if (mapping == null) {
            System.out.println("Kernel.GetMapping: Seg fault: Unmapped page " + virtualPageNum + " for PID: " + currentProcess.pid);
            Exit(); // Will exit current process
            return;
        }


        // If mapping exists and page is valid (!= -1), can update TLB and return
        if (mapping.physicalPageNumber != -1) {
            System.out.println("Kernel.GetMapping: Page is already in memory for PID " + currentProcess.pid +
                    " Virtual page " + virtualPageNum + " Physical page " + mapping.physicalPageNumber);
            int randomIndex = (int) (Math.random() * 2); // for 0 or 1
            Hardware.TLB[randomIndex][0] = virtualPageNum;
            Hardware.TLB[randomIndex][1] = mapping.physicalPageNumber;

            System.out.println("Kernel.GetMapping: Updated TLB[" + randomIndex + "]");
            return;
        }

        // If mapping exists and physical page mapping is invalid (-1) that is a page fault
        // First, attempt to get free physical page
        int freePhysicalPage = findFreePhysicalPage();

        // If unable to get free physical page, then perform a page swap
        if (freePhysicalPage == -1) {
            System.out.println("Kernel.GetMapping: No free page found, calling performPageSwap...");
            freePhysicalPage = performPageSwap();
            if (freePhysicalPage == -1) {
                // Page swap was unsuccessful
                System.err.println("Kernel.GetMapping: ERROR: Page swap failed for PID "
                        + currentProcess.pid + " Virtual page " + virtualPageNum + ". Terminating process.");
                Exit();
                return;
            }
        }

        // Assign physical frame to the mapping
        mapping.physicalPageNumber = freePhysicalPage;
        System.out.println("Kernel.GetMapping: Assigned physical page " + freePhysicalPage + " to virtual page " + virtualPageNum + " for PID " + currentProcess.pid);

        // Load data into the frame
        if (mapping.diskPageNumber != -1) { // This page was previously swapped out
            // Load from swap file
            System.out.println("Kernel.GetMapping: Loading virtual page " + virtualPageNum + " from swap slot " +
                    mapping.diskPageNumber + " into physical page " + freePhysicalPage);

            // Calculate byte offset within the swap file where this page's data starts.
            // mapping.diskPageNumber is like the page index within the swap file.
            long diskOffset = (long)mapping.diskPageNumber * PAGE_SIZE;

            // Position vfs's pointer for the swap file to the correct location
            vfs.seek(swapFileId, (int) diskOffset);

            // Read one page of data from the swap file beginning at the offset
            byte[] pageData = vfs.read(swapFileId, PAGE_SIZE);

            // Error handling
            if (pageData == null) {
                System.err.println("Kernel.GetMapping: ERROR: Failed to read full page from swap file for disk page "
                        + mapping.diskPageNumber + ". Filling with zeroes instead.");
                // Rather than crashing return all zeroes for the page data
                pageData = new byte[PAGE_SIZE]; // This defaults to all zeroes.
            }

            // Calculating start physical address
            int physicalAddressStart = freePhysicalPage * PAGE_SIZE;

            // Copy data from pageData buffer into actual physical memory
            try {
                if (physicalAddressStart >= 0 && (physicalAddressStart + PAGE_SIZE) <= Hardware.PhysicalMemory.length) {
                    System.arraycopy(pageData, 0, Hardware.PhysicalMemory, physicalAddressStart, PAGE_SIZE);
                } else {
                    System.err.println("Kernel.GetMapping: ERROR: Invalid physical address " + freePhysicalPage + " during swap in.");
                }
            } catch (ArrayIndexOutOfBoundsException e) {
                System.err.println("Kernel.GetMapping: ERROR: Array index out of bounds during physical memory copy for swap in Physical page " + freePhysicalPage);
                e.printStackTrace();
            }
        } else {
            // When mapping.diskPageNumber = -1 (page was never swapped out)
            // This case occurs during the first access to a page that was lazy allocated

            // Must provide a zero filled page back to the process
            System.err.println("Kernel.GetMapping: Zero-filling physical page " + freePhysicalPage +
                    " for virtual page " + virtualPageNum + ". First access or no disk backing.");
            // Creating a zero-filled buffer
            byte[] zeroPage = new byte[PAGE_SIZE];

            // Calculate starting Physical address
            int startPhysicalAddress = freePhysicalPage * PAGE_SIZE;

            // Copy zeroes into physical memory frame
            try {
                if (startPhysicalAddress >= 0 && (startPhysicalAddress + PAGE_SIZE) <= Hardware.PhysicalMemory.length) {
                    System.arraycopy(zeroPage, 0, Hardware.PhysicalMemory, startPhysicalAddress, PAGE_SIZE);
                }
            } catch (ArrayIndexOutOfBoundsException e) {
                System.err.println("Kernel.GetMapping: ERROR: Array index out of bounds during physical memory copy for zero fill in Physical page " + freePhysicalPage);
                e.printStackTrace();
            }
        }

        // Update TLB with new mapping
        System.out.println("Kernel.GetMapping: Page Fault handled. Updating TLB for virtual page " + virtualPageNum + " to physical page " + freePhysicalPage);
        int randomIndex = random.nextInt(Hardware.TLB.length);
        Hardware.TLB[randomIndex][0] = virtualPageNum;
        Hardware.TLB[randomIndex][1] = freePhysicalPage;
        System.out.println("Kernel.GetMapping: Updated TLB[" + randomIndex + "]");
    }

    // Finds index of first available page frame and marks it as used
    // Returns index of physical page from freeSpace
    private int findFreePhysicalPage() {
        for (int p = 0; p < freeSpace.length; p++) {
            if (freeSpace[p]) {
                freeSpace[p] = false; // Mark as used
                System.out.println("Kernel.findFreePhysicalPage: Found free physical page " + p);
                return p;
            }
        }
        System.out.println("Kernel.findFreePhysicalPage: No free physical page found");
        return -1;
    }

    // Handles case where no physical page is available, and we need to swap out a page from a random victim process
    // Writes the victim page to the swap file and updates its page table entry
    // Returns index to the page frame that was freed or -1 if error
    private int performPageSwap() {
        System.out.println("Kernel.performPageSwap: No free physical pages. Starting page swap");
        if (swapFileId < 0) {
            System.err.println("Kernel.performPageSwap: ERROR: Swap file not open.");
            return -1;
        }

        while (true) {
            PCB victimProcess = scheduler.getRandomProcess();
            if (victimProcess == null) {
                System.err.println("Kernel.performPageSwap: ERROR: No process available");
                return -1;
            }

            // Find page in victim process that is currently in physical memory
            VirtualToPhysicalMapping victimMapping = null;
            int victimVirtualPage = -1;
            int victimPhysicalPage = -1;

            for (int i = 0; i < victimProcess.pageTable.length; i++) {
                VirtualToPhysicalMapping mapping = victimProcess.pageTable[i];
                if (mapping != null && mapping.physicalPageNumber != -1) {
                    victimMapping = mapping;
                    victimVirtualPage = i;
                    victimPhysicalPage = mapping.physicalPageNumber;
                    System.out.println("Kernel.performPageSwap: Found victim page: PID " +
                            victimProcess.pid + " virtual page: " + victimVirtualPage + " physical page: " + victimPhysicalPage);
                    break; // Found page to swap
                }
            }
            if (victimMapping == null) {
                System.out.println("Kernel.performPageSwap: PID " + victimProcess.pid + " had no pages in physical " +
                        "memory. Trying another victim.");
                continue;
            }

            // Page is found so proceed with swap
            // Assign disk location
            if (victimMapping.diskPageNumber == -1) {
                victimMapping.diskPageNumber = nextSwapPageNumber++;
                System.out.println("Kernel.performPageSwap: Assigning swap slot " + victimMapping.diskPageNumber +
                        " for PID " + victimProcess.pid + " virtual page " + victimVirtualPage);
            }

            // Prepare the data buffer
            byte[] pageData = new byte[PAGE_SIZE];
            int physicalAddressStart = victimPhysicalPage * PAGE_SIZE;
            try {
                // Check bounds before copying
                if (physicalAddressStart >= 0 && (physicalAddressStart + PAGE_SIZE) <= Hardware.PhysicalMemory.length) {
                    System.arraycopy(Hardware.PhysicalMemory, physicalAddressStart, pageData, 0, PAGE_SIZE);
                    System.out.println("Kernel.performPageSwap: Copied data from physical page " + victimPhysicalPage);
                } else {
                    System.err.println("Kernel.performPageSwap ERROR: Invalid physical address calculation for victim P" + victimPhysicalPage);
                    continue; // Try another victim
                }
            } catch (ArrayIndexOutOfBoundsException e) {
                System.err.println("Kernel.performPageSwap ERROR: Array index out of bounds during physical memory copy for P" + victimPhysicalPage);
                e.printStackTrace();
                continue; // Try another victim
            }

            // Write the data to the swap file
            long diskOffset = (long)victimMapping.diskPageNumber * PAGE_SIZE;
            System.out.println("Kernel.performPageSwap: Seeking swap file to offset " + diskOffset);
            vfs.seek(swapFileId, (int) diskOffset);
            int bytesWritten = vfs.write(swapFileId, pageData);
            if (bytesWritten != PAGE_SIZE) {
                System.err.println("Kernel.performPageSwap ERROR: Failed to write complete page to swap file. Instead wrote " + bytesWritten);
                continue; // Find another victim...
            }
            System.out.println("Kernel.performPageSwap: Wrote page data for PID " + victimProcess.pid + " V" + victimVirtualPage + " to swap slot " + victimMapping.diskPageNumber);

            // Update victim mapping
            victimMapping.physicalPageNumber = -1;

            // Return the newly free physical page number
            System.out.println("Kernel.performPageSwap: Succesfully swapped out PID " + victimProcess.pid +
                    " Virtual page" + victimVirtualPage + " Physical page " + victimPhysicalPage + " is now free.");
            return victimPhysicalPage;
        }

    }

    /*
     * Allocates a contiguous block of virtual memory for the current process using lazy allocation
     * VirtualToPhysical objects are created and placed in page table for the requested virtual pages
     * Physical pages are not allocated here
     * Physical pages will be allocated on demand by GetMapping during the first access (page fault).
     */
    private int AllocateMemory(int sizeInBytes) {
        // Validate size and get process
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

        // Find contiguous virtual pages
        // Search for a contiguous block of 'null' entries in the page table,
        // 'Null' means unallocated virtual page
        int startVirtualPage = -1;
        int consecutiveNullCount = 0;
        for (int v = 0; v < currentProcess.pageTable.length; v++) {
            if (currentProcess.pageTable[v] == null) { // Check if virtual page slot is unallocated
                consecutiveNullCount++;
                if (consecutiveNullCount == numberOfPages) {
                    startVirtualPage = v - numberOfPages + 1; // Found enough pages this is start of the block
                    break;
                }
            } else {
                consecutiveNullCount = 0; // reset count
            }
        }

        // check if consecutive virtual pages were found
        if (startVirtualPage == -1) {
            System.out.println("Kernel.AllocateMemory: Allocate Memory Failed. Not enough contiguous address space for PID " + currentProcess.pid);
            return -1; // Return failure
        }
        System.out.println("Kernel.AllocateMemory: Found contiguous virtual pages starting at " + startVirtualPage + " for PID " + currentProcess.pid);

        // Allocate virtually by creating VirtualToPhysical mapping with no physical page mapping for now
        // Physical pages will be assigned later by OS.GetMapping on first access (page fault)
        System.out.println("Kernel.AllocateMemory: Creating virtual mappings for PID " + currentProcess.pid);
        for (int i = 0; i < numberOfPages; i++) {
            int currentVirtualPage = startVirtualPage + i;
            // Create new VirtualToPhysicalMapping which defaults to -1 for physical and disk page
            VirtualToPhysicalMapping newMapping = new VirtualToPhysicalMapping();
            currentProcess.pageTable[currentVirtualPage] = newMapping; // Mapping object placed in page table slot
            System.out.println("Kernel.AllocateMemory: Virtual Page " + currentVirtualPage + " allocated for PID " + currentProcess.pid);
        }
        System.out.println();

        // Returning start virtual address
        int startVirtualAddress = startVirtualPage * PAGE_SIZE;
        System.out.println("Kernel.AllocateMemory: Allocated " + sizeInBytes + " bytes successfully. Starting Virtual Address: " + startVirtualAddress);
        return startVirtualAddress;
    }

    // Frees a block of memory beginning at the virtual address pointer.
    // Unmaps the corresponding virtual pages in the process's page table
    // Marks the corresponding physical pages as free.
    private boolean FreeMemory(int pointer, int sizeInBytes) {

        if (sizeInBytes <= 0 || sizeInBytes % PAGE_SIZE != 0) { // Validate size is not less than zero or not a multiple of page size.
            System.err.println("Kernel.FreeMemory Error: Invalid size " + sizeInBytes + ". Must be a positive multiple of " + PAGE_SIZE);
            return false; // Failure
        }

        if (pointer < 0 || pointer % PAGE_SIZE != 0) { // Validate pointer is not less than zero or not a multiple of page size
            System.err.println("Kernel.FreeMemory Error: Invalid pointer " + pointer);
            return false;
        }
        int numberOfPages = sizeInBytes / PAGE_SIZE; // Calculate number of pages
        int startVirtualPage = pointer / PAGE_SIZE; // Calculate start virtual page
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
            int currentVirtualPage = i;
            VirtualToPhysicalMapping mapping = currentProcess.pageTable[currentVirtualPage];

            // Check to see if mapping exists
            if (mapping != null) {
                // check to see if the page is in physical memory
                int physicalPage = mapping.physicalPageNumber;
                if (physicalPage != -1) {
                    // Ensure physical page index is valid before using it
                    if (physicalPage >= 0 && physicalPage <= freeSpace.length) {
                        freeSpace[physicalPage] = true; // Mark physical page as free
                        System.out.println("Kernel.FreeMemory: Freed physical page " + physicalPage + " for PID " + currentProcess.pid);
                        // Invalidate TLB entry
                        invalidateTLBEntry(currentVirtualPage);
                    } else {
                        // Page table had an invalid physical page number
                        System.err.println("Kernel.FreeMemory ERROR: Virtual page " + currentVirtualPage + " mapped to invalid physical page " + physicalPage);
                    }
                } else {
                    // Page was allocated virtually but is not in physical RAM
                    System.out.println("Kernel.FreeMemory: Virtual page " + currentVirtualPage + " was mapped virutally but was not in physical memory.");
                }

                // Remove mapping from page table. Virtual page will no longer be allocated to the process
                currentProcess.pageTable[currentVirtualPage] = null;
            } else {
                // Virtual page in the range was not allocated in the first place
                System.out.println("Kernel.FreeMemory: Virtual page " + currentVirtualPage + " was not mapped.");
                // Ensure null
                currentProcess.pageTable[currentVirtualPage] = null;
            }
        }
        System.out.println("Kernel.FreeMemory: Completed freeing request for PID " + currentProcess.pid);
        return true;
    }

    // Helper method to invalidate TLB entry for the given virtual page
    private void invalidateTLBEntry(int virtualPageNum) {
        for (int j = 0; j < Hardware.TLB.length; j++) {
            if (Hardware.TLB[j][0] == virtualPageNum) {
                Hardware.TLB[j][0] = Hardware.INVALID_PAGE; // invalidate the entry for the current virtual page
                Hardware.TLB[j][1] = Hardware.INVALID_PAGE; // physical page
                System.out.println("Kernel: Invalidated TLB entry for virtual page " + virtualPageNum);
            }
        }

    }

    // Free all memory associated with a process. Clearing TLB also happens in Scheduler.switchProcess()
    private void FreeAllMemory(PCB currentlyRunning) {
        System.out.println("Kernel.FreeAllMemory: Freeing all memory for PID " + currentlyRunning.pid);
        // Marks all page table entries as free. Marks all physical pages as not in use.
        // Invalidates TLB entries for any virtual pages that are associated with the current process
        for (int i = 0; i < currentlyRunning.pageTable.length; i++) {
            VirtualToPhysicalMapping mapping = currentlyRunning.pageTable[i];
            if (mapping != null) {
                int physicalPage = mapping.physicalPageNumber;
                if (physicalPage != -1) {
                    if (physicalPage >= 0 && physicalPage <= freeSpace.length) {
                        if (!freeSpace[physicalPage]) {
                            freeSpace[physicalPage] = true; // Mark physical page as free
                        }
                    } else {
                        // Physical page number was invalid
                        System.out.println("Kernel.FreeAllMemory: WARNING: Invalid physical page: " + physicalPage + " for PID " + currentlyRunning + " for virtual page " + i);
                    }
                    // Invalidate TLB
                    invalidateTLBEntry(i);
                }
                // Remove mapping
                currentlyRunning.pageTable[i] = null;
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
