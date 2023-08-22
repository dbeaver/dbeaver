package org.jkiss.dbeaver.ext.yashandb.ui.actions;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.commands.IElementUpdater;
import org.eclipse.ui.menus.UIElement;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.yashandb.model.YashanDBObjectType;
import org.jkiss.dbeaver.ext.yashandb.model.YashanDBUtils;
import org.jkiss.dbeaver.ext.yashandb.model.source.YashanDBSourceObject;
import org.jkiss.dbeaver.ext.yashandb.model.source.YashanDBStatefulObject;
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
 * @Description:
 * @Author dengqh
 * @Date 2023/7/5 17:34
 */
public abstract class YashanDBTaskHandler extends AbstractHandler implements IElementUpdater
{
    private static final Log log = Log.getLog(YashanDBTaskHandler.class);

    protected List<YashanDBSourceObject> getYashanDBSourceObjects(UIElement element) {
        List<YashanDBSourceObject> objects = new ArrayList<>();
        IWorkbenchPartSite partSite = UIUtils.getWorkbenchPartSite(element.getServiceLocator());
        if (partSite != null) {
            final ISelectionProvider selectionProvider = partSite.getSelectionProvider();
            if (selectionProvider != null) {
                ISelection selection = selectionProvider.getSelection();
                if (selection instanceof IStructuredSelection && !selection.isEmpty()) {
                    for (Iterator<?> iter = ((IStructuredSelection) selection).iterator(); iter.hasNext(); ) {
                        final Object item = iter.next();
                        final YashanDBSourceObject sourceObject = RuntimeUtils.getObjectAdapter(item, YashanDBSourceObject.class);
                        if (sourceObject != null) {
                            objects.add(sourceObject);
                        }
                    }
                }
            }
            if (objects.isEmpty()) {
                final IWorkbenchPart activePart = partSite.getPart();
                final YashanDBSourceObject sourceObject = RuntimeUtils.getObjectAdapter(activePart, YashanDBSourceObject.class);
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
            YashanDBStatefulObject schemaObject,
            YashanDBObjectType objectType)
    {
        try {
            try (JDBCPreparedStatement dbStat = session.prepareStatement(
                    "SELECT * FROM " + YashanDBUtils.getSysSchemaPrefix(schemaObject.getDataSource()) + "ALL_ERRORS WHERE OWNER=? AND NAME=? AND TYPE=? ORDER BY SEQUENCE")) {
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


