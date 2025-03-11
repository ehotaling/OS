import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

// Simulates a simple file system using Java's RandomAccessFile
// Supports opening, closing, reading, writing, and seeking within files
public class FakeFileSystem implements Device {
    private static final int MAX_FILES = 10; // Maximum number of open files allowed
    private RandomAccessFile[] files; // Array to hold open file references

    // Initializes the file array
    public FakeFileSystem() {
        files = new RandomAccessFile[MAX_FILES];
    }

    // Finds the first available slot in the file array
    private int findEmptySlot() {
        for (int i = 0; i < MAX_FILES; i++) {
            if (files[i] == null) {
                return i;
            }
        }
        return -1;
    }

    // Opens a file with the given filename
    public int open(String s) {
        if (s == null || s.trim().isEmpty()) {
            throw new IllegalArgumentException("Filename cannot be null or empty");
        }
        int index = findEmptySlot();
        if (index == -1) {
            return -1;
        }
        try {
            files[index] = new RandomAccessFile(s.trim(), "rw"); // Opens file in read-write mode
        } catch (FileNotFoundException e) {
            return -1;
        }
        return index;
    }

    // Closes the file at the given index
    public void close(int id) {
        if (id >= 0 && id < MAX_FILES && files[id] != null) {
            try {
                files[id].close();
            } catch (IOException e) {
                // Ignore errors on close
            }
            files[id] = null;
        }
    }

    // Reads a number of bytes from the file
    public byte[] read(int id, int size) {
        if (id < 0 || id >= MAX_FILES || files[id] == null) {
            return new byte[0];
        }
        byte[] data = new byte[size];
        try {
            int bytesRead = files[id].read(data); // Reads bytes from the file
            if (bytesRead < size && bytesRead > 0) {
                byte[] actualData = new byte[bytesRead];
                System.arraycopy(data, 0, actualData, 0, bytesRead); // Trims unused bytes
                return actualData;
            }
        } catch (IOException e) {
            return new byte[0];
        }
        return data;
    }

    // Moves the file pointer to a specific position
    public void seek(int id, int to) {
        if (id < 0 || id >= MAX_FILES || files[id] == null) {
            return;
        }
        try {
            files[id].seek(to);
        } catch (IOException e) {
            // Ignore seek errors
        }
    }

    // Writes data to the file and returns bytes written
    public int write(int id, byte[] data) {
        if (id < 0 || id >= MAX_FILES || files[id] == null) {
            return 0;
        }
        try {
            files[id].write(data);
            return data.length;
        } catch (IOException e) {
            return 0;
        }
    }
}
