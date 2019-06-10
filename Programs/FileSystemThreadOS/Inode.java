/* P5: File System
 * CSS 430
 * Date: 6/7/2019
 * Authors: Daniel Yan and Emily Krasser
 *
 * Description:
 * Inode class used for keep track of items for FileSystem.
 */

public class Inode {
    public static final int INODE_SIZE = 32;         // fix to 32 bytes
    public static final int DIRECT_SIZE = 11;        // # direct pointers

    private static final int BUF_SIZE = 512;
    private static final int SHORT_BYTE_SIZE = 2;
    private static final int INT_BYTE_SIZE = 4;
    private static final int OFFSET_DISK = 1;
    //Determined by calculating (BUF_SIZE / INODE_SIZE) which is 16
    private static final int DISK_BLOCK = BUF_SIZE / INODE_SIZE;
    public static final int SUCCESS = 0;
    public static final int ERR_BLOCK_REG = -1;
    public static final int ERR_BLOCK_UNUSED = -2;
    public static final int ERR_INDIRECT_NULL = -3;

    public int length;          // file size in bytes
    public short count;         // # file-table entries pointing to
    public short flag;                      // 0 = unused, 1 = used, ...
    public short[] direct = new short[DIRECT_SIZE];  // direct pointers
    public short indirect;                  // a indirect pointer




    /*
    * A default constructor
    */
    Inode()
    {
        length = 0;
        count = 0;
        flag = 1;

        for (int i = 0; i < DIRECT_SIZE; ++i)
            direct[i] = ERR_BLOCK_REG;
        indirect = (short)ERR_BLOCK_REG;
    }

    /*
     * Retrieve inode from disk based off on an inputted index in the DISK
     */
    Inode(short iNumber)
    {
        //Calculates proper blockNumber and blockOffset needed for DISK access
        int blockNumber = OFFSET_DISK + (iNumber / DISK_BLOCK);
        int blockOffset = (iNumber % DISK_BLOCK) * INODE_SIZE;
        //Create a byte[] for the block data
        byte[] blockData = new byte[BUF_SIZE];
        //Read in the data based on blockNumber
        SysLib.rawread(blockNumber, blockData);

        //Initialize member values
        length = SysLib.bytes2int(blockData, blockOffset);
        count = SysLib.bytes2short(blockData, (blockOffset += INT_BYTE_SIZE));
        flag = SysLib.bytes2short(blockData, (blockOffset += SHORT_BYTE_SIZE));

        //Set direct[i] values based on offset
        blockOffset += SHORT_BYTE_SIZE;
        for (int i = 0; i < DIRECT_SIZE; ++i)
        {
            direct[i] = SysLib.bytes2short(blockData, blockOffset);
            blockOffset += SHORT_BYTE_SIZE;
        }
        //Set indirect value based on offset
        indirect = SysLib.bytes2short(blockData, blockOffset);
        blockOffset += SHORT_BYTE_SIZE;
    }

    /*
     * Save to DISK based on a specified index for inode
     */
    void toDisk(short iNumber)
    {
        int blockNumber = 0, blockOffset = 0;
        //Data here is only Initialized to size of the inode
        byte[] blockData = new byte[INODE_SIZE];
        //Change the offset values
        SysLib.int2bytes(length, blockData, blockOffset);
        SysLib.short2bytes((short) count, blockData,
         (blockOffset += INT_BYTE_SIZE));
        SysLib.short2bytes((short) flag, blockData,
         (blockOffset += SHORT_BYTE_SIZE));

        //Set offset based on direct
        blockOffset += SHORT_BYTE_SIZE;
        for (blockNumber = 0; blockNumber < DIRECT_SIZE; ++blockNumber)
        {
            SysLib.short2bytes((short) direct[blockNumber],
             blockData, blockOffset);
            blockOffset += SHORT_BYTE_SIZE;
        }

        SysLib.short2bytes((short) indirect, blockData, blockOffset);

        blockOffset += SHORT_BYTE_SIZE;
        blockNumber = OFFSET_DISK + (iNumber / DISK_BLOCK);

        //Read from disk
        byte[] diskData = new byte[BUF_SIZE];
        SysLib.rawread(blockNumber, diskData);
        //Copy from inode to disk and write to it
        blockOffset = (iNumber % DISK_BLOCK) * INODE_SIZE;
        System.arraycopy(blockData, 0, diskData, blockOffset, INODE_SIZE);
        SysLib.rawwrite(blockNumber, diskData);
    }

    /*
     * Check if the block to write to is valid
     */
    boolean registerIndexBlock(short iNumber)
    {
        for (int i = 0; i < DIRECT_SIZE; ++i) {
            if (direct[i] != ERR_BLOCK_REG)
              continue;
            return false;
        }

        if (indirect != ERR_BLOCK_REG)
            return false;

        indirect = iNumber;
        byte[] blockData = new byte[BUF_SIZE];

        for (int i = 0; i < (BUF_SIZE/SHORT_BYTE_SIZE); ++i)
        {
            SysLib.short2bytes((short)ERR_BLOCK_REG,
             blockData, (i * SHORT_BYTE_SIZE));
        }
        SysLib.rawwrite((int) iNumber, blockData);

        return true;
    }

    /*
     * Find a specific block
     */
    int findTargetBlock(int targetBlock)
    {
        int blockID = targetBlock / BUF_SIZE;
        if (blockID < DIRECT_SIZE)
        {
            return direct[blockID];
        }

        if (indirect < 0)
        {
            return ERR_BLOCK_REG;
        }

        byte[] blockData = new byte[BUF_SIZE];
        SysLib.rawread((int)indirect, blockData);
        int blockOffset = blockID - DIRECT_SIZE;

        return SysLib.bytes2short(blockData, blockOffset * SHORT_BYTE_SIZE);
    }

    /*
     * Register a value to a block based on index
     */
    int registerTargetBlock(int targetBlock, short iNumber)
    {
        int blockID = targetBlock / BUF_SIZE;
        if (blockID < DIRECT_SIZE)
        {
            if (this.direct[blockID] >= 0)
            {
                return ERR_BLOCK_REG;
            }

            if (blockID > 0 && direct[blockID - 1] == ERR_BLOCK_REG)
            {
                return ERR_BLOCK_UNUSED;
            }
            direct[blockID] = iNumber;
            return SUCCESS;
        }

        if (this.indirect < 0)
        {
            return ERR_INDIRECT_NULL;
        }

        byte[] blockData = new byte[BUF_SIZE];
        SysLib.rawread((int) indirect, blockData);
        int blockOffset = blockID - DIRECT_SIZE;

        if (SysLib.bytes2short(blockData, blockOffset * SHORT_BYTE_SIZE) > 0)
        {
            SysLib.cerr((String)("indexBlock, indirectNumber = " + blockOffset +
            " contents = " + SysLib.bytes2short(blockData,
            blockOffset * SHORT_BYTE_SIZE) + "\n"));
            return ERR_BLOCK_REG;
        }

        SysLib.short2bytes((short)iNumber, blockData,
         blockOffset * SHORT_BYTE_SIZE);
        SysLib.rawwrite(indirect, blockData);
        return SUCCESS;
    }

    /*
     * Free up index block
     */
    byte[] unregisterIndexBlock()
    {
        if (indirect >= 0)
        {
            byte[] blockData = new byte[BUF_SIZE];
            SysLib.rawread((int) indirect, blockData);
            indirect = (short)ERR_BLOCK_REG;
            return blockData;
        }
        return null;
    }

    /*
    * Finds index block based on indirect
    */
    int findIndexBlock()
    {
      return indirect;
    }
}
