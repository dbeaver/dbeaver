package org.jkiss.dbeaver.model.dbc;

import java.util.List;

/**
 * Result set table metadata
 */
public interface DBCTableMetaData {

    /**
     * Table name
     * @return table name
     */
    String getTableName();

    /**
     * Table alias
     * @return table alias in query
     */
    String getTableAlias();

    /**
     * Checks table is identitied.
     * Table is identitied if resultset contains at least one set of this table columns which will unique
     * identify table row
     * @return true if this table has at least one unique identitier in the whole resultset.
     */
    boolean isIdentitied();

    /**
     * Gets best table identifier.
     * Best identifier is a primary key. If no such one then any unique key fits.
     * @return list of identifier columns which identifies this table row the best way
     * or null if no identifiers found.
     */
    List<DBCColumnMetaData> getBestIdentifier();

}
