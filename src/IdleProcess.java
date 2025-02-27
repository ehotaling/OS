public class IdleProcess extends UserlandProcess {
    @Override
    public void main() {
        System.out.println("IdleProcess.main: Idle process started."); // Debug print
        while (true) {
            try {
                cooperate();
                Thread.sleep(50);
            } catch (Exception e) { }
        }
    }
}
