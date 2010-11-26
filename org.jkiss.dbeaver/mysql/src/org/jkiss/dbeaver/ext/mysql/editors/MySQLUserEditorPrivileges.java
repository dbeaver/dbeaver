/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.mysql.editors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.DatabaseObjectChangeAction;
import org.jkiss.dbeaver.ext.mysql.controls.PrivilegesPairList;
import org.jkiss.dbeaver.ext.mysql.model.MySQLUser;
import org.jkiss.dbeaver.runtime.AbstractDatabaseObjectCommand;
import org.jkiss.dbeaver.runtime.load.DatabaseLoadService;
import org.jkiss.dbeaver.runtime.load.LoadingUtils;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.ObjectEditorPageControl;

import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.Map;

/**
 * MySQLUserEditorPrivileges
 */
public class MySQLUserEditorPrivileges extends MySQLUserEditorAbstract
{
    static final Log log = LogFactory.getLog(MySQLUserEditorPrivileges.class);

    private PageControl pageControl;
    private org.eclipse.swt.widgets.List catList;
    private PrivilegesPairList privPair;

    private volatile Map<String, Map<String, Boolean>> catalogPrivileges;
    private boolean isLoaded = false;

    public void createPartControl(Composite parent)
    {
        pageControl = new PageControl(parent, SWT.NONE);

        Composite container = UIUtils.createPlaceholder(pageControl, 3, 5);
        GridData gd = new GridData(GridData.FILL_BOTH);
        container.setLayoutData(gd);

        {
            Composite catalogGroup = UIUtils.createControlGroup(container, "Catalog", 1, GridData.FILL_VERTICAL, 250);

            catList = new org.eclipse.swt.widgets.List(catalogGroup, SWT.BORDER | SWT.SINGLE);
            gd = new GridData(GridData.FILL_BOTH);
            catList.setLayoutData(gd);
            catList.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    int selIndex = catList.getSelectionIndex();
                    if (selIndex < 0) {
                        privPair.setModel(Collections.<String, Boolean>emptyMap());
                    } else {
                        Map<String, Boolean> privs = catalogPrivileges.get(catList.getItem(selIndex));
                        if (privs != null) {
                            privPair.setModel(privs);
                        }
                    }
                }
            });
        }

        {
            Composite privsGroup = UIUtils.createControlGroup(container, "Privileges", 1, GridData.FILL_VERTICAL, 500);
            gd = (GridData)privsGroup.getLayoutData();
            gd.horizontalSpan = 2;
            gd.widthHint = 400;

            privPair = new PrivilegesPairList(privsGroup);
            privPair.addListener(SWT.Modify, new Listener() {
                public void handleEvent(Event event)
                {
                    addChangeCommand(new AbstractDatabaseObjectCommand<MySQLUser>() {
                        public void updateObjectState(MySQLUser object)
                        {
                        }
                        public DatabaseObjectChangeAction[] getChangeActions()
                        {
                            return null;
                        }
                    });
                }
            });
        }

        pageControl.createProgressPanel();
    }

    public synchronized void activatePart()
    {
        if (isLoaded) {
            return;
        }
        isLoaded = true;
        LoadingUtils.executeService(
            new DatabaseLoadService<Map<String, Map<String, Boolean>>>("Load catalog privileges", getUser().getDataSource()) {
                public Map<String, Map<String, Boolean>> evaluate()
                    throws InvocationTargetException, InterruptedException
                {
                    try {
                        return getUser().getCatalogPrivileges(getProgressMonitor());
                    }
                    catch (DBException e) {
                        log.error(e);
                    }
                    return null;
                }
            },
            pageControl.createLoadVisualizer());
    }

    private class PageControl extends ObjectEditorPageControl {
        public PageControl(Composite parent, int style) {
            super(parent, style, MySQLUserEditorPrivileges.this);
        }

        public ProgressVisualizer<Map<String, Map<String, Boolean>>> createLoadVisualizer() {
            return new SessionsVisualizer();
        }

        private class SessionsVisualizer extends ProgressVisualizer<Map<String, Map<String, Boolean>>> {
            @Override
            public void completeLoading(Map<String, Map<String, Boolean>> privs) {
                super.completeLoading(privs);
                if (privs == null) {
                    return;
                }
                catalogPrivileges = privs;

                for (String catalog : catalogPrivileges.keySet()) {
                    catList.add(catalog);
                }
                //setInfo(String.valueOf(catalogPrivileges.size()) + " privileges");
            }
        }

    }


}