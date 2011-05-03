package org.jkiss.dbeaver.model.impl.jdbc.struct;

/**
 * Column key type
 */
public interface JDBCColumnKeyType {
    /**
     * Checks presence in unique key
     * @return true if column is in any primary/unique key column list
     */
    boolean isInUniqueKey();

    /**
     * Checks presence in reference key
     * @return true if column is in any foreign key column list
     */
    boolean isInReferenceKey();

}
