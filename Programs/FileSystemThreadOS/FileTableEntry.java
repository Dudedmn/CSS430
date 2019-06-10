/* P5: File System
 * CSS 430
 * Date: 6/7/2019
 * Authors: Daniel Yan and Emily Krasser
 *
 * Description:
 * FileTableEntries to use in FileTables.
 */

public class FileTableEntry
{       // Each table entry should have
    public int seekPtr = 0;         // a file seek pointer
    public final Inode inode;       // a reference to its inode
    public final short iNumber;     // this inode number
    public int count;               // # threads sharing this entry
    public final String mode;       // "r", "w", "w+", or "a"

    FileTableEntry(Inode inode, short inumber, String m)
    {
        this.inode = inode;  // the seek pointer is set to the file top
        this.iNumber = inumber;
        this.count = 1;  // at least on thread is using this entry
        this.mode = m;  // once access mode is set, it never changes

        if (this.mode.compareTo("a") == 0)  // if mode is append,
        {
            this.seekPtr = this.inode.length;  // seekPtr points to the end of file
        }
    }
}
