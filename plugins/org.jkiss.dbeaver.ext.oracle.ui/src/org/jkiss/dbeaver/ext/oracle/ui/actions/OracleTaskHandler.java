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
package org.jkiss.dbeaver.ext.oracle.ui.actions;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.commands.IElementUpdater;
import org.eclipse.ui.menus.UIElement;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.oracle.model.OracleObjectType;
import org.jkiss.dbeaver.ext.oracle.model.OracleUtils;
import org.jkiss.dbeaver.ext.oracle.model.source.OracleSourceObject;
import org.jkiss.dbeaver.ext.oracle.model.source.OracleStatefulObject;
import org.jkiss.dbeaver.model.exec.compile.DBCCompileError;
import org.jkiss.dbeaver.model.exec.compile.DBCCompileLog;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.utils.RuntimeUtils;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Base task handler
 */
public abstract class OracleTaskHandler extends AbstractHandler implements IElementUpdater
{
    private static final Log log = Log.getLog(OracleTaskHandler.class);

    protected List<OracleSourceObject> getOracleSourceObjects(UIElement element) {
        List<OracleSourceObject> objects = new ArrayList<>();
        IWorkbenchPartSite partSite = UIUtils.getWorkbenchPartSite(element.getServiceLocator());
        if (partSite != null) {
            final ISelectionProvider selectionProvider = partSite.getSelectionProvider();
            if (selectionProvider != null) {
                ISelection selection = selectionProvider.getSelection();
                if (selection instanceof IStructuredSelection && !selection.isEmpty()) {
                    for (Iterator<?> iter = ((IStructuredSelection) selection).iterator(); iter.hasNext(); ) {
                        final Object item = iter.next();
                        final OracleSourceObject sourceObject = RuntimeUtils.getObjectAdapter(item, OracleSourceObject.class);
                        if (sourceObject != null) {
                            objects.add(sourceObject);
                        }
                    }
                }
            }
            if (objects.isEmpty()) {
                final IWorkbenchPart activePart = partSite.getPart();
                final OracleSourceObject sourceObject = RuntimeUtils.getObjectAdapter(activePart, OracleSourceObject.class);
                if (sourceObject != null) {
                    objects.add(sourceObject);
                }
            }
        }
        return objects;
    }

    public static boolean logObjectErrors(
        JDBCSession session,
        DBCCompileLog compileLog,
        OracleStatefulObject schemaObject,
        OracleObjectType objectType)
    {
        try {
            try (JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT * FROM " + OracleUtils.getSysSchemaPrefix(schemaObject.getDataSource()) + "ALL_ERRORS WHERE OWNER=? AND NAME=? AND TYPE=? ORDER BY SEQUENCE")) {
                dbStat.setString(1, schemaObject.getSchema().getName());
                dbStat.setString(2, schemaObject.getName());
                dbStat.setString(3, objectType.getTypeName());
                try (ResultSet dbResult = dbStat.executeQuery()) {
                    boolean hasErrors = false;
                    while (dbResult.next()) {
                        DBCCompileError error = new DBCCompileError(
                            "ERROR".equals(dbResult.getString("ATTRIBUTE")),
                            dbResult.getString("TEXT"),
                            dbResult.getInt("LINE"),
                            dbResult.getInt("POSITION"));
                        hasErrors = true;
                        if (error.isError()) {
                            compileLog.error(error);
                        } else {
                            compileLog.warn(error);
                        }
                    }
                    return !hasErrors;
                }
            }
        } catch (Exception e) {
            log.error("Can't read user errors", e);
            return false;
        }
    }

}