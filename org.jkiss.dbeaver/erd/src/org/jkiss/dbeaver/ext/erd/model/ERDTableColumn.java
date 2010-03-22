package org.jkiss.dbeaver.ext.erd.model;

import org.jkiss.dbeaver.model.struct.DBSTableColumn;
import org.jkiss.dbeaver.model.struct.DBSPrimaryKey;
import org.jkiss.dbeaver.model.struct.DBSConstraintColumn;
import org.jkiss.dbeaver.DBException;

import java.util.Collection;

/**
 * ERDTableColumn
 */
public class ERDTableColumn implements Comparable<ERDTableColumn> {

    private ERDTable table;
    private DBSTableColumn metaColumn;
    private String name;
    private int keyNumber;

    public ERDTableColumn(ERDTable table, DBSTableColumn metaColumn)
        throws DBException
    {
        this.table = table;
        this.metaColumn = metaColumn;
        this.name = metaColumn.getName();
        this.keyNumber = 0;
        // Try to get key number from primary key constraint metaColumn
        DBSPrimaryKey primaryKey = table.getPrimaryKey();
        if (primaryKey != null) {
            Collection<DBSConstraintColumn> constrColumns = primaryKey.getColumns();
            for (DBSConstraintColumn constrCol : constrColumns) {
                if (constrCol.getTableColumn().equals(metaColumn)) {
                    keyNumber = constrCol.getOrdinalPosition();
                    break;
                }
            }
        }
    }

    public ERDTable getTable()
    {
        return table;
    }

    public DBSTableColumn getMetaColumn()
    {
        return metaColumn;
    }

    public String getName()
    {
        return name;
    }

    public boolean isKey()
    {
        return keyNumber > 0;
    }

    public int getKeyNumber()
    {
        return keyNumber;
    }

    public int compareTo(ERDTableColumn o)
    {
        return
            keyNumber > 0 ? -keyNumber : metaColumn.getOrdinalPosition() -
            o.keyNumber > 0 ? -o.keyNumber : o.metaColumn.getOrdinalPosition();
    }

}
