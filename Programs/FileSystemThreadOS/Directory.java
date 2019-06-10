/* P5: File System
 * CSS 430
 * Date: 6/8/2019
 * Authors: Daniel Yan and Emily Krasser
 *
 * Description:
 * Implements Unix-like directory structure for ThreadOS File System.
 */

public class Directory
{
    private static int maxChars = 30;  // Max characters for each file name
    private final static int ERROR = -1;
    // Directory entries
    private int[] fsizes;
    private char[][] fnames;

    /*
    * Initializes the "/" directory
    */
    public Directory(int maxInumber)
    {
        this.fsizes = new int[maxInumber];

        for (int i = 0; i < maxInumber; ++i)
        {
            this.fsizes[i] = 0;  // All files initialized to 0
        }

        this.fnames = new char[maxInumber][maxChars];
        String root = "/";  // Entry (Inode) 0 is "/"
        this.fsizes[0] = root.length();  // fsize[0] is the size of "/"
        root.getChars(0, this.fsizes[0], this.fnames[0], 0);  // fnames[0] includes "/"
    }

    /*
    * Assumes data[] received directory information from disk
    * and initializes the Directory instance with this data[]
    */
    public void bytes2directory(byte[] data)
    {
        int n = 0;

        for (int i = 0; i < this.fsizes.length; i++)
        {
            // Get ints
            this.fsizes[i] = SysLib.bytes2int((byte[])data, (int)n);
            n += 4;
        }

        for (int j = 0; j < this.fnames.length; j++)
        {
            // Get the file names based off of the indexes from the bytes
            String filename = new String(data, n, maxChars * 2);
            filename.getChars(0, this.fsizes[j], this.fnames[j], 0);
            n += maxChars * 2;
        }

    }

    /*
    * Converts and return Directory information into a plain byte array
    * which will be written back to disk
    * Note: only meaningfull directory information is be converted
    * into bytes.
    */
    public byte[] directory2bytes()
    {
        // Declare an array of bytes that can hold the directory bytes
        byte[] arr = new byte[this.fsizes.length * 4 + this.fnames.length * maxChars * 2];

        int n = 0;

        for (int i = 0; i < this.fsizes.length; i++)
        {
            // Get bytes
            SysLib.int2bytes((int)this.fsizes[i], (byte[])arr, (int)n);
            n += 4;
        }

        for (int j = 0; j < this.fnames.length; j++)
        {
            // Get file names, convert to bytes and add to the directory byte array
            String string = new String(this.fnames[j], 0, this.fsizes[j]);
            byte[] fArr = string.getBytes();
            System.arraycopy(fArr, 0, arr, n, fArr.length);
            n += maxChars * 2;
        }
        return arr;
    }

    /*
    * filename is the one of a file to be created.
    * allocates a new inode number for this filename
    */
    public short ialloc(String filename)
    {
        for (short s = 1; s < this.fsizes.length; s = (short)(s + 1))
        {
            if (this.fsizes[s] != 0) { continue; }
            // If fsizes[s] is free, then allocate a new inode number
            this.fsizes[s] = Math.min(filename.length(), maxChars);
            // Copies the filename to fnames at index s
            filename.getChars(0, this.fsizes[s], this.fnames[s], 0);
            // Return s for success
            return s;
        }
        // Return negative one in case of failure
        return ERROR;
    }

    /*
    * Deallocates the iNumber (inode number)
    * and the corresponding file will be deleted
    */
    public boolean ifree(short iNumber)
    {
        // If there's something at the index, then deallocate and return true
        if (this.fsizes[iNumber] > 0)
        {
            this.fsizes[iNumber] = 0;
            return true;
        }
        // Otherwise, return false
        return false;
    }

    /*
    * Returns this file's iNumber
    */
    public short namei(String filename)
    {
        // Go through fnames and fsizes, looking for the corresponding name
        for (short s = 0; s < this.fsizes.length; s = (short)(s + 1))
        {
            // If the file names are the same then return the index
            String filenameFromChars;
            if (this.fsizes[s] != filename.length() ||
                filename.compareTo(filenameFromChars =
                new String(this.fnames[s], 0, this.fsizes[s])) != 0) { continue; }
            return s;
        }
        // Otherwise, return negative one
        return ERROR;
    }
}
