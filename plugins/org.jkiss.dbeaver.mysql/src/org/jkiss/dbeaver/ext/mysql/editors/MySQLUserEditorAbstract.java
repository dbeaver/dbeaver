/*
 * Copyright (C) 2010-2015 Serge Rieder serge@jkiss.org
 * Copyright (C) 2011-2012 Eugene Fradkin eugene.fradkin@gmail.com
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jkiss.dbeaver.ext.mysql.editors;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.mysql.MySQLMessages;
import org.jkiss.dbeaver.ext.mysql.model.MySQLGrant;
import org.jkiss.dbeaver.ext.mysql.model.MySQLUser;
import org.jkiss.dbeaver.runtime.load.DatabaseLoadService;
import org.jkiss.dbeaver.runtime.load.LoadingUtils;
import org.jkiss.dbeaver.ui.controls.ObjectEditorPageControl;
import org.jkiss.dbeaver.ui.editors.AbstractDatabaseObjectEditor;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

/**
 * MySQLUserEditorAbstract
 */
public abstract class MySQLUserEditorAbstract extends AbstractDatabaseObjectEditor<MySQLUser>
{

    void loadGrants()
    {
        LoadingUtils.createService(
            new DatabaseLoadService<List<MySQLGrant>>(MySQLMessages.editors_user_editor_abstract_load_grants, getDatabaseObject().getDataSource()) {
                @Override
                public java.util.List<MySQLGrant> evaluate() throws InvocationTargetException, InterruptedException
                {
                    try {
                        return getDatabaseObject().getGrants(getProgressMonitor());
                    }
                    catch (DBException e) {
                        throw new InvocationTargetException(e);
                    }
                }
            },
            getPageControl().createGrantsLoadVisualizer())
            .schedule();
    }

    @Override
    public void setFocus()
    {
        if (getPageControl() != null) {
            getPageControl().setFocus();
        }
    }

    protected abstract UserPageControl getPageControl();
    protected abstract void processGrants(List<MySQLGrant> grants);

    protected class UserPageControl extends ObjectEditorPageControl {
        public UserPageControl(Composite parent) {
            super(parent, SWT.NONE, MySQLUserEditorAbstract.this);
        }

        public ProgressVisualizer<List<MySQLGrant>> createGrantsLoadVisualizer() {
            return new ProgressVisualizer<List<MySQLGrant>>() {
                @Override
                public void completeLoading(List<MySQLGrant> grants) {
                    super.completeLoading(grants);
                    processGrants(grants);
                }
            };
        }
    }

}