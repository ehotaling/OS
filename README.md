# Simulated Operating System

This project is a Java-based simulation of a simplified operating system. It demonstrates core OS concepts including process management, scheduling, virtual memory with paging and swapping, device management via a Virtual File System (VFS), and inter-process communication.

## Features

* **Process Management:**
    * Creation of userland processes with unique Process IDs (PIDs).
    * Process termination and cleanup.
    * Process state management (running, sleeping, waiting for message).
* **Scheduling:**
    * Priority-based scheduling with three levels: real-time, interactive, and background.
    * A timer interrupt mechanism (simulated every 250ms) to request process preemption.
    * Cooperative multitasking where processes can yield control using `cooperate()`.
    * Process demotion: Processes that continuously exceed their time quantum are demoted in priority.
    * An `IdleProcess` runs when no other processes are ready.
* **Memory Management:**
    * **Virtual Memory:** Each process has its own virtual address space.
    * **Paging:** Memory is divided into 1KB pages.
    * **Page Tables:** Each process has a page table (`PCB.pageTable`) mapping virtual pages to physical page frames or disk locations (100 virtual pages per process).
    * **Lazy Allocation:** Physical memory frames are allocated to pages only when a page is first accessed (on a page fault).
    * **Translation Lookaside Buffer (TLB):** A 2-entry TLB (`Hardware.TLB`) caches recent virtual-to-physical page mappings to speed up address translation. The TLB is cleared on context switches.
    * **Page Fault Handling:** The `Kernel.GetMapping()` method handles page faults. If a page is not in physical memory but is on disk, it's swapped in. If it's a new page (lazy allocation), a zero-filled frame is provided.
    * **Page Swapping:** When physical memory is full and a page fault occurs, a victim page is selected (randomly from a non-idle, non-current process), written to a `swapfile.swp` on disk (managed by `FakeFileSystem`), and its physical frame is reused.
    * **Memory Allocation/Deallocation:** Processes can request (`OS.AllocateMemory`) and release (`OS.FreeMemory`) blocks of virtual memory.
    * **Segmentation Faults:** Accessing unallocated or out-of-bounds virtual memory results in the offending process being terminated.
* **Device Management:**
    * A standardized `Device` interface (`Device.java`) defines common operations: `open`, `close`, `read`, `seek`, `write`.
    * **Virtual File System (VFS):** The `VFS.java` class acts as an abstraction layer, routing device calls from processes to the appropriate physical or simulated device.
    * **Simulated Devices:**
        * `FakeFileSystem.java`: Simulates a file system using Java's `RandomAccessFile`, allowing file creation, read, write, and seek operations. It also manages the `swapfile.swp`.
        * `RandomDevice.java`: Simulates a device that generates random numbers, with an optional seed for reproducibility.
    * Each process maintains a list of its open devices in its PCB.
* **Inter-Process Communication (IPC):**
    * Processes can send and receive messages using `KernelMessage` objects.
    * The `OS.SendMessage()` and `OS.WaitForMessage()` system calls facilitate message passing, managed by the `Kernel`.
    * Processes can look up the PID of another process by its name (class name) using `OS.GetPidByName()`.
* **System Call Interface:**
    * The `OS.java` class provides the public API for userland processes to request kernel services.
    * System calls are handled by the `Kernel.java` class, which runs in a separate thread from userland processes.

## Project Structure

The project is organized into several Java files within the `src` directory:

* **Core OS Components:**
    * `Main.java`: Entry point for the OS simulation.
    * `OS.java`: Provides the system call interface for userland processes.
    * `Kernel.java`: The heart of the operating system, handling system calls, process scheduling, memory management, and device I/O.
    * `Process.java`: Abstract base class for all processes (both kernel and userland).
    * `UserlandProcess.java`: Abstract base class for processes that run in user mode.
    * `PCB.java`: Process Control Block, storing all relevant information about a process.
    * `Scheduler.java`: Implements the process scheduling logic.
* **Memory Management:**
    * `Hardware.java`: Simulates physical memory and the TLB.
    * `VirtualToPhysicalMapping.java`: Represents an entry in a process's page table.
* **Device Management:**
    * `Device.java`: Interface defining standard device operations.
    * `VFS.java`: Virtual File System.
    * `FakeFileSystem.java`: Implementation of a simulated file system.
    * `RandomDevice.java`: Implementation of a simulated random number generator device.
* **Inter-Process Communication:**
    * `KernelMessage.java`: Structure for messages passed between processes.
* **Test Processes & Initialization:**
    * `IdleProcess.java`: A process that runs when no other process is available.
    * `HelloWorld.java`, `GoodbyeWorld.java`: Simple processes for testing basic execution.
    * `DeviceInitProcess.java`, `DeviceTestProcess.java`, `FileTestProcess.java`: Test device and file system operations.
    * `MessagesInitProcess.java`, `Ping.java`, `Pong.java`: Test inter-process communication.
    * `MemoryTestInitProcess.java`, `MemoryTestProcess.java`: Test memory allocation, read/write, freeing, and segmentation faults.
    * `VirtualMemoryTestInitProcess.java`, `PiggyProcess.java`: Stress test the virtual memory system, forcing page faults and swapping.
    * `SleepTestProcess.java`: Tests the `OS.Sleep()` functionality.
    * `testDemotion.java`: A process designed to test priority demotion by not cooperating.

## How to Compile and Run

1.  **Compilation:**
    Compile all `.java` files located in the `src` directory. If you are using an IDE like IntelliJ IDEA, it should handle the compilation automatically.
    ```bash
    javac src/*.java
    ```
2.  **Execution:**
    Run the `Main` class.
    ```bash
    java src.Main
    ```
    By default, the `Main.java` file is configured to start the `VirtualMemoryTestInitProcess`:
    ```java
    // In Main.java
    public static void main(String[] args) throws InterruptedException {
        System.out.println("Main: Starting OS with Memory Tests..."); // Debug print
        OS.Startup(new VirtualMemoryTestInitProcess()); // OS.Startup(new VirtualMemoryTestInitProcess());
        System.out.println("Main: OS Startup complete."); // Debug print
    }
    ```
3.  **Running Other Tests:**
    To run different test scenarios, you need to modify the `OS.Startup()` call in `Main.java` to pass a different initial `UserlandProcess`. For example, to test device functionalities:
    ```java
    // Modify in Main.java
    OS.Startup(new DeviceInitProcess());
    ```
    Or to test message passing:
    ```java
    // Modify in Main.java
    OS.Startup(new MessagesInitProcess());
    ```

## Key Concepts Demonstrated

* **System Calls:** The `OS` class methods (e.g., `OS.CreateProcess`, `OS.Open`, `OS.AllocateMemory`) simulate system calls that transition from user mode to kernel mode to perform privileged operations.
* **Kernel and Userland Separation:** The `Kernel` process runs with higher privileges and manages system resources, while `UserlandProcess` instances run with restricted access, interacting with the system via the `OS` API.
* **Process Lifecycle:** Processes are created, scheduled, executed, can sleep, wait for events (like messages), and eventually exit.
* **Cooperative and Preemptive Multitasking:** Processes can voluntarily yield CPU using `cooperate()`, and a timer interrupt can preempt the currently running process.
* **Virtual Address Translation:** The `Hardware` class simulates reading and writing to memory, which involves virtual-to-physical address translation, potentially looking up the TLB or triggering a page fault handled by `Kernel.GetMapping`.
* **Swapping and Paging:** The system can handle more virtual memory than available physical memory by swapping pages out to a disk file (`swapfile.swp`) and loading them back on demand.
* **Device Abstraction:** The VFS provides a uniform way to interact with different types of devices.

**Note:** The simulation includes extensive `System.out.println` statements for debugging and tracing the OS behavior. It is recommended to save the console output to a file for detailed analysis, especially for complex tests like virtual memory stress testing.
