public class TestInitProcess extends UserlandProcess {
    @Override
    public void main() throws InterruptedException {
        try {
            // create device test process
            System.out.println("TestInitProcess: Creating DeviceTestProcess");
            int pid1 = OS.CreateProcess(new DeviceTestProcess(), OS.PriorityType.interactive);
            System.out.println("TestInitProcess: Created DeviceTestProcess with PID " + pid1);
            // create file test process
            System.out.println("TestInitProcess: Creating FileTestProcess");
            int pid2 = OS.CreateProcess(new FileTestProcess(), OS.PriorityType.interactive);
            System.out.println("TestInitProcess: Created FileTestProcess with PID " + pid2);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        // yield control repeatedly so that other processes can run
        while (true) {
            OS.Sleep(1000); // sleep to yield control
        }
    }
}
