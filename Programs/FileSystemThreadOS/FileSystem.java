/* P5: File System
 * CSS 430
 * Due: 6/9/19
 * Daniel Yan and Emily Krasser
 *
 * Description:
 * A Unix like file system on ThreadOS that allows the user programs
 * to access persistent data on disk by way of stream-oriented files 
 * rather than direct access to disk blocks with rawread and rawwrite.
 */

public class FileSystem {
    // Variables to hold superblock, directory, and filetable
    private SuperBlock superblock;
    private Directory directory;
    private FileTable filetable;

    private final int SEEK_SET = 0;
    private final int SEEK_CUR = 1;
    private final int SEEK_END = 2;

    private final static int BUF_SIZE = 512;
    private final static int DIRECT_SIZE = 11;
    private final static int ERROR = -1;

    public FileSystem(int diskSize)
    {
        // Initialize superblock, directory, and filetable
        this.superblock = new SuperBlock(diskSize);
        this.directory = new Directory(this.superblock.inodeBlocks);
        this.filetable = new FileTable(this.directory);

        FileTableEntry ftEntry = this.open("/", "r");

        int rootEntrySize = this.fsize(ftEntry);

        // If the root entry has something in it, then read it and put into directory
        if (rootEntrySize > 0)
        {
            byte[] dataBlock = new byte[rootEntrySize];
            this.read(ftEntry, dataBlock);
            this.directory.bytes2directory(dataBlock);
        }

        // Close the table entry
        this.close(ftEntry);
    }

    /*
    * Syncronizes data on memory to the disk before shutdown.
    */
    void sync() {
        // Open the root directory, translate to bytes, and write it to the disk
        FileTableEntry ftEntry = this.open("/", "w");
        byte[] dataBlock = this.directory.directory2bytes();
        this.write(ftEntry, dataBlock);
        // Close the entry and synchronize the superblock
        this.close(ftEntry);
        this.superblock.sync();
    }

    /*
    * Formats the disk (Disk.java's data contents). The parameter files specifies
    * the maximum number of files to be created (the number of inodes to be allocated)
    * in your file system. The return value is 0 on success, otherwise -1.
    */
    int format(int files)
    {
        this.superblock.format(files);
        this.directory = new Directory(this.superblock.inodeBlocks);
        this.filetable = new FileTable(this.directory);
        return 0;
    }

    /*
    * Opens the file specified by the fileName string in the given mode (where
    * "r" = ready only, "w" = write only, "w+" = read/write, "a" = append). The call
    * allocates a new file descriptor, fd to this file. The file is created if it does
    * not exist in the mode "w", "w+" or "a". SysLib.open must return a negative number
    * as an error value if the file does not exist in the mode "r". Note that the file
    * descriptors 0, 1, and 2 are reserved as the standard input, output, and error,
    * and therefore a newly opened file must receive a new descriptor numbered in the
    * range between 3 and 31. If the calling thread's user file descriptor table is full,
    * SysLib.open should return an error value. The seek pointer is initialized to zero in
    * the mode "r", "w", and "w+", whereas initialized at the end of the file in the mode "a".
    */
    FileTableEntry open(String fileName, String mode)
    {
        FileTableEntry ftEntry = this.filetable.falloc(fileName, mode);
        if (mode == "w" && !this.deallocAllBlocks(ftEntry))
        {
            return null;
        }
        return ftEntry;
    }

    /*
     * Closes the file corresponding to fd, commits all file transactions on
     * this file, and unregisters fd from the user file descriptor table of the calling
     * thread's TCB. The return value is 0 in success, otherwise -1.
     */
    int close(FileTableEntry fd)
    {
        FileTableEntry ftEntry = fd;
        synchronized (ftEntry)
        {
            --fd.count;
            if (fd.count > 0)
            {
                return 0;
            }
        }

        // If the entry has been successfully freed, then return 0. If not, return -1
        if (this.filetable.ffree(fd))
        {
            return 0;
        }
        else
        {
            return ERROR;
        }
    }

    /*
     * Returns the size in bytes of the file indicated by fd.
     */
    int fsize(FileTableEntry fd)
    {
        FileTableEntry ftEntry = fd;
        synchronized (ftEntry)
        {
            return fd.inode.length;
        }
    }

    /*
     * Reads up to buffer.length bytes from the file indicated by fd,
     * starting at the position currently pointed to by the seek pointer.
     * If bytes remaining between the current seek pointer and the end of
     * file are less than buffer.length, SysLib.read reads as many bytes
     * as possible, putting them into the beginning of buffer. It increments
     * the seek pointer by the number of bytes to have been read. The return
     * value is the number of bytes that have been read, or a negative value upon an error.
     */
    int read(FileTableEntry fd, byte[] buffer)
    {
        // If the root entry is returned then return an error
        if (fd.mode == "w" || fd.mode == "a")
        {
            return ERROR;
        }

        int result = 0;
        FileTableEntry ftEntry = fd;
        synchronized (ftEntry)
        {
            int delta, targetBlock;
            // Loops while i is greater than 0, the file table entry's seek pointer is greater than
            // the size of the entry itself, and while finding the target block does not result in an error
            for (int i = buffer.length; i > 0 && fd.seekPtr < this.fsize(fd) &&
            (targetBlock = fd.inode.findTargetBlock(fd.seekPtr)) != ERROR; i -= delta)
            {
                byte[] block = new byte[BUF_SIZE];
                SysLib.rawread((int)targetBlock, (byte[])block);

                int startPos = fd.seekPtr % BUF_SIZE;
                int endPos = BUF_SIZE - startPos;
                int pos = this.fsize(fd) - fd.seekPtr;

                // Find lenghth of array elements to be copied and copy
                delta = Math.min(Math.min(endPos, i), pos);
                System.arraycopy(block, startPos, buffer, result, delta);

                // Add delta to the file table entry's seekp pointer and to the result that is to be returned
                fd.seekPtr += delta;
                result += delta;
            }
            return result;
        }
    }

    /*
     * Writes the contents of buffer to the file indicated by fd, starting at
     * the position indicated by the seek pointer. The operation may overwrite
     * existing data in the file and/or append to the end of the file. SysLib.write
     * increments the seek pointer by the number of bytes to have been written. The
     * return value is the number of bytes that have been written, or a negative value upon an error.
     */
    int write(FileTableEntry fd, byte[] buffer)
    {
        // If the root entry is returned then return an error
        if (fd.mode == "r")
        {
            return ERROR;
        }

        FileTableEntry ftEntry = fd;
        synchronized (ftEntry)
        {
            int delta;
            int startPos = 0;
            for (int i = buffer.length; i > 0; i -= delta)
            {
                byte[] block;
                short s;
                int blockNumber = fd.inode.findTargetBlock(fd.seekPtr);

                // Error check the inode for error cases
                if (blockNumber == -1)
                {
                    short s2 = (short)this.superblock.getFreeBlock();
                    switch (fd.inode.registerTargetBlock(fd.seekPtr, s2))
                    {
                        // If successful, then break
                        case 0: {
                            break;
                        }
                        // Else output error for reg or unused
                        case -1: {
                            SysLib.cerr((String)"ThreadOS: filesystem panic on write\n");
                            return ERROR;
                        }
                        case -2: {
                          SysLib.cerr((String)"ThreadOS: filesystem panic on write\n");
                          return ERROR;
                        }
                        // Else output error for indirect null
                        case -3: {
                            s = (short)this.superblock.getFreeBlock();
                            if (!fd.inode.registerIndexBlock(s))
                            {
                                SysLib.cerr((String)"ThreadOS: panic on write\n");
                                return ERROR;
                            }
                            if (fd.inode.registerTargetBlock(fd.seekPtr, s2) == 0) { break; }
                            SysLib.cerr((String)"ThreadOS: panic on write\n");
                            return ERROR;
                        }
                    }

                    // Set block number to superblock free block
                    blockNumber = s2;
                }

                // If the rawread doesnt work, then exit
                if (SysLib.rawread((int)blockNumber, (byte[])(block = new byte[BUF_SIZE])) == -1)
                {
                    System.exit(2);
                }

                s = (short) (fd.seekPtr % BUF_SIZE);
                int min = BUF_SIZE - s;
                delta = Math.min(min, i);

                // Copy buffer from positions and write it to the
                System.arraycopy(buffer, startPos, block, s, delta);
                SysLib.rawwrite((int)blockNumber, (byte[])block);

                // Add delta to the file table entry's seekp pointer and to the result that is to be returned
                fd.seekPtr += delta;
                startPos += delta;

                if (fd.seekPtr <= fd.inode.length) { continue; }

                fd.inode.length = fd.seekPtr;
            }
            // Write to disk
            fd.inode.toDisk(fd.iNumber);

            // Return the number of bytest that have been written to disk
            return startPos;
        }
    }

    private boolean deallocAllBlocks(FileTableEntry fd)
    {
        // If there are no blocks then return false
        if (fd.inode.count != 1)
        {
            return false;
        }

        byte[] block = fd.inode.unregisterIndexBlock();

        if (block != null)
        {
            short s;
            int n = 0;
            while ((s = SysLib.bytes2short((byte[])block, (int)n)) != ERROR)
            {
                // Return blocks
                this.superblock.returnBlock(s);
            }
        }

        // Go through and return blocks
        for (int i = 0; i < DIRECT_SIZE; ++i)
        {
            // If there is no pointer at index, continue
            if (fd.inode.direct[i] == ERROR) { continue; }
            this.superblock.returnBlock(fd.inode.direct[i]);
            fd.inode.direct[i] = ERROR;
        }

        // Write the file table entry's inode index to the disk
        fd.inode.toDisk(fd.iNumber);
        return true;
    }

    /*
     * Deletes the file specified by fileName. All blocks used by file are freed.
     * If the file is currently open, it is not deleted and the operation returns a -1.
     * If successfully deleted a 0 is returned.
     */
    int delete(String fileName)
    {
        FileTableEntry fileTableEntry = this.open(fileName, "w");
        short s = fileTableEntry.iNumber;
        // Return 0 if successfully deleted, or -1 if not successful
        if (this.directory.ifree(s))
        {
            return this.close(fileTableEntry);
        }
        else
        {
            return ERROR;
        }
    }

    /*
     * Updates the seek pointer corresponding to fd.
     */
    int seek(FileTableEntry fd, int offset, int whence)
    {
        FileTableEntry ftEntry = fd;
        synchronized (ftEntry)
        {
            //Set the end of file for clamping
            switch (whence) {
                // If whence is SEEK_SET (= 0), the file's seek pointer is set
                // to offset bytes from the beginning of the file
                case SEEK_SET: {
                    if (offset >= 0 && offset <= this.fsize(fd))
                    {
                        fd.seekPtr = offset;
                        break;
                    }
                    return ERROR;
                }
                // If whence is SEEK_CUR (= 1), the file's seek pointer is set
                // to its current value plus the offset. The offset can be positive or negative.
                case SEEK_CUR: {
                    if (fd.seekPtr + offset >= 0 && fd.seekPtr + offset <= this.fsize(fd))
                    {
                        fd.seekPtr += offset;
                        break;
                    }
                    return ERROR;
                }
                // If whence is SEEK_END (= 2), the file's seek pointer is set
                // to the size of the file plus the offset. The offset can be positive or negative.
                case SEEK_END: {
                    if (this.fsize(fd) + offset >= 0 && this.fsize(fd) + offset <= this.fsize(fd))
                    {
                        fd.seekPtr = this.fsize(fd) + offset;
                        break;
                    }
                    return ERROR;
                }
                //Case where seek status code is not recognized
                default: {
                  SysLib.cerr("Seek error: Whence " +
                  whence + " is unrecognized");
                }
                return ERROR;
            }

            //Clamping seekPtr to either 0 if below 0 or to end of file
            // if greater than file size
            if(fd.seekPtr < 0)
               fd.seekPtr = 0;
            else if(fd.seekPtr > this.fsize(fd))
              fd.seekPtr = this.fsize(fd);


            // Return the seek pointer
            return fd.seekPtr;
        }
    }
}
