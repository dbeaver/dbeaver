package org.jkiss.dbeaver.ext.dm.ui.editors;

import java.util.HashMap;

import org.eclipse.jface.action.IContributionManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.swt.widgets.Composite;
import org.jkiss.dbeaver.ext.dm.model.DmDataSource;
import org.jkiss.dbeaver.ext.dm.model.lock.DmLock;
import org.jkiss.dbeaver.ext.dm.model.lock.DmLockManager;
import org.jkiss.dbeaver.ext.ui.locks.edit.AbstractLockEditor;
import org.jkiss.dbeaver.ext.ui.locks.manage.LockManagerViewer;
import org.jkiss.dbeaver.model.admin.locks.DBAServerLock;
import org.jkiss.dbeaver.model.admin.locks.DBAServerLockItem;
import org.jkiss.dbeaver.model.admin.locks.DBAServerLockManager;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;

public class DmLockEditor extends AbstractLockEditor {

    @Override
    protected LockManagerViewer createLockViewer(DBCExecutionContext executionContext, Composite parent) {

        DBAServerLockManager<DBAServerLock, DBAServerLockItem> lockManager = (DBAServerLockManager) new DmLockManager((DmDataSource) executionContext.getDataSource());

        return new LockManagerViewer(this, parent, lockManager) {
            @Override
            protected void contributeToToolbar(DBAServerLockManager<DBAServerLock, DBAServerLockItem> sessionManager, IContributionManager contributionManager) {
                contributionManager.add(new Separator());
            }

            @Override
            protected void onLockSelect(final DBAServerLock lock) {
                super.onLockSelect(lock);
                if (lock != null) {
                    final DmLock pLock = (DmLock) lock;
                    super.refreshDetail(new HashMap<String, Object>() {{
                        put(DmLockManager.sidHold, pLock.getHold_sid());
                        put(DmLockManager.sidWait, pLock.getWait_sid());
                    }});
                }
            }
        };
    }

}

