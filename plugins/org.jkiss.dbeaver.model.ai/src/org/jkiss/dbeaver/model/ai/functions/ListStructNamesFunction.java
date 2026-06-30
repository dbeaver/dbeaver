/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.ai.functions;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.ai.AIFunction;
import org.jkiss.dbeaver.model.ai.AIFunctionContext;
import org.jkiss.dbeaver.model.ai.AIFunctionVerifier;
import org.jkiss.dbeaver.model.ai.engine.AIDatabaseContext;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;

public abstract class ListStructNamesFunction implements AIFunction, AIFunctionVerifier {
    public static final Log log = Log.getLog(ListStructNamesFunction.class);

    protected boolean isChildTypeSupported(
        @NotNull AIFunctionContext context,
        @NotNull Class<? extends DBSObject> type
    ) {
        AIDatabaseContext dbContext = context.getContext();
        DBCExecutionContext executionContext = dbContext == null ? null : dbContext.getExecutionContext();
        if (executionContext != null) {
            DBPDataSource dataSource = executionContext.getDataSource();
            if (dataSource instanceof DBSObjectContainer oc) {
                try {
                    Class<? extends DBSObject> catType = oc.getPrimaryChildType(context.getMonitor());
                    if (type.isAssignableFrom(catType)) {
                        return true;
                    }
                } catch (DBException e) {
                    log.debug(e);
                }
            }
        }
        return false;
    }

}
