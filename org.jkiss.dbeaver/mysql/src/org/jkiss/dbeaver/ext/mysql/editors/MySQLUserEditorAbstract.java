/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.mysql.editors;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TableItem;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.mysql.model.MySQLGrant;
import org.jkiss.dbeaver.ext.mysql.model.MySQLPrivilege;
import org.jkiss.dbeaver.ext.mysql.model.MySQLTable;
import org.jkiss.dbeaver.ext.mysql.model.MySQLUser;
import org.jkiss.dbeaver.ext.mysql.runtime.MySQLUserManager;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.runtime.load.DatabaseLoadService;
import org.jkiss.dbeaver.runtime.load.LoadingUtils;
import org.jkiss.dbeaver.ui.DBIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.ObjectEditorPageControl;
import org.jkiss.dbeaver.ui.editors.AbstractDatabaseObjectEditor;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * MySQLUserEditorAbstract
 */
public abstract class MySQLUserEditorAbstract extends AbstractDatabaseObjectEditor<MySQLUser, MySQLUserManager>
{

    public MySQLUser getUser() {
        return getObjectManager().getObject();
    }

    void loadGrants()
    {
        LoadingUtils.executeService(
            new DatabaseLoadService<List<MySQLGrant>>("Load grants", getUser().getDataSource()) {
                public java.util.List<MySQLGrant> evaluate() throws InvocationTargetException, InterruptedException
                {
                    try {
                        return getUser().getGrants(getProgressMonitor());
                    }
                    catch (DBException e) {
                        throw new InvocationTargetException(e);
                    }
                }
            },
            getPageControl().createGrantsLoadVisualizer());
    }

    protected abstract UserPageControl getPageControl();
    protected abstract void processGrants(List<MySQLGrant> grants);

    protected class UserPageControl extends ObjectEditorPageControl {
        public UserPageControl(Composite parent) {
            super(parent, SWT.NONE, MySQLUserEditorAbstract.this);
        }

        public ProgressVisualizer<List<MySQLGrant>> createGrantsLoadVisualizer() {
            return new ProgressVisualizer<List<MySQLGrant>>() {
                public void completeLoading(List<MySQLGrant> grants) {
                    super.completeLoading(grants);
                    processGrants(grants);
                }
            };
        }
    }

}