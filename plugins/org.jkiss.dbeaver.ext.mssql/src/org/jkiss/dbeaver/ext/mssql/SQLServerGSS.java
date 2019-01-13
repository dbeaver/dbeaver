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

package org.jkiss.dbeaver.ext.mssql;

import org.ietf.jgss.*;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.utils.CommonUtils;

import java.util.Properties;

/**
 * SQLServerGSS
 */
public class SQLServerGSS {

    public static void initCredentials(DBPConnectionConfiguration connectionInfo, Properties properties) throws DBCException {
        if (!CommonUtils.isEmpty(connectionInfo.getUserName())) {
            try {
                GSSManager gssManager = GSSManager.getInstance();
                GSSName name = gssManager.createName(connectionInfo.getUserName(), GSSName.NT_USER_NAME);
                GSSCredential impersonatedUserCredential = gssManager.createCredential(name, GSSCredential.DEFAULT_LIFETIME, (Oid)null, GSSCredential.ACCEPT_ONLY);
                properties.put("gsscredential", impersonatedUserCredential);
            } catch (GSSException e) {
                throw new DBCException ("Error initializing GSS", e);
            }
        }
    }
}
