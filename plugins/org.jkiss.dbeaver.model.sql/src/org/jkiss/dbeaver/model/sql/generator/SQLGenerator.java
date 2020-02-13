/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.model.sql.generator;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBPScriptObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithResult;
import org.jkiss.dbeaver.model.struct.DBSEntity;

import java.lang.reflect.InvocationTargetException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public abstract class SQLGenerator<OBJECT> extends DBRRunnableWithResult<String> {
    protected List<OBJECT> objects;
    private boolean fullyQualifiedNames = true;
    private boolean compactSQL = false;
    private Map<String, Object> generatorOptions = new LinkedHashMap<>();

    public void initGenerator(List<OBJECT> objects) {
        this.objects = objects;
    }

    public List<OBJECT> getObjects() {
        return objects;
    }

    public boolean isFullyQualifiedNames() {
        return fullyQualifiedNames;
    }

    public void setFullyQualifiedNames(boolean fullyQualifiedNames) {
        this.fullyQualifiedNames = fullyQualifiedNames;
    }

    public boolean isCompactSQL() {
        return compactSQL;
    }

    public void setCompactSQL(boolean compactSQL) {
        this.compactSQL = compactSQL;
    }

    public Object getGeneratorOption(String name) {
        return generatorOptions.get(name);
    }

    public void setGeneratorOption(String name, Object value) {
        if (value == null) {
            generatorOptions.remove(name);
        } else {
            generatorOptions.put(name, value);
        }
    }

    protected String getLineSeparator() {
        return compactSQL ? " " : "\n";
    }

    protected String getEntityName(DBSEntity entity) {
        if (fullyQualifiedNames) {
            return DBUtils.getObjectFullName(entity, DBPEvaluationContext.DML);
        } else {
            return DBUtils.getQuotedIdentifier(entity);
        }
    }

    protected void addOptions(Map<String, Object> options) {
        options.put(DBPScriptObject.OPTION_FULLY_QUALIFIED_NAMES, isFullyQualifiedNames());
        options.put(DBPScriptObject.OPTION_SCRIPT_FORMAT_COMPACT, isCompactSQL());
        options.putAll(generatorOptions);
    }

    @Override
    public void run(DBRProgressMonitor monitor) throws InvocationTargetException, InterruptedException
    {
        StringBuilder sql = new StringBuilder(100);
        try {
            for (OBJECT object : objects) {
                generateSQL(monitor, sql, object);
            }
        } catch (DBException e) {
            throw new InvocationTargetException(e);
        }
        result = sql.toString();
    }

    protected abstract void generateSQL(DBRProgressMonitor monitor, StringBuilder sql, OBJECT object)
        throws DBException;

}
