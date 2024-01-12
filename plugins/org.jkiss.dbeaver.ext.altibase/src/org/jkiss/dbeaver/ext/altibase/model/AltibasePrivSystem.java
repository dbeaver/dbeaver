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
package org.jkiss.dbeaver.ext.altibase.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;

import java.sql.ResultSet;

public class AltibasePrivSystem extends AltibasePriv {
    
    private String grantorName;
    private boolean isGranted;
    
    protected AltibasePrivSystem(AltibaseGrantee user, ResultSet resultSet) {
        super(user, JDBCUtils.safeGetString(resultSet, "PRIV_NAME"));
        grantorName = JDBCUtils.safeGetString(resultSet, "GRANTOR_NAME");
        isGranted = (grantorName != null);
    }
    
    // For special account: SYSTEM_, SYS account
    protected AltibasePrivSystem(AltibaseGrantee user, String privName) {
        super(user, privName);
        isGranted = true;
    }
    
    @NotNull
    @Override
    @Property(viewable = true, order = 2)
    public String getName() {
        return super.getName();
    }
    
    @Property(viewable = true, order = 3)
    public boolean getGranted() {
        return isGranted;
    }
    
    @Property(viewable = true, order = 4)
    public String getGrantor() {
        return grantorName;
    }
}
