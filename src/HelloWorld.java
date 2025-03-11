public class HelloWorld extends UserlandProcess {
    @Override
    public void main() {
        // implements run with an infinite loop of printing
        while (true) {
            try {
                cooperate();
                System.out.println("Hello World");
                Thread.sleep(300);
            } catch (Exception e) {}

        }
    }
}
