/**
 * Author: Daniel Yan
 * Date: 5/10/2019
 * Synchronized queue
 */
 /**
  * Synchronized Queue class made to provide a threadsafe environment for
  * threads that are being held through a queue.
 */
public class SyncQueue {
    //Queue made from QueueNode
    private QueueNode[] queueNode;
    //Default Thread ID (TID)
    private static int DEFAULT_TID = 0;
    //Default condition number
    private static int DEFAULT_COND_NUM = 10;

    /**
     * Creates a queue and allows threads to wait for
     * the default condition number
     */
    public SyncQueue()
    {
        queueNode = new QueueNode[DEFAULT_COND_NUM];
        //Initializes by creating QueueNode for each element in the queue array
        for(int i = 0; i < DEFAULT_COND_NUM; i++)
        {
            queueNode[i] = new QueueNode();

        }
    }

    /**
     * Creates a queue and allow threads to wait for a default condition number
     * or a condMax number of condition/event types.
     * @param condMax Defines the size or the number of queues
     */
    public SyncQueue(int condMax)
    {
        queueNode = new QueueNode[condMax];
        for(int i = 0; i < condMax; i++)
        {
            queueNode[i] = new QueueNode();
        }
    }

    /**
     * Calling thread is sleeped until the inputted condition is satisfied
     * @param condition position at which we sleep
     * @return status of sleep()
     */
    public int enqueueAndSleep(int condition)
    {
      //Check valid condition
        if(condition >= 0 && condition < queueNode.length)
                return queueNode[condition].sleep();
        return -1;
    }

    /**
     * Dequeues and wakes up a thread waiting for a given condition.
     * The FCFS (first-come-first-service) order does not matter.
     * @param condition condition at which the thread is woken up
     */
    public void dequeueAndWakeup(int condition)
    {
        if(condition >= 0 && condition < queueNode.length)
        {
            //DEFAULT_TID == 0
            queueNode[condition].wake(DEFAULT_TID);
        }
    }

    /**
     * Dequeues and wakes up a thread waiting for a given condition.
     * If there are more than one thread waiting for the same condition,
     * only one thread is dequeued and resumed.
     *  The FCFS (first-come-first-service) order does not matter.
     * @param condition condition at which the thread is woken up
     * @param tid the TID that will be woken
     */
    public void dequeueAndWakeup(int condition, int tid)
    {
      //Ensure condition is positive for validity
        if(condition >= 0 && condition < queueNode.length )
        {
            //Wake up the corresponding thread that isn't default
              queueNode[condition].wake(tid);
        }
    }
}
