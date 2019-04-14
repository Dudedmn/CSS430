import java.util.*;

class Shell2 extends Thread
{
   //command line string to contain the full command
   private String cmdLine;

   // constructor for shell
   public Shell2( ) {
      cmdLine = "";
   }

   // required run method for this Shell Thread
   public void run( ) {

      // build a simple command that invokes PingPong 
      cmdLine = "PingPong abc 100";

      // must have an array of arguments to pass to exec()
	SysLib.cout("Shell2 is starting\n");
	StringBuffer buffer = new StringBuffer();
	SysLib.cin(buffer);
      String[] args = SysLib.stringToArgs(buffer.toString());
      SysLib.cout("Your command:\t" + buffer.toString());

      // run the command
      int tid = SysLib.exec( args );
      SysLib.cout("Started Thread tid=" + tid + "\n");

      // wait for completion then exit back to ThreadOS
      SysLib.join();
      SysLib.cout("Done!\n");
      SysLib.exit();
   }
}

