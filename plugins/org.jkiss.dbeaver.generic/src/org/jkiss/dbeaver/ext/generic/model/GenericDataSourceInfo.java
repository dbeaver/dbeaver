/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.generic.model;

import org.jkiss.dbeaver.model.exec.jdbc.JDBCDatabaseMetaData;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCDataSourceInfo;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Generic data source info
 */
class GenericDataSourceInfo extends JDBCDataSourceInfo {

    private static List<String> EXEC_KEYWORDS = new ArrayList<String>();

    static {
        EXEC_KEYWORDS.add("EXEC");
        //EXEC_KEYWORDS.add("EXECUTE");
        EXEC_KEYWORDS.add("CALL");
        //EXEC_KEYWORDS.add("BEGIN");
        //EXEC_KEYWORDS.add("DECLARE");
    }

    public GenericDataSourceInfo(DBSObject owner, JDBCDatabaseMetaData metaData)
    {
        super(owner, metaData);
    }

    @Override
    public Collection<String> getExecuteKeywords()
    {
        return EXEC_KEYWORDS;
    }
}
