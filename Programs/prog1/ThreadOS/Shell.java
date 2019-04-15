import java.util.*;
/*
Author: Daniel Yan
Program: Shell.java
Purpose: Shell program built to load programs in sequential or concurrent
processes. Uses a HashSet as part of its implementation (unordered set)
See https://stackoverflow.com/questions/5139724/whats-the-difference-between-hashset-and-set
for more clarification on HashSet usage
*/

class Shell2 extends Thread
{
   //Constructor for Shell
   public Shell2()
   {
   }

   //Required run method for Thread
   //Run the actual Shell
   public void run()
   {
     //Keep count of number of runs
     int runCount = 1;
     //HashSet (unordered) made to count current processes
     Set<Integer> currentProcesses = new HashSet<Integer>();
     //While loop to run for any arbitrary number of arguments
     while(true)
     {
      //Print out shell number of statements
      SysLib.cout("shell[" + runCount + "]% ");
    	StringBuffer buffer = new StringBuffer(); //Buffer to read
    	SysLib.cin(buffer); //Buffer input
      if(buffer.toString().equals("exit"))
      {
        SysLib.cout("exit\n");
        break; //Exit loop
      }
      //Ignore empty buffers, check error cases of only a ";" or "&"
      else if (!buffer.toString().isEmpty()
            || !buffer.toString().equals(";")
            || !buffer.toString().equals("&"))
      {
        runCount++; //Increment number of runs
        //For each command that is made before a semi-colon
        for(String commands : buffer.toString().split(";"))
        {
          int currentProcess = 0;
          for(String parallelCommands : commands.split("&"))
          {
            String[] args = SysLib.stringToArgs(parallelCommands);
            //Check length validity
            if(args.length < 0)
            {
              SysLib.cout("Error found in length of command\n");
              break;
            }
            else if(args.length > 0)
            {
              //Display command run
              SysLib.cout(args[0] + "\n");
              //Execute the current process
              currentProcess = SysLib.exec(args);
              //Check if there was an error in execution
              if(currentProcess < 0)
              {
                SysLib.cout("Error found in executing argument\n");
                break;
              }
              //Process execution was successful
              else
              {
                //Add current process to unordered set
                currentProcesses.add(currentProcess);
              }
            }
          } //End of "&" for each loop
          //If the HashSet contains items, remove them
          while(!currentProcesses.isEmpty())
          {
            //Wait for child threads to terminate
            currentProcess = SysLib.join();
            //Check the status of child termination
            if(currentProcess < 0)
            {
              SysLib.cout("Error found in child termination process\n");
              break;
            }
            //SysLib.join() was run successfully
            else
            {
              //Check if current HashSet contains process, if so remove it
              if(currentProcesses.contains(currentProcess))
              {
                currentProcesses.remove(currentProcess);
              }
            }
          }
        } //End of ";"" for each Loop
      }
    } //End of while loop
    //Exit the method
    SysLib.exit();
    return;
  }
}
