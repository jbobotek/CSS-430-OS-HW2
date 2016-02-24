/** Scheduler.java
 * Created by Jeremy on 2/5/2016.
 *
 * Scheduler is ThreadOS's thread manager, it provides a queue0 and schedule for the current threads
 */

import java.util.Vector;

public class Scheduler extends Thread { //
    private Vector queue0;
    private Vector queue1;
    private Vector queue2;
    private int timeSlice;
    private int zeroSlice;
    private int twoSlice;
    private static final int DEFAULT_TIME_SLICE = 1000;
    private boolean[] tids;
    private static final int DEFAULT_MAX_THREADS = 10000;
    private int nextId = 0;

    private void initTid(int threadCount) {
        this.tids = new boolean[threadCount];

        for(int var2 = 0; var2 < threadCount; ++var2) {
            this.tids[var2] = false;
        }

    }

    private int getNewTid() {
        for(int var1 = 0; var1 < this.tids.length; ++var1) {
            int var2 = (this.nextId + var1) % this.tids.length;
            if(!this.tids[var2]) {
                this.tids[var2] = true;
                this.nextId = (var2 + 1) % this.tids.length;
                return var2;
            }
        }

        return -1;
    }

    private boolean returnTid(int var1) {
        if(var1 >= 0 && var1 < this.tids.length && this.tids[var1]) {
            this.tids[var1] = false;
            return true;
        } else {
            return false;
        }
    }

    // getMyTcb
    // Modified: finds the current thread's TCB from one of the active thread queues and returns it.
    public TCB getMyTcb() {
        Thread current = Thread.currentThread();

        synchronized(this.queue0) {
            for(int i = 0; i < this.queue0.size(); ++i) {
                TCB temp = (TCB)this.queue0.elementAt(i);
                Thread dummy = temp.getThread();
                if(dummy == current) {
                    return temp;
                }
            }
        }
        synchronized(this.queue1) {
            for(int i = 0; i < this.queue1.size(); ++i) {
                TCB temp = (TCB)this.queue1.elementAt(i);
                Thread dummy = temp.getThread();
                if(dummy == current) {
                    return temp;
                }
            }
        }
        synchronized(this.queue2) {
            for(int i = 0; i < this.queue2.size(); ++i) {
                TCB temp = (TCB)this.queue2.elementAt(i);
                Thread dummy = temp.getThread();
                if(dummy == current) {
                    return temp;
                }
            }
        }
        return null;
    }

    public int getMaxThreads() {
        return this.tids.length;
    }

    public Scheduler() {
        this.timeSlice = DEFAULT_TIME_SLICE;
        this.zeroSlice = DEFAULT_TIME_SLICE/2;
        this.twoSlice = DEFAULT_TIME_SLICE*2;
        this.queue0 = new Vector();
        this.queue1 = new Vector();
        this.queue2 = new Vector();
        this.initTid(DEFAULT_MAX_THREADS);
    }

    public Scheduler(int aQuantum) {
        this.timeSlice = aQuantum;
        this.zeroSlice = aQuantum/2;
        this.twoSlice = aQuantum*2;
        this.queue0 = new Vector();
        this.queue1 = new Vector();
        this.queue2 = new Vector();
        this.initTid(DEFAULT_MAX_THREADS);
    }

    public Scheduler(int aQuantum, int threadCount) {
        this.timeSlice = aQuantum;
        this.zeroSlice = aQuantum/2;
        this.twoSlice = aQuantum*2;
        this.queue0 = new Vector();
        this.queue1 = new Vector();
        this.queue2 = new Vector();
        this.initTid(threadCount);
    }

    private void schedulerSleep(int time) {
        try {
            Thread.sleep((long)time);
        } catch (InterruptedException var2) {
            ;
        }

    }

    // addThread
    // Modified: allocates a new TCB to this thread t and adds the TCB to the active thread queue.
    // This new TCB receives the calling thread's id as its parent id.
    public TCB addThread(Thread t) {
        //t.setPriority(2);// new threads start out at top of queue

        TCB tTCB = this.getMyTcb();
        int pid = tTCB != null?tTCB.getTid():-1;
        int tid = this.getNewTid();
        if(tid == -1) {
            return null;
        } else {
            TCB newTCB = new TCB(t, tid, pid);
            /*if(tTCB != null) {
                for(int var6 = 0; var6 < 32; ++var6) {
                    newTCB.ftEnt[var6] = tTCB.ftEnt[var6];
                    if(newTCB.ftEnt[var6] != null) {
                        ++newTCB.ftEnt[var6].count;
                    }
                }
            }*/

            this.queue0.add(newTCB);
            return newTCB;
        }
    }

    public boolean deleteThread( ) {
        TCB tcb = getMyTcb( );
        if ( tcb!= null )
            return tcb.setTerminated( );
        else
            return false;
    }

    public void sleepThread(int aQuantum) {
        try {
            sleep((long)aQuantum);
        } catch (InterruptedException var3) {
            ;
        }

    }

    public void run() {
        Thread current = null;
        //this.setPriority(6);

        while(true) {
            while(true) {
                while(true) {
                    try {
                        // Handle Threads in queue0 first
                        if(this.queue0.size() != 0) {
                            TCB first = (TCB)this.queue0.firstElement();

                            //check for end of life status
                            if(!first.getTerminated()) {
                                current = first.getThread();

                                if(current != null) {
                                    //if already started
                                    if(current.isAlive()) {
                                        //resume
                                        current.resume();//current.setPriority(2);
                                    } else {//not started yet
                                        current.start();
                                        //current.setPriority(2);
                                    }
                                }

                                //wait half quantum
                                this.schedulerSleep(this.zeroSlice);

                                synchronized(this.queue0) {
                                    if(current != null && current.isAlive()) {
                                        current.suspend();//current.setPriority(4);//Thread taking too long, suspend
                                    }
                                    //Thread has taken too long, move to lower queue
                                    this.queue0.remove(first);
                                    this.queue1.add(first);
                                }
                            } else {//Thread has completed, remove and return
                                this.queue0.remove(first);
                                this.returnTid(first.getTid());
                            }
                        }
                        else if (this.queue1.size() != 0){//queue1 lower priority than queue0
                            TCB first = (TCB)this.queue1.firstElement();
                            if(!first.getTerminated()) {
                                current = first.getThread();
                                if(current != null) {
                                    if (current.isAlive()) {
                                        current.resume();//current.setPriority(2);
                                    } else {
                                        current.start();
                                        //current.setPriority(2);
                                    }
                                }

                                //check every half quantum to see if queue0 !empty
                                this.schedulerSleep(this.zeroSlice);

                                if(this.queue0.size() != 0){
                                    current.suspend();//current.setPriority(4);//suspend Thread if queue0 !empty
                                    continue;
                                }

                                //second round
                                this.schedulerSleep(this.zeroSlice);

                                if(this.queue0.size() != 0){
                                    current.suspend();//current.setPriority(4);
                                    continue;
                                }

                                synchronized(this.queue1) {//Thread has taken too long
                                    if(current != null && current.isAlive()) {
                                        current.suspend();//current.setPriority(4);//suspend
                                    }

                                    //move Thread to lower priority queue
                                    this.queue1.remove(first);
                                    this.queue2.add(first);
                                }
                            } else {
                                this.queue1.remove(first);
                                this.returnTid(first.getTid());
                            }
                        }
                        else if (this.queue2.size() != 0){//lower priority than queue0 and queue1
                            TCB first = (TCB)this.queue2.firstElement();
                            if(!first.getTerminated()) {
                                current = first.getThread();
                                if(current != null) {
                                    if(current.isAlive()) {
                                        current.resume();//current.setPriority(2);
                                    } else {
                                        current.start();
                                        //current.setPriority(2);
                                    }
                                }

                                //check if queue0 and queue1 are !empty every half quantum
                                for(int x = 0; x < 4; x++){
                                    this.schedulerSleep(this.zeroSlice);

                                    if(this.queue0.size() != 0){
                                        current.suspend();//current.setPriority(4);//suspend if queue !empty
                                        continue;
                                    }

                                    if(this.queue1.size() != 0){
                                        current.suspend();//current.setPriority(4);//suspend if queue !empty
                                        continue;
                                    }
                                }

                                synchronized(this.queue2) {
                                    if(current != null && current.isAlive()) {
                                        current.suspend();//current.setPriority(4);//thread taken too long, suspend
                                    }

                                    this.queue2.remove(first);
                                    this.queue2.add(first);
                                }
                            } else {
                                this.queue2.remove(first);
                                this.returnTid(first.getTid());
                            }
                        }
                    } catch (NullPointerException var6) {
                        ;
                    }
                }
            }
        }
    }
}

