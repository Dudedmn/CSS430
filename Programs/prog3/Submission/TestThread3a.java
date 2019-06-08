/**
 * Author: Daniel Yan
 * Date: 5/12/2019
 * Purpose: Part of the tests for Test3, primarily focusing on computation
 */

import java.util.Date;
//import java.lang.*;

class TestThread3a extends Thread
{

    public TestThread3a ( )
    {

    }
    //CPU waste run
    public void run( )
     {
       long startTime = new Date().getTime();

        SysLib.cout("Starting computations ... \n");
        long cpuWaste = 0;
      	for(int j = 0; j < factorial(13); j++)
        {
            cpuWaste += j;
      	}
        //Exit program
        long endTime = new Date().getTime();
        SysLib.cout("Computation Elapsed Time: " + (endTime - startTime) + "ms \n");
  		  SysLib.exit( );
    }
    private int factorial(int num){
    	if(num == 1)
       return 1;
      return factorial(num - 1) * num;
    }
}
