package simpledb;

import java.io.*;
import java.util.*;

/**
 * HeapFile is an implementation of a DbFile that stores a collection of tuples
 * in no particular order. Tuples are stored on pages, each of which is a fixed
 * size, and the file is simply a collection of those pages. HeapFile works
 * closely with HeapPage. The format of HeapPages is described in the HeapPage
 * constructor.
 *
 * @see simpledb.HeapPage#HeapPage
 * @author Sam Madden
 */
public class HeapFile implements DbFile {

    private final File file;
    private final TupleDesc td;
    private final int tableId;
    final int pageSize = BufferPool.getPageSize();

    /**
     * Constructs a heap file backed by the specified file.
     *
     * @param f
     *            the file that stores the on-disk backing store for this heap
     *            file.
     */
    public HeapFile(File f, TupleDesc td) {
        // some code goes here

        this.file = f;
        this.td = td;
        this.tableId = file.getAbsoluteFile().hashCode();
    }

    /**
     * Returns the File backing this HeapFile on disk.
     *
     * @return the File backing this HeapFile on disk.
     */
    public File getFile() {
        // some code goes here
        return file;
    }

    /**
     * Returns an ID uniquely identifying this HeapFile. Implementation note:
     * you will need to generate this tableid somewhere to ensure that each
     * HeapFile has a "unique id," and that you always return the same value for
     * a particular HeapFile. We suggest hashing the absolute file name of the
     * file underlying the heapfile, i.e. f.getAbsoluteFile().hashCode().
     *
     * @return an ID uniquely identifying this HeapFile.
     */
    // which is tid
    public int getId() {
        // some code goes here
        return tableId;
    }

    /**
     * Returns the TupleDesc of the table stored in this DbFile.
     *
     * @return TupleDesc of this DbFile.
     */
    public TupleDesc getTupleDesc() {
        // some code goes here
        return td;
    }

    // see DbFile.java for javadocs
    public Page readPage(PageId pid) throws IllegalArgumentException{
        // some code goes here
        // GOT IT!

        try {
            byte[] bytes = HeapPage.createEmptyPageData(); // all 0

            int accessFrom = pid.getPageNumber() * BufferPool.getPageSize();
            // use randomaccessfile to access the file can access the file from the middle
            // reference: https://javarevisited.blogspot.com/2015/02/randomaccessfile-example-in-java-read-write-String.html
            // reference 2: https://examples.javacodegeeks.com/core-java/io/randomaccessfile/java-randomaccessfile-example/
            RandomAccessFile reader = new RandomAccessFile(file, "r");
            reader.seek(accessFrom);
            reader.read(bytes);
            reader.close();

            // When page is full
            return new HeapPage((HeapPageId) pid, bytes);
        } catch (IOException e) {
            throw new IllegalArgumentException();
        }

    }

    // see DbFile.java for javadocs
    public void writePage(Page page) throws IOException {
        // some code goes here
        // Lab2 implementation
//        int numTuples = (BufferPool.getPageSize()*8) / (tdSize * 8 + 1);
//        int headerSize = (int) Math.ceil(numTuples / 8.0);
//        byte[] empty = new byte[numTuples * 8 + headerSize];
//        page.getPageData() =
        try{

            RandomAccessFile randomAccessFile = new RandomAccessFile(file, "rw");
            randomAccessFile.seek(BufferPool.getPageSize() * page.getId().getPageNumber());
            randomAccessFile.write(page.getPageData());
            randomAccessFile.close();
        }
        catch (IOException e){
            e.printStackTrace();
        }

    }

    /**
     * Returns the number of pages in this HeapFile.
     */
    public int numPages() {
        // some code goes here
//        return numPages; causing error
        return (int)(file.length()/ pageSize);
    }

    // see DbFile.java for javadocs
    public ArrayList<Page> insertTuple(TransactionId tid, Tuple t)
            throws DbException, IOException, TransactionAbortedException {
        // some code goes here
        // Lab2:
        ArrayList<Page> changePages = new ArrayList<>();
        for (int i = 0; i < numPages(); ++i) {
            PageId pid = new HeapPageId(getId(), i);
            // Acquire write lock
            HeapPage page = (HeapPage) Database.getBufferPool().getPage(tid, pid, Permissions.READ_WRITE);

            // if there are spaces, insert tuple can be performed
            if (page.getNumEmptySlots() > 0) {
                page.insertTuple(t);
                changePages.add(page);
                page.markDirty(true, tid);
            }
        }

        // every pages in BufferPool is full, then need to create a new blank page.
        // no thing added to the changePages list.
        if(changePages.size() == 0){
            PageId pageId = new HeapPageId(getId(), numPages());
            HeapPage blank = new HeapPage((HeapPageId)pageId, new byte[BufferPool.getPageSize()]);
            writePage(blank);

            HeapPage newPage = (HeapPage) Database.getBufferPool().getPage(tid, pageId, Permissions.READ_WRITE);
            newPage.insertTuple(t);
            newPage.markDirty(true, tid);
            changePages.add(newPage);
        }

        return changePages;

    }

    // see DbFile.java for javadocs
    public ArrayList<Page> deleteTuple(TransactionId tid, Tuple t) throws DbException,
            TransactionAbortedException {
        // some code goes here

        if (t.getRecordId() == null) {
            throw new DbException("Record id is null");
        }

        HeapPage page = (HeapPage)Database.getBufferPool().getPage(tid, t.getRecordId().getPageId(), Permissions.READ_WRITE);
        page.deleteTuple(t);
        page.markDirty(true, tid);

        return new ArrayList<Page>(List.of(page));
    }

    // see DbFile.java for javadocs
    public DbFileIterator iterator(TransactionId tid) {
        // some code goes here
        return new HeapFileIterator(tid, this);
//        return null;
    }
}


// pass all the tests in lab3
