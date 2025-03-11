public class FileTestProcess extends UserlandProcess {
    @Override
    public void main() throws InterruptedException {
        // open file device with filename testfile.txt
        int fileSlot = OS.Open("file testfile.txt");
        System.out.println("FileTestProcess: Opened file device slot " + fileSlot);
        // create content to write
        byte[] content = "HelloFile".getBytes();
        // write content to file device
        int bytesWritten = OS.Write(fileSlot, content);
        System.out.println("FileTestProcess: Wrote " + bytesWritten + " bytes to file");
        // seek to beginning of file
        OS.Seek(fileSlot, 0);
        // read 10 bytes from file device
        byte[] fileData = OS.Read(fileSlot, 10);
        System.out.println("FileTestProcess: Read file data " + new String(fileData));
        // close file device
        OS.Close(fileSlot);
        System.out.println("FileTestProcess: Closed file device");
        // exit process
        OS.Exit();
    }
}
