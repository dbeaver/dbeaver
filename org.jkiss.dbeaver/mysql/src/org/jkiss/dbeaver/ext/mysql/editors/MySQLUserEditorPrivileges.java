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
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.mysql.controls.PrivilegesPairList;
import org.jkiss.dbeaver.runtime.load.AbstractLoadService;
import org.jkiss.dbeaver.runtime.load.LoadingUtils;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.ProgressPageControl;

import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.Map;

/**
 * MySQLUserEditorGeneral
 */
public class MySQLUserEditorPrivileges extends MySQLUserEditorAbstract
{
    static final Log log = LogFactory.getLog(MySQLUserEditorPrivileges.class);

    private PageControl pageControl;
    private org.eclipse.swt.widgets.List catList;
    private PrivilegesPairList privPair;

    private Map<String, Map<String, Boolean>> catalogPrivileges;

    public void createPartControl(Composite parent)
    {
        pageControl = new PageControl(parent, SWT.NONE);

        Composite container = new Composite(pageControl, SWT.NONE);
        GridLayout gl = new GridLayout(3, false);
        container.setLayout(gl);
        GridData gd = new GridData(GridData.FILL_BOTH);
        container.setLayoutData(gd);

        {
            Composite catalogGroup = UIUtils.createControlGroup(container, "Catalog", 1, GridData.FILL_VERTICAL, 200);

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
            Composite privsGroup = UIUtils.createControlGroup(container, "Privileges", 1, GridData.FILL_VERTICAL, 0);
            gd = (GridData)privsGroup.getLayoutData();
            gd.horizontalSpan = 2;
            gd.widthHint = 400;

            privPair = new PrivilegesPairList(privsGroup);
        }

        pageControl.createProgressPanel();
    }

    public void activatePart()
    {
        try {
            LoadingUtils.executeService(
                new AbstractLoadService<Map<String, Map<String, Boolean>>>("Load catalog privileges") {
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
        catch (Exception ex) {
            log.error("Can't obtain trigger body", ex);
        }
    }

    private class PageControl extends ProgressPageControl {
        public PageControl(Composite parent, int style) {
            super(parent, style, getSite().getPart());
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
            }
        }

    }


}