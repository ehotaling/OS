public class RealTimeTestProcess extends UserlandProcess {
    @Override
    public void main() throws InterruptedException {
        // Perform one quantum of work:
        System.out.println("Hello World");
        for (int i = 0; i < 1000; i++) {
            double dummy = Math.sqrt(i);
        }
        System.out.println("RealTimeTestProcess (PID: " + OS.GetPID() + ") running at high priority.");
        cooperate();  // If needed, signal the scheduler to switch processes.
        // Return after one quantum; the scheduler will call main() again next quantum.
    }
}
