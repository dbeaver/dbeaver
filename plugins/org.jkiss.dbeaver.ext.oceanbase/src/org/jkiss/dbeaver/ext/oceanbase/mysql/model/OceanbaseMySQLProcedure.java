/*
 * DBeaver - Universal Database Manager
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

package org.jkiss.dbeaver.ext.oceanbase.mysql.model;

import java.sql.ResultSet;
import java.util.Collection;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.mysql.model.MySQLCatalog;
import org.jkiss.dbeaver.ext.mysql.model.MySQLProcedure;
import org.jkiss.dbeaver.ext.mysql.model.MySQLProcedureParameter;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;

public class OceanbaseMySQLProcedure extends MySQLProcedure {
    private OceanbaseMySQLCatalog container = (OceanbaseMySQLCatalog) getContainer();

    OceanbaseMySQLProcedure(MySQLCatalog catalog, ResultSet dbResult) {
        super(catalog, dbResult);
    }

    @Override
    public Collection<MySQLProcedureParameter> getParameters(DBRProgressMonitor monitor) throws DBException {
        return container.getOceanbaseProceduresCache().getChildren(monitor, container, this);
    }

    @Override
    public DBSObject refreshObject(@NotNull DBRProgressMonitor monitor) throws DBException {
        return container.getOceanbaseProceduresCache().refreshObject(monitor, container, this);
    }

}
