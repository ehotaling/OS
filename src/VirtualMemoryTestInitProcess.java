public class VirtualMemoryTestInitProcess extends UserlandProcess {

    // We have 1024 physical pages. Each PiggyProcess uses 100 virtual pages.
    // More than 10 PiggyProcesses actively touching memory should force swapping.
    private static final int NUM_PIGGIES = 10;

    @Override
    public void main() throws InterruptedException {
        System.out.println("\nVirtualMemoryTestInitProcess: Starting Virtual Memory Stress Test...");
        System.out.println("VirtualMemoryTestInitProcess: Physical Memory Pages = " + 1024); // Assuming 1MB/1KB
        System.out.println("VirtualMemoryTestInitProcess: Piggy Virtual Pages = " + PiggyProcess.VIRTUAL_PAGES);
        System.out.println("VirtualMemoryTestInitProcess: Will create " + NUM_PIGGIES + " PiggyProcesses.");

        for (int i = 0; i < NUM_PIGGIES; i++) {
            System.out.println("VirtualMemoryTestInitProcess: Creating PiggyProcess #" + (i + 1));
            int pid = OS.CreateProcess(new PiggyProcess(), OS.PriorityType.interactive);
            if (pid == -1) {
                System.err.println("VirtualMemoryTestInitProcess: Failed to create PiggyProcess #" + (i+1));
            }
            cooperate(); // Give scheduler a chance
        }

        System.out.println("VirtualMemoryTestInitProcess: All PiggyProcesses created. InitProcess exiting.");
        OS.Exit();
    }
}