package simpledb;

import java.util.Iterator;
import java.util.NoSuchElementException;

public class HeapFileIterator implements DbFileIterator{

    private final TransactionId tid;
    private final HeapFile hf;
    private int pgNo;
    private Iterator<Tuple> tupleIterator = null;

    public HeapFileIterator(TransactionId tid, HeapFile hf){
        this.tid = tid;
        this.hf = hf;
    }
    @Override
    public void open() throws DbException, TransactionAbortedException {
        // start from the beginning
        pgNo = 0;
        PageId pid = new HeapPageId(hf.getId(), pgNo);
        tupleIterator = ((HeapPage)Database.getBufferPool().getPage(tid, pid, Permissions.READ_ONLY)).iterator();
    }

    @Override
    public boolean hasNext() throws DbException, TransactionAbortedException {
        if (tupleIterator == null || pgNo >= hf.numPages())
            return false;
        // still need to check every page later on
        while (!tupleIterator.hasNext()){
            pgNo++;
            if (pgNo >= hf.numPages())
                return false;

             tupleIterator = openPageByPageNumber(pgNo).iterator();
        }
        return true;
    }

    @Override
    public Tuple next() throws DbException, TransactionAbortedException, NoSuchElementException {
        if(!hasNext()){
            throw new NoSuchElementException();
        }

        return tupleIterator.next();
    }

    @Override
    public void rewind() throws DbException, TransactionAbortedException {
        pgNo = 0;
        PageId pid = new HeapPageId(hf.getId(), pgNo);
        tupleIterator = ((HeapPage)Database.getBufferPool().getPage(tid, pid, Permissions.READ_ONLY)).iterator();
    }

    @Override
    public void close() {
        pgNo = 0;
        tupleIterator = null;
    }

    public HeapPage openPageByPageNumber(int pgNo) throws NoSuchElementException, TransactionAbortedException, DbException{
        PageId pid = new HeapPageId(hf.getId(), pgNo);
        return ((HeapPage)Database.getBufferPool().getPage(tid, pid, Permissions.READ_ONLY));

    }
}
