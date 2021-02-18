/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2013-2015 Denis Forveille (titou10.titou10@gmail.com)
 * Copyright (C) 2010-2021 DBeaver Corp and others
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jkiss.dbeaver.ext.db2.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;

import java.sql.ResultSet;

/**
 * DB2 Bufferpool
 * 
 * @author Denis Forveille
 */
public class DB2Bufferpool extends DB2GlobalObject {

    private String name;
    private Integer id;
    private String dbpgName;
    private Integer nPages;
    private Integer pageSize;
    private Integer numBlockPages;
    private Integer blockSize;

    // -----------------
    // Constructors
    // -----------------

    public DB2Bufferpool(DB2DataSource db2DataSource, ResultSet dbResult)
    {
        super(db2DataSource, true);

        this.name = JDBCUtils.safeGetString(dbResult, "BPNAME");
        this.id = JDBCUtils.safeGetInteger(dbResult, "BUFFERPOOLID");
        this.dbpgName = JDBCUtils.safeGetString(dbResult, "DBPGNAME");
        this.nPages = JDBCUtils.safeGetInteger(dbResult, "NPAGES");
        this.pageSize = JDBCUtils.safeGetInteger(dbResult, "PAGESIZE");
        this.numBlockPages = JDBCUtils.safeGetInteger(dbResult, "NUMBLOCKPAGES");
        this.blockSize = JDBCUtils.safeGetInteger(dbResult, "BLOCKSIZE");
    }

    // -----------------
    // Properties
    // -----------------

    @NotNull
    @Override
    @Property(viewable = true, editable = false, order = 1)
    public String getName()
    {
        return name;
    }

    @Property(viewable = true, editable = false, order = 2)
    public Integer getId()
    {
        return id;
    }

    @Property(viewable = true, editable = false, order = 3)
    public String getDbpgName()
    {
        return dbpgName;
    }

    @Property(viewable = true, editable = false, order = 4)
    public Integer getnPages()
    {
        return nPages;
    }

    @Property(viewable = true, editable = false, order = 5)
    public Integer getPageSize()
    {
        return pageSize;
    }

    @Property(viewable = true, editable = false, order = 7)
    public Integer getNumBlockPages()
    {
        return numBlockPages;
    }

    @Property(viewable = true, editable = false, order = 8)
    public Integer getBlockSize()
    {
        return blockSize;
    }
}
