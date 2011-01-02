/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.exec;

/**
 * Query transformer
 */
public interface DBCQueryTransformer {

    void setParameters(Object ... parameters);

    String transformQueryString(String query)
        throws DBCException;

    void transformStatement(DBCStatement statement, int parameterIndex)
        throws DBCException;

}
