package simpledb.parallel;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;

import simpledb.*;


import simpledb.parallel.Exchange.ParallelOperatorID;

/**
 * Workers do the real query execution. A query received by the server will be
 * pre-processed and then dispatched to the workers.
 *
 * To execute a query on a worker, 4 steps are proceeded:
 *
 * 1) A worker receive a OpIterator instance as its execution plan. The worker
 * then stores the plan and does some pre-processing, e.g. initializes the data
 * structures which are needed during the execution of the plan.
 *
 * 2) Each worker sends back to the server a message (it's id) to notify the
 * server that the query plan has been successfully received. And then each
 * worker waits for the server to send the "start" message.
 *
 * 3) Each worker executes its query plan after "start" is received.
 *
 * 4) After the query plan finishes, each worker removes the query plan and
 * related data structures, and then waits for next query plan
 *
 * */
public class Worker {

    static final String usage = "Usage: worker worker_identifier(format: host:port) server_identifier [--data datadir]";
    public final static String DEFAULT_DATA_DIR = "data";

    /**
     * The working thread, which executes the query plan
     * */
    public class WorkingThread extends Thread {
        public void run() {
            while (true) {
                OpIterator query = null;
                // synchronized (Worker.this) {
                query = Worker.this.queryPlan;
                // }
                if (query != null) {
                     try {
//                     some code goes here
                         if(query instanceof CollectProducer) {
                             CollectProducer newQuery = (CollectProducer) query;
                             newQuery.open();
                             newQuery.fetchNext();
                             newQuery.close();
                         }
                    } catch (DbException e1) {
                        e1.printStackTrace();
                    } catch (TransactionAbortedException e1) {
                         e1.printStackTrace();
                     }
                    Worker.this.finishQuery();
                }

                synchronized (Worker.this.workingThread) {
                    try {
                        // wait until a query plan is received
                        Worker.this.workingThread.wait();
                    } catch (InterruptedException e) {
                        break;
                    }
                }
            }
        }
    }

    public static void main(String[] args) throws Throwable {
        if (args.length < 2 || args.length > 4) {
            System.out.println("Invalid number of arguments.\n" + usage);
            ParallelUtility.shutdownVM();
        }
        String dataDir = DEFAULT_DATA_DIR;

        // Checking if a data argument was specified.
        // This argument is used for testing
        if (args.length >= 3 && args[2].equals("--data")) {
            dataDir = args[3];
            args = ParallelUtility.removeArg(args, 2);
            args = ParallelUtility.removeArg(args, 2);
        }

        // Instantiate a new worker
        Worker w = new Worker(args[0], args[1]);
        int port = w.port;

        // The worker uses the same schema as the server
        // Here, we are loading a copy of that schema
        Database.getCatalog().loadSchema(
                dataDir + "/" + port + "/catalog.schema");

        // Prepare to receive messages over the network
        w.init();
        // Start the actual message handler by binding
        // the acceptor to a network socket
        // Now the worker can accept messages
        w.start();
        // Periodically detect if the server (i.e., coordinator)
        // is still running. IF the server goes down, the
        // worker will stop itself
        new WorkerLivenessController(w).start();

        System.out.println("Worker started at port:" + port);

        // From now on, the worker will listen for
        // messages to arrive on the network. These messages
        // will be handled by the WorkerHandler (see class WorkerHandler below,
        // in particular method messageReceived).

    }

    /**
     * Initiallize
     * */
    public void init() throws IOException {
        acceptor.setHandler(minaHandler);
    }

    public void start() throws IOException {
        acceptor.bind(new InetSocketAddress(host, port));
        workingThread.start();
        // You need to implement for Lab 6: Make sure to start the worker thread.
    }

    /**
     * The ID of this worker, it's simply the address of this worker: host:port
     * */
    final String workerID;

    final int port;
    final String host;
    /**
     * The server address
     * */
    final InetSocketAddress server;

    /**
     * The acceptor, which binds to a TCP socket and waits for connections
     *
     * The Server sends messages, including query plans and control messages, to
     * the worker through this acceptor.
     *
     * Other workers send tuples during query execution to this worker also
     * through this acceptor.
     * */
    final NioSocketAcceptor acceptor;

    /**
     * The current query plan
     * */
    private volatile OpIterator queryPlan = null;

    /**
     * A indicator of shutting down the worker
     * */
    private volatile boolean toShutdown = false;

    /**
     * Return true if the worker is now executing a query.
     * */
    public boolean isRunning() {
        return this.queryPlan != null;
    }

    public final WorkingThread workingThread;

    /**
     * The IoHandler for network communication. The methods of this handler are
     * called when some communication events happen. For example a message is
     * received, a new connection is created, etc.
     * */
    final WorkerHandler minaHandler;

    /**
     * The I/O buffer, all the ExchangeMessages sent to this worker are buffered
     * here.
     * */
    public final HashMap<ParallelOperatorID, LinkedBlockingQueue<ExchangeMessage>> inBuffer;

    public Worker(String workerID, String serverAddr) {
        this.workerID = workerID;
        String[] ts = workerID.split(":");
        port = Integer.parseInt(ts[1]);
        host = ts[0];
        String[] server = serverAddr.split(":");
        this.server = new InetSocketAddress(server[0],
                Integer.parseInt(server[1]));
        acceptor = ParallelUtility.createAcceptor();
        inBuffer = new HashMap<ParallelOperatorID, LinkedBlockingQueue<ExchangeMessage>>();

        workingThread = new WorkingThread();
        workingThread.setDaemon(false);
        minaHandler = new WorkerHandler();
    }

    /**
     * localize the received query plan. Some information that are required to
     * get the query plan executed need to be replaced by local versions. For
     * example, the table in the SeqScan operator need to be replaced by the
     * local table. Note that Producers and Consumers also needs local
     * information.
     * */
    public void localizeQueryPlan(OpIterator queryPlan) {
        // some code goes here

        if (queryPlan instanceof SeqScan) {
            SeqScan seqscan = (SeqScan) queryPlan;
            int tableId = Database.getCatalog().getTableId(seqscan.getTableName());
            seqscan.reset(tableId, seqscan.getAlias());
        }
        // Check the type of the queryPlan
        if (queryPlan instanceof Operator) {
            if (queryPlan instanceof Producer) {
                ((Producer) queryPlan).setThisWorker(Worker.this);
            }
            else if (queryPlan instanceof Consumer) {
                LinkedBlockingQueue<ExchangeMessage> buffer = inBuffer.get(((Consumer) queryPlan).getOperatorID());
                ((Consumer) queryPlan).setBuffer(buffer);
            }

            OpIterator[] children = ((Operator) queryPlan).getChildren();

            for (OpIterator child : children) {
                localizeQueryPlan(child);
            }
            ((Operator) queryPlan).setChildren(children);
        }
    }

    /**
     * Find out all the ParallelOperatorIDs of all consuming operators:
     * ShuffleConsumer, CollectConsumer, and BloomFilterConsumer running at this
     * worker. The inBuffer needs the IDs to distribute the ExchangeMessages
     * received.
     * */
    public static void collectConsumerOperatorIDs(OpIterator root,
                                                  ArrayList<ParallelOperatorID> oIds) {
        if (root instanceof Consumer)
            oIds.add(((Consumer) root).getOperatorID());
        if (root instanceof Operator)
            for (OpIterator c : ((Operator) root).getChildren())
                collectConsumerOperatorIDs(c, oIds);
    }

    /**
     * execute the current query, note that this method is invoked by the Mina
     * IOHandler thread. Typically, IOHandlers should focus on accepting/routing
     * IO requests, rather than do heavily loaded work.
     * */
    public void executeQuery() {
        System.out.println("Query started");
        synchronized (Worker.this.workingThread) {
            Worker.this.workingThread.notifyAll();
        }
    }

    /**
     * This method should be called whenever the system is going to shutdown
     * */
    public void shutdown() {
        System.out
                .println("Shutdown requested. Please wait when cleaning up...");
        ParallelUtility.unbind(acceptor);
        this.toShutdown = true;
    }

    /**
     * this method should be called when a query is received from the server.
     *
     * It does the initialization and preparation for the execution of the
     * query.
     * */
    public void receiveQuery(OpIterator query) {
        System.out.println("Query received");
        if (Worker.this.queryPlan != null) {
            System.err
                    .println("Error: Worker is still processing. New query refused");
            return;
        }

//        new QueryPlanVisualizer().printQueryPlanTree(query, System.out); from piazza
        ArrayList<ParallelOperatorID> ids = new ArrayList<ParallelOperatorID>();
        collectConsumerOperatorIDs(query, ids);
        Worker.this.inBuffer.clear();
        for (ParallelOperatorID id : ids) {
            Worker.this.inBuffer.put(id,
                    new LinkedBlockingQueue<ExchangeMessage>());
        }
        // }
        Worker.this.localizeQueryPlan(query);
        new QueryPlanVisualizer().printQueryPlanTree(query, System.out);
        Worker.this.queryPlan = query;
    }

    /**
     * This method should be called when a data item is received
     * */
    public void receiveData(ExchangeMessage data) {
        if (data instanceof TupleBag)
            System.out.println("TupleBag received from " + data.getWorkerID()
                    + " to Operator: " + data.getOperatorID());
        else if (data instanceof BloomFilterBitSet)
            System.out.println("BitSet received from " + data.getWorkerID()
                    + " to Operator: " + data.getOperatorID());
        LinkedBlockingQueue<ExchangeMessage> q = null;
        q = Worker.this.inBuffer.get(data.getOperatorID());

        q.offer(data);
    }

    /**
     * This method should be called when a query is finished
     * */
    public void finishQuery() {
        if (this.queryPlan != null) {
            this.inBuffer.clear();
            this.queryPlan = null;
        }
        System.out.println("My part of the query finished");
    }

    /**
     * Mina acceptor handler. This is where all messages arriving from other
     * workers and from the coordinator will be processed
     * */
    public class WorkerHandler extends IoHandlerAdapter {

        @Override
        public void exceptionCaught(IoSession session, Throwable cause) {
            System.out.println("exception caught");
            cause.printStackTrace();
            ParallelUtility.closeSession(session);
        }

        /**
         * Got called everytime a message is received.
         * */
        @Override
        public void messageReceived(IoSession session, Object message) {
            /**
             * Instructions from the Server
             * */
            if (message instanceof String) {
                String str = (String) message;
                if (str.equals("shutdown")) {
                    Worker.this.toShutdown = true;
                    return;
                } else if (str.equals("start")) {
                    Worker.this.executeQuery();
                }
            }
            /**
             * The query plan sent by the server
             * */
            else if (message instanceof OpIterator) {
                Worker.this.receiveQuery((OpIterator) message);
                // Tell the server that the query has received
                session.write(Worker.this.workerID);
            }
            /**
             * The data sent from other workers.
             * */
            else if (message instanceof ExchangeMessage) {
                Worker.this.receiveData((ExchangeMessage) message);
            } else {
                System.err.println("Error: Unknown message received: "
                        + message);
            }

        }
    }

    /**
     * The controller class which decides whether this worker should shutdown or
     * not. 1) it detects whether the server is still alive. If the server got
     * killed because of any reason, the workers will be terminated. 2) it
     * detects whether a shutdown message is received.
     * */
    public static class WorkerLivenessController extends TimerTask {
        private final Timer timer = new Timer();
        private volatile boolean inRun = false;
        final Worker thisWorker;

        public WorkerLivenessController(Worker worker) {
            thisWorker = worker;
        }

        public void run() {
            if (thisWorker.toShutdown) {
                thisWorker.shutdown();
                ParallelUtility.shutdownVM();
            }
            if (inRun)
                return;
            inRun = true;
            IoSession serverSession = null;
            int numTry = 0;
            try {
                while ((serverSession = ParallelUtility.createSession(
                        thisWorker.server, thisWorker.minaHandler, 3000)) == null
                        && numTry < 3)
                    numTry++;
            } catch (RuntimeException e) {
                // e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (serverSession == null) {
                System.out
                        .println("Cannot connect the server. Maybe the server is down. I'll shutdown now.");
                System.out.println("Bye!");
                this.timer.cancel();
                ParallelUtility.shutdownVM();
            }
            ParallelUtility.closeSession(serverSession);
            inRun = false;
        }

        public void start() {
            try {
                timer.schedule(this, (long) (Math.random() * 3000) + 5000, // initial
                                                                           // delay
                        (long) (Math.random() * 2000) + 1000); // subsequent
                                                               // rate
            } catch (IllegalStateException e) {// already get canceled, ignore
            }
        }
    }
}
