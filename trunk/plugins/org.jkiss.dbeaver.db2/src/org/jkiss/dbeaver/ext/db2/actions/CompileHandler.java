/*
 * Copyright (C) 2010-2013 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jkiss.dbeaver.ext.db2.actions;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.commands.IElementUpdater;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.menus.UIElement;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.ext.IDatabasePersistAction;
import org.jkiss.dbeaver.ext.db2.editors.DB2ObjectType;
import org.jkiss.dbeaver.ext.db2.model.source.DB2SourceObject;
import org.jkiss.dbeaver.ext.db2.views.DB2CompilerDialog;
import org.jkiss.dbeaver.model.DBPEvent;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.exec.DBCStatement;
import org.jkiss.dbeaver.model.exec.DBCStatementType;
import org.jkiss.dbeaver.model.exec.compile.DBCCompileError;
import org.jkiss.dbeaver.model.exec.compile.DBCCompileLog;
import org.jkiss.dbeaver.model.exec.compile.DBCCompileLogBase;
import org.jkiss.dbeaver.model.exec.compile.DBCSourceHost;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.model.struct.DBSObjectState;
import org.jkiss.dbeaver.runtime.RuntimeUtils;
import org.jkiss.dbeaver.ui.TextUtils;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.utils.ContentUtils;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.InvocationTargetException;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class CompileHandler extends AbstractHandler implements IElementUpdater {
   static final Log log = LogFactory.getLog(CompileHandler.class);

   @Override
   public Object execute(ExecutionEvent event) throws ExecutionException {
      final List<DB2SourceObject> objects = getSelectedObjects(event);
      if (!objects.isEmpty()) {
         final Shell activeShell = HandlerUtil.getActiveShell(event);
         if (objects.size() == 1) {
            final DB2SourceObject unit = objects.get(0);

            final IWorkbenchPart activePart = HandlerUtil.getActiveEditor(event);
            DBCSourceHost sourceHost = RuntimeUtils.getObjectAdapter(activePart, DBCSourceHost.class);
            if (sourceHost == null) {
               sourceHost = (DBCSourceHost) activePart.getAdapter(DBCSourceHost.class);
            }
            if (sourceHost != null && sourceHost.getSourceObject() != unit) {
               sourceHost = null;
            }

            final DBCCompileLog compileLog = sourceHost == null ? new DBCCompileLogBase() : sourceHost.getCompileLog();
            compileLog.clearLog();
            Throwable error = null;
            try {
               DBeaverUI.runInProgressService(new DBRRunnableWithProgress() {
                  @Override
                  public void run(DBRProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
                     try {
                        compileUnit(monitor, compileLog, unit);
                     } catch (DBCException e) {
                        throw new InvocationTargetException(e);
                     }
                  }
               });
               if (compileLog.getError() != null) {
                  error = compileLog.getError();
               }
            } catch (InvocationTargetException e) {
               error = e.getTargetException();
            } catch (InterruptedException e) {
               return null;
            }
            if (error != null) {
               UIUtils.showErrorDialog(activeShell, "Unexpected compilation error", null, error);
            } else if (!CommonUtils.isEmpty(compileLog.getErrorStack())) {
               // Show compile errors
               int line = -1, position = -1;
               StringBuilder fullMessage = new StringBuilder();
               for (DBCCompileError oce : compileLog.getErrorStack()) {
                  fullMessage.append(oce.toString()).append(ContentUtils.getDefaultLineSeparator());
                  if (line < 0) {
                     line = oce.getLine();
                     position = oce.getPosition();
                  }
               }

               // If compiled object is currently open in editor - try to position on error line
               if (sourceHost != null && sourceHost.getSourceObject() == unit && line > 0 && position > 0) {
                  sourceHost.positionSource(line, position);
                  activePart.getSite().getPage().activate(activePart);
               }

               String errorTitle = unit.getName() + " compilation failed";
               if (sourceHost != null) {
                  sourceHost.setCompileInfo(errorTitle, true);
                  sourceHost.showCompileLog();
               }
               UIUtils.showErrorDialog(activeShell, errorTitle, fullMessage.toString());
            } else {
               String message = unit.getName() + " compiled successfully";
               if (sourceHost != null) {
                  sourceHost.setCompileInfo(message, true);
               }
               UIUtils.showMessageBox(activeShell, "Done", message, SWT.ICON_INFORMATION);
            }
         } else {
            DB2CompilerDialog dialog = new DB2CompilerDialog(activeShell, objects);
            dialog.open();
         }
      }
      return null;
   }

   private List<DB2SourceObject> getSelectedObjects(ExecutionEvent event) {
      List<DB2SourceObject> objects = new ArrayList<DB2SourceObject>();
      final ISelection currentSelection = HandlerUtil.getCurrentSelection(event);
      if (currentSelection instanceof IStructuredSelection && !currentSelection.isEmpty()) {
         for (Iterator<?> iter = ((IStructuredSelection) currentSelection).iterator(); iter.hasNext();) {
            final Object element = iter.next();
            final DB2SourceObject sourceObject = RuntimeUtils.getObjectAdapter(element, DB2SourceObject.class);
            if (sourceObject != null) {
               objects.add(sourceObject);
            }
         }
      }
      if (objects.isEmpty()) {
         final IWorkbenchPart activePart = HandlerUtil.getActivePart(event);
         final DB2SourceObject sourceObject = RuntimeUtils.getObjectAdapter(activePart, DB2SourceObject.class);
         if (sourceObject != null) {
            objects.add(sourceObject);
         }
      }
      return objects;
   }

   @Override
   public void updateElement(UIElement element, Map parameters) {
      List<DB2SourceObject> objects = new ArrayList<DB2SourceObject>();
      IWorkbenchPartSite partSite = UIUtils.getWorkbenchPartSite(element.getServiceLocator());
      if (partSite != null) {
         final ISelectionProvider selectionProvider = partSite.getSelectionProvider();
         if (selectionProvider != null) {
            ISelection selection = selectionProvider.getSelection();
            if (selection instanceof IStructuredSelection && !selection.isEmpty()) {
               for (Iterator<?> iter = ((IStructuredSelection) selection).iterator(); iter.hasNext();) {
                  final Object item = iter.next();
                  final DB2SourceObject sourceObject = RuntimeUtils.getObjectAdapter(item, DB2SourceObject.class);
                  if (sourceObject != null) {
                     objects.add(sourceObject);
                  }
               }
            }
         }
         if (objects.isEmpty()) {
            final IWorkbenchPart activePart = partSite.getPart();
            final DB2SourceObject sourceObject = RuntimeUtils.getObjectAdapter(activePart, DB2SourceObject.class);
            if (sourceObject != null) {
               objects.add(sourceObject);
            }
         }
      }
      if (!objects.isEmpty()) {
         if (objects.size() > 1) {
            element.setText("Compile " + objects.size() + " objects");
         } else {
            final DB2SourceObject sourceObject = objects.get(0);
            String objectType = TextUtils.formatWord(sourceObject.getSourceType().name());
            element.setText("Compile " + objectType/* + " '" + sourceObject.getName() + "'" */);
         }
      }
   }

   public static boolean compileUnit(DBRProgressMonitor monitor, Log compileLog, DB2SourceObject unit) throws DBCException {
      final IDatabasePersistAction[] compileActions = unit.getCompileActions();
      if (CommonUtils.isEmpty(compileActions)) {
         return true;
      }

      final JDBCExecutionContext context = unit.getDataSource().openContext(monitor, DBCExecutionPurpose.UTIL,
                                                                            "Compile '" + unit.getName() + "'");
      try {
         boolean success = true;
         for (IDatabasePersistAction action : compileActions) {
            final String script = action.getScript();
            compileLog.trace(script);

            if (monitor.isCanceled()) {
               break;
            }
            try {
               final DBCStatement dbStat = context.prepareStatement(DBCStatementType.QUERY, script, false, false, false);
               try {
                  dbStat.executeStatement();
               } finally {
                  dbStat.close();
               }
               action.handleExecute(null);
            } catch (DBCException e) {
               action.handleExecute(e);
               throw e;
            }
            if (action instanceof DB2ObjectPersistAction) {
               if (!logObjectErrors(context, compileLog, unit, ((DB2ObjectPersistAction) action).getObjectType())) {
                  success = false;
               }
            }
         }
         final DBSObjectState oldState = unit.getObjectState();
         unit.refreshObjectState(monitor);
         if (unit.getObjectState() != oldState) {
            unit.getDataSource().getContainer().fireEvent(new DBPEvent(DBPEvent.Action.OBJECT_UPDATE, unit));
         }

         return success;
      } finally {
         context.close();
      }
   }

   public static boolean logObjectErrors(JDBCExecutionContext context,
                                         Log compileLog,
                                         DB2SourceObject unit,
                                         DB2ObjectType objectType) {
      try {
         final JDBCPreparedStatement dbStat = context
                  .prepareStatement("SELECT * FROM SYS.ALL_ERRORS WHERE OWNER=? AND NAME=? AND TYPE=? ORDER BY SEQUENCE");
         try {
            dbStat.setString(1, unit.getSchema().getName());
            dbStat.setString(2, unit.getName());
            dbStat.setString(3, objectType.getTypeName());
            final ResultSet dbResult = dbStat.executeQuery();
            try {
               boolean hasErrors = false;
               while (dbResult.next()) {
                  DBCCompileError error = new DBCCompileError("ERROR".equals(dbResult.getString("ATTRIBUTE")),
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
            } finally {
               dbResult.close();
            }
         } finally {
            dbStat.close();
         }
      } catch (Exception e) {
         log.error("Can't read user errors", e);
         return false;
      }
   }

}