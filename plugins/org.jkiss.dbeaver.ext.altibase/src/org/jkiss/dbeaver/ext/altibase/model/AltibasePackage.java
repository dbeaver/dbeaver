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

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.altibase.AltibaseConstants;
import org.jkiss.dbeaver.ext.generic.model.GenericCatalog;
import org.jkiss.dbeaver.ext.generic.model.GenericPackage;
import org.jkiss.dbeaver.ext.generic.model.GenericStructContainer;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.util.Map;

public class AltibasePackage extends GenericPackage {

    private String source;

    private boolean status; // 0: Valid, 1: Invalid
    private boolean hasBody;

    public AltibasePackage(GenericStructContainer container, String packageName, JDBCResultSet dbResult) {
        super(container, packageName, true);

        status = (JDBCUtils.safeGetInt(dbResult, "STATUS") == 0);
    }

    public void setBody(boolean hasBody) {
        this.hasBody = hasBody;
    }

    @Override
    public String getObjectDefinitionText(DBRProgressMonitor monitor, Map<String, Object> options) throws DBException {
        if (source == null) {
            source = "-- Package specification " 
                    + AltibaseConstants.NEW_LINE 
                    + ((AltibaseMetaModel) getDataSource().getMetaModel())
                        .getPackageDDL(monitor, this, AltibaseConstants.PACKAGE_TYPE_SPEC)
                    + AltibaseConstants.NEW_LINE 
                    + "-- Package body " 
                    + AltibaseConstants.NEW_LINE;

            if (hasBody) {
                source += ((AltibaseMetaModel) getDataSource().getMetaModel())
                        .getPackageDDL(monitor, this, AltibaseConstants.PACKAGE_TYPE_BODY);
            } else {
                source += "-- No body definition";
            }
        }
        return source;
    }

    @Property(viewable = true, order = 5)
    public boolean isValid() {
        return status;
    }
    
    @Override
    @Property(viewable = false, hidden = true, order = 3)
    public GenericCatalog getCatalog() {
        return super.getCatalog();
    }
}
