package com.softartisans.timberwolf.hbase;

import java.util.HashMap;
import java.util.Map;

/**
 * Manages the set of HBaseTables currently in use.
 */
public class HBaseManager {

    private Map<String, IHBaseTable> tables = new HashMap<String, IHBaseTable> ();

    /**
     * Adds a HBaseTable to the underlying tables collection. If the table
     * already exists, it will do nothing.
     * @param table The IHBaseTable to add.
     */
    public void add(IHBaseTable table)
    {
        if (!tables.containsValue(table))
        {
            tables.put(table.getName(), table);
        }
    }

    /**
     * Gets a table by table name from HBase or from the underlying collection,
     * to prevent multiple references to the same HTable.
     * @param tableName The name of the HTable to get.
     * @return The IHBaseTable for this table name.
     */
    public IHBaseTable get(String tableName)
    {
        // TODO: Actually go get a table from HBase.
        return tables.get(tableName);
    }
}
