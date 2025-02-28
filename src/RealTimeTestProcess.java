public class RealTimeTestProcess extends UserlandProcess {
    @Override
    public void main() throws InterruptedException {
        System.out.println("RealTimeTestProcess started with PID: " + OS.GetPID());
        while (true) {
            for (double i = 0; i < 100_000; i++) {
                System.out.println("RealTimeTestProcess (PID: " + OS.GetPID() + ") is running at real-time priority.");
            }
            System.out.println("RealTimeTestProcess (PID: " + OS.GetPID() + ") calling cooperate");
            cooperate();
        }
    }
}
