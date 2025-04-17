public class MemoryTestInitProcess extends UserlandProcess {
    @Override
    public void main() throws InterruptedException {
        System.out.println("\nMemoryTestInitProcess: Starting Memory Tests...");

        // Create two memory test processes with different identifiers
        System.out.println("MemoryTestInitProcess: Creating Process A.");
        OS.CreateProcess(new MemoryTestProcess((byte)'A'), OS.PriorityType.interactive);
        cooperate();

        System.out.println("MemoryTestInitProcess: Creating Process B.");
        OS.CreateProcess(new MemoryTestProcess((byte)'B'), OS.PriorityType.interactive);
        cooperate();

        System.out.println("MemoryTestInitProcess: Test processes created. InitProcess will now exit.");
        System.out.println("MemoryTestInitProcess: Exiting.");
        OS.Exit();
    }
}