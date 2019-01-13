/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2013-2015 Denis Forveille (titou10.titou10@gmail.com)
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
package org.jkiss.dbeaver.ext.db2.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.db2.editors.DB2ObjectType;
import org.jkiss.dbeaver.ext.db2.model.dict.DB2PackageDepType;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.utils.CommonUtils;

import java.sql.ResultSet;

/**
 * DB2 Package Dependency
 * 
 * @author Denis Forveille
 */
public class DB2PackageDep extends DB2Object<DB2Package> {

    private DB2PackageDepType packageDepType;
    private DB2Schema depSchema;
    private String depModuleName;
    private String depModuleId;
    private String tabAuth;
    private String binder;
    private String binderType;
    private String varAuth;
    private String version;

    // -----------------------
    // Constructors
    // -----------------------
    public DB2PackageDep(DB2Package db2Package, ResultSet resultSet) throws DBException
    {
        // TODO DF: Bad should be BTYPE+BSCHEMA+BNAME
        super(db2Package, JDBCUtils.safeGetString(resultSet, "BNAME"), true);

        DB2DataSource db2DataSource = db2Package.getDataSource();

        this.tabAuth = JDBCUtils.safeGetString(resultSet, "TABAUTH");
        // this.uniqueId = JDBCUtils.safeGetString(resultSet, "UNIQUE_ID");
        this.version = JDBCUtils.safeGetString(resultSet, "PKGVERSION");

        String depSchemaName = JDBCUtils.safeGetStringTrimmed(resultSet, "BSCHEMA");

        // Some dependencies are just numbers, not valid in a enum...
        String depType = JDBCUtils.safeGetString(resultSet, "BTYPE");
        this.packageDepType = CommonUtils.valueOf(DB2PackageDepType.class, depType);
        if (packageDepType == null) {
            this.packageDepType = CommonUtils.valueOf(DB2PackageDepType.class, DB2PackageDepType.FAKE_PREFIX + depType);
        }
        if (this.packageDepType != null) {
            DB2ObjectType db2ObjectType = packageDepType.getDb2ObjectType();
            if (db2ObjectType != null) {
                depSchema = getDataSource().getSchemaCache().getCachedObject(depSchemaName);
            }
        }

        if (db2DataSource.isAtLeastV9_7()) {
            this.binder = JDBCUtils.safeGetString(resultSet, "BINDER");
            this.binderType = JDBCUtils.safeGetString(resultSet, "BINDERTYPE");
            this.varAuth = JDBCUtils.safeGetString(resultSet, "VARAUTH");
            this.depModuleName = JDBCUtils.safeGetString(resultSet, "BMODULENAME");
            this.depModuleId = JDBCUtils.safeGetString(resultSet, "BMODULEID");
        }
    }

    // -----------------
    // Properties
    // -----------------

    @NotNull
    @Override
    @Property(viewable = true, editable = false, order = 1)
    public String getName()
    {
        return super.getName();
    }

    @Property(viewable = true, editable = false, order = 2)
    public DB2PackageDepType getPackageDepType()
    {
        return packageDepType;
    }

    @Property(viewable = true, editable = false, order = 3)
    public DB2Schema getDepSchema()
    {
        return depSchema;
    }

    @Property(viewable = true, editable = false, order = 4)
    public DBSObject getDepObject(DBRProgressMonitor monitor) throws DBException
    {
        if (packageDepType == null || packageDepType.getDb2ObjectType() == null) {
            return null;
        }
        // Some dependencies are in Modules...Concatenate modulename and name
        String name = getName();
        if (depModuleName != null) {
            name = depModuleName + "." + name;
        }
        return packageDepType.getDb2ObjectType().findObject(monitor, depSchema, name);
    }

    @Property(viewable = true, editable = false, order = 5)
    public String getVersion()
    {
        return version;
    }

    @Property(viewable = true, editable = false, order = 6)
    public String getBinder()
    {
        return binder;
    }

    @Property(viewable = true, editable = false, order = 7)
    public String getBinderType()
    {
        return binderType;
    }

    @Property(viewable = false, editable = false)
    public String getDepModuleId()
    {
        return depModuleId;
    }

    @Property(viewable = false, editable = false)
    public String getTabAuth()
    {
        return tabAuth;
    }

    @Property(viewable = false, editable = false)
    public String getVarAuth()
    {
        return varAuth;
    }

}
