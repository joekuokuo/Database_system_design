package simpledb.parallel;

import org.apache.mina.core.future.IoFutureListener;
import org.apache.mina.core.future.WriteFuture;
import org.apache.mina.core.session.IoSession;
import simpledb.*;
import simpledb.OpIterator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
/**
 * The producer part of the Shuffle Exchange operator.
 * 
 * ShuffleProducer distributes tuples to the workers according to some
 * partition function (provided as a PartitionFunction object during the
 * ShuffleProducer's instantiation).
 * 
 * */
public class ShuffleProducer extends Producer {

    private static final long serialVersionUID = 1L;
    private OpIterator child;
    private final ParallelOperatorID operatorID;
    private final SocketInfo[] workers;
    private PartitionFunction<?, ?> pf;
    private WorkingThread threadRunning;

    public String getName() {
        return "shuffle_p";
    }

    public ShuffleProducer(OpIterator child, ParallelOperatorID operatorID,
                           SocketInfo[] workers, PartitionFunction<?, ?> pf) {
        super(operatorID);
        // some code goes here
        this.child = child;
        this.operatorID = operatorID;
        this.workers = workers;
        this.pf = pf;
    }

    public void setPartitionFunction(PartitionFunction<?, ?> pf) {
        // some code goes here
        this.pf = pf;
    }

    public SocketInfo[] getWorkers() {
        // some code goes here
        return workers;
    }

    public PartitionFunction<?, ?> getPartitionFunction() {
        // some code goes here
        return pf;
    }

    // some code goes here
    class WorkingThread extends Thread {
        public void run() {

            // some code goes here
            // Used map to store the information between workers and IoSessions and buffer and time.
            Map<String, IoSession> workerIdSession  = new HashMap<>();
            Map<String, ArrayList<Tuple>> workerIdBuffer = new HashMap<>();
            Map<String, Long> workerIdTime = new HashMap<>();

            // read every worker for the worker array assign default value for them.
            for (SocketInfo w : workers){
                IoSession newSession = ParallelUtility.createSession(
                        w.getAddress(), getThisWorker().minaHandler, -1);
                workerIdSession.put(w.getId(), newSession);
                ArrayList<Tuple> t = new ArrayList<>();
                workerIdBuffer.put(w.getId(), t); // assign every worker a list of tuple
                Long time = System.currentTimeMillis();
                workerIdTime.put(w.getId(), time);
            }

            try{
                while(child.hasNext()){
                    Tuple tup = child.next();
                    TupleDesc td = child.getTupleDesc();
                    int partitionPos = pf.partition(tup, td);
                    SocketInfo consumerWorker = workers[partitionPos];
                    String consumerWorkerId = consumerWorker.getId();

                    ArrayList<Tuple> tuplesBuffer = workerIdBuffer.get(consumerWorkerId);
                    IoSession session = workerIdSession.get(consumerWorkerId);
                    long timePast = workerIdTime.get(consumerWorkerId);
                    tuplesBuffer.add(tup);

                    // if the tupleBuffer size exceed the max size of tupleBag
                    // write the session into disk.
                    if (tuplesBuffer.size() >= TupleBag.MAX_SIZE){
                        session.write(new TupleBag(
                                operatorID,
                                getThisWorker().workerID,
                                tuplesBuffer.toArray(new Tuple[] {}),
                                getTupleDesc()));
                        tuplesBuffer.clear();
                        timePast = System.currentTimeMillis();
                    }

                    // if the tupleBuffer size is greater than the minimun size of tupleBag
                    if (tuplesBuffer.size() >= TupleBag.MIN_SIZE){
                        long timeNow = System.currentTimeMillis();
                        // if the time gap is bigger than the max time gap in tupleBag
                        // write the session into disk.
                        if(timeNow - timePast > TupleBag.MAX_MS){
                            session.write(new TupleBag(
                                    operatorID,
                                    getThisWorker().workerID,
                                    tuplesBuffer.toArray(new Tuple[] {}),
                                    getTupleDesc()));
                            tuplesBuffer.clear();
                            timePast = timeNow;
                        }
                    }
                    workerIdTime.put(consumerWorkerId, timePast); // update the new timestamp

                }
                for (SocketInfo w : workers){
                    String workerId = w.getId();
                    ArrayList<Tuple> tuplesBuffer = workerIdBuffer.get(workerId);
                    IoSession session = workerIdSession.get(workerId);
                    if(tuplesBuffer.size() > 0){
                        session.write(new TupleBag(
                                operatorID,
                                getThisWorker().workerID,
                                tuplesBuffer.toArray(new Tuple[] {}),
                                getTupleDesc()));
                    }
                    session.write(new TupleBag(operatorID,
                            getThisWorker().workerID)).addListener(new IoFutureListener<WriteFuture>() {

                        @Override
                        public void operationComplete(WriteFuture future) {
                            ParallelUtility.closeSession(future.getSession());
                        }});
                }

            } catch (TransactionAbortedException e) {
                e.printStackTrace();
            } catch (DbException e) {
                e.printStackTrace();
            }

        }
    }

    @Override
    public void open() throws DbException, TransactionAbortedException {
        // some code goes here
        super.open();
        child.open();
        threadRunning = new WorkingThread();
        threadRunning.run();
    }

    public void close() {
        // some code goes here
        super.close();
        child.close();
    }

    @Override
    public void rewind() throws DbException, TransactionAbortedException {
        close();
        open();
    }

    @Override
    public TupleDesc getTupleDesc() {
        // some code goes here
//        return null;
        return child.getTupleDesc();
    }

    @Override
    protected Tuple fetchNext() throws DbException, TransactionAbortedException {
        // some code goes here
        // return null;
        try {
            threadRunning.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return null;
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
            this.child = children[0];
        }
    }
}
