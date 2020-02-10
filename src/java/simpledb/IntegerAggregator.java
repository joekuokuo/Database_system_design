package simpledb;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
/**
 * Knows how to compute some aggregate over a set of IntFields.
 */
public class IntegerAggregator implements Aggregator {

    private static final long serialVersionUID = 1L;
    private final int gbfield;
    private final Type gbFieldType;
    private final int afield;
    private final Op what;

    private final Map<Field, Integer> countGroupedBy;
    private final Map<Field, Integer> valueGroupedBy;
    private String groupFieldName;
    /**
     * Aggregate constructor
     *
     * @param gbfield
     *            the 0-based index of the group-by field in the tuple, or
     *            NO_GROUPING if there is no grouping
     * @param gbfieldtype
     *            the type of the group by field (e.g., Type.INT_TYPE), or null
     *            if there is no grouping
     * @param afield
     *            the 0-based index of the aggregate field in the tuple
     * @param what
     *            the aggregation operator
     */

    public IntegerAggregator(int gbfield, Type gbfieldtype, int afield, Op what) {
        // some code goes here
        this.gbfield = gbfield;
        this.gbFieldType = gbfieldtype;
        this.afield = afield;
        this.what = what;
        this.countGroupedBy = new HashMap<Field, Integer>();
        this.valueGroupedBy = new HashMap<Field, Integer>();
    }

    /**
     * Merge a new tuple into the aggregate, grouping as indicated in the
     * constructor
     * 
     * @param tup
     *            the Tuple containing an aggregate field and a group-by field
     */
    public void mergeTupleIntoGroup(Tuple tup) {
        // some code goes here
        Field groupField;

        if (gbfield == NO_GROUPING){
            groupField = new IntField(0);
            groupFieldName = null;
        }
        else {
            groupField = tup.getField(gbfield);
            groupFieldName = tup.getTupleDesc().getFieldName(gbfield);
        }

        int aggregateValue = ((IntField)tup.getField(afield)).getValue();
        // Haven't seen
        if (!countGroupedBy.containsKey(groupField)) {
            countGroupedBy.put(groupField, 1);
            valueGroupedBy.put(groupField, aggregateValue);

        }
        // Seen before
        else {
            countGroupedBy.put(groupField, countGroupedBy.get(groupField) + 1);
            int value = valueGroupedBy.get(groupField);

            // Op :
            switch (what) {
                case SUM:
                case AVG:
                    valueGroupedBy.put(groupField, value + aggregateValue);
                    break;
                case MAX:
                    valueGroupedBy.put(groupField, Math.max(value, aggregateValue));
                    break;
                case MIN:
                    valueGroupedBy.put(groupField, Math.min(value, aggregateValue));
                    break;
                case COUNT:
                default:
                    break;
            }
        }
    }

    /**
     * Create a OpIterator over group aggregate results.
     * 
     * @return a OpIterator whose tuples are the pair (groupVal, aggregateVal)
     *         if using group, or a single (aggregateVal) if no grouping. The
     *         aggregateVal is determined by the type of aggregate specified in
     *         the constructor.
     */
    public OpIterator iterator() {
        // some code goes here
        boolean grouped = (gbfield != NO_GROUPING);

        // Construct a new td
        TupleDesc td;
        Type[] typeArr;
        String[] stringArr;
        // Construct a new td
        if (grouped) {
            typeArr = new Type[]{gbFieldType, Type.INT_TYPE};
            stringArr = new String[]{groupFieldName, what.toString()};
        }
        else {
            typeArr = new Type[]{Type.INT_TYPE};
            stringArr = new String[]{what.toString()};
        }
        td = new TupleDesc(typeArr, stringArr);

        // Construct a new tuple list
        ArrayList<Tuple> tuples = new ArrayList<>();

        for (Field group : countGroupedBy.keySet()) {
            Tuple tuple = new Tuple(td);
            int value = valueGroupedBy.get(group);
            if (what == Op.AVG){
                value /= countGroupedBy.get(group);
            }
            if (what == Op.COUNT){
                value = countGroupedBy.get(group);
            }

            if (grouped) {
                tuple.setField(0, group);
            }
            tuple.setField(grouped ? 1:0, new IntField(value));
            tuples.add(tuple);
        }
        return new TupleIterator(td, tuples);


    }

}
