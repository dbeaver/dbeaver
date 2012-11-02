package org.jkiss.dbeaver.ui.editors.sql.templates;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.text.templates.TemplateContext;
import org.eclipse.jface.text.templates.TemplateVariableResolver;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.ext.IDataSourceProvider;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
import org.jkiss.dbeaver.model.struct.DBSObjectSelector;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Entity resolver
 */
public class SQLEntityResolver extends TemplateVariableResolver {

    static final Log log = LogFactory.getLog(SQLEntityResolver.class);

    public SQLEntityResolver()
    {
        super("table", "Database table");
    }

    @Override
    protected String resolve(TemplateContext context)
    {
        return super.resolve(context);
    }

    @Override
    protected String[] resolveAll(final TemplateContext context)
    {
        final List<String> names = new ArrayList<String>();
        if (context instanceof IDataSourceProvider) {
            try {
                DBeaverCore.getInstance().runInProgressService(new DBRRunnableWithProgress() {
                    @Override
                    public void run(DBRProgressMonitor monitor)
                        throws InvocationTargetException, InterruptedException
                    {
                        try {
                            resolveTables(monitor, ((IDataSourceProvider) context).getDataSource(), names);
                        } catch (DBException e) {
                            throw new InvocationTargetException(e);
                        }
                    }
                });
            } catch (InvocationTargetException e) {
                log.error(e.getTargetException());
            } catch (InterruptedException e) {
                // skip
            }
        }
        if (!CommonUtils.isEmpty(names)) {
            return names.toArray(new String[names.size()]);
        }
        return super.resolveAll(context);
    }

    private void resolveTables(DBRProgressMonitor monitor, DBPDataSource dataSource, List<String> names) throws DBException
    {
        if (dataSource instanceof DBSObjectSelector) {
            DBSObject selectedObject = ((DBSObjectSelector)dataSource).getSelectedObject();
            if (selectedObject instanceof DBSObjectContainer) {
                makeProposalsFromChildren(monitor, (DBSObjectContainer)selectedObject, names);
                return;
            }
        }
        if (dataSource instanceof DBSObjectContainer) {
            makeProposalsFromChildren(monitor, (DBSObjectContainer)dataSource, names);
        }
   }

    private void makeProposalsFromChildren(DBRProgressMonitor monitor, DBSObjectContainer container, List<String> names) throws DBException
    {
        Collection<? extends DBSObject> children = container.getChildren(monitor);
        if (CommonUtils.isEmpty(children)) {
            return;
        }
        for (DBSObject child : children) {
            if (child instanceof DBSEntity) {
                names.add(child.getName());
            }
        }
    }
}
