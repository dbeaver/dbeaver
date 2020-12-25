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
package org.jkiss.dbeaver.ext.db2.model.security;

import org.jkiss.dbeaver.ext.db2.model.DB2DataSource;
import org.jkiss.dbeaver.model.access.DBAUser;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.sql.ResultSet;

/**
 * DB2 User
 * 
 * @author Denis Forveille
 */
public class DB2User extends DB2Grantee implements DBAUser {

    // -----------------------
    // Constructors
    // -----------------------
    public DB2User(DBRProgressMonitor monitor, DB2DataSource dataSource, ResultSet resultSet)
    {
        super(monitor, dataSource, resultSet, "GRANTEE");

    }

    // -----------------------
    // Business Contract
    // -----------------------

    @Override
    public DB2AuthIDType getType()
    {
        return DB2AuthIDType.U;
    }

}
