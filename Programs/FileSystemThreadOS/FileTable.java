/* P5: File System
 * CSS 430
 * Date: 6/8/2019
 * Authors: Daniel Yan and Emily Krasser
 *
 * Description:
 * FileTable to keep track of file entries.
*/

import java.util.Vector;


public class FileTable {
    private static final int FLAG_UNUSED = 0;
    private static final int FLAG_USED = 1;
    private static final int FLAG_READ = 2;
    private static final int FLAG_WRITE = 3;
    private static final int FLAG_TO_DELETE = 4;
    private Vector table = new Vector();  // the actual entity of this file table
    private Directory dir;  // the root directory

    public FileTable(Directory directory)
    {
        this.dir = directory;  // receive a reference to the Director from the file system
    }

    /*
    * Allocates a new file (structure) table entry for this file name
    * allocate/retrieve and register the corresponding inode using dir
    * increment this inode's count
    * immediately write back this inode to the disk
    * return a reference to this file (structure) table entry
    */
    public synchronized FileTableEntry falloc(String filename, String mode)
    {
        Inode inode;
        short s;
        flagCheck : {
            inode = null;
            //If the filename dir is root or if the file exists in dir based on
            //namei
            while ((s = filename.equals("/") ? (short)0 : (short)this.dir.namei(filename)) >= 0)
            {
                inode = new Inode(s);

                // If mode is read, and if inode flag is either used or unused,
                // then set flag to used and then wait
                if (mode.compareTo("r") == 0)
                {
                    if (inode.flag == FLAG_UNUSED || inode.flag == FLAG_USED)
                    {
                        inode.flag = FLAG_USED;
                        break flagCheck;
                    }
                    //Try to wait for thread to finish
                    try
                    {
                        this.wait();
                    }
                    catch (InterruptedException interruptedException) {}
                    continue;
                }
                //inode to be deleted, prevent other threads from accessing
                if(inode.flag == FLAG_TO_DELETE)
                    return null;
                //If flag is in unused or write state, set to read
                if (inode.flag == FLAG_UNUSED || inode.flag == FLAG_WRITE)
                {
                    inode.flag = FLAG_READ;
                    break flagCheck;
                }

                //If flag is used or in read state, set to delete
                if (inode.flag == FLAG_USED || inode.flag == FLAG_READ)
                {
                    // Set flag to 4 or 5, respectively
                    inode.flag = (short)(inode.flag + 3);
                    inode.toDisk(s);
                }

                try
                {
                    // Try to wait for thread to finish
                    this.wait();
                }
                catch (InterruptedException interruptedException) {}
            }

            // If the mode is not set to read then allocate a new inode for the file
            if (mode.compareTo("r") != 0)
            {
                s = this.dir.ialloc(filename);
                inode = new Inode();
                inode.flag = FLAG_READ;
            }

            else
            {
                // Otherwise, return null
                return null;
            }
        }

        inode.count = (short)(inode.count + 1);
        // Save newly allocated inode to disk
        inode.toDisk(s);
        // Make a new file table entry for it, add it to the table, and return the entry
        FileTableEntry fileTableEntry = new FileTableEntry(inode, s, mode);
        this.table.addElement(fileTableEntry);
        return fileTableEntry;
    }

    /*
    * Receive a file table entry reference
    * save the corresponding inode to the disk
    * free this file table entry.
    * return true if this file table entry found in my table
    */
    public synchronized boolean ffree(FileTableEntry e)
    {
        // Attempt to remove the file table entry from the table
        if (this.table.removeElement((Object)e))
        {
            e.inode.count = (short)(e.inode.count - 1);
            switch (e.inode.flag)
            {
                case 1: {
                    // Reset flag to unused
                    e.inode.flag = FLAG_UNUSED;
                    break;
                }
                case 2: {
                    // Reset flag to unused
                    e.inode.flag = FLAG_UNUSED;
                    break;
                }
                //Cases to delete, set to write
                case 4: {
                    e.inode.flag = FLAG_WRITE;
                    break;
                }
                case 5: {
                    e.inode.flag = FLAG_WRITE;
                }
            }

            // Save the data to the disk and set it to null
            e.inode.toDisk(e.iNumber);
            e = null;
            this.notify();
            // Return true when successful
            return true;
        }
        // If unsuccessful in removing the
        return false;
    }

    // should be called before starting a format
    public synchronized boolean fempty()
    {
        return this.table.isEmpty();  // return if table is empty
    }
}
