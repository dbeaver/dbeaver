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
        final List<DBSEntity> entities = new ArrayList<DBSEntity>();
        if (context instanceof IDataSourceProvider) {
            try {
                DBeaverCore.getInstance().runInProgressService(new DBRRunnableWithProgress() {
                    @Override
                    public void run(DBRProgressMonitor monitor)
                        throws InvocationTargetException, InterruptedException
                    {
                        try {
                            resolveTables(monitor, ((IDataSourceProvider) context).getDataSource(), entities);
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
        if (!CommonUtils.isEmpty(entities)) {
            String[] result = new String[entities.size()];
            for (int i = 0; i < entities.size(); i++) {
                DBSEntity entity = entities.get(i);
                result[i] = entity.getName();
            }
            return result;
        }
        return super.resolveAll(context);
    }

    static void resolveTables(DBRProgressMonitor monitor, DBPDataSource dataSource, List<DBSEntity> entities) throws DBException
    {
        if (dataSource instanceof DBSObjectSelector) {
            DBSObject selectedObject = ((DBSObjectSelector) dataSource).getSelectedObject();
            if (selectedObject instanceof DBSObjectContainer) {
                makeProposalsFromChildren(monitor, (DBSObjectContainer) selectedObject, entities);
                return;
            }
        }
        if (dataSource instanceof DBSObjectContainer) {
            makeProposalsFromChildren(monitor, (DBSObjectContainer) dataSource, entities);
        }
    }

    static void makeProposalsFromChildren(DBRProgressMonitor monitor, DBSObjectContainer container, List<DBSEntity> names) throws DBException
    {
        Collection<? extends DBSObject> children = container.getChildren(monitor);
        if (CommonUtils.isEmpty(children)) {
            return;
        }
        for (DBSObject child : children) {
            if (child instanceof DBSEntity) {
                names.add((DBSEntity) child);
            }
        }
    }
}
