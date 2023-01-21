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
package org.jkiss.dbeaver.model.ai.formatter;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCTable;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
import org.jkiss.utils.CommonUtils;

import java.util.List;

public class GPTRequestFormatter {
    public static String addDBMetadataToRequest(
        DBRProgressMonitor monitor,
        String request,
        DBSObjectContainer context
    ) throws DBException {
        if (context == null || context.getDataSource() == null || CommonUtils.isEmptyTrimmed(request)) {
            return request;
        }

        StringBuilder additionalMetadata = new StringBuilder();
        //additionalMetadata.append("Use SQL\n");
        additionalMetadata.append("### ").append(context.getDataSource().getSQLDialect().getDialectName()).append(" SQL tables, with their properties:\n#\n");
        generateObjectDescription(monitor, additionalMetadata, context);
        additionalMetadata.append("#\n###").append(request.trim()).append("\nSELECT");
        return additionalMetadata.toString();
    }


    private static void generateObjectDescription(
        @NotNull DBRProgressMonitor monitor,
        @NotNull StringBuilder request,
        @NotNull DBSObject object
    ) throws DBException {
        if (object instanceof JDBCTable) {
            request.append("# ").append(DBUtils.getObjectFullName(object, DBPEvaluationContext.DDL));
            List<? extends DBSEntityAttribute> attributes = ((JDBCTable<?, ?>) object).getAttributes(monitor);
            if (attributes != null) {
                request.append("(");
                for (int i = 0; i < attributes.size(); i++) {
                    if (i != 0) {
                        request.append(", ");
                    }
                    DBSEntityAttribute attribute = attributes.get(i);
                    request.append(attribute.getName());
                }
                request.append(")");
            }
            request.append("\n");
        } else if (object instanceof DBSObjectContainer) {
            for (DBSObject child : ((DBSObjectContainer) object).getChildren(monitor)) {
                generateObjectDescription(monitor, request, child);
            }
        }
    }

    private GPTRequestFormatter() {

    }
}
