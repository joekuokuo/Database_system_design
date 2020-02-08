package simpledb;
import java.util.*;

/**
 * Knows how to compute some aggregate over a set of StringFields.
 */
public class StringAggregator implements Aggregator {

    private static final long serialVersionUID = 1L;
    private int gbfield;
    private Type gbfieldtype;
    private int afield;
    private Op what;

    private final Map<Field, Integer> countGroupBy;
    private int noGroupCount;
    private String groupFieldName;
    /**
     * Aggregate constructor
     * @param gbfield the 0-based index of the group-by field in the tuple, or NO_GROUPING if there is no grouping
     * @param gbfieldtype the type of the group by field (e.g., Type.INT_TYPE), or null if there is no grouping
     * @param afield the 0-based index of the aggregate field in the tuple
     * @param what aggregation operator to use -- only supports COUNT
     * @throws IllegalArgumentException if what != COUNT
     */

    public StringAggregator(int gbfield, Type gbfieldtype, int afield, Op what) {
        // some code goes here
        if (what != Op.COUNT) {
            throw new IllegalArgumentException();
        }

        this.gbfield = gbfield;
        this.gbfieldtype = gbfieldtype;
        this.afield = afield;
        this.what = what;
        this.noGroupCount = 0;
        countGroupBy = new HashMap<Field, Integer>();

    }

    /**
     * Merge a new tuple into the aggregate, grouping as indicated in the constructor
     * @param tup the Tuple containing an aggregate field and a group-by field
     */
    public void mergeTupleIntoGroup(Tuple tup) {
        // some code goes here
        if (gbfield == NO_GROUPING) {
            noGroupCount++;
        } else {
            groupFieldName = tup.getTupleDesc().getFieldName(gbfield);
            Field groupField = tup.getField(gbfield);
            if (!countGroupBy.containsKey(groupField)) {
                countGroupBy.put(groupField, 1);
            } else {
                int count = countGroupBy.get(groupField);
                countGroupBy.put(groupField, count+1);
            }
        }
    }

    /**
     * Create a OpIterator over group aggregate results.
     *
     * @return a OpIterator whose tuples are the pair (groupVal,
     *   aggregateVal) if using group, or a single (aggregateVal) if no
     *   grouping. The aggregateVal is determined by the type of
     *   aggregate specified in the constructor.
     */
    public OpIterator iterator() {
        // some code goes here
        TupleDesc td = new TupleDesc(
                new Type[]{gbfieldtype, Type.INT_TYPE},
                new String[]{groupFieldName, what.toString()});

        ArrayList<Tuple> tuples = new ArrayList<>();
        if (gbfield != NO_GROUPING) {


            for (Field group : countGroupBy.keySet()) {

                Tuple tuple = new Tuple(td);
                tuple.setField(0, group);
                tuple.setField(1, new IntField(countGroupBy.get(group)));
                tuples.add(tuple);
            }

        }
        else{
            for (Field group : countGroupBy.keySet()) {
                int value = countGroupBy.get(group);
                Tuple tuple = new Tuple(td);
                tuple.setField(0, new IntField(value));
                tuples.add(tuple);
            }

        }
        return new TupleIterator(td, tuples);

    }

}
