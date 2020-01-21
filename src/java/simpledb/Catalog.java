package simpledb;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The Catalog keeps track of all available tables in the database and their
 * associated schemas.
 * For now, this is a stub catalog that must be populated with tables by a
 * user program before it can be used -- eventually, this should be converted
 * to a catalog that reads a catalog table from disk.
 * 
 * @Threadsafe
 */
public class Catalog {

//    ArrayList<DbFile> tableList;
//    ArrayList<Integer> idList;
//    ArrayList<String> name;
//    ArrayList<String> pkeyField;
//    String[] tableValueList;

    private HashMap<String, DbFile> nameMap;
    private HashMap<Integer, DbFile> idMap;
    private HashMap<String, DbFile> pkeyFieldMap;
    private HashMap<Integer, String> idToNameMap;
    private HashMap<Integer, String> idTopkeyfieldMap;


    /**
     * Constructor.
     * Creates a new, empty catalog.
     */
    public Catalog() {
        // some code goes here
        nameMap = new HashMap<>();
        idMap = new HashMap<>();
        pkeyFieldMap = new HashMap<>();
        idToNameMap = new HashMap<>();
        idTopkeyfieldMap = new HashMap<>();

//        tableList = new ArrayList<>();
//        idList = new ArrayList<>();
//        name = new ArrayList<>();
//        pkeyField = new ArrayList<>();

    }

    /**
     * Add a new table to the catalog.
     * This table's contents are stored in the specified DbFile.
     * @param file the contents of the table to add;  file.getId() is the identfier of
     *    this file/tupledesc param for the calls getTupleDesc and getFile
     * @param name the name of the table -- may be an empty string.  May not be null.  If a name
     * conflict exists, use the last table to be added as the table for a given name.
     * @param pkeyField the name of the primary key field
     */
    public void addTable(DbFile file, String name, String pkeyField) {
        // some code goes here

        nameMap.put(name, file);
        idMap.put(file.getId(), file);
        pkeyFieldMap.put(pkeyField, file);
        idToNameMap.put(file.getId(), name);
        idTopkeyfieldMap.put(file.getId(), pkeyField);
//        if (this.name.contains(name)){
//            int i = this.name.indexOf(name);
//            this.tableList.set(i, file);
//            this.pkeyField.set(i, pkeyField);
//        }
//        if (this.idList.contains(file.getId())){
//            int i = this.idList.indexOf(file.getId());
//            this.tableList.set(i, file);
//            this.name.set(i, name);
//            this.pkeyField.set(i, pkeyField);
//        }
//        else{
//            tableValueList = new String[3];
//
////            tableValueList = new ArrayList<>();
//
//            // set id into tableValueList
//            tableValueList[0] = Integer.toString(file.getId());
//
//            // set name into tableValueList
//            tableValueList[1] = name;
//
//            // set pkeyField into tableValueList
//            tableValueList[2] = pkeyField;
//            // set a map to store file and the rest of it's values
//            map.put(file, tableValueList);
//
//
//            tableList.add(file);
//            idList.add(file.getId());
//            this.name.add(name);
//            this.pkeyField.add(pkeyField);
//        }

    }

    public void addTable(DbFile file, String name) {
        addTable(file, name, "");
    }

    /**
     * Add a new table to the catalog.
     * This table has tuples formatted using the specified TupleDesc and its
     * contents are stored in the specified DbFile.
     * @param file the contents of the table to add;  file.getId() is the identfier of
     *    this file/tupledesc param for the calls getTupleDesc and getFile
     */
    public void addTable(DbFile file) {
        addTable(file, (UUID.randomUUID()).toString());
    }

    /**
     * Return the id of the table with a specified name,
     * @throws NoSuchElementException if the table doesn't exist
     */
    public int getTableId(String name) throws NoSuchElementException {
        // some code goes here
        if (this.nameMap.containsKey(name)){
//            int i = this.name.indexOf(name);
            return this.nameMap.get(name).getId();
        }

        else {
            throw new NoSuchElementException("No such name.");
        }

    }

    /**
     * Returns the tuple descriptor (schema) of the specified table
     * @param tableid The id of the table, as specified by the DbFile.getId()
     *     function passed to addTable
     * @throws NoSuchElementException if the table doesn't exist
     */
    public TupleDesc getTupleDesc(int tableid) throws NoSuchElementException {
        // some code goes here
//        for (int i = 0; i < tableList.size(); i++){
//            if (this.tableList.get(i).getId() == tableid){
//                return tableList.get(i).getTupleDesc();
//            }
//
//        }
        if (this.idMap.containsKey(tableid)){
            return this.idMap.get(tableid).getTupleDesc();
        }
        throw new NoSuchElementException("No such id.");
    }

    /**
     * Returns the DbFile that can be used to read the contents of the
     * specified table.
     * @param tableid The id of the table, as specified by the DbFile.getId()
     *     function passed to addTable
     */
    public DbFile getDatabaseFile(int tableid) throws NoSuchElementException {
        // some code goes here

//        for (int i = 0; i < tableList.size(); i++){
//            if (this.tableList.get(i).getId() == tableid){
//                return tableList.get(i);
//            }
//        }
        if (this.idMap.containsKey(tableid)){
            return this.idMap.get(tableid);
        }
        throw new NoSuchElementException("No such id.");
    }

    public String getPrimaryKey(int tableid) {
        // some code goes here
//        if (idList.contains(tableid)){
//            return pkeyField.get(idList.indexOf(tableid));
//        }
        if (this.idMap.containsKey(tableid)){
            return this.idTopkeyfieldMap.get(tableid);
        }
        throw new NoSuchElementException("No such id.");
    }

    public Iterator<Integer> tableIdIterator() {
        // some code goes here
//        return tableIdIterator();
        return idMap.keySet().iterator();
//        return null;
    }

    public String getTableName(int id) {
        // some code goes here
//        if (idList.contains(id)){
//            return name.get(idList.indexOf(id));
//        }
        if (this.idMap.containsKey(id)){
            return this.idToNameMap.get(id);
        }
        throw new NoSuchElementException("No such id.");
    }
    
    /** Delete all tables from the catalog */
    public void clear() {
        // some code goes here
        nameMap = new HashMap<>();
        idMap = new HashMap<>();
        pkeyFieldMap = new HashMap<>();
        idToNameMap = new HashMap<>();
        idTopkeyfieldMap = new HashMap<>();
//        ArrayList<DbFile> tableList = new ArrayList<>();
//        ArrayList<Integer> idList = new ArrayList<>();
//        ArrayList<String> name = new ArrayList<>();
//        ArrayList<String> pkeyField = new ArrayList<>();
    }
    
    /**
     * Reads the schema from a file and creates the appropriate tables in the database.
     * @param catalogFile
     */
    public void loadSchema(String catalogFile) {
        String line = "";
        String baseFolder=new File(new File(catalogFile).getAbsolutePath()).getParent();
        try {
            BufferedReader br = new BufferedReader(new FileReader(new File(catalogFile)));
            
            while ((line = br.readLine()) != null) {
                //assume line is of the format name (field type, field type, ...)
                String name = line.substring(0, line.indexOf("(")).trim();
                //System.out.println("TABLE NAME: " + name);
                String fields = line.substring(line.indexOf("(") + 1, line.indexOf(")")).trim();
                String[] els = fields.split(",");
                ArrayList<String> names = new ArrayList<String>();
                ArrayList<Type> types = new ArrayList<Type>();
                String primaryKey = "";
                for (String e : els) {
                    String[] els2 = e.trim().split(" ");
                    names.add(els2[0].trim());
                    if (els2[1].trim().toLowerCase().equals("int"))
                        types.add(Type.INT_TYPE);
                    else if (els2[1].trim().toLowerCase().equals("string"))
                        types.add(Type.STRING_TYPE);
                    else {
                        System.out.println("Unknown type " + els2[1]);
                        System.exit(0);
                    }
                    if (els2.length == 3) {
                        if (els2[2].trim().equals("pk"))
                            primaryKey = els2[0].trim();
                        else {
                            System.out.println("Unknown annotation " + els2[2]);
                            System.exit(0);
                        }
                    }
                }
                Type[] typeAr = types.toArray(new Type[0]);
                String[] namesAr = names.toArray(new String[0]);
                TupleDesc t = new TupleDesc(typeAr, namesAr);
                HeapFile tabHf = new HeapFile(new File(baseFolder+"/"+name + ".dat"), t);
                addTable(tabHf,name,primaryKey);
                System.out.println("Added table : " + name + " with schema " + t);
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(0);
        } catch (IndexOutOfBoundsException e) {
            System.out.println ("Invalid catalog entry : " + line);
            System.exit(0);
        }
    }
}

