public class Hardware {
    public static byte[] PhysicalMemory = new byte[1024 * 1024]; // Physical Memory
    public static int[][] TLB = new int[2][2]; // TLB cache TLB[i][0] → Virtual page number, TLB[i][1] → Physical page number
    private static final int pageSize = 1024; // 1 KB Pages
    public static final int INVALID_PAGE = -1; // Define an invalid page number

    // Runs when class is loaded, initializes TLB to invalid.
    static {
        System.out.println("Hardware: Initializing TLB...");
        for (int i = 0; i < TLB.length; i++) {
            TLB[i][0] = INVALID_PAGE; // Invalidate virtual page entry
            TLB[i][1] = INVALID_PAGE; // Invalidate physical page entry
        }
        System.out.println("Hardware: TLB Initialized.");
    }

    // Simulate LOAD instruction. Gets virtual page, gets physical page, checks TLB and returns data
    public static byte Read(int virtualAddress) throws InterruptedException {
        int virtualPage = virtualAddress / pageSize;
        int pageOffset = virtualAddress % pageSize;
        int physicalAddr = getPhysicalAddr(virtualPage, pageOffset); // Get Physical Address from TLB
        return PhysicalMemory[physicalAddr]; // Returns byte value from memory for the physical address
    }


    // Simulate STORE instruction. Gets virtual page, gets physical page, checks TLB and writes data
    public static void Write(int virtualAddress, byte value) throws InterruptedException {
        int virtualPageNum = virtualAddress / pageSize;
        int pageOffset = virtualAddress % pageSize;
        int physicalAddr = getPhysicalAddr(virtualPageNum, pageOffset);
        PhysicalMemory[physicalAddr] = value; // Writes byte value to given memory address
    }

    // Returns physical address of a virtual page
    private static int getPhysicalAddr(int virtualPageNum, int pageOffset) throws InterruptedException {
        Integer physicalPageNum = searchTLB(virtualPageNum);
        if (physicalPageNum == null) {
            OS.GetMapping(virtualPageNum);
            physicalPageNum = searchTLB(virtualPageNum);
            if (physicalPageNum == null) {
                throw new RuntimeException("Page fault: TLB miss not resolved by OS");
            }
        }
        return (physicalPageNum * pageSize) + pageOffset;

    }


    // Searches TLB cache for virtual page and returns mapped physical page if found, null if not
    private static Integer searchTLB(int virtualPageNum) {
        if (virtualPageNum < 0) return null;
        for (int i = 0; i < TLB.length; i++) {
            // check if the virtual page number in the TLB is valid and matches
            if (TLB[i][0] != INVALID_PAGE && TLB[i][0] == virtualPageNum) {
                return TLB[i][1]; // Return mapped physical page
            }
        }
        return null;
    }
}
