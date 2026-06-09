/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.yashandb.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.oracle.model.OracleDataSource;
import org.jkiss.dbeaver.ext.oracle.model.OracleTablespace;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.utils.CommonUtils;

import java.sql.ResultSet;
import java.util.Map;


/**
 * YashanDBTablespace
 */
public class YashanDBTablespace extends OracleTablespace {

    private final YashanDBContents yashanDBContents;
    private final YashanDBAllocationType yashanDBAllocationType;
    private final YashanDBSegmentSpaceManagement yashanDBSegmentSpaceManagement;

    public YashanDBTablespace(OracleDataSource dataSource, ResultSet dbResult) {
        super(dataSource, dbResult);
        this.yashanDBContents = CommonUtils.valueOf(YashanDBContents.class,
            JDBCUtils.safeGetString(dbResult, "CONTENTS"), null, true);
        this.yashanDBAllocationType = CommonUtils.valueOf(YashanDBAllocationType.class,
            JDBCUtils.safeGetString(dbResult, "ALLOCATION_TYPE"), null, true);
        this.yashanDBSegmentSpaceManagement = CommonUtils.valueOf(YashanDBSegmentSpaceManagement.class,
            JDBCUtils.safeGetString(dbResult, "SEGMENT_SPACE_MANAGEMENT"), null, true);
    }

    @Override
    @Property(editable = true, hidden = true, order = 30)
    public Contents getContents() {
        // YashanDB custom
        return null;
    }

    @Override
    @Property(editable = true, hidden = true, order = 34)
    public AllocationType getAllocationType() {
        // YashanDB custom
        return null;
    }

    @Override
    @Property(editable = true, hidden = true, order = 36)
    public SegmentSpaceManagement getSegmentSpaceManagement() {
        // YashanDB custom
        return null;
    }

    @Property(editable = true, order = 30)
    public YashanDBContents getYashanDBContents() {
        return yashanDBContents;
    }

    @Property(editable = true, order = 34)
    public YashanDBAllocationType getYashanDBAllocationType() {
        return yashanDBAllocationType;
    }

    @Property(editable = true, order = 36)
    public YashanDBSegmentSpaceManagement getYashanDBSegmentSpaceManagement() {
        return yashanDBSegmentSpaceManagement;
    }

    @Override
    public String getObjectDefinitionText(@NotNull DBRProgressMonitor monitor, @NotNull Map<String, Object> options) throws DBException {
        // YashanDB not support yet
        return "-- EMPTY DDL";
    }

    public enum YashanDBContents {
        PERMANENT, TEMPORARY, UNDO, SWAP
    }

    public enum YashanDBAllocationType {
        UNIFORM, AUTO
    }

    public enum YashanDBSegmentSpaceManagement {
        BITMAP
    }
}
