public class GoodbyeWorld extends UserlandProcess {
    @Override
    public void main() {

        // infinite loop that just prints “Goodbye world”
        // calls cooperate() inside the loop so OS will switch processes
        // Without calling os.sleep() process will be demoted
        while (true) {
            try {
                cooperate();
                System.out.println("Goodbye world");
                thread.sleep(50);
            } catch (Exception e) {}
        }
    }
}
