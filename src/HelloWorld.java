public class HelloWorld extends UserlandProcess {
    @Override
    public void main() {

        // // implements run with an infinite loop of printing
        while (true) {
            try {
                cooperate();
                System.out.println("Hello World");
                Thread.sleep(50); // sleep for 50 ms
            } catch (Exception e) {}

        }
    }
}
