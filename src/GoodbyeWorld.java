public class GoodbyeWorld extends UserlandProcess {
    @Override
    public void main() {

        // infinite loop that just prints “Goodbye world”
        // calls cooperate() inside the loop so OS will switch processes
        while (true) {
            try {
                cooperate();
                System.out.println("Goodbye world");
                Thread.sleep(300);
            } catch (Exception e) {}
        }
    }
}
