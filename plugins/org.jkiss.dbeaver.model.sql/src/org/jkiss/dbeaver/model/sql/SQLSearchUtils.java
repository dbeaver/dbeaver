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
package org.jkiss.dbeaver.model.sql;

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.DBObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.parser.SQLIdentifierDetector;
import org.jkiss.dbeaver.model.struct.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Metadata search utils
 */
public class SQLSearchUtils
{
    private static final Log log = Log.getLog(SQLSearchUtils.class);

    @Nullable
    public static DBSObject findObjectByFQN(DBRProgressMonitor monitor, DBSObjectContainer sc, DBCExecutionContext executionContext, List<String> nameList, boolean useAssistant, SQLIdentifierDetector identifierDetector) {
        if (nameList.isEmpty()) {
            return null;
        }
        {
            List<String> unquotedNames = new ArrayList<>(nameList.size());
            for (String name : nameList) {
                unquotedNames.add(DBUtils.getUnQuotedIdentifier(executionContext.getDataSource(), name));
            }

            DBSObject result = findObjectByPath(monitor, executionContext, sc, unquotedNames, identifierDetector, useAssistant);
            if (result != null) {
                return result;
            }
        }
        {
            // Fix names (convert case or remove quotes)
            for (int i = 0; i < nameList.size(); i++) {
                String name = nameList.get(i);
                String unquotedName = DBUtils.getUnQuotedIdentifier(executionContext.getDataSource(), name);
                if (!unquotedName.equals(name)) {
                    name = unquotedName;
                } else {
                    name = DBObjectNameCaseTransformer.transformName(sc.getDataSource(), name);
                }
                nameList.set(i, name);
            }
            return findObjectByPath(monitor, executionContext, sc, nameList, identifierDetector, useAssistant);
        }
    }

    public static DBSObject findObjectByPath(DBRProgressMonitor monitor, DBCExecutionContext executionContext, DBSObjectContainer sc, List<String> nameList, SQLIdentifierDetector identifierDetector, boolean useAssistant) {
        try {
            DBSObject childObject = null;
            while (childObject == null) {
                childObject = DBUtils.findNestedObject(monitor, executionContext, sc, nameList);
                if (childObject == null) {
                    DBSObjectContainer parentSc = DBUtils.getParentAdapter(DBSObjectContainer.class, sc);
                    if (parentSc == null) {
                        break;
                    }
                    sc = parentSc;
                }
            }
            if (childObject == null && nameList.size() <= 1) {
                if (useAssistant) {
                    // No such object found - may be it's start of table name
                    DBSStructureAssistant structureAssistant = DBUtils.getAdapter(DBSStructureAssistant.class, sc);
                    if (structureAssistant != null) {
                        String objectNameMask = nameList.get(0);
                        DBSStructureAssistant.ObjectsSearchParams params = new DBSStructureAssistant.ObjectsSearchParams(
                                structureAssistant.getAutoCompleteObjectTypes(),
                                identifierDetector.removeQuotes(objectNameMask)
                        );
                        params.setParentObject(sc);
                        params.setCaseSensitive(identifierDetector.isQuoted(objectNameMask));
                        params.setMaxResults(2);
                        Collection<DBSObjectReference> tables = structureAssistant.findObjectsByMask(monitor, executionContext, params);
                        if (!tables.isEmpty()) {
                            return tables.iterator().next().resolveObject(monitor);
                        }
                    }
                }
                return null;
            } else {
                return childObject;
            }
        } catch (DBException e) {
            log.error(e);
            return null;
        }
    }
}