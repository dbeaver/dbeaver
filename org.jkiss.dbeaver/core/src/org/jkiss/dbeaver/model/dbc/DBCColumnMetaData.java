package org.jkiss.dbeaver.model.dbc;

import org.jkiss.dbeaver.model.struct.DBSForeignKey;
import org.jkiss.dbeaver.DBException;

import java.util.List;

/**
 * DBCColumnMetaData
 */
public interface DBCColumnMetaData
{
    int getIndex();

    boolean isAutoIncrement();

    boolean isNullable();

    int getDisplaySize();

    String getLabel();

    String getName();

    int getPrecision();

    int getScale();

    String getTableName();

    int getType();

    String getTypeName();

    boolean isReadOnly();

    boolean isWritable();

    /**
     * Owner table metadata
     * @return table metadata
     * @throws DBCException on any DB error
     */
    DBCTableMetaData getTable() throws DBException;

    /**
     * Check this column is a reference.
     * Reference columns are included in one or more foreign keys. 
     * @return true or false.
     * @throws DBCException on any DB error
     */
    boolean isReference() throws DBException;

    /**
     * Gets list of foreign keys in which this column is contained.
     * @return list of foreign keys. List can be empty or result can be null if this column is not a reference
     * @throws DBCException on any DB error
     */
    List<DBSForeignKey> getReferences() throws DBException;

}
