package org.jkiss.dbeaver.ui.editors.sql.templates;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.text.templates.TemplateContext;
import org.eclipse.jface.text.templates.TemplateVariable;
import org.eclipse.jface.text.templates.TemplateVariableResolver;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.ext.IDataSourceProvider;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity resolver
 */
public class SQLAttributeResolver extends TemplateVariableResolver {

    static final Log log = LogFactory.getLog(SQLAttributeResolver.class);

    public SQLAttributeResolver()
    {
        super("column", "Table column");
    }

    @Override
    protected String[] resolveAll(final TemplateContext context)
    {
        TemplateVariable tableVariable = ((SQLContext) context).getTemplateVariable("table");
        final String tableName = tableVariable == null ? null : tableVariable.getDefaultValue();
        if (!CommonUtils.isEmpty(tableName)) {
            final List<DBSEntityAttribute> attributes = new ArrayList<DBSEntityAttribute>();
            if (context instanceof IDataSourceProvider) {
                try {
                    DBRRunnableWithProgress runnable = new DBRRunnableWithProgress() {
                        @Override
                        public void run(DBRProgressMonitor monitor)
                            throws InvocationTargetException, InterruptedException
                        {
                            try {
                                List<DBSEntity> entities = new ArrayList<DBSEntity>();
                                SQLEntityResolver.resolveTables(monitor, ((IDataSourceProvider) context).getDataSource(), context, entities);
                                if (!CommonUtils.isEmpty(entities)) {
                                    DBSEntity table = DBUtils.findObject(entities, tableName);
                                    if (table != null) {
                                        attributes.addAll(CommonUtils.safeCollection(table.getAttributes(monitor)));
                                    }
                                }
                            } catch (DBException e) {
                                throw new InvocationTargetException(e);
                            }
                        }
                    };
                    DBeaverUI.runInProgressService(runnable);
                } catch (InvocationTargetException e) {
                    log.error(e.getTargetException());
                } catch (InterruptedException e) {
                    // skip
                }
            }
            if (!CommonUtils.isEmpty(attributes)) {
                String[] result = new String[attributes.size()];
                for (int i = 0; i < attributes.size(); i++) {
                    DBSEntityAttribute entity = attributes.get(i);
                    result[i] = entity.getName();
                }
                return result;
            }
        }
        return super.resolveAll(context);
    }

    @Override
    public void resolve(TemplateVariable variable, TemplateContext context)
    {
        super.resolve(variable, context);
        if (variable instanceof SQLVariable) {
            ((SQLVariable)variable).setResolver(this);
        }
    }

}
