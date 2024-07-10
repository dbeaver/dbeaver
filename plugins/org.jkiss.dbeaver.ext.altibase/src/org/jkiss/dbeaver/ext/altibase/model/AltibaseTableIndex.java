/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.altibase.AltibaseConstants;
import org.jkiss.dbeaver.ext.generic.model.GenericTableBase;
import org.jkiss.dbeaver.ext.generic.model.GenericTableIndex;
import org.jkiss.dbeaver.model.DBPScriptObject;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.rdb.DBSIndexType;

import java.util.Map;

public class AltibaseTableIndex extends GenericTableIndex implements DBPScriptObject {

    private String source;
    
    public AltibaseTableIndex(GenericTableBase table, boolean nonUnique, String qualifier, long cardinality,
            String indexName, DBSIndexType indexType, boolean persisted) {
        super(table, nonUnique, qualifier, cardinality, indexName, indexType, persisted);
    }
    
    public boolean isSystemGenerated() {
        return this.getName().startsWith(AltibaseConstants.SYSTEM_GENERATED_PREFIX);
    }
    
    @Override
    public String getObjectDefinitionText(DBRProgressMonitor monitor, Map<String, Object> options) throws DBException {
        if (source == null && isPersisted()) {
            source = ((AltibaseMetaModel) getDataSource().getMetaModel()).getIndexDDL(monitor, this, options);
        }
        return source;
    }
    
    @Override
    @Property(hidden = true)
    public String getQualifier() {
        return "";
    }
}
