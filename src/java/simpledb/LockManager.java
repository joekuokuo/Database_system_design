
package simpledb;

import java.util.ArrayList;
import java.util.Map;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
public class LockManager {


    int sharedLock;
    Map<PageId, List<Lock>> pageLock;

    Map<TransactionId, PageId> blockMap;



    public LockManager() {
        pageLock = new ConcurrentHashMap<>();

    }
    // Read Lock
    public synchronized boolean acquireSharedLock(TransactionId tid, PageId pid){
        ArrayList<Lock> lockList = (ArrayList)pageLock.get(pid);
        if (lockList != null && lockList.size() != 0){
            if(lockList.size() == 1){
                Lock lock = lockList.get(0);
                if(lock.tid == tid){
                    return lock.lockType == Locks.SharedLock || lockPage(tid, pid, Permissions.READ_ONLY);
                }
                else {
                    return lock.lockType == Locks.SharedLock ? lockPage(tid, pid, Permissions.READ_ONLY) : block(tid, pid);
                }
            }

            else {
                for (Lock lock : lockList){
                    if(lock.lockType == Locks.ExclusiveLock){
                        return lock.tid.equals(tid) || block(tid, pid);

                    }
                    else if (lock.tid == tid){
                        return true;
                    }
                }
            }
            lockPage(tid, pid, Permissions.READ_ONLY);

        }
        else {
            lockPage(tid, pid, Permissions.READ_ONLY);
        }

        return false;
    }

    private boolean block(TransactionId tid, PageId pid){
        blockMap.put(tid, pid);
        return false;
    }


    private boolean lockPage(TransactionId tid, PageId pid, Permissions perm){

        return true;
    }

    public synchronized void holdSharedLock(){

    }


    public synchronized boolean releaseSharedLock(){

        return true;
    }

    public synchronized boolean getSharedLock(){

        return false;
    }




    // Write Lock
    public synchronized boolean acquireExclusiveLock(TransactionId tid, PageId pid){



        return true;

    }
    public synchronized void holdExclusiveLock(){

    }


    public synchronized boolean releaseExclusiveLock(){

        return true;
    }

    public synchronized boolean getLock(){

        return false;
    }


    private enum Locks{
        NoLock, SharedLock, ExclusiveLock;
    }

    private class Lock{
        private TransactionId tid;
        private Locks lockType;

        public Lock(TransactionId tid, Permissions perm){
            this.tid = tid;
            lockType = Locks.NoLock;

        }

        public TransactionId getTid(){
            return tid;
        }

        public Locks getLock(){
            return lockType;
        }


    }
}
