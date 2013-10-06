package org.jkiss.dbeaver.ui.editors.sql.templates;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.text.templates.TemplateContext;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
import org.jkiss.utils.CommonUtils;

import java.util.Collection;
import java.util.List;

/**
 * Container resolver
 */
public class SQLContainerResolver<T extends DBSObjectContainer> extends SQLObjectResolver<T> {

    static final Log log = LogFactory.getLog(SQLContainerResolver.class);
    public static final String VAR_NAME_SCHEMA = "schema";
    public static final String VAR_NAME_CATALOG = "catalog";

    private Class<T> objectType;

    public SQLContainerResolver(String id, String title, Class<T> objectType)
    {
        super(id, title);
        this.objectType = objectType;
    }

    @Override
    protected void resolveObjects(DBRProgressMonitor monitor, DBPDataSource dataSource, TemplateContext context, List<T> entities) throws DBException
    {
        DBSObjectContainer objectContainer = DBUtils.getAdapter(DBSObjectContainer.class, dataSource);
        if (objectContainer != null) {
            makeProposalsFromChildren(monitor, objectContainer, entities);
        }
    }

    void makeProposalsFromChildren(DBRProgressMonitor monitor, DBSObjectContainer container, List<T> names) throws DBException
    {
        Collection<? extends DBSObject> children = container.getChildren(monitor);
        if (CommonUtils.isEmpty(children)) {
            return;
        }
        for (DBSObject child : children) {
            if (objectType.isInstance(child)) {
                names.add(objectType.cast(child));
            }
        }
    }
}
