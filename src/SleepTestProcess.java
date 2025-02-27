public class SleepTestProcess extends UserlandProcess {
    @Override
    public void main() throws InterruptedException {
        while (true) {
            System.out.println("SleepTestProcess (PID: " + OS.GetPID() + ") running at real-time priority.");
            OS.Sleep(100); // Sleep for 100 milliseconds before hitting timeout
            System.out.println("SleepTestProcess woke up and is running again.");
            cooperate();
        }
    }
}
