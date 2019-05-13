/**
 * Author: Daniel Yan
 * Date: 5/12/2019
 * Purpose: Mimic the given Test3.class to provide a computation and disk
 * writes to determine different performance in kernel versions of old and
 * the modified one
 */
import java.util.Date;
import java.util.Random;

public class Test3 extends Thread
{
    private int pairNum, countThread = 0;
    private long startTime, endTime;

    /**
     * No args constructor that generates a random X pair of threads
     * that is within X = ~ 3 -4
     */
    public Test3()
    {
      pairNum = new Random().nextInt(4) + 1;
    }

    /**
     * Constructor for Test3 that generate X pairs of threads
     * @param args the input where args[0] is the X
     */
    public Test3(String[] args)
    {
        pairNum = Integer.parseInt(args[0]);
        // if(pairNum >= 1 && pairNum <= 4)
        // //run void method as the entered value is valid
        // {
        // }
        // else
        // {
        //   SysLib.cerr("X - pairs may only be between ~1-4\n Exiting...");
        //   System.exit(0);
        // }
    }

    /**
     * Run method to execute TestThread3a and TestThread3b
     */
    public void run()
    {
        // Get the starting time when executing
        startTime = new Date().getTime();
        //TestThread3a has to due with computational calculations
        String[] computations = SysLib.stringToArgs("TestThread3a");
        //TestThread3b has to do with disk writes
        String[] diskWrites = SysLib.stringToArgs("TestThread3b");
        for (int i = 0; i < pairNum; i++)
        {
            SysLib.exec(computations);
            SysLib.exec(diskWrites);

        }
        SysLib.cout("Running Executions...please wait\n");
        // Terminate all threads, which is equivalent to twice the pairNum
        for (int i = 0; i < pairNum * 2; i++)
        {
          SysLib.join();
        }


            SysLib.cout("Finished executing threads...\n");
            // Get the ending time
            endTime = new Date().getTime();
            // Calculate the elapsed time in ms
            SysLib.cout("The total elapsed time is " + (endTime - startTime) + " ms" + "\n");
            SysLib.exit();
    }


}
