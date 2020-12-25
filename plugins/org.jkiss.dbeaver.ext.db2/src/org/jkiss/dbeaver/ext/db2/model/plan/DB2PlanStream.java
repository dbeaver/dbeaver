/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2013-2015 Denis Forveille (titou10.titou10@gmail.com)
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.db2.model.plan;

import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.utils.CommonUtils;

/**
 * DB2 EXPLAIN_STREAM table
 * 
 * @author Denis Forveille
 */
public class DB2PlanStream {

    private DB2PlanStatement db2Statement;

    private Integer streamId;

    private DB2PlanNodeType sourceType;
    private Integer sourceId;

    private DB2PlanNodeType targetType;
    private Integer targetId;

    private String objectSchema;
    private String objectName;

    private Double streamCount;

    // ------------
    // Constructors
    // ------------

    public DB2PlanStream(JDBCResultSet dbResult, DB2PlanStatement db2Statement)
    {
        this.db2Statement = db2Statement;

        this.streamId = JDBCUtils.safeGetInteger(dbResult, "STREAM_ID");
        this.sourceType = CommonUtils.valueOf(DB2PlanNodeType.class, JDBCUtils.safeGetString(dbResult, "SOURCE_TYPE"));
        this.sourceId = JDBCUtils.safeGetInteger(dbResult, "SOURCE_ID");
        this.targetType = CommonUtils.valueOf(DB2PlanNodeType.class, JDBCUtils.safeGetString(dbResult, "TARGET_TYPE"));
        this.targetId = JDBCUtils.safeGetInteger(dbResult, "TARGET_ID");
        this.objectSchema = JDBCUtils.safeGetStringTrimmed(dbResult, "OBJECT_SCHEMA");
        this.objectName = JDBCUtils.safeGetString(dbResult, "OBJECT_NAME");
        this.streamCount = JDBCUtils.safeGetDouble(dbResult, "STREAM_COUNT");
    }

    public String getSourceName()
    {
        if (sourceType.equals(DB2PlanNodeType.O)) {
            // Operator
            return DB2PlanOperator.buildName(sourceId);
        } else {
            // Data Object
            return DB2PlanObject.buildName(objectSchema, objectName);
        }
    }

    public String getTargetName()
    {
        if (targetType.equals(DB2PlanNodeType.O)) {
            // Operator
            return DB2PlanOperator.buildName(targetId);
        } else {
            // D: Data Object
            return DB2PlanObject.buildName(objectSchema, objectName);
        }
    }

    // ----------------
    // Standard Getters
    // ----------------

    public Integer getStreamId()
    {
        return streamId;
    }

    public DB2PlanNodeType getSourceType()
    {
        return sourceType;
    }

    public Integer getSourceId()
    {
        return sourceId;
    }

    public DB2PlanNodeType getTargetType()
    {
        return targetType;
    }

    public Integer getTargetId()
    {
        return targetId;
    }

    public String getObjectSchema()
    {
        return objectSchema;
    }

    public String getObjectName()
    {
        return objectName;
    }

    public DB2PlanStatement getDb2Statement()
    {
        return db2Statement;
    }

    public Double getStreamCount()
    {
        return streamCount;
    }

}
