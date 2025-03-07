public class HelloWorld extends UserlandProcess {
    @Override
    public void main() {
        // implements run with an infinite loop of printing
        // Without calling os.sleep() process will be demoted
        while (true) {
            try {
                cooperate();
                System.out.println("Hello World");
                thread.sleep(50);
            } catch (Exception e) {}

        }
    }
}
