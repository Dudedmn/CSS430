/**
 * Author: Daniel Yan
 * Date: 5/10/2019
 * Synchronized queue
 */
import java.util.Vector;

/**
 * Used for SyncQueue for holding a thread inside a queue and waking it or
 * sleeping when necessary.
 */
public class QueueNode {

  private Vector<Integer> queueNode;

  /**
   * Constructs a new queueNode
   */
  public QueueNode()
  {
          queueNode.clear();
          queueNode = new Vector<>();
  }

  /**
   *Places a thread to sleep using wait()
   * @return Status of removal from the queueNode
   */
  public synchronized int sleep( )
  {
          if(queueNode.isEmpty())
          {
                  try
                  {
                          wait( );

                  }
                  //Error catching for issue in sleep()
                  catch ( InterruptedException ie )
                  {
                          SysLib.cerr("Error in sleep() for QueueNode"); //an error has occured
                          SysLib.cerr(ie.toString() + "\n"); //an error has occured
                  }
                  return queueNode.remove(0);
          }
          return -1;
  }

  /**
   * Wakes a thread by enqueuing it into the queueNode.
   * Uses synchronized to provide threadsafe execution
   * @param tid TID of the  enqueued queueNode
   */
  public synchronized void wake(int tid)
  {
          queueNode.add(tid);
          notify();
  }
}
