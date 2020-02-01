package simpledb;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

// Tests git push
/**
 * Tuple maintains information about the contents of a tuple. Tuples have a
 * specified schema specified by a TupleDesc object and contain Field objects
 * with the data for each field.
 */
public class Tuple implements Serializable {

    private TupleDesc td;
    private RecordId rid;
    private ArrayList<Field> arrFields;
    private static final long serialVersionUID = 1L;
//    private final TupleDesc ;

    /**
     * Create a new tuple with the specified schema (type).
     *
     * @param td
     *            the schema of this tuple. It must be a valid TupleDesc
     *            instance with at least one field.
     */
    public Tuple(TupleDesc td) {
        // some code goes here

        for (int i = 0; i < td.numFields(); i++) {
            // QUESTION: is this necessary ?
            if (td.getFieldType(i) == Type.INT_TYPE || td.getFieldType(i) == Type.STRING_TYPE) {
                continue;
            } else {
                return;
            }
        }
        this.td = td;
        arrFields = new ArrayList<>();

    }

    /**
     * @return The TupleDesc representing the schema of this tuple.
     */
    public TupleDesc getTupleDesc() {
        // some code goes here
        return td;
    }

    /**
     * @return The RecordId representing the location of this tuple on disk. May
     *         be null.
     */
    public RecordId getRecordId() {
        // some code goes here
        return rid;
    }

    /**
     * Set the RecordId information for this tuple.
     *
     * @param rid
     *            the new RecordId for this tuple.
     */
    public void setRecordId(RecordId rid) {
        // some code goes here
        this.rid = rid;
    }

    /**
     * Change the value of the ith field of this tuple.
     *
     * @param i
     *            index of the field to change. It must be a valid index.
     * @param f
     *            new value for the field.
     */
    public void setField(int i, Field f) {
        // some code goes here
        if (i >= 0 || i < td.numFields()){
            arrFields.add(i, f);
        }
    }

    /**
     * @return the value of the ith field, or null if it has not been set.
     *
     * @param i
     *            field index to return. Must be a valid index.
     */
    public Field getField(int i) {
        // some code goes here

        if (i >= 0|| i < td.numFields()){
            if (arrFields.get(i) == null){
                return null;
            }
            else {
                return arrFields.get(i);
            }
        }

        return arrFields.get(i);
    }

    /**
     * Returns the contents of this Tuple as a string. Note that to pass the
     * system tests, the format needs to be as follows:
     *
     * column1\tcolumn2\tcolumn3\t...\tcolumnN
     *
     * where \t is any whitespace (except a newline)
     */
    public String toString() {
        // some code goes here
        String l = "";
        l += arrFields.get(0).toString() + "\t";
        for (int i = 1; i < td.numFields(); i++){
            l += "\t" + arrFields.get(i).toString();
        }
        return l;
    }

    /**
     * @return
     *        An iterator which iterates over all the fields of this tuple
     * */
    public Iterator<Field> fields()
    {
        // some code goes here
        // Parameters: This method takes the array a which is required to be converted into a List.
        // Reference: Arrays asList() method in Java with Examples https://www.geeksforgeeks.org/arrays-aslist-method-in-java-with-examples/
        return arrFields.iterator();
//        return Arrays.asList(fields).iterator();
    }

    /**
     * reset the TupleDesc of this tuple (only affecting the TupleDesc)
     * */
    public void resetTupleDesc(TupleDesc td)
    {
        // some code goes here
        this.td = td;
    }

    public static Tuple merge(Tuple t1, Tuple t2) {
        Tuple tuple = new Tuple(TupleDesc.merge(t1.getTupleDesc(), t2.getTupleDesc()));
        tuple.rid = null;
        System.arraycopy(t1.arrFields.toArray(), 0, tuple.arrFields.toArray(), 0, t1.getTupleDesc().numFields());
        System.arraycopy(t2.arrFields.toArray(), 0, tuple.arrFields.toArray(), t1.getTupleDesc().numFields(),
                t2.getTupleDesc().numFields());
        return tuple;
    }
}
