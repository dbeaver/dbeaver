/*
 * DBeaver - Universal Database Manager
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

package org.jkiss.dbeaver.ext.oracle.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.oracle.model.source.OracleSourceObject;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBPRefreshableObject;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectState;
import org.jkiss.utils.CommonUtils;

import java.sql.ResultSet;
import java.util.Map;

/**
 * Java class
 */
public class OracleJavaClass extends OracleSchemaObject implements OracleSourceObject, DBPRefreshableObject {

    private final boolean isInner;
    private final boolean isAbstract;
    private final boolean isFinal;
    private final boolean isStatic;
    private final boolean isDebug;
    private final String source;
    private final String superClass;

    private String sourceCode;

    public enum Accessibility {
        PUBLIC,
        PRIVATE,
        PROTECTED
    }

    private boolean isInterface;
    private Accessibility accessibility;

    protected OracleJavaClass(OracleSchema schema, ResultSet dbResult)
    {
        super(schema, JDBCUtils.safeGetString(dbResult, "NAME"), true);
        this.isInterface = "INTERFACE".equals(JDBCUtils.safeGetString(dbResult, "KIND"));
        this.accessibility = CommonUtils.valueOf(Accessibility.class, JDBCUtils.safeGetString(dbResult, "ACCESSIBILITY"));

        this.isInner = JDBCUtils.safeGetBoolean(dbResult, "IS_INNER", OracleConstants.YES);
        this.isAbstract = JDBCUtils.safeGetBoolean(dbResult, "IS_ABSTRACT", OracleConstants.YES);
        this.isFinal = JDBCUtils.safeGetBoolean(dbResult, "IS_FINAL", OracleConstants.YES);
        this.isStatic = JDBCUtils.safeGetBoolean(dbResult, "IS_STATIC", OracleConstants.YES);
        this.isDebug = JDBCUtils.safeGetBoolean(dbResult, "IS_DEBUG", OracleConstants.YES);
        this.source = JDBCUtils.safeGetString(dbResult, "SOURCE");
        this.superClass = JDBCUtils.safeGetString(dbResult, "SUPER");
    }

    @Property(viewable = true, order = 2)
    public Accessibility getAccessibility()
    {
        return accessibility;
    }

    @Property(viewable = true, order = 3)
    public boolean isInterface()
    {
        return isInterface;
    }

    @Property(viewable = false, order = 10)
    public boolean isInner() {
        return isInner;
    }

    @Property(viewable = true, order = 11)
    public boolean isAbstract() {
        return isAbstract;
    }

    @Property(viewable = true, order = 12)
    public boolean isFinal() {
        return isFinal;
    }

    @Property(viewable = true, order = 13)
    public boolean isStatic() {
        return isStatic;
    }

    @Property(viewable = false, order = 14)
    public boolean isDebug() {
        return isDebug;
    }

    @Property(viewable = true, order = 15)
    public String getSuperClass() {
        return superClass;
    }

    @Override
    public OracleSourceType getSourceType() {
        return OracleSourceType.JAVA_SOURCE;
    }

    public String getSourceName() {
        return source;
    }

    @Override
    public DBEPersistAction[] getCompileActions(DBRProgressMonitor monitor) {
        return new DBEPersistAction[] {
            new OracleObjectPersistAction(
                OracleObjectType.JAVA_CLASS,
                "Compile Java class",
                "ALTER JAVA CLASS " + getFullyQualifiedName(DBPEvaluationContext.DDL) + " COMPILE"
            )};
    }

    @NotNull
    @Override
    public DBSObjectState getObjectState() {
        return DBSObjectState.NORMAL;
    }

    @Override
    public void refreshObjectState(@NotNull DBRProgressMonitor monitor) throws DBCException {

    }

    @Override
    public String getObjectDefinitionText(DBRProgressMonitor monitor, Map<String, Object> options) throws DBException {
        if (sourceCode != null) {
            return sourceCode;
        }
        if (CommonUtils.isEmpty(source)) {
            return "-- Source unavailable for " + getName();
        }
        sourceCode = OracleUtils.getSource(monitor, this, false, false);
        if (CommonUtils.isEmpty(sourceCode)) {
            sourceCode = "-- No source code found for Java class " + getName();
        }
        return sourceCode;
    }

    @Override
    public void setObjectDefinitionText(String source) {
        this.sourceCode = source;
    }

    @Nullable
    @Override
    public DBSObject refreshObject(@NotNull DBRProgressMonitor monitor) throws DBException {
        sourceCode = null;
        return this;
    }

}
