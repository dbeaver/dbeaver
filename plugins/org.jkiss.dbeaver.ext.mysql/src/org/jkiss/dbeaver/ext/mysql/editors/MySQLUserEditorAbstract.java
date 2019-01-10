/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
 * Copyright (C) 2011-2012 Eugene Fradkin (eugene.fradkin@gmail.com)
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
package org.jkiss.dbeaver.ext.mysql.editors;

import org.eclipse.jface.action.IContributionManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.mysql.MySQLMessages;
import org.jkiss.dbeaver.ext.mysql.model.MySQLGrant;
import org.jkiss.dbeaver.ext.mysql.model.MySQLUser;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.load.DatabaseLoadService;
import org.jkiss.dbeaver.ui.LoadingJob;
import org.jkiss.dbeaver.ui.controls.ObjectEditorPageControl;
import org.jkiss.dbeaver.ui.editors.AbstractDatabaseObjectEditor;
import org.jkiss.dbeaver.ui.editors.DatabaseEditorUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

/**
 * MySQLUserEditorAbstract
 */
public abstract class MySQLUserEditorAbstract extends AbstractDatabaseObjectEditor<MySQLUser>
{

    void loadGrants()
    {
        LoadingJob.createService(
            new DatabaseLoadService<List<MySQLGrant>>(MySQLMessages.editors_user_editor_abstract_load_grants, getDatabaseObject().getDataSource()) {
                @Override
                public java.util.List<MySQLGrant> evaluate(DBRProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
                    try {
                        return getDatabaseObject().getGrants(monitor);
                    } catch (DBException e) {
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

        @Override
        public void fillCustomActions(IContributionManager contributionManager) {
            super.fillCustomActions(contributionManager);
            DatabaseEditorUtils.contributeStandardEditorActions(getSite(), contributionManager);
        }
    }

}