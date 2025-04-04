public class IdleProcess extends UserlandProcess {
    @Override
    public void main() {
        while (true) {
            try {
                System.out.println("IdleProcess: Running");
                cooperate();
                Thread.sleep(10);
            } catch (Exception e) { }
        }
    }
}
