/**
 * Author - Daniel Yan
 * Date - 5/27/2019
 * Purpose - Testing program for Cache.java using 4 types of tests:
 * Random Accesses - Read and write randomly across the disk
 * Localized Accesses - Read and write a small selection of blocks
 * Mixed Accesses - 90% should be localized and 10% should be random
 * Adversary Accesses - Disk accesses which fails to use the cache properly
 */
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Date;
import java.util.Random;

public class Test4 extends Thread{
    //Helper global variables
    private int testCase = -1;
    private boolean cacheStatus = false;
    private static final int blockSize = 512, blockSector = 10;
    private static final int arraySize = 250;
    private long blockReadStart, blockReadEnd;
    private long blockWriteStart, blockWriteEnd;
    private Random randNum;
    private String label;

    /**
     * Constructor which parses in the command line arguments and
     * initalizes preset sizes for testing.
    */
    public Test4 (String args[])
    {
        //Parse the first character argument
        if (args[0].toUpperCase().equals("ENABLED"))
        {
          cacheStatus = true;
        }
        else if (args[0].toUpperCase().equals("DISABLED"))
        {
          cacheStatus = false;
        }
        else
        {
          SysLib.cerr("INVALID Cache choice case entered");
          SysLib.exit();
        }

        //Parse the second character argument
        testCase = Integer.parseInt(args[1]);

        if(testCase < 1 || testCase > 5)
        {
          SysLib.cerr("INVALID Test case entered");
          SysLib.exit();
        }


    }
    /**
     * run() method necessary for Thread extension. First flushes to clear the
     * cache before running any test. Switch case to run specific test.
     */
    public void run(){
        SysLib.flush();

        switch (testCase){
            case 1: randomAccessTest();
                break;
            case 2: localizedAccessTest();
                break;
            case 3: mixedAccessTest();
                break;
            case 4: adversaryAccessTest();
                break;
            case 5:
              randomAccessTest();
              localizedAccessTest();
              mixedAccessTest();
              adversaryAccessTest();
              break;
            default:
            SysLib.cerr("Invalid Argument found.");
                break;
        }
        //Syncs the disk based on caching
        sync();
        SysLib.exit();
    }

    public void sync()
    {
        if (cacheStatus)
        {
            SysLib.csync();
        }
        else
        {
            SysLib.sync();
        }
    }

    /**
     * Provides a random access test which does reads and writes randomly
     * accross the disk with either the cache enabled or disabled.
    */
    public void randomAccessTest()
    {
        label = "Random Access Test ";
        //Fill array with random bytes
        byte[] writeBlock = new byte[blockSize];
        new Random().nextBytes(writeBlock);
        //Used for writing and reading
        ArrayList<Integer> index = new ArrayList<>();
        blockWriteStart = getTime();
        for (int i = 0; i < arraySize; i++)
        {
            int randIndex = randInt(blockSize);
            index.add(randIndex);
            write(randIndex, writeBlock);
        }
        blockWriteEnd = getTime();

        byte[] readBlock = new byte[blockSize];
        blockReadStart = getTime();
        for(int i = 0; i < arraySize; i++)
        {
            read(index.get(i), readBlock);
        }
        blockReadEnd = getTime();
        //Check if block byte arrays are different sizes
        if(!Arrays.equals(readBlock, writeBlock))
        {
            SysLib.cerr("Read and write block sizes differ\n");
        }
        //Print formatted results
        resultCheck();

    }

    /**
     * Provides a localized access test which does reads and writes
     * in a small sector of the disk with either the cache enabled or disabled.
     */
    public void localizedAccessTest()
    {
        label = "Localized Access Test";
        byte[] writeBlock = new byte[blockSize];
        new Random().nextBytes(writeBlock);
        //Start the time for block writes
        blockWriteStart = getTime();
        for (int i = 0; i < arraySize; i++)
        {
          //We only use 20 here to represent a small sector of the disk
            for (int j = 0; j < blockSector; j++)
            {
                write(j, writeBlock);
            }
        }
        //End time for block writes
        blockWriteEnd = getTime();

        byte[] readBlock = new byte[blockSize];
        //Start time for block reads
        blockReadStart = getTime();
        for (int i = 0; i < arraySize; i++)
        {
            for (int j = 0; j < blockSector; j++){
                read(j, readBlock);
            }
        }
        //End time for block reads
        blockReadEnd = getTime();
        //Check if block byte arrays are different sizes
        if(!Arrays.equals(readBlock, writeBlock))
        {
            SysLib.cerr("Read and write block sizes differ\n");
        }
        //Print formatted results
        resultCheck();
    }

    /**
     * Provides a mixed access test which does reads and writes
     * with 90% localized and 10% randomly accross the disk
     * with either the cache enabled or disabled.
     */
  public void mixedAccessTest()
  {
        label = "Mixed Access Test";
        //Fill array with random bytes
        byte[] writeBlock = new byte[blockSize];
        new Random().nextBytes(writeBlock);
        //Write and read data
        ArrayList<Integer> index = new ArrayList<>();
        int randIndex;
        blockWriteStart = getTime();
        for (int i = 0; i < arraySize; i++)
        {
            if (i >= arraySize * 0.9)
            {
                // This is the localizedAccessTest portion
                randIndex = randInt(blockSize);
            }
            else
            {
                //This is the randomAccessTest portion
                randIndex = randInt(blockSector);
            }
            index.add(randIndex);
            write(randIndex, writeBlock);

        }
        blockWriteEnd = getTime();

        //Read blocks
        byte[] readBlock = new byte[blockSize];
        blockReadStart = getTime();
        for (int i = 0; i < arraySize; i++)
        {
            read(index.get(i), readBlock);
        }
        blockReadEnd = getTime();
        //Check if block byte arrays are different sizes
        if(!Arrays.equals(readBlock, writeBlock))
        {
            SysLib.cerr("Read and write block sizes differ\n");
        }
        //Print formatted results
        resultCheck();
    }

    /**
     * Provides an adversary test which uses disk accesses with bad usage
     * of the cache.
     */
    public void adversaryAccessTest()
    {
        label = "Adversary Access Test";
        byte[] writeBlock = new byte[blockSize];
        //Write random bytes into the byte array
        new Random().nextBytes(writeBlock);
        //Start the timer for writing
        blockWriteStart = getTime();
        for (int i = 0; i < blockSector; i++)
        {
          for (int j = 0; j < blockSize; j++)
          writeBlock[j] = (byte) (j);
          for (int j = 0; j < blockSector; j++)
          {
            write(i + blockSector * j, writeBlock);
          }
        }
        //End the timer for writing
        blockWriteEnd = getTime();
        //Byte array for reading
        byte[] readBlock = new byte[blockSize];
        //Start the timer for reading
        blockReadStart = getTime();
        for (int i = 0; i < blockSector; i++)
        {
            for (int j = 0; j < blockSector; j++)
            {
              read(i + blockSector * j, readBlock);
              for (int k = 0; k < blockSize; k++)
              {
                //If the index values are incorrect, something is wrong
              	if (readBlock[k] != writeBlock[k])
                {
              		SysLib.cerr("ERROR\n");
              		SysLib.exit();
            	}
            }
          }
        }
        //End the timer for reading
        blockReadEnd = getTime();
        //Check if block byte arrays are different sizes
        if(!Arrays.equals(readBlock, writeBlock))
        {
            SysLib.cerr("Read and write block sizes differ\n");
        }
        resultCheck();
    }

    /**
     * Based on cache status of either enabled or disabled, uses cread for
     * cached reading or rawread for non cached reading.
     */
     public void read(int blockId, byte buffer[])
    {
        if (cacheStatus)
        {
            SysLib.cread(blockId, buffer);
        }
        else
        {
            SysLib.rawread(blockId, buffer);
        }
    }

    /**
    * Based on cache status of either enabled or disabled, uses cwrite for
    * cached writing or rawrite for non cached writing.
     */
     public void write(int blockId, byte buffer[])
    {
        if (cacheStatus)
        {
            SysLib.cwrite(blockId, buffer);
        }
        else
        {
            SysLib.rawwrite(blockId, buffer);
        }
    }

    /**
     * Formatted text for getting average time and showcasing results
     */
     public void resultCheck()
    {

        //Set the cache status label
        String status;
        if(cacheStatus)
          status = "Enabled";
        else
          status = "Disabled";
        //Formatted text for testing
        SysLib.cout("Test executing using: " + label + "\n");
        SysLib.cout("Caching Status: " + status + "\n");
        SysLib.cout("Average Write Time: " + avgWriteTime() +"ms\n");
        SysLib.cout("Average Read Time: " + avgReadTime() +"ms\n" );
        SysLib.cout("Execution Time: " + execTime() + "ms\n");
    }

    /**
     * Gets a random integer based on a value
     */
    public int randInt(int val)
    {
        return new Random().nextInt(val) + 1;
    }

    /**
     * Gets the average block write time
     */
    public long avgWriteTime()
    {
        return (blockWriteEnd - blockWriteStart) / arraySize;
    }

    /**
     * Gets the average block read time
     */
    public long avgReadTime()
    {
        return (blockReadEnd - blockReadStart) / arraySize;
    }

    /**
     * Gets the block execution time
     */
    public long execTime()
    {
      return (avgWriteTime() + avgReadTime());
    }

    /**
     * Gets the current time using a Date object
     */
    public long getTime()
    {
        return new Date().getTime();
    }
  }
