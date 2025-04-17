import java.util.Random;
/**
 * MemoryTestProcess tests the functionality
 * of the simulated paging system, including memory allocation, read/write
 * operations via the Hardware class, memory freeing, and segmentation
 * fault handling upon invalid access.
 */
public class MemoryTestProcess extends UserlandProcess {

    // Unique identifier for this process instance. Used for writing/verifying memory content.
    private byte identifier;
    // Stores the starting virtual address returned by the first AllocateMemory call.
    private int baseAddress1 = -1;
    // Stores the starting virtual address returned by the second AllocateMemory call.
    private int baseAddress2 = -1;
    // Size for the first memory allocation (2 pages = 2 * 1024 bytes).
    private static final int ALLOC_SIZE_1 = 2048; // 2 pages
    // Size for the second memory allocation (1 page = 1 * 1024 bytes).
    private static final int ALLOC_SIZE_2 = 1024; // 1 page
    // Page size as defined in the assignment (1KB).
    private static final int PAGE_SIZE = 1024;
    // Random number generator for selecting which process attempts a segfault test.
    private Random random = new Random();


    public MemoryTestProcess(byte id) {
        this.identifier = id;
        System.out.println("MemoryTestProcess [" + (char) identifier + "]: Created.");
    }

    @Override
    public void main() throws InterruptedException {
        System.out.println("MemoryTestProcess [" + (char) identifier + "]: Starting main loop.");

        // --- Test 1: Initial Allocation ---
        // Tests the OS/Kernel call: int AllocateMemory(int size)
        // Verifies that the kernel can find and map contiguous physical pages
        // into the process's virtual address space and return the starting virtual address.
        System.out.println("MemoryTestProcess [" + (char) identifier + "]: Attempting to allocate " + ALLOC_SIZE_1 + " bytes.");
        baseAddress1 = OS.AllocateMemory(ALLOC_SIZE_1); // System call
        if (baseAddress1 == -1) {
            System.err.println("MemoryTestProcess [" + (char) identifier + "]: FAILED initial allocation.");
            OS.Exit(); // Exit if allocation fails
            return;
        }
        System.out.println("MemoryTestProcess [" + (char) identifier + "]: Allocated block 1 at virtual address " + baseAddress1);

        // --- Test 2: Write to allocated memory (block 1) ---
        // Tests the Hardware.Write(int address, byte value) method, which simulates
        // a STORE instruction. This implicitly tests the virtual-to-physical address
        // translation, TLB lookup/miss handling, and OS.GetMapping kernel call upon TLB miss.
        System.out.println("MemoryTestProcess [" + (char) identifier + "]: Writing identifier to block 1...");
        try {
            // Write to different locations within the allocated block
            Hardware.Write(baseAddress1, identifier); // Write at the start (offset 0)
            Hardware.Write(baseAddress1 + PAGE_SIZE, identifier); // Write at the start of the second page (offset 1024)
            Hardware.Write(baseAddress1 + ALLOC_SIZE_1 - 1, identifier); // Write at the very end (offset 2047)
        } catch (Exception e) {
            // Catch potential exceptions during the hardware simulation (though segfaults should ideally kill the process directly)
            System.err.println("MemoryTestProcess [" + (char) identifier + "]: ERROR during write to block 1: " + e.getMessage());
            e.printStackTrace();
            OS.Exit();
            return;
        }
        System.out.println("MemoryTestProcess [" + (char) identifier + "]: Finished writing to block 1.");
        cooperate(); // Yield CPU to allow other processes to run, testing scheduling.

        // --- Test 3: Read from allocated memory (block 1) and verify ---
        // Tests the Hardware.Read(int address) method, simulating a LOAD instruction.
        // Again, implicitly tests address translation, TLB, and GetMapping.
        // Verifies that data written previously is read back correctly, showing memory integrity
        // for this process and implicitly testing non-interference between processes (if others run).
        System.out.println("MemoryTestProcess [" + (char) identifier + "]: Reading identifier from block 1...");
        try {
            byte val1 = Hardware.Read(baseAddress1);
            byte val2 = Hardware.Read(baseAddress1 + PAGE_SIZE);
            byte val3 = Hardware.Read(baseAddress1 + ALLOC_SIZE_1 - 1);

            // Verify data integrity
            if (val1 == identifier && val2 == identifier && val3 == identifier) {
                System.out.println("MemoryTestProcess [" + (char) identifier + "]: SUCCESS reading back correct data from block 1.");
            } else {
                System.err.println("MemoryTestProcess [" + (char) identifier + "]: FAILED reading back data from block 1! Read: " + val1 + "," + val2 + "," + val3);
            }
        } catch (Exception e) {
            System.err.println("MemoryTestProcess [" + (char) identifier + "]: ERROR during read from block 1: " + e.getMessage());
            e.printStackTrace();
            OS.Exit();
            return;
        }
        cooperate(); // Yield CPU

        // --- Test 4: Extend Memory (Allocate second block) ---
        // Tests the ability to call AllocateMemory multiple times within the same process,
        // potentially allocating non-contiguous virtual memory blocks mapped to
        // available physical pages. Addresses fragmentation handling.
        System.out.println("MemoryTestProcess [" + (char) identifier + "]: Attempting to allocate second block of " + ALLOC_SIZE_2 + " bytes.");
        baseAddress2 = OS.AllocateMemory(ALLOC_SIZE_2); // Second system call
        if (baseAddress2 == -1) {
            System.err.println("MemoryTestProcess [" + (char) identifier + "]: FAILED second allocation.");
            // Allow process to continue to potentially test segfaults even if second allocation fails
        } else {
            System.out.println("MemoryTestProcess [" + (char) identifier + "]: Allocated block 2 at virtual address " + baseAddress2);

            // --- Test 5: Write/Read for second block ---
            // Further tests Hardware.Read/Write on the newly allocated second block.
            System.out.println("MemoryTestProcess [" + (char) identifier + "]: Writing/Reading identifier for block 2...");
            try {
                Hardware.Write(baseAddress2, identifier); // Write at start of block 2
                Hardware.Write(baseAddress2 + ALLOC_SIZE_2 - 1, identifier); // Write at end of block 2
                byte val4 = Hardware.Read(baseAddress2);
                byte val5 = Hardware.Read(baseAddress2 + ALLOC_SIZE_2 - 1);
                // Verify data integrity for the second block
                if (val4 == identifier && val5 == identifier) {
                    System.out.println("MemoryTestProcess [" + (char) identifier + "]: SUCCESS writing/reading back correct data from block 2.");
                } else {
                    System.err.println("MemoryTestProcess [" + (char) identifier + "]: FAILED writing/reading back data from block 2! Read: " + val4 + "," + val5);
                }
            } catch (Exception e) {
                System.err.println("MemoryTestProcess [" + (char) identifier + "]: ERROR during R/W for block 2: " + e.getMessage());
                e.printStackTrace();
                OS.Exit();
                return;
            }
        }
        cooperate(); // Yield CPU

        // --- Test 6: Free Memory ---
        // Tests the OS/Kernel call: boolean FreeMemory(int pointer, int size)
        // Verifies that the kernel can unmap virtual pages and mark the corresponding
        // physical pages as available in its free list (`freeSpace` array).
        System.out.println("MemoryTestProcess [" + (char) identifier + "]: Attempting to free block 1...");
        // Check if baseAddress1 is valid before trying to free
        if (baseAddress1 != -1) {
            boolean freed1 = OS.FreeMemory(baseAddress1, ALLOC_SIZE_1); // System call
            System.out.println("MemoryTestProcess [" + (char) identifier + "]: Free block 1 result: " + freed1);
        } else {
            System.out.println("MemoryTestProcess [" + (char) identifier + "]: Skipping free for block 1 (was not allocated).");
        }

        // Check if baseAddress2 is valid before trying to free
        if (baseAddress2 != -1) {
            System.out.println("MemoryTestProcess [" + (char) identifier + "]: Attempting to free block 2...");
            boolean freed2 = OS.FreeMemory(baseAddress2, ALLOC_SIZE_2); // System call
            System.out.println("MemoryTestProcess [" + (char) identifier + "]: Free block 2 result: " + freed2);
        } else {
            System.out.println("MemoryTestProcess [" + (char) identifier + "]: Skipping free for block 2 (was not allocated).");
        }
        cooperate(); // Yield CPU
        // After this point, accessing memory at baseAddress1 or baseAddress2 should cause a segfault.

        // --- Test 7: Segmentation Fault Test ---
        // Tests the requirement that accessing invalid memory causes the process to be killed.
        // This involves Hardware.Read/Write triggering OS.GetMapping, which should detect
        // either an unmapped page (if within [0..99] but freed/never allocated) or an
        // invalid page number (if >= 100) and call Kernel.Exit.

        // Randomly select one process instance to attempt a segfault test.
        boolean testSegfault = random.nextBoolean();

        if (testSegfault && ((char) identifier == 'A')) { // Let's make Process A test invalid page number
            System.out.println("\nMemoryTestProcess [" + (char) identifier + "]: === ATTEMPTING INVALID READ (EXPECT SEGFAULT - Out of Bounds) ===");
            // This virtual address corresponds to page 101, which is outside the process's
            // valid page table range [0..99] according to assignment spec (PCB page table size 100).
            int invalidAddress = 101 * PAGE_SIZE;
            System.out.println("MemoryTestProcess [" + (char) identifier + "]: Reading from invalid address " + invalidAddress);
            // This Hardware.Read call should trigger OS.GetMapping(101) -> Kernel detects invalid page -> Kernel.Exit
            byte invalidData = Hardware.Read(invalidAddress);
            // Execution should NOT reach here if segfault handling is correct.
            System.err.println("MemoryTestProcess [" + (char) identifier + "]: !!! SEGFAULT TEST FAILED - Read completed with value: " + invalidData + " !!!");
            System.err.println("MemoryTestProcess [" + (char) identifier + "]: !!! REACHED CODE AFTER INVALID READ !!!");

        } else if (testSegfault && ((char) identifier == 'B')) { // Let's make Process B test unmapped/freed page
            System.out.println("\nMemoryTestProcess [" + (char) identifier + "]: === ATTEMPTING INVALID WRITE (EXPECT SEGFAULT - Unmapped/Freed) ===");
            // This address corresponds to virtual page 0, which was allocated and then FREED by Test 6.
            // Accessing it now should result in an "Unmapped page" segfault from GetMapping.
            // Alternatively, could use an address like 50 * PAGE_SIZE which was never allocated.
            int unmappedAddress = baseAddress1 != -1 ? baseAddress1 : 0; // Use address 0 (page 0) which should be freed.
            System.out.println("MemoryTestProcess [" + (char) identifier + "]: Writing to unmapped/freed address " + unmappedAddress);
            // This Hardware.Write call should trigger OS.GetMapping(0) -> Kernel finds pageTable[0] is -1 -> Kernel.Exit
            Hardware.Write(unmappedAddress, (byte) 'X');
            // Execution should NOT reach here if segfault handling is correct.
            System.err.println("MemoryTestProcess [" + (char) identifier + "]: !!! SEGFAULT TEST FAILED - Write completed !!!");
            // Execution should definitely NOT reach here.
            System.err.println("MemoryTestProcess [" + (char) identifier + "]: !!! REACHED CODE AFTER INVALID WRITE !!!");
        }
        // If the process wasn't killed by a segfault test, it reaches the end.
        System.out.println("MemoryTestProcess [" + (char) identifier + "]: Reached end of main loop.");
        OS.Exit();
    }
}