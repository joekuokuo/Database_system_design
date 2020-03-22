package simpledb.parallel;

import java.util.Iterator;
import simpledb.DbException;
import simpledb.OpIterator;
import simpledb.TransactionAbortedException;
import simpledb.Tuple;
import simpledb.TupleDesc;
import java.util.*;
/**
 * The consumer part of the Shuffle Exchange operator.
 *
 * A ShuffleProducer operator sends tuples to all the workers according to some
 * PartitionFunction, while the ShuffleConsumer (this class) encapsulates the
 * methods to collect the tuples received at the worker from multiple source
 * workers' ShuffleProducer.
 *
 * */
public class ShuffleConsumer extends Consumer {

    private static final long serialVersionUID = 1L;

    private TupleDesc td;
    private OpIterator child;
//    private ParallelOperatorID operatorID;
    private SocketInfo[] workers;
    private HashMap<String, Integer> workerIndex;
    private transient Iterator<Tuple> tuples;
    private BitSet workerBS;
    private transient int interiorBufferIndex;
    private transient ArrayList<TupleBag> interiorBuffer;

    public String getName() {
        return "shuffle_c";
    }

    public ShuffleConsumer(ParallelOperatorID operatorID, SocketInfo[] workers) {
            this(null, operatorID, workers);
        }

    public ShuffleConsumer(ShuffleProducer child,
                ParallelOperatorID operatorID, SocketInfo[] workers) {
        super(operatorID);
        // some code goes here
        this.child = child;
        this.workers = workers;
        workerIndex = new HashMap<>();
        workerBS = new BitSet(workers.length);

        td = null;
        if(child != null){
            td = child.getTupleDesc();
        }

        int index = 0;
        for (SocketInfo w : workers){
            workerIndex.put(w.getId(), index++);
        }
    }

    @Override
    public void open() throws DbException, TransactionAbortedException {
        // some code goes here
        super.open();
        tuples = null;
        interiorBuffer = new ArrayList<>();
        interiorBufferIndex = 0;
        if (child != null){
           child.open();
        }

    }

    @Override
    public void rewind() throws DbException, TransactionAbortedException {
        // some code goes here
        tuples = null;
        interiorBufferIndex = 0;
    }

    @Override
    public void close() {
        // some code goes here
        super.close();
        setBuffer(null);
        tuples = null;
        interiorBufferIndex = -1;
        interiorBuffer = null;
        workerBS.clear();
        if (child != null){
            child.close();
        }
    }

    @Override
    public TupleDesc getTupleDesc() {
        // some code goes here

        if (child != null){
            td = child.getTupleDesc();
        }
        return td;

    }

    /**
     *
     * Retrieve a batch of tuples from the buffer of ExchangeMessages. Wait if
     * the buffer is empty.
     *
     * @return Iterator over the new tuples received from the source workers.
     *         Return <code>null</code> if all source workers have sent an end
     *         of file message.
     */
    Iterator<Tuple> getTuples() throws InterruptedException {
        // some code goes here
        if (interiorBufferIndex < interiorBuffer.size()){
            return interiorBuffer.get(interiorBufferIndex++).iterator();
        }
        while (workerBS.nextClearBit(0) < workers.length){
            // wait for an element to arrive.
            TupleBag tupleBag = (TupleBag) this.take(-1);

            if (tupleBag.isEos()){
                workerBS.set(workerIndex.get(tupleBag.getWorkerID()));
            }
            else{
                interiorBuffer.add(tupleBag);
                interiorBufferIndex++;
                return tupleBag.iterator();
            }
        }
        return null;
    }

    @Override
    protected Tuple fetchNext() throws DbException, TransactionAbortedException {
        // some code goes here
        while (tuples == null || !tuples.hasNext()){
            try{
                tuples = getTuples();
            } catch (InterruptedException e) {
                e.printStackTrace();
                throw new DbException(e.getLocalizedMessage());
            }
            if(tuples == null){
                return null;
            }
        }
        return tuples.next();
    }

    @Override
    public OpIterator[] getChildren() {
        // some code goes here
//        return null;
        return new OpIterator[]{child};
    }

    @Override
    public void setChildren(OpIterator[] children) {
        // some code goes here
        if(children.length == 1){
            child = children[0];
            td = child.getTupleDesc();
        }
    }

}


