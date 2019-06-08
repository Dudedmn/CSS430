/*
Multi Feedback Queue Scheduling (MFQS) with RR for queues implementation
for scheduler. CPU burst times are calculated in milliseconds.
Author: Daniel Yan
Date: 4/27/2019
*/
import java.util.*;

public class Scheduler extends Thread
{
//Static default values for max threads and time quantum slices
private static final int DEFAULT_MAX_THREADS = 10000;
private static final int DEFAULT_TIME_SLICE = 1000;
//Different queues with different time quantums for MFQS
private Vector queueZero;     //Time quantum = 500ms or timeSlice/2
private Vector queueOne;     //Time quantum = 1000 ms or timeslice
private Vector queueTwo;     //Time quantum = 2000 ms or timeslice * 2

//Value to indicate time quantum slices for RR
private int timeSlice;
// Indicate which ids have been used
private boolean[] tids;
// Allocate an ID array, each element indicating if that id has been used
private int nextId = 0;

/*
 * Default constructor for MFQS Scheduler. Sets the time quantum slices
 * to the given default value of 1000ms. Max thread size is 10000.
*/
public Scheduler( )
{
        timeSlice = DEFAULT_TIME_SLICE;
        //Initialize needed queues
        queueZero = new Vector( );
        queueOne = new Vector( );
        queueTwo = new Vector( );
        initTid( DEFAULT_MAX_THREADS );
}

/*
 * Specified quantum constructor for MFQS Scheduler. Sets the time quantum
 * slices to given value. Max thread size is 10000 still.
*/
public Scheduler( int quantum )
{
        timeSlice = quantum;
        queueZero = new Vector( );
        queueOne = new Vector( );
        queueTwo = new Vector( );
        initTid( DEFAULT_MAX_THREADS );
}

/*
 * Further specified constructor for MFQS Scheduler. Sets the time quantum
 * slices and max threads to the given values.
*/
public Scheduler( int quantum, int maxThreads )
{
        timeSlice = quantum;
        queueZero = new Vector( );
        queueOne = new Vector( );
        queueTwo = new Vector( );
        initTid( maxThreads );
}

/*
 * Initialize tids[] array to flag if a thread is working.
*/
private void initTid( int maxThreads )
{
        tids = new boolean[maxThreads];
        for ( int i = 0; i < maxThreads; i++ )
                tids[i] = false;
}

/*
 * Searches for an available threadID and provides the ID for it if available.
*/
private int getNewTid( )
{
        for ( int i = 0; i < tids.length; i++ )
        {
                int tentative = ( nextId + i ) % tids.length;
                if ( tids[tentative] == false )
                {
                        tids[tentative] = true;
                        nextId = ( tentative + 1 ) % tids.length;
                        return tentative;
                }
        }
        return -1;
}

/*
 * Return the thread ID and set the corresponding tids element to be unused.
*/
private boolean returnTid( int tid )
{
        if ( tid >= 0 && tid < tids.length && tids[tid] == true )
        {
                tids[tid] = false;
                return true;
        }
        return false;
}

/*
 * Retrieve the current Thread Control Block (TCB) from the queue.
*/
public TCB getMyTcb( )
{
        Thread myThread = Thread.currentThread( ); // Get my thread object
        //Traverse in queueZero
        synchronized( queueZero )
        {
                for ( int i = 0; i < queueZero.size( ); i++ )
                {
                        TCB tcb = (TCB) queueZero.elementAt( i );
                        Thread thread = tcb.getThread( );
                        if ( thread == myThread ) // if this is my TCB, return it
                                return tcb;
                }
        }
        //Traverse in queueOne
        synchronized( queueOne )
        {
                for ( int i = 0; i < queueOne.size( ); i++ )
                {
                        TCB tcb = (TCB) queueOne.elementAt( i );
                        Thread thread = tcb.getThread( );
                        if ( thread == myThread ) // if this is my TCB, return it
                                return tcb;
                }
        }
        //Traverse in queueTwo
        synchronized( queueTwo )
        {
                for ( int i = 0; i < queueTwo.size( ); i++ )
                {
                        TCB tcb = (TCB) queueTwo.elementAt( i );
                        Thread thread = tcb.getThread( );
                        if ( thread == myThread ) // if this is my TCB, return it
                                return tcb;
                }
        }
        return null;
}

/*
 * Get maximum number of threads the Scheduler can hold.
*/
public int getMaxThreads( )
{
        return tids.length;
}

/*
 * Sleep the scheduler according to the specified queue.
*/
private void schedulerSleep( )
{
        try {
                //If queueZero is not empty sleep it for 500ms
                if(!queueZero.isEmpty())
                {
                        Thread.sleep(timeSlice/2);
                }
                //If queueOne is not empty sleep it for 1000ms
                else if(!queueOne.isEmpty())
                {
                        Thread.sleep(timeSlice);
                }
                //If queueZero and queueOne is empty sleep queueTwo for 2000ms
                else
                {
                        Thread.sleep(timeSlice * 2);
                }
        }
        catch ( InterruptedException e )
        {
        }
}

/*
 * Add TCB to queueZero for future execution.
*/
public TCB addThread( Thread t )
{
        TCB parentTcb = getMyTcb( ); // get my TCB and find my TID
        int pid = ( parentTcb != null ) ? parentTcb.getTid( ) : -1;
        int tid = getNewTid( ); // get a new TID
        if ( tid == -1)
                return null;
        TCB tcb = new TCB( t, tid, pid ); // create a new TCB
        //New initial threads are always added to queueZero
        queueZero.add( tcb );
        return tcb;
}

/*
 * Get the current TCB that runs and terminate it.
*/
public boolean deleteThread( )
{
        TCB tcb = getMyTcb( );
        if ( tcb!= null )
                return tcb.setTerminated( );
        else
                return false;
}

/*
 * Sleep the Scheduler for the specified amount of milliseconds.
*/
public void sleepThread( int milliseconds )
{
        try {
                sleep( milliseconds );
        }
        catch ( InterruptedException e )
        {
        }
}

/*
 * Execute all TCBs for all queues. If the time quantum allotted in one queue
 * is not enough, the TCB will be moved to the next available quueu.
*/
public void run( )
{
    Thread current = null;
    while ( true )
    {
        try
        {
            //If all queues are empty, keep waiting
            if (queueZero.isEmpty() && queueOne.isEmpty()
                && queueTwo.isEmpty())
                    continue;
            if(!queueZero.isEmpty())
            {

                // get the next TCB and its thread
                TCB currentTCB = (TCB) queueZero.firstElement( );
                if ( currentTCB.getTerminated( ) == true )
                {
                        queueZero.remove( currentTCB );
                        returnTid( currentTCB.getTid( ) );
                        continue;
                }
                current = currentTCB.getThread( );
                if ( current != null )
                {
                        if ( current.isAlive( ) )
                                current.resume();
                        else
                        {
                                // Spawn must be controlled by Scheduler
                                // Scheduler must start a new thread
                                current.start( );
                        }
                }

                schedulerSleep( );

                synchronized ( queueZero ) {
                        if ( current != null && current.isAlive( ) )
                                current.suspend();
                        queueZero.remove( currentTCB ); // rotate this TCB to the end
                        queueOne.add( currentTCB );
                }
            }
            else if (!queueOne.isEmpty())
            {

                // get the next TCB and its thread
                TCB currentTCB = (TCB) queueOne.firstElement( );
                if ( currentTCB.getTerminated( ) == true ) {
                        queueOne.remove( currentTCB );
                        returnTid( currentTCB.getTid( ) );
                        continue;
                }
                current = currentTCB.getThread( );
                if ( current != null )
                {
                        if ( current.isAlive( ) )
                                current.resume();
                        else
                        {
                                // Spawn must be controlled by Scheduler
                                // Scheduler must start a new thread
                                current.start( );
                        }
                }

                schedulerSleep( );

                synchronized ( queueOne ) {
                        if ( current != null && current.isAlive( ) )
                                current.suspend();
                        queueOne.remove( currentTCB ); // rotate this TCB to the end
                        queueTwo.add( currentTCB );
                }
            }
            else
            {

                // get the next TCB and its thread
                TCB currentTCB = (TCB) queueTwo.firstElement( );
                if ( currentTCB.getTerminated( ) == true )
                {
                        queueTwo.remove( currentTCB );
                        returnTid( currentTCB.getTid( ) );
                        continue;
                }
                current = currentTCB.getThread( );
                if ( current != null )
                {
                        if ( current.isAlive( ) )
                                current.resume();
                        else {
                                // Spawn must be controlled by Scheduler
                                // Scheduler must start a new thread
                                current.start( );
                        }
                }

                schedulerSleep( );

                synchronized ( queueTwo ) {
                        if ( current != null && current.isAlive( ) )
                                current.suspend();
                        queueTwo.remove( currentTCB ); // rotate this TCB to the end
                        queueTwo.add( currentTCB );
                }
            }
        }
        catch ( NullPointerException e3 )
        {
        };
    }
}
}
