/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2022 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.altibase.model;

import java.sql.ResultSet;

import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.utils.ByteNumberFormat;

public class AltibaseDataFile4Mem extends AltibaseDataFile {

    private long dbFileSize;
    
    protected AltibaseDataFile4Mem(AltibaseTablespace tablespace, ResultSet dbResult) {
        super(tablespace, dbResult);
        dbFileSize = (long) JDBCUtils.safeGetDouble(dbResult, "DBFILE_SIZE");
    }

    @Property(viewable = true, order = 3, formatter = ByteNumberFormat.class)
    public long getDbFileSize()
    {
        return dbFileSize;
    }
}
