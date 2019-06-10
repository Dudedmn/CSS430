/* P5: File System
 * CSS 430
 * Date: 6/7/2019
 * Authors: Daniel Yan and Emily Krasser
 *
 * Description:
 * SuperBlock which refers to the 0th block of the DISK. Synchronizes disk.
 */

public class SuperBlock {
    private final int defaultInodeBlocks = 64;
    public static final int INODESIZE = 32;         // fix to 32 bytes
    private static final int BUF_SIZE = 512;

    public int totalBlocks;     // the number of disk blocks
    public int inodeBlocks;     // the number of inodes
    public int freeList;        // the block number of the free list's head

    /*
     * Read the SuperBlock based on the Disk Size
     */
    public SuperBlock(int diskSize)
    {
        byte[] blockData = new byte[BUF_SIZE];
        SysLib.rawread(0, blockData);
        totalBlocks = SysLib.bytes2int(blockData, 0);
        inodeBlocks = SysLib.bytes2int(blockData, 4);
        freeList = SysLib.bytes2int(blockData, 8);

        if (totalBlocks == diskSize && inodeBlocks > 0 && freeList >= 2)
        {
            return;
        }

        totalBlocks = diskSize;
        SysLib.cerr((String)"default format( 64 )\n");
        format(defaultInodeBlocks);
    }

    /*
     * Sync the blocks in the DISK
     */
    void sync()
    {
        byte[] blockData = new byte[BUF_SIZE];
        SysLib.int2bytes(totalBlocks, blockData, 0);
        SysLib.int2bytes(inodeBlocks, blockData, 4);
        SysLib.int2bytes(freeList, blockData, 8);
        SysLib.rawwrite(0, blockData);
        SysLib.cerr((String)"Superblock synchronized\n");
    }

    /*
     * Format the file to a specified format
     */
    void format(int fileFormat)
    {
        int blockNum;
        byte[] blockData;
        Inode node;
        inodeBlocks = fileFormat;

        for (blockNum = 0; blockNum < inodeBlocks; blockNum = blockNum + 1)
        {
            node = new Inode();
            node.flag = 0;
            node.toDisk((short)blockNum);
        }

        //Offset for free list is 2
        for (blockNum = freeList = 2 + inodeBlocks * INODESIZE / BUF_SIZE;
         blockNum < totalBlocks; ++blockNum)
        {
            blockData = new byte[BUF_SIZE];

            for (int i = 0; i < BUF_SIZE; ++i)
            {
                blockData[i] = 0;
            }

            SysLib.int2bytes(blockNum + 1, blockData, 0);
            SysLib.rawwrite(blockNum, blockData);
        }
        sync();
    }

    /*
     * Gets the index of the blockID for the next free block in the freelist
     */
    public int getFreeBlock()
    {
        int index = freeList;
        //Free block check
        if (index != -1)
        {
            //Temporary block used for disk
            byte[] blockData = new byte[BUF_SIZE];
            SysLib.rawread(index, blockData);
            //Update pointer head for freelist
            freeList = SysLib.bytes2int(blockData, 0);
            SysLib.int2bytes(0, blockData, 0);
            SysLib.rawwrite(index, blockData);
        }
        return index;
    }

    /*
     * Placed the blockID into the list of free blocks
     */
    public boolean returnBlock(int blockID)
    {
        //Check if blockID is valid
        if (blockID >= 0)
        {
            //Initialize temp block to 0
            byte[] blockData = new byte[BUF_SIZE];
            for (int i = 0; i < BUF_SIZE; ++i)
            {
                blockData[i] = 0;
            }

            SysLib.int2bytes(freeList, blockData, 0);
            //Add block id to free list
            SysLib.rawwrite(blockID, blockData);
            freeList = blockID;
            return true;
        }
        return false;
    }
}
