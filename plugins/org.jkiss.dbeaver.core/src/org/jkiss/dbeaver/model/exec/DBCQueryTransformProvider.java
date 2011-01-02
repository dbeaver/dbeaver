/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.exec;

/**
 * Query transform provider.
 * This interface could be implemented by {@link org.jkiss.dbeaver.model.DBPDataSource} implementor.
 */
public interface DBCQueryTransformProvider {

    /**
     * Creates new query transformer
     * @param type transformation type
     * @return new transformer or null if transformation of specified type is not supported
     */
    DBCQueryTransformer createQueryTransformer(DBCQueryTransformType type);

}
