package org.jkiss.dbeaver.ext.cubrid.ui.views;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.dbeaver.ext.cubrid.model.CubridDataSource;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.navigator.DBNDataSource;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;
import org.jkiss.dbeaver.ui.navigator.NavigatorUtils;

public class CubridOIDHandler extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        final Shell activeShell = HandlerUtil.getActiveShell(event);
        final ISelection selection = HandlerUtil.getCurrentSelection(event);
        final DBNNode node = NavigatorUtils.getSelectedNode(selection);
        if (node instanceof DBNDataSource) {
            DBNDataSource dataSourceNode = (DBNDataSource) node;
            DataSourceDescriptor descriptor = (DataSourceDescriptor) dataSourceNode.getDataSourceContainer();
            DBPDataSource dataSource = descriptor.getDataSource();
            CubridDataSource cubrid = (CubridDataSource) dataSource;
            try {
                JDBCSession session = DBUtils.openMetaSession(new VoidProgressMonitor(), cubrid, "GetSession");
                CubridOIDSearchDialog dialog = new CubridOIDSearchDialog(activeShell, session);
                dialog.open();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }
}
