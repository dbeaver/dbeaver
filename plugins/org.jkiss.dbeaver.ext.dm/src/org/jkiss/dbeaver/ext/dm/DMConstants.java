/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.dm;

import org.jkiss.dbeaver.model.struct.rdb.DBSIndexType;

/**
 * @author Shengkai Bai
 */
public class DMConstants {


    // 位图索引 BITMAP
    // 空间索引 SPATIAL
    // 聚簇索引 CLUSTER
    public static final DBSIndexType INDEX_TYPE_BITMAP = new DBSIndexType("BITMAP", "BitMap");
    public static final DBSIndexType INDEX_TYPE_SPATIAL = new DBSIndexType("SPATIAL", "SPATIAL");
    public static final DBSIndexType INDEX_TYPE_CLUSTER = new DBSIndexType("CLUSTER", "cluster");
    public static final DBSIndexType INDEX_TYPE_UNIQUE = new DBSIndexType("UNIQUE", "Unique");

    public enum ObjectType {
        CLASS,
        CLASS_HEAD,
        CLASS_BODY,
        COL_STATISTICS,
        COMMENT,
        CONSTRAINT,
        CONTEXT,
        CONTEXT_INDEX,
        DATABASE_EXPORT,
        DB_LINK,
        DIRECTORY,
        DOMAIN,
        FUNCTION,
        INDEX,
        INDEX_STATISTICS,
        JOB,
        OBJECT_GRANT,
        PACKAGE,
        PKG_SPEC,
        PKG_BODY,
        POLICY,
        PROCEDURE,
        ROLE,
        ROLE_GRANT,
        SCHEMA_EXPORT,
        SEQUENCE,
        SYNONYM,
        SYSTEM_GRANT,
        TABLE,
        TABLE_STATISTICS,
        TABLE_EXPORT,
        TABLESPACE,
        TRIGGER,
        USER,
        VIEW,
        TYPE,
        MATERIALIZED_VIEW,
        MATERIALIZED_VIEW_LOG,
        PROFILE,
    }
}
