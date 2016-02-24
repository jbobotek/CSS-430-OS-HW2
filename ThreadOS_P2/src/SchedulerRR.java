/** SchedulerRR.java
 * Created by Jeremy on 2/5/2016.
 *
 * Scheduler is ThreadOS's thread manager, it provides a queue and schedule for the current threads
 */

import java.util.Vector;

public class Scheduler extends Thread { //
    private Vector queue;
    private int timeSlice;
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

    public TCB getMyTcb() {
        Thread var1 = Thread.currentThread();
        Vector var2 = this.queue;
        synchronized(this.queue) {
            for(int var3 = 0; var3 < this.queue.size(); ++var3) {
                TCB var4 = (TCB)this.queue.elementAt(var3);
                Thread var5 = var4.getThread();
                if(var5 == var1) {
                    return var4;
                }
            }

            return null;
        }
    }

    public int getMaxThreads() {
        return this.tids.length;
    }

    public Scheduler() {
        this.timeSlice = DEFAULT_TIME_SLICE;
        this.queue = new Vector();
        this.initTid(DEFAULT_MAX_THREADS);
    }

    public Scheduler(int aQuantum) {
        this.timeSlice = aQuantum;
        this.queue = new Vector();
        this.initTid(DEFAULT_MAX_THREADS);
    }

    public Scheduler(int aQuantum, int threadCount) {
        this.timeSlice = aQuantum;
        this.queue = new Vector();
        this.initTid(threadCount);
    }

    private void schedulerSleep() {
        try {
            Thread.sleep((long)this.timeSlice);
        } catch (InterruptedException var2) {
            ;
        }

    }

    public TCB addThread(Thread var1) {
        //var1.setPriority(2);
        TCB var2 = this.getMyTcb();
        int var3 = var2 != null?var2.getTid():-1;
        int var4 = this.getNewTid();
        if(var4 == -1) {
            return null;
        } else {
            TCB var5 = new TCB(var1, var4, var3);
            if(var2 != null) {
                for(int var6 = 0; var6 < 32; ++var6) {
                    var5.ftEnt[var6] = var2.ftEnt[var6];
                    if(var5.ftEnt[var6] != null) {
                        ++var5.ftEnt[var6].count;
                    }
                }
            }

            this.queue.add(var5);
            return var5;
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
                        if(this.queue.size() != 0) {
                            TCB var2 = (TCB)this.queue.firstElement();
                            if(!var2.getTerminated()) {
                                current = var2.getThread();
                                if(current != null) {
                                    if(current.isAlive()) {
                                        current.resume(); //var1.setPriority(4);
                                    } else {
                                        current.start();
                                        //current.setPriority(4);
                                    }
                                }

                                this.schedulerSleep();
                                Vector var3 = this.queue;
                                synchronized(this.queue) {
                                    if(current != null && current.isAlive()) {
                                       current.suspend(); //current.setPriority(2);
                                    }

                                    this.queue.remove(var2);
                                    this.queue.add(var2);
                                }
                            } else {
                                this.queue.remove(var2);
                                this.returnTid(var2.getTid());
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

