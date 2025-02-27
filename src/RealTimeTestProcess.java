public class RealTimeTestProcess extends UserlandProcess {
    @Override
    public void main() throws InterruptedException {
        while (true) {
            System.out.println("Hello World");
            for (double i = 0; i < 100_000_000; i++) {
                double dummy = Math.sqrt(i);
            }
            for (double x = 0; x < 100_000_000; x++) {
                double dummy = Math.sqrt(x);
            }
            System.out.println("RealTimeTestProcess (PID: " + OS.GetPID() + ") running at high priority.");
        }
    }
}
