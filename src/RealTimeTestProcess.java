// Real time process that intentionally runs for a long time to see demotion and timeout
public class RealTimeTestProcess extends UserlandProcess {
    @Override
    public void main() throws InterruptedException {
        while (true) {
            System.out.println("Hello World");
            // Long, compute intensive loop ensures the process runs for a long time before cooperating.
            for (int i = 0; i < 1000; i++) {
                double dummy = Math.sqrt(i);
            }
            System.out.println("RealTimeTestProcess (PID: " + OS.GetPID() + ") running at high priority.");
            cooperate();
        }
    }
}
