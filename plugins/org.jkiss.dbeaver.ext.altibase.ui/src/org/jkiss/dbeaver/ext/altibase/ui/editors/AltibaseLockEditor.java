/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.altibase.ui.editors;

import org.eclipse.jface.action.IContributionManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.swt.widgets.Composite;
import org.jkiss.dbeaver.ext.altibase.model.AltibaseDataSource;
import org.jkiss.dbeaver.ext.altibase.model.lock.AltibaseLock;
import org.jkiss.dbeaver.ext.altibase.model.lock.AltibaseLockManager;
import org.jkiss.dbeaver.ext.ui.locks.edit.AbstractLockEditor;
import org.jkiss.dbeaver.ext.ui.locks.manage.LockManagerViewer;
import org.jkiss.dbeaver.model.admin.locks.DBAServerLock;
import org.jkiss.dbeaver.model.admin.locks.DBAServerLockItem;
import org.jkiss.dbeaver.model.admin.locks.DBAServerLockManager;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;

import java.util.HashMap;

public class AltibaseLockEditor extends AbstractLockEditor {

    @Override
    protected LockManagerViewer createLockViewer(DBCExecutionContext executionContext, Composite parent) {

        DBAServerLockManager<DBAServerLock, DBAServerLockItem> lockManager = 
                (DBAServerLockManager) new AltibaseLockManager((AltibaseDataSource) executionContext.getDataSource());

        return new LockManagerViewer(this, parent, lockManager) {
            @Override
            protected void contributeToToolbar(DBAServerLockManager<DBAServerLock, 
                    DBAServerLockItem> sessionManager, IContributionManager contributionManager) {
                contributionManager.add(new Separator());
            }

            @Override
            protected void onLockSelect(final DBAServerLock lock) {
                super.onLockSelect(lock);
                if (lock != null) {
                    final AltibaseLock pLock = (AltibaseLock) lock;
                    super.refreshDetail(new HashMap<String, Object>() {{
                            put(AltibaseLockManager.sidHold, pLock.getHold_sid());
                            put(AltibaseLockManager.sidWait, pLock.getWait_sid()); 
                        }
                    });
                }
            }
        };
    }
}