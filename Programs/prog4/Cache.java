/**
 *
 * Author - Daniel Yan
 * Date - 5/27/2019
 * Purpose - Making a disk cache that implements a Second Chance Algorithm (SCA)
 * approach for caching.
 */
import java.util.*;

public class Cache
{
  //Private class object which is an array of page entries
  PageEntry[] pageTable;
  private int victim, blockSize;
  private final static int EMPTY_BLOCK = -1;
  private final static int NOT_FOUND = Integer.MIN_VALUE;

  //Contains necessary values for a page entry
  private class PageEntry
  {
    byte[] blockData;
    int blockId;
    boolean referenceBit, dirtyBit;

    private PageEntry(int blockSize)
    {
      //Initializes the the block
      blockData = new byte[blockSize];
      //Set block to empty
      blockId = EMPTY_BLOCK;
      //Default bit values
      referenceBit = false;
      dirtyBit = false;
    }
  }

  /**
   * Constructor which insubstantiates the pageTable with page entries based
   * on a blockSize. The victim value is also set here.
   */
  public Cache(int blockSize, int cacheBlocks)
  {
    //cacheBlocks really refers to pageSize
    pageTable = new PageEntry[cacheBlocks];
    this.blockSize = blockSize;
    victim = cacheBlocks - 1;
    for(int i = 0; i < pageTable.length; i++)
    {
      //Initialize pageTable with proper size and default values
      pageTable[i] = new PageEntry(blockSize);
    }
  }

  /**
   * Reads into a byte buffer[] array with a valid blockId check. If the
   * blockId is within the cache, the corresponding disk block is read from the
   * disk. Otherwise an empty blockId is found to read. If an empty one cannot
   * be found, the SCA is used to determine a victim.
   */
  public synchronized boolean read(int blockId, byte buffer[])
  {
    if(blockId < 0)
    {
      SysLib.cerr("Invalid blockId found");
      return false;
    }
    else
    {
        int index = findBlock(blockId);
        //If block is found in the Cache
        if(index != NOT_FOUND)
        {
          //Reads from cache and sets appropriate values
          readCache(index, blockId, buffer);
          return true;
        }
        //If block is empty
        index = findBlock(EMPTY_BLOCK);
        if(index != NOT_FOUND)
        {
          //Read data from the disk, then copy to the buffer
          SysLib.rawread(blockId, pageTable[index].blockData);
          //Reads from cache and sets appropriate values
          readCache(index, blockId, buffer);
          return true;
        }
        //Finds the victim, saves it to disk
        diskWrite(findVictim());
        //Reads the victim data
        SysLib.rawread(blockId, pageTable[victim].blockData);
        //Read from the caceh block
       	readCache(victim, blockId, buffer);
        return true;
    }
  }
  /**
   *
   *
   */
  public synchronized boolean write(int blockId, byte buffer[])
  {
    if(blockId < 0)
    {
      SysLib.cerr("Invalid blockId found");
      return false;
    }
    else
    {
        int index = findBlock(blockId);
        //If block is found in the Cache
        if(index != NOT_FOUND)
        {
          //Reads from cache and sets appropriate values
          addCache(index, blockId, buffer);
          return true;
        }
        //Block is empty
        index = findBlock(EMPTY_BLOCK);
        if(index != NOT_FOUND)
        {
          //Add block to cache and sets appropriate values
          addCache(index, blockId, buffer);
          return true;
        }
        //Finds the victim, saves it to disk
        diskWrite(findVictim());
        //Adds the victim to cache
        addCache(victim, blockId, buffer);
        return true;
    }
  }
  /**
   * Ensures the blocks have fresh copies, writes all dirtyBit blocks back
   * to disk
   */
  public synchronized void sync()
  {
    //Go through all the page entries
    for(int i = 0; i < pageTable.length; i++)
    {
      //Check if the dirtyBit is set and the block is not empty, writes the
      // dirtyBit block if true
      if(pageTable[i].dirtyBit && pageTable[i].blockId != EMPTY_BLOCK)
        diskWrite(i);
    }
    //Sync disk
    SysLib.sync();
  }

  /**
   * Flushes all the cached blocks by setting them to the default status.
   * Writes back dirtyBit blocks to disk
   */
  public synchronized void flush()
  {
    //Go through all page entries
    for(int i = 0; i < pageTable.length; i++)
    {
      //If the block is empty, skip it as the other bits sholdn't be touched
      if(pageTable[i].blockId != EMPTY_BLOCK)
        continue;
      //If the block has the dirty bit set, write the data to disk
      if(pageTable[i].dirtyBit)
        diskWrite(i);
        //Flushes the rest of the block values
        pageTable[i].blockId = EMPTY_BLOCK;
        pageTable[i].dirtyBit = false;
        pageTable[i].referenceBit = false;
    }
    //Sync disk
    SysLib.sync();
  }

  /**
   * Writes to the disk to save the data before it's removed.
   * Done by checking if the dirty bit has been set, then writes it with
   * rawwrite and sets the dirty bit back
   */
  private void diskWrite(int victimIndex)
  {
		if(pageTable[victimIndex].dirtyBit
    && pageTable[victimIndex].blockId != EMPTY_BLOCK)
    {
			SysLib.rawwrite(pageTable[victimIndex].blockId,
       pageTable[victimIndex].blockData);
       //Set dity bit to false
			pageTable[victimIndex].dirtyBit = false;
		}
  }

  /**
   * Assumes victim here is already set to last index of the pageTable. Uses
   * the SCA to determine a victim to find
   */
  private int findVictim()
  {
    int cycle = victim;
    boolean dirtyStatus = false, refStatus = false;
    while (true)
     {

        victim = (victim + 1) % pageTable.length;
        //Reset if victim is too large
        if(victim >= pageTable.length)
          victim = 0;

    		if (!pageTable[victim].referenceBit && (!pageTable[victim].dirtyBit
        || dirtyStatus))
    			return victim;

        //Cycle has been completed
        if(victim == cycle)
        {
          if(dirtyStatus)
            refStatus = true;
            //Dirty bit overwrite
          dirtyStatus = !dirtyStatus;
        }

        //Reset referenceBit
        if(refStatus)
    		  pageTable[victim].referenceBit = false;
      }
  }
  /**
   * Helper method to find a specified block and return the index in the
   * pageTable. Otherwise the NOT_FOUND value is returned
   */
  private int findBlock(int valToFind)
  {
    for (int i = 0; i < pageTable.length; i++)
    {
      if(pageTable[i].blockId == valToFind)
      {
        return i;
      }
    }
    return NOT_FOUND;
  }

  /**
   * Reads a value from the cache based on the blockId, index in the pageTable
   * and the buffer byte[] to add. The referenceBit is set to indicate read
   * status.
   */
  private void readCache(int index, int blockId, byte[] buffer)
  {
    //Copy byte array from blockData into buffer
    System.arraycopy(pageTable[index].blockData, 0, buffer, 0, blockSize);
    //Necessary status set for the entry
    pageTable[index].blockId = blockId;
    pageTable[index].referenceBit = true;
  }
  /**
   * Adds a value to the cache based on the blockId, index in the pageTable
   * and the buffer byte[] to add. The dirtyBit and referenceBit are set to
   * indicate the read and wrote status.
   */
  private void addCache(int index, int blockId, byte[] buffer)
  {
      //Copy byte array from buffer into blockData
      System.arraycopy(buffer, 0, pageTable[index].blockData, 0, blockSize);
      //Necessary status set for the entry
      pageTable[index].dirtyBit = true;
      pageTable[index].blockId = blockId;
      pageTable[index].referenceBit = true;
  }
}
