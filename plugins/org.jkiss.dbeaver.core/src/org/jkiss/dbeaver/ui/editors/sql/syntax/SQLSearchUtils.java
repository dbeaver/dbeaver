/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ui.editors.sql.syntax;

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.impl.DBObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.ui.editors.sql.syntax.parser.SQLIdentifierDetector;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
import org.jkiss.dbeaver.model.struct.DBSObjectReference;
import org.jkiss.dbeaver.model.struct.DBSStructureAssistant;

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
    public static DBSObject findObjectByFQN(DBRProgressMonitor monitor, DBSObjectContainer sc, DBPDataSource dataSource, List<String> nameList, boolean useAssistant, SQLIdentifierDetector identifierDetector) {
        if (nameList.isEmpty()) {
            return null;
        }
        {
            List<String> unquotedNames = new ArrayList<>(nameList.size());
            for (String name : nameList) {
                unquotedNames.add(DBUtils.getUnQuotedIdentifier(dataSource, name));
            }

            DBSObject result = findObjectByPath(monitor, sc, unquotedNames, identifierDetector, useAssistant);
            if (result != null) {
                return result;
            }
        }
        {
            // Fix names (convert case or remove quotes)
            for (int i = 0; i < nameList.size(); i++) {
                String name = nameList.get(i);
                String unquotedName = DBUtils.getUnQuotedIdentifier(dataSource, name);
                if (!unquotedName.equals(name)) {
                    name = unquotedName;
                } else {
                    name = DBObjectNameCaseTransformer.transformName(sc.getDataSource(), name);
                }
                nameList.set(i, name);
            }
            return findObjectByPath(monitor, sc, nameList, identifierDetector, useAssistant);
        }
    }

    public static DBSObject findObjectByPath(DBRProgressMonitor monitor, DBSObjectContainer sc, List<String> nameList, SQLIdentifierDetector identifierDetector, boolean useAssistant) {
        try {
            DBSObject childObject = null;
            while (childObject == null) {
                childObject = DBUtils.findNestedObject(monitor, sc, nameList);
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
                        Collection<DBSObjectReference> tables = structureAssistant.findObjectsByMask(
                            monitor,
                            sc,
                            structureAssistant.getAutoCompleteObjectTypes(),
                            identifierDetector.removeQuotes(objectNameMask),
                            identifierDetector.isQuoted(objectNameMask),
                            false,
                            2);
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