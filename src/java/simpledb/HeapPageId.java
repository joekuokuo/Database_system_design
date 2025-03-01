package simpledb;
import java.util.Objects;
import java.io.Serializable;
/** Unique identifier for HeapPage objects. */
public class HeapPageId implements PageId,Serializable {

    private final int tableId;
    private final int pgNo;
    /**
     * Constructor. Create a page id structure for a specific page of a
     * specific table.
     *
     * @param tableId The table that is being referenced
     * @param pgNo The page number in that table.
     */
    public HeapPageId(int tableId, int pgNo) {
        // some code goes here
        this.tableId = tableId;
        this.pgNo = pgNo;
    }

    /** @return the table associated with this PageId */
    public int getTableId() {
        // some code goes here
        return tableId;
    }

    /**
     * @return the page number in the table getTableId() associated with
     *   this PageId
     */
    public int getPageNumber() {
        // some code goes here
        return pgNo;
    }

    /**
     * @return a hash code for this page, represented by the concatenation of
     *   the table number and the page number (needed if a PageId is used as a
     *   key in a hash table in the BufferPool, for example.)
     * @see BufferPool
     */
    public int hashCode() {
        // some code goes here
        return tableId * 10000 + pgNo;
    }

    /**
     * Compares one PageId to another.
     *
     * @param o The object to compare against (must be a PageId)
     * @return true if the objects are equal (e.g., page numbers and table
     *   ids are the same)
     */
    public boolean equals(Object o) {
        // some code goes here
        // To determine whether they are the same class.
        // reference: https://stackoverflow.com/questions/6821810/determine-if-two-java-objects-are-of-the-same-class
        // boolean result = object1.getClass().equals( object2.getClass());
        if (o == null || !o.getClass().equals(this.getClass())){
            return false;
        }
        // transform o to HeapPagId class
        HeapPageId that = (HeapPageId) o;

        // check if they share the same tableId and pgNo.
        if (this.tableId == that.tableId && this.pgNo == that.pgNo) {
            return true;
        }
        return false;
    }

    /**
     *  Return a representation of this object as an array of
     *  integers, for writing to disk.  Size of returned array must contain
     *  number of integers that corresponds to number of args to one of the
     *  constructors.
     */
    public int[] serialize() {
        int data[] = new int[2];

        data[0] = getTableId();
        data[1] = getPageNumber();

        return data;
    }

}
