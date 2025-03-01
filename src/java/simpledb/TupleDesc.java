package simpledb;

import java.io.Serializable;
import java.util.*;

/**
 * TupleDesc describes the schema of a tuple.
 */
public class TupleDesc implements Serializable {

    /** Create an arraylist to store fields  **/
    private ArrayList<TDItem> itrFieldList;

    private int size = 0;


    /**
     * A help class to facilitate organizing the information of each field
     * */
    public static class TDItem implements Serializable {

        private static final long serialVersionUID = 1L;

        /**
         * The type of the field
         * */
        public final Type fieldType;
        
        /**
         * The name of the field
         * */
        public final String fieldName;

        public TDItem(Type t, String n) {
            this.fieldName = n;
            this.fieldType = t;
        }

        public String toString() {
            return fieldName + "(" + fieldType + ")";
        }
    }

    /**
     * @return
     *        An iterator which iterates over all the field TDItems
     *        that are included in this TupleDesc
     * */
    public Iterator<TDItem> iterator() {
        // some code goes here

        // How to use a iterator() in ArrayList
        // SOURCE: https://www.geeksforgeeks.org/arraylist-iterator-method-in-java-with-examples/
        // ps: iterator includes: hasNext(), next()
        return itrFieldList.iterator();
    }

    private static final long serialVersionUID = 1L;

    /**
     * Create a new TupleDesc with typeAr.length fields with fields of the
     * specified types, with associated named fields.
     * 
     * @param typeAr
     *            array specifying the number of and types of fields in this
     *            TupleDesc. It must contain at least one entry.
     * @param fieldAr
     *            array specifying the names of the fields. Note that names may
     *            be null.
     */
    public TupleDesc(Type[] typeAr, String[] fieldAr) {
        // some code goes here
        // use the two inputs typeAr and fieldAr to pass the values to the new created TDItem "dummy", and add it to the arraylist.
        itrFieldList = new ArrayList<>();
        for(int i = 0; i < typeAr.length; i++){
            TDItem dummy = new TDItem(typeAr[i], fieldAr[i]);
            itrFieldList.add(dummy);
            size += dummy.fieldType.getLen();
        }

        // put the size here.
    }

    /**
     * Constructor. Create a new tuple desc with typeAr.length fields with
     * fields of the specified types, with anonymous (unnamed) fields.
     * 
     * @param typeAr
     *            array specifying the number of and types of fields in this
     *            TupleDesc. It must contain at least one entry.
     */
    public TupleDesc(Type[] typeAr) {
        // some code goes here

        // Similar approach to TupleDesc(Type[] typeAr, String[] fieldAr), but set the name of the field null.
        itrFieldList = new ArrayList<>();
        for(int i = 0; i < typeAr.length; i++){
            TDItem dummy = new TDItem(typeAr[i], "");
            itrFieldList.add(dummy);
            size += dummy.fieldType.getLen();
        }
        // put the size here.
    }

    /**
     * @return the number of fields in this TupleDesc
     */
    public int numFields() {
        // some code goes here
//        int count = 0;
//        Iterator<TDItem> iter = itrFieldList.iterator();
//        while(iter.hasNext()){
//            count ++;
//            iter.next();
//        }
        // use size() to determine the number of field instead
        return itrFieldList.size();
    }

    /**
     * Gets the (possibly null) field name of the ith field of this TupleDesc.
     * 
     * @param i
     *            index of the field name to return. It must be a valid index.
     * @return the name of the ith field
     * @throws NoSuchElementException
     *             if i is not a valid field reference.
     */
    public String getFieldName(int i) throws NoSuchElementException {
        // some code goes here
//
        if (i < 0 || i >= itrFieldList.size()){
            throw new NoSuchElementException("i is not a valid field reference.");
        }
        TDItem e = itrFieldList.get(i);
        return e.fieldName;
    }

    /**
     * Gets the type of the ith field of this TupleDesc.
     * 
     * @param i
     *            The index of the field to get the type of. It must be a valid
     *            index.
     * @return the type of the ith field
     * @throws NoSuchElementException
     *             if i is not a valid field reference.
     */
    public Type getFieldType(int i) throws NoSuchElementException {
        // some code goes here
        if (i < 0 || i >= itrFieldList.size()){
            throw new NoSuchElementException("i is not a valid field reference.");
        }
        TDItem e = itrFieldList.get(i);
        return e.fieldType;
    }

    /**
     * Find the index of the field with a given name.
     * 
     * @param name
     *            name of the field.
     * @return the index of the field that is first to have the given name.
     * @throws NoSuchElementException
     *             if no field with a matching name is found.
     */
    public int fieldNameToIndex(String name) throws NoSuchElementException {
        // some code goes here
//        itrFieldList.contains()
        for (int i = 0; i < itrFieldList.size(); i++){
            if (itrFieldList.get(i).fieldName.equals(name)){
                return i;
            }
        }
        throw new NoSuchElementException("No field with a matching name is found.");
    }

    /**
     * @return The size (in bytes) of tuples corresponding to this TupleDesc.
     *         Note that tuples from a given TupleDesc are of a fixed size.
     */
    // QUESTION: is this the total size of the TupleDesc?
    public int getSize() {
        // some code goes here
//        int size = 0;
//        for (TDItem d : itrFieldList){
//            size += d.fieldType.getLen();
//        } Move to the front
        return size;
    }

    /**
     * Merge two TupleDescs into one, with td1.numFields + td2.numFields fields,
     * with the first td1.numFields coming from td1 and the remaining from td2.
     * 
     * @param td1
     *            The TupleDesc with the first fields of the new TupleDesc
     * @param td2
     *            The TupleDesc with the last fields of the TupleDesc
     * @return the new TupleDesc
     */
    public static TupleDesc merge(TupleDesc td1, TupleDesc td2) {
        // some code goes here
        int totalSize = td1.numFields() + td2.numFields();
        Type[] t1 = new Type[totalSize];
        String[] s1 = new String[totalSize];

        // set java String[]
        // https://stackoverflow.com/questions/2564298/java-how-to-initialize-string
        for (int i = 0; i < td1.numFields(); i++){
            t1[i] = td1.getFieldType(i);
            s1[i] = td1.getFieldName(i);
        }
        for (int i = 0; i < td2.numFields(); i++){
            t1[i + td1.numFields()] = td2.getFieldType(i);
            s1[i + td1.numFields()] = td2.getFieldName(i);
        }

        TupleDesc tdMerge = new TupleDesc(t1,s1);
        return tdMerge;
    }

    /**
     * Compares the specified object with this TupleDesc for equality. Two
     * TupleDescs are considered equal if they have the same number of items
     * and if the i-th type in this TupleDesc is equal to the i-th type in o
     * for every i.
     * 
     * @param o
     *            the Object to be compared for equality with this TupleDesc.
     * @return true if the object is equal to this TupleDesc.
     */

    public boolean equals(Object o) {
        // some code goes here

        /** usage of instanceof:
        instanceof is a keyword that is used for checking if a reference variable is containing a given type of object reference or not. **/
        if (o instanceof TupleDesc){
            if (((TupleDesc) o).numFields() == this.numFields()){
                for (int i = 0; i < this.numFields(); i++) {
                    if (((TupleDesc) o).getFieldType(i).equals(this.getFieldType(i))) {
                        continue;
                    } else {
                        return false;
                    }
                }
            }
            else {
                return false;
            }

        }
        else {
            return false;
        }
        return true;
    }

    public int hashCode() {
        // If you want to use TupleDesc as keys for HashMap, implement this so
        // that equal objects have equals hashCode() results
        throw new UnsupportedOperationException("unimplemented");
    }

    /**
     * Returns a String describing this descriptor. It should be of the form
     * "fieldType[0](fieldName[0]), ..., fieldType[M](fieldName[M])", although
     * the exact format does not matter.
            *
            * @return String describing this descriptor.
     */
    public String toString() {
        // some code goes here
        String result = "";
        for (TDItem d: itrFieldList){
            result += (d.fieldType.toString() + "(");
            result += (d.fieldName + ")");
        }
        return result;
    }
}
