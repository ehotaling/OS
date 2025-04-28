public class PiggyProcess extends UserlandProcess {

    public static final int VIRTUAL_PAGES = 100;
    private static final int PAGE_SIZE = 1024;
    private static final int ALLOC_SIZE = VIRTUAL_PAGES * PAGE_SIZE;

    @Override
    public void main() throws InterruptedException {
        int myPid = OS.GetPID(); // Get PID for unique identifier
        byte identifierByte = (byte) (myPid % 256); // Use PID as identifier
        System.out.println("PiggyProcess [" + myPid + "]: Starting, using identifier " + identifierByte);

        // Allocate maximum virtual memory
        System.out.println("PiggyProcess [" + myPid + "]: Allocating " + ALLOC_SIZE + " bytes...");
        int baseAddress = OS.AllocateMemory(ALLOC_SIZE);
        if (baseAddress == -1) {
            System.err.println("PiggyProcess [" + myPid + "]: FAILED allocation!");
            OS.Exit();
            return;
        }
        System.out.println("PiggyProcess [" + myPid + "]: Allocated memory starting at VA " + baseAddress);

        // Touch every page by writing to it and force page faults
        System.out.println("PiggyProcess [" + myPid + "]: Writing identifier to all " + VIRTUAL_PAGES + " pages...");
        for (int i = 0; i < VIRTUAL_PAGES; i++) {
            int address = baseAddress + i * PAGE_SIZE;
            try {
                Hardware.Write(address, identifierByte);
            } catch (Exception e) {
                System.err.println("PiggyProcess [" + myPid + "]: ERROR writing to VPage " + i + " (VA " + address + "): " + e.getMessage());
                OS.Exit(); return; // Exit on error
            }
            if (i % 10 == 0) { // Cooperate periodically
                cooperate();
                // System.out.print("."); // Indicate progress
            }
        }
        System.out.println("\nPiggyProcess [" + myPid + "]: Finished writing to all pages.");
        cooperate();


        // 3. Verify data by reading back from each page
        System.out.println("PiggyProcess [" + myPid + "]: Verifying data in all " + VIRTUAL_PAGES + " pages...");
        boolean success = true;
        for (int i = 0; i < VIRTUAL_PAGES; i++) {
            int address = baseAddress + i * PAGE_SIZE;
            try {
                byte valueRead = Hardware.Read(address);
                if (valueRead != identifierByte) {
                    System.err.println("\nPiggyProcess [" + myPid + "]: FAILED verification at VPage " + i + " (VA " + address + ")! Expected " + identifierByte + ", got " + valueRead);
                    success = false;
                }


            } catch (Exception e) {
                System.err.println("\nPiggyProcess [" + myPid + "]: ERROR reading from VPage " + i + " (VA " + address + "): " + e.getMessage());
                success = false;
                OS.Exit(); return; // Exit on error
            }
            if (i % 10 == 0) { // Cooperate periodically
                cooperate();
                // System.out.print("*"); // Indicate progress
            }
        }

        if (success) {
            System.out.println("\nPiggyProcess [" + myPid + "]: SUCCESS! All pages verified correctly.");
        } else {
            System.err.println("\nPiggyProcess [" + myPid + "]: Verification FAILED for one or more pages.");
        }

        // Sleep for a while and hold onto the memory.
        OS.Sleep(500);
        // Exit cleanly
        System.out.println("PiggyProcess [" + myPid + "]: Exiting.");
        OS.Exit();
    }
}