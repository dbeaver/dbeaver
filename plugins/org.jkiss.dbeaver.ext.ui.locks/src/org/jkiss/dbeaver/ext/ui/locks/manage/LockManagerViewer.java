/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2017 Andrew Khitrin (ahitrin@gmail.com) 
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
package org.jkiss.dbeaver.ext.ui.locks.manage;

import java.util.Collection;
import java.util.Map;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IContributionManager;
import org.eclipse.jface.viewers.*;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchSite;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.ui.locks.graph.LockGraphicalView;
import org.jkiss.dbeaver.ext.ui.locks.table.LockTable;
import org.jkiss.dbeaver.ext.ui.locks.table.LockTableDetail;
import org.jkiss.dbeaver.model.admin.locks.DBAServerLock;
import org.jkiss.dbeaver.model.admin.locks.DBAServerLockItem;
import org.jkiss.dbeaver.model.admin.locks.DBAServerLockManager;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.UIUtils;

/**
 * LockManagerViewer
 */
public class LockManagerViewer {

    public static final String keyType = "type";
    public static final String typeWait = "wait";
    public static final String typeHold = "hold";

    private Font boldFont;
    private LockListControl lockTable;
    private LockTableDetail blockedTable;
    private LockTableDetail blockingTable;
    private Label blockedLabel;
    private Label blockingLabel;
    private DBAServerLock<?> curLock;
    private LockGraphManager<?, ?> graphManager;
    private LockGraphicalView gv;

    @SuppressWarnings("unused")
    private final DBAServerLockManager<DBAServerLock<?>, DBAServerLockItem> lockManager;

    private Action killAction = new Action("Kill waiting session", UIUtils.getShardImageDescriptor(ISharedImages.IMG_ELCL_STOP)) {
        @Override
        public void run() {
        	if (curLock != null) {
                DBAServerLock<?> root = graphManager.getGraph(curLock).getLockRoot();
                alterSession();
                refreshLocks(root);
                setTableLockSelect(root);        		
        	}
        }
    };


    public LockGraphManager<?, ?> getGraphManager() {
        return graphManager;
    }

    public void dispose() {
        lockTable.disposeControl();
        UIUtils.dispose(boldFont);
    }

    protected LockManagerViewer(IWorkbenchPart part, Composite parent, final DBAServerLockManager<DBAServerLock<?>, DBAServerLockItem> lockManager) {


        this.graphManager = (LockGraphManager<?, ?>) lockManager;

        boldFont = UIUtils.makeBoldFont(parent.getFont());
        Composite composite = UIUtils.createPlaceholder(parent, 1);

        SashForm sashMain = UIUtils.createPartDivider(part, composite, SWT.HORIZONTAL | SWT.SMOOTH);
        sashMain.setLayoutData(new GridData(GridData.FILL_BOTH));

        SashForm sash = UIUtils.createPartDivider(part, sashMain, SWT.VERTICAL | SWT.SMOOTH);
        sash.setLayoutData(new GridData(GridData.FILL_BOTH));

        this.lockManager = lockManager;
        lockTable = new LockListControl(sash, part.getSite(), lockManager, lockManager.getLocksType());
        lockTable.createProgressPanel(composite);

        lockTable.getItemsViewer().addSelectionChangedListener(new ISelectionChangedListener() {
            @Override
            public void selectionChanged(SelectionChangedEvent event) {
                onLockSelect(getSelectedLock());
            }
        });

        lockTable.loadData();

        SashForm infoSash = UIUtils.createPartDivider(part, sash, SWT.HORIZONTAL | SWT.SMOOTH);
        infoSash.setLayoutData(new GridData(GridData.FILL_BOTH));

        Composite cBlocked = UIUtils.createPlaceholder(infoSash, 1, 5);

        blockedLabel = new Label(cBlocked, SWT.NULL);
        blockedLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        blockedLabel.setFont(boldFont);

        blockedTable = new LockTableDetail(cBlocked, SWT.SHEET, part.getSite(), lockManager);
        blockedTable.setLayoutData(new GridData(GridData.FILL_BOTH));

        Composite cBlocking = UIUtils.createPlaceholder(infoSash, 1, 5);

        blockingLabel = new Label(cBlocking, SWT.NULL);
        blockingLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        blockingLabel.setFont(boldFont);

        blockingTable = new LockTableDetail(cBlocking, SWT.SHEET, part.getSite(), lockManager);
        blockingTable.setLayoutData(new GridData(GridData.FILL_BOTH));

        gv = new LockGraphicalView(this);
        gv.createPartControl(sashMain);

        sashMain.setWeights(new int[]{3, 1});
        sash.setWeights(new int[]{4, 1});

    }

    protected void onLockSelect(DBAServerLock<?> lock) {
        curLock = lock;
        refreshGraph(curLock);
    }

    public void setTableLockSelect(DBAServerLock<?> lock) {
        ColumnViewer itemsViewer = lockTable.getItemsViewer();
        itemsViewer.getControl().setRedraw(false);
        try {
            itemsViewer.setSelection(new StructuredSelection(lock), true);
        } finally {
            itemsViewer.getControl().setRedraw(true);
        }
        curLock = lock;
    }

    protected void contributeToToolbar(DBAServerLockManager<DBAServerLock<?>, DBAServerLockItem> sessionManager, IContributionManager contributionManager) {

    }

    public Action getKillAction() {
        return killAction;
    }

    private DBAServerLock<?> getSelectedLock() {
        ISelection selection = lockTable.getSelectionProvider().getSelection();
        if (selection instanceof IStructuredSelection && !selection.isEmpty()) {
            return (DBAServerLock<?>) ((IStructuredSelection) selection).getFirstElement();
        } else {
            return null;
        }
    }

    private void refreshGraph(DBAServerLock<?> selected) {
        gv.drawGraf(selected);
    }

    public void refreshLocks(DBAServerLock<?> selected) {
        lockTable.loadData(false);
        gv.drawGraf(selected);

    }

    public void refreshDetail(Map<String, Object> options) {
        StringBuilder sb = new StringBuilder("Wait - ");
        sb.append(curLock.getTitle());
        blockedLabel.setText(sb.toString());
        blockedTable.getOptions().putAll(options);
        blockedTable.getOptions().put(keyType, typeWait);
        blockedTable.loadData(false);
        sb.setLength(0);
        if (curLock.getHoldBy() != null) {
            sb.append("Hold - ");
            sb.append(curLock.getHoldBy().getTitle());
            blockingLabel.setText(sb.toString());
        }
        blockingTable.getOptions().putAll(options);
        blockingTable.getOptions().put(keyType, typeHold);
        blockingTable.loadData();

    }

    public void alterSession() {
        if (UIUtils.confirmAction(
                "Terminate",
                NLS.bind("Teminate session?", "Terminate"))) {

            lockTable.createAlterService(curLock, null).schedule();
        }
    }

    public Composite getControl() {
        return lockTable.getControl();
    }

    private class LockListControl extends LockTable {

        private Class<DBAServerLock<?>> locksType;

        LockListControl(SashForm sash, IWorkbenchSite site, DBAServerLockManager<DBAServerLock<?>, DBAServerLockItem> lockManager, Class<DBAServerLock<?>> locksType) {
            super(sash, SWT.SHEET, site, lockManager);
            this.locksType = locksType;
        }

        @Nullable
        @Override
        protected Class<?>[] getListBaseTypes(Collection<DBAServerLock<?>> items) {
            return new Class[] { locksType };
        }

        @Override
        protected void fillCustomActions(IContributionManager contributionManager) {
            contributeToToolbar(getLockManager(), contributionManager);
            contributionManager.add(new Action("Refresh locks", DBeaverIcons.getImageDescriptor(UIIcon.REFRESH)) {
                @Override
                public void run() {
                    refreshLocks(curLock);
                }
            });
            contributionManager.add(killAction);
        }

    }

}