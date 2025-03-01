package simpledb;

import javax.xml.crypto.Data;
import java.io.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import java.util.concurrent.ConcurrentHashMap;

/**
 * BufferPool manages the reading and writing of pages into memory from
 * disk. Access methods call into it to retrieve pages, and it fetches
 * pages from the appropriate location.
 * <p>
 * The BufferPool is also responsible for locking;  when a transaction fetches
 * a page, BufferPool checks that the transaction has the appropriate
 * locks to read/write the page.
 *
 * @Threadsafe, all fields are final
 */
public class BufferPool {
    /** Bytes per page, including header. */
    private static final int DEFAULT_PAGE_SIZE = 4096;

    private static int pageSize = DEFAULT_PAGE_SIZE;

    /** Default number of pages passed to the constructor. This is used by
     other classes. BufferPool should use the numPages argument to the
     constructor instead. */
    public static final int DEFAULT_PAGES = 50;

    private Page[] Pages;
    private final int numPages;
//    private HashMap<TransactionId, Page> tidToPage;

    // Switch to ConcurrentHashMap since there was a concurrent issue while testing.
    private ConcurrentHashMap<PageId, Page> pidToPage;

//    private HashMap<Permissions, Page> permToPage;

    // For eviction part.
    private HashMap<PageId, Integer> leastUsedPage;
    private int mark = 0;

    // LockManager
    private LockManager lockManager;

    private final int SLEEP_INTERVAL;
    /**
     * Creates a BufferPool that caches up to numPages pages.
     *
     * @param numPages maximum number of pages in this buffer pool.
     */
    public BufferPool(int numPages) {
        // some code goes here
        this.numPages = numPages;
        this.Pages = new Page[numPages];
//        tidToPage = new HashMap<>();
        pidToPage = new ConcurrentHashMap<>();
//        permToPage = new HashMap<>();
        leastUsedPage = new HashMap<>();
        lockManager = new LockManager();
        SLEEP_INTERVAL = 400;
    }

    public static int getPageSize() {
        return pageSize;
    }

    // THIS FUNCTION SHOULD ONLY BE USED FOR TESTING!!
    public static void setPageSize(int pageSize) {
        BufferPool.pageSize = pageSize;
    }

    // THIS FUNCTION SHOULD ONLY BE USED FOR TESTING!!
    public static void resetPageSize() {
        BufferPool.pageSize = DEFAULT_PAGE_SIZE;
    }

    /**
     * Retrieve the specified page with the associated permissions.
     * Will acquire a lock and may block if that lock is held by another
     * transaction.
     * <p>
     * The retrieved page should be looked up in the buffer pool.  If it
     * is present, it should be returned.  If it is not present, it should
     * be added to the buffer pool and returned.  If there is insufficient
     * space in the buffer pool, a page should be evicted and the new page
     * should be added in its place.
     *
     * @param tid the ID of the transaction requesting the page
     * @param pid the ID of the requested page
     * @param perm the requested permissions on the page
     */



    public Page getPage(TransactionId tid, PageId pid, Permissions perm)
            throws TransactionAbortedException, DbException {
        // some code goes here

        HeapPage page = (HeapPage) pidToPage.get(pid);

        boolean res;
        if (perm == Permissions.READ_ONLY){
            res = lockManager.acquireSharedLock(tid, pid);
        }
        else{
            res = lockManager.acquireExclusiveLock(tid, pid);
        }

        while (!res) {
            try{
                if (lockManager.deadlockOccurred(tid, pid)){
                    throw new TransactionAbortedException();
                }
                Thread.sleep(SLEEP_INTERVAL); // Not sleeping well...fail

            }
            catch (InterruptedException e){
//                throw new InterruptedException("Thread interrupted");
                System.out.println("Thread interrupted.");
            }
            if (perm == Permissions.READ_ONLY){
                res = lockManager.acquireSharedLock(tid, pid);
            }
            else {
                res = lockManager.acquireExclusiveLock(tid, pid);
            }
        }

        if (pidToPage.size() >= numPages){
//            throw new DbException("There is insufficient space in the buffer pool.");
            evictPage();
        }

        if (page != null) {
            return page;
        }
        else{
            page = (HeapPage) Database.getCatalog().getDatabaseFile(pid.getTableId()).readPage(pid);
        }

        pidToPage.put(pid, page);
        return page;
    }

    /**
     * Releases the lock on a page.
     * Calling this is very risky, and may result in wrong behavior. Think hard
     * about who needs to call this and why, and why they can run the risk of
     * calling it.
     *
     * @param tid the ID of the transaction requesting the unlock
     * @param pid the ID of the page to unlock
     */
    public  void releasePage(TransactionId tid, PageId pid) {
        // some code goes here
        // not necessary for lab1|lab2
        // Lab3

        // Unlock the page. If it's already unlocked, then throw IllegalArgumentException Error.
        if(! lockManager.unlock(tid, pid)){
            throw new IllegalArgumentException();
        }
    }

    /**
     * Release all locks associated with a given transaction.
     *
     * @param tid the ID of the transaction requesting the unlock
     */
    public void transactionComplete(TransactionId tid) throws IOException {
        // some code goes here
        // not necessary for lab1|lab2
        // lab3

        // Assume it's going to commit.
        transactionComplete(tid, true);
    }

    /** Return true if the specified transaction has a lock on the specified page */
    public boolean holdsLock(TransactionId tid, PageId p) {
        // some code goes here
        // not necessary for lab1|lab2
        // Lab3

        return lockManager.checklockStatus(tid, p);
    }

    /**
     * Commit or abort a given transaction; release all locks associated to
     * the transaction.
     *
     * @param tid the ID of the transaction requesting the unlock
     * @param commit a flag indicating whether we should commit or abort
     */

    public void transactionComplete(TransactionId tid, boolean commit)
            throws IOException {
        // some code goes here
        // not necessary for lab1|lab2
        // lab 3
        // refer: https://www.geeksforgeeks.org/iterate-map-java/

        // iterate through the whole map.

//        Iterator<Entry<PageId, Page>> nextPage = pidToPage.entrySet().iterator();
//        while (nextPage.hasNext()) {
//            Entry<PageId, Page> next = nextPage.next();
//            PageId pid = next.getKey();
//            Page page = next.getValue();
//
//            if (tid.equals(page.isDirty())) {
//                // Commit
//                if (commit) {
//
//                    flushPage(pid); // flush when commit the transaction
//                    page.markDirty(false, null);
////                    Database.getLogFile().logWrite(tid, page.getBeforeImage(), page);
//                    page.setBeforeImage();
//
//                }
//                // Abort
//                else {
//                    pidToPage.put(pid, page.getBeforeImage());
//
//                }
//            }

        // Or we can just use map.value to iterate :)
        if(commit){
            for(Page p : pidToPage.values()) {
                if (tid.equals(p.isDirty())) {
                    // Force policy
                    // flush when commit the transaction
                    // flushPage(p.getId());

                    // No force policy
                    Database.getLogFile().logWrite(tid, p.getBeforeImage(), p);
                }
                p.setBeforeImage();
            }
        }
        else {
            for(Page p : pidToPage.values()) {
                pidToPage.put(p.getId(), p.getBeforeImage());
            }
        }

        lockManager.releaseAllTransactionLocks(tid);
    }

    /**
     * Add a tuple to the specified table on behalf of transaction tid.  Will
     * acquire a write lock on the page the tuple is added to and any other
     * pages that are updated (Lock acquisition is not needed for lab2).
     * May block if the lock(s) cannot be acquired.
     *
     * Marks any pages that were dirtied by the operation as dirty by calling
     * their markDirty bit, and adds versions of any pages that have
     * been dirtied to the cache (replacing any existing versions of those pages) so
     * that future requests see up-to-date pages.
     *
     * @param tid the transaction adding the tuple
     * @param tableId the table to add the tuple to
     * @param t the tuple to add
     */
    public void insertTuple(TransactionId tid, int tableId, Tuple t)
            throws DbException, IOException, TransactionAbortedException {
        // some code goes here
        // lab2
        try{
            ArrayList<Page> changedPages;
            DbFile dbFile = Database.getCatalog().getDatabaseFile(tableId);
            HeapFile hf = (HeapFile)dbFile;
            changedPages = hf.insertTuple(tid, t);

            //iterate through affectedPages and markDirty
            //also update cached pages
            for (Page page : changedPages) {
                pidToPage.put(page.getId(), page);
            }
        }
        catch (DbException e){

            e.printStackTrace();
        }
        catch (TransactionAbortedException e){
            e.printStackTrace();
        }
        catch (IOException e){
            e.printStackTrace();
        }


    }

    /**
     * Remove the specified tuple from the buffer pool.
     * Will acquire a write lock on the page the tuple is removed from and any
     * other pages that are updated. May block if the lock(s) cannot be acquired.
     *
     * Marks any pages that were dirtied by the operation as dirty by calling
     * their markDirty bit, and adds versions of any pages that have
     * been dirtied to the cache (replacing any existing versions of those pages) so
     * that future requests see up-to-date pages.
     *
     * @param tid the transaction deleting the tuple.
     * @param t the tuple to delete
     */
    public void deleteTuple(TransactionId tid, Tuple t)
            throws DbException, TransactionAbortedException {
        // some code goes here
        // lab2
        DbFile f = Database.getCatalog().getDatabaseFile(t.getRecordId().getPageId().getTableId());
        ArrayList<Page> pages = null;

        try {
            pages = f.deleteTuple(tid, t);
        }
        catch (IOException e) {
            e.printStackTrace();
        }

        for (Page page : pages) {
            page.markDirty(true, tid);
        }

    }

    /**
     * Flush all dirty pages to disk.
     * NB: Be careful using this routine -- it writes dirty data to disk so will
     *     break simpledb if running in NO STEAL mode.
     */
    public synchronized void flushAllPages() throws IOException {
        // some code goes here
        // not necessary for lab1
        for (Page p : pidToPage.values()) {
            flushPage(p.getId());
        }
    }

    /** Remove the specific page id from the buffer pool.
     Needed by the recovery manager to ensure that the
     buffer pool doesn't keep a rolled back page in its
     cache.

     Also used by B+ tree files to ensure that deleted pages
     are removed from the cache so they can be reused safely
     */
    public synchronized void discardPage(PageId pid) {
        // some code goes here
        // not necessary for lab1
    }

    /**
     * Flushes a certain page to disk
     * @param pid an ID indicating the page to flush
     */
    private synchronized void flushPage(PageId pid) throws IOException {
        // some code goes here
        // not necessary for lab1
        DbFile file = Database.getCatalog().getDatabaseFile(pid.getTableId());
        HeapPage page = (HeapPage) pidToPage.get(pid);
        TransactionId dirtyTid = page.isDirty();
        if (dirtyTid != null) {
            Database.getLogFile().logWrite(dirtyTid, page.getBeforeImage(), page);
            Database.getLogFile().force();
            pidToPage.remove(pid);
            file.writePage(page);
            page.markDirty(false, null);
        }
    }

    /** Write all pages of the specified transaction to disk.
     */
    public synchronized void flushPages(TransactionId tid) throws IOException {
        // some code goes here
        // not necessary for lab1|lab2
        for (PageId pid: pidToPage.keySet()){
            TransactionId dirtyTid = pidToPage.get(pid).isDirty();
            if(dirtyTid.equals(tid)){
                flushPage(pid);
            }
        }

    }

    /**
     * Discards a page from the buffer pool.
     * Flushes the page to disk to ensure dirty pages are updated on disk.
     */
    private synchronized void evictPage() throws DbException {
        // some code goes here
        // not necessary for lab1

        Iterator<Entry<PageId, Page>> it = pidToPage.entrySet().iterator();

        while (pidToPage.size() >= numPages && it.hasNext()) {
            Entry<PageId, Page> next = it.next();
            PageId pid = next.getKey();
            HeapPage p = (HeapPage)next.getValue();

            // Disabled for Lab 5
            // NO STEAL policy
            // never evict dirty pages.
//            if (p.isDirty() != null) {
//                continue;
//            }
            pidToPage.remove(pid);
        }

        if (pidToPage.size() >= numPages) {
            throw new DbException("Running out buffer pool.");
        }

    }

}

