package simpledb;

import java.util.ArrayList;
import java.util.Map;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
public class LockManager {


    private Map<PageId, List<Lock>> pageLock;
    private Map<TransactionId, PageId> blockMap;

    public LockManager() {
        // Avoid concurrent issues
        pageLock = new ConcurrentHashMap<>();
        blockMap = new ConcurrentHashMap<>();
    }

    // Read Lock
    public synchronized boolean acquireSharedLock(TransactionId tid, PageId pid){
        ArrayList<Lock> lockList = (ArrayList<Lock>)pageLock.get(pid);

        if (lockList != null && lockList.size() != 0){
            // if only one lock in the list
            if(lockList.size() == 1){
                Lock lock = lockList.get(0);
                if(lock.tid != tid){
                    return lock.lockType == Locks.SharedLock ? lockPage(tid, pid, Permissions.READ_ONLY) : block(tid, pid);
                }
                else {
                    return true;
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
            return lockPage(tid, pid, Permissions.READ_ONLY);

        }

        // No lock case
        else {
            return lockPage(tid, pid, Permissions.READ_ONLY);
        }

    }

    // Write Lock
    public synchronized boolean acquireExclusiveLock(TransactionId tid, PageId pid){

        ArrayList<Lock> lockList = (ArrayList<Lock>)pageLock.get(pid);

        if (lockList != null && lockList.size() != 0){
            // if only one lock in the list
            if(lockList.size() == 1){
                Lock lock = lockList.get(0);
                return lock.tid == tid ? lock.lockType == Locks.ExclusiveLock || lockPage(tid, pid, Permissions.READ_WRITE) : block(tid, pid);
            }

            // if only two lock in the list
            else if (lockList.size() == 2){
                for (Lock lock : lockList){
                    if(lock.tid == tid && lock.lockType == Locks.ExclusiveLock){
                        return true;
                    }
                }

                return block(tid, pid);
            }

            else{
                return block(tid, pid);
            }
        }

        // No lock case
        else {
            return lockPage(tid, pid, Permissions.READ_WRITE);
        }

    }
    private synchronized boolean block(TransactionId tid, PageId pid){
        // block through TransactionId & PageId
        blockMap.put(tid, pid);
        return false;
    }


    private synchronized boolean lockPage(TransactionId tid, PageId pid, Permissions perm){
        Lock newlock = new Lock(tid, perm);
        ArrayList<Lock> lockList = (ArrayList<Lock>)pageLock.get(pid);
        if(lockList == null){
            lockList = new ArrayList<>();
        }
        lockList.add(newlock);
        pageLock.put(pid, lockList);

        if (blockMap.containsKey(tid)){
            blockMap.remove(tid);
        }
        return true;
    }

    public synchronized boolean unlock(TransactionId tid, PageId pid){
        // check on a specific pageId
        ArrayList<Lock> newlockList = (ArrayList<Lock>)pageLock.get(pid);

        if(newlockList == null || newlockList.size() == 0){
            return false;
        }
        // go through all the locks in a specific page to check whether a specific transaction is lock or not.
        for (Lock lock: newlockList){
            if(lock.tid == tid){
                newlockList.remove(lock);
                pageLock.put(pid, newlockList);
                return true;
            }
        }
        return false;
    }

    // different with unlock by not actually remove locks in lockList and update the pageLock.
    public synchronized boolean checklockStatus(TransactionId tid, PageId pid) {
        // check on a specific pageId
        ArrayList<Lock> lockList = (ArrayList<Lock>)pageLock.get(pid);

        if(lockList == null || lockList.size() == 0){
            return false;
        }

        // go through all the locks in a specific page to check whether a specific transaction is lock or not.
        for (Lock lock: lockList){
            if(lock.tid == tid){
                return true;
            }
        }
        return false;
    }

    public synchronized void releaseAllTransactionLocks(TransactionId tid){
        ArrayList<PageId> pids = new ArrayList<>();
        for (Map.Entry<PageId, List<Lock>> entry : pageLock.entrySet()){
            for (Lock lock : entry.getValue()){
                if(lock.tid == tid){
                    pids.add(entry.getKey());
                    // ASK TA: Why I cannot do this? ConcurrentModificationException
//                    unlock(tid, entry.getKey());
                }
            }
        }

        for (PageId pid : pids) {
            unlock(tid, pid);
        }

    }

    // Deadlock detection.
    private synchronized List<PageId> getAllLockPageByTid(TransactionId tid) {
        ArrayList<PageId> pids = new ArrayList<>();
        for (Map.Entry<PageId, List<Lock>> entry : pageLock.entrySet()) {
            for (Lock ls : entry.getValue()) {
                if (ls.tid.equals(tid)) {
                    pids.add(entry.getKey());
                }
            }
        }
        return pids;
    }

    public synchronized boolean deadlockOccurred(TransactionId tid, PageId pid) {//T1为tid，P3为pid
        List<Lock> whoseLock = pageLock.get(pid);
        if (whoseLock == null || whoseLock.size() == 0) {
            return false;
        }
        List<PageId> pids = getAllLockPageByTid(tid); // get all pageId locked by tid

        // for all locks in the pageId
        for (Lock lock : whoseLock) {
            TransactionId lockHolder = lock.tid;

            // if the lockHolder's tid is not the tid in whoseLock
            if (!lockHolder.equals(tid)) {

                // determine whether the lockHolder is waiting for pid or not.
                boolean isWaiting = isWaitingResources(lockHolder, pids, tid);
                if (isWaiting) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Determine whether tid is directly or indirectly waiting for some resources in pids
     *
     * @param tid
     * @param pids
     * @param tidToRemove Exclude the effect by itself
     *
     * @return
     */
    private synchronized boolean isWaitingResources(TransactionId tid, List<PageId> pids, TransactionId tidToRemove) {
        // get the waiting page's PageId in blockMap.
        PageId waitingPage = blockMap.get(tid);
        if (waitingPage == null) {
            return false;
        }

        // if the PageId of the waiting page is in the pids return true.
        for (PageId pid : pids) {
            if (pid.equals(waitingPage)) {
                return true;
            }
        }

        // get the whoseLock from waitingPage PageId
        List<Lock> whoseLock = pageLock.get(waitingPage);

        // if nothing in the whoseLock, then it's waiting for no pids.
        if (whoseLock == null || whoseLock.size() == 0) return false;

        // if something in whoseLock, then check every locks in it.
        for (Lock lock : whoseLock) {
            TransactionId lockHolder = lock.tid;

            // if the lockHolder is not tidToRemove:
            if (!lockHolder.equals(tidToRemove)) {
                // need to check the lockHolder and pids and tidToRemove again to make sure
                // the lockHolder has no dependency with pids.
                boolean isWaiting = isWaitingResources(lockHolder, pids, tidToRemove);
                if (isWaiting) {
                    return true;
                }
            }
        }

        // if tid passes all the for loop, which means every lockHolder is not directly or indirectly waiting for pids.
        return false;
    }


    //

    private enum Locks{
        NoLock, SharedLock, ExclusiveLock;
    }

    private class Lock{
        private TransactionId tid;
        private Locks lockType;

        public Lock(TransactionId tid, Permissions perm){
            this.tid = tid;
            if (perm == Permissions.READ_ONLY){
                lockType = Locks.SharedLock;

            }
            else {
                lockType = Locks.ExclusiveLock;
            }
        }
    }
}

// lab3 pass all the tests
