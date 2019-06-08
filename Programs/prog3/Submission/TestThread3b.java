/**
 * Author: Daniel Yan
 * Date: 5/12/2019
 * Purpose: Part of the tests for Test3.java, primarily focused on disk writes
 * and disk reads
 */
import java.util.Date;

class TestThread3b extends Thread {

	private byte[] block;								//init a byte array

    public TestThread3b ( )
     {

     }

    public void run( )
    {
        long startTime = new Date().getTime();
        SysLib.cout("Starting disk writes and reads ... \n");
    		block = new byte[512];						//initialize a block
    		for (int i = 0; i < 100; i++)
        {
    			SysLib.rawwrite(i, block);				//write to disk
    			SysLib.rawread(i, block);					//read from disk
    		}
        long endTime = new Date().getTime();
        SysLib.cout("Disk Elapsed Time: " + (endTime - startTime) + "ms \n");
    		SysLib.exit( );
    }
}
