/*
 * Represents an entry in a process's page table.
 * Maps a virtual page number to either a physical page frame in RAM
 * or a page slot on disk that is in the swap file
 */
public class VirtualToPhysicalMapping {

    // The physical RAM page frame where the data currently resides (or -1 if not in RAM)
    public int physicalPageNumber;

    // The location (page number/offset) within the swap file where the page's data is stored
    // if it's not currently in RAM (or -1 if it hasn't been swapped out yet)
    public int diskPageNumber;

    // Initializes the mapping to indicate the page is neither in physical memory nor on disk initially
    public VirtualToPhysicalMapping() {
        this.physicalPageNumber = -1; // -1 means not in RAM
        this.diskPageNumber = -1; // -1 means not on disk, or it has never been swapped
    }


}
