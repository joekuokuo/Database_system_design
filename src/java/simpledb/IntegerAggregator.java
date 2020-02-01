package simpledb;

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
        Field groupField = gbfield == NO_GROUPING ? new IntField(0): tup.getField(gbfield);
        groupFieldName = gbfield == NO_GROUPING ? null : tup.getTupleDesc().getFieldName(gbfield);
        IntField aggregateField = (IntField) tup.getField(afield);
        int countAddition = what == Op.SC_AVG ? ((IntField) tup.getField(afield+1)).getValue() : 1;
        if (!countGroupedBy.containsKey(groupField)) {
            countGroupedBy.put(groupField, countAddition);
            valueGroupedBy.put(groupField, aggregateField.getValue());
        } else {
            countGroupedBy.put(groupField, countGroupedBy.get(groupField)+countAddition);
            Integer value = valueGroupedBy.get(groupField);
            Integer aggregateValue = aggregateField.getValue();
            switch (what) {
                case AVG:
                case SUM_COUNT:
                case SUM:
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
        boolean hasGroup = gbfield != NO_GROUPING;
        // To be continue...



        throw new
        UnsupportedOperationException("please implement me for lab2");
    }

}
