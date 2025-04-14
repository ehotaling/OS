public class Hardware {
    public static byte[] PhysicalMemory = new byte[1024 * 1024]; // Physical Memory
    public static int[][] TLB = new int[2][2]; // TLB cache TLB[i][0] → Virtual page number, TLB[i][1] → Physical page number
    private static int pageSize = 1024;

    // Simulate LOAD instruction. Gets virtual page, gets physical page, checks TLB and returns data
    public static byte Read(int virtualAddress) {
        int virtualPage = virtualAddress / pageSize;
        int pageOffset = virtualAddress % pageSize;
        int physicalAddr = getPhysicalAddr(virtualPage, pageOffset); // Get Physical Address from TLB
        return PhysicalMemory[physicalAddr]; // returns byte value from memory for the physical address
    }


    // Simulate STORE instruction. Gets virtual page, gets physical page, checks TLB and writes data
    public static void Write(int virtualAddress, byte value) {
        int virtualPage = virtualAddress / 1024;
        int pageOffset = virtualAddress % 1024;
        int physicalAddr = getPhysicalAddr(virtualPage, pageOffset);
        PhysicalMemory[physicalAddr] = value; // Writes byte value to given memory address
    }

    // Returns physical address of a virtual page
    private static int getPhysicalAddr(int virtualPageNum, int pageOffset) {
        Integer physicalPageNum = searchTLB(virtualPageNum);
        if (physicalPageNum == null) {
            OS.getMapping(virtualPageNum);
            physicalPageNum = searchTLB(virtualPageNum);
            if (physicalPageNum == null) {
                throw new RuntimeException("Page fault: TLB miss not resolved by OS");
            }
        }
        return (physicalPageNum * pageSize) + pageOffset;

    }

    private static Integer searchTLB(int virtualPageNum) {
        for (int i = 0; i < TLB.length; i++) {
            if (TLB[i][0] == virtualPageNum) {
                return TLB[i][1];
            }
        }
        return null;
    }
}
