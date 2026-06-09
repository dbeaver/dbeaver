/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.yashandb.ui.editors;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IContributionManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.swt.widgets.Composite;
import org.jkiss.dbeaver.ext.oracle.model.OracleDataSource;
import org.jkiss.dbeaver.ext.oracle.model.session.OracleServerSessionManager;
import org.jkiss.dbeaver.ext.oracle.ui.editors.OracleSessionEditor;
import org.jkiss.dbeaver.ext.yashandb.model.session.YashanDBServerSession;
import org.jkiss.dbeaver.ext.yashandb.model.session.YashanDBServerSessionManager;
import org.jkiss.dbeaver.ext.yashandb.ui.internal.YashanDBUIMessages;
import org.jkiss.dbeaver.model.admin.sessions.DBAServerSession;
import org.jkiss.dbeaver.model.admin.sessions.DBAServerSessionManager;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.ui.ActionUtils;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.views.session.SessionManagerViewer;
import org.jkiss.utils.CommonUtils;

import java.util.HashMap;
import java.util.Map;


/**
 * YashanDBSessionEditor
 */
public class YashanDBSessionEditor extends OracleSessionEditor {

    @Override
    public void createEditorControl(Composite parent) {
        // YashanDB only support kill session
        killSessionAction = new DisconnectSessionAction(true);
        super.createEditorControl(parent);
    }

    @Override
    protected SessionManagerViewer createSessionViewer(DBCExecutionContext executionContext, Composite parent) {
        return new SessionManagerViewer<YashanDBServerSession>(this, parent,
            new YashanDBServerSessionManager((OracleDataSource) executionContext.getDataSource())) {

            private boolean showBackground;
            private boolean showInactive;

            @Override
            protected void contributeToToolbar(DBAServerSessionManager sessionManager,
                                               IContributionManager contributionManager) {
                contributionManager.add(killSessionAction);
                contributionManager.add(new Separator());

                contributionManager.add(ActionUtils.makeActionContribution(new Action(
                    YashanDBUIMessages.views_session_manager_viewer_show_background, Action.AS_CHECK_BOX) {
                    {
                        setImageDescriptor(DBeaverIcons.getImageDescriptor(UIIcon.CONFIGURATION));
                        setToolTipText(YashanDBUIMessages.views_session_manager_viewer_show_background_tasks_tip);
                        setChecked(showBackground);
                    }

                    @Override
                    public void run() {
                        showBackground = isChecked();
                        refreshPart(YashanDBSessionEditor.this, true);
                    }
                }, true));

                contributionManager.add(ActionUtils.makeActionContribution(
                    new Action(YashanDBUIMessages.views_session_manager_viewer_show_inactive, Action.AS_CHECK_BOX) {
                        {
                            setImageDescriptor(DBeaverIcons.getImageDescriptor(UIIcon.CONFIGURATION));
                            setToolTipText(
                                YashanDBUIMessages.views_session_manager_viewer_show_inactive_sessions_tip);
                            setChecked(showInactive);
                        }

                        @Override
                        public void run() {
                            showInactive = isChecked();
                            refreshPart(YashanDBSessionEditor.this, true);
                        }
                    }, true));
            }

            @Override
            protected void onSessionSelect(DBAServerSession session) {
                super.onSessionSelect(session);
                killSessionAction.setEnabled(session != null);
            }

            @Override
            protected void loadSettings(IDialogSettings settings) {
                showBackground = CommonUtils.toBoolean(settings.get("showBackground"));
                showInactive = CommonUtils.toBoolean(settings.get("showInactive"));
                super.loadSettings(settings);
            }

            @Override
            protected void saveSettings(IDialogSettings settings) {
                super.saveSettings(settings);
                settings.put("showBackground", showBackground);
                settings.put("showInactive", showInactive);
            }

            @Override
            public Map<String, Object> getSessionOptions() {
                Map<String, Object> options = new HashMap<>();
                if (showBackground) {
                    options.put(OracleServerSessionManager.OPTION_SHOW_BACKGROUND, true);
                }
                if (showInactive) {
                    options.put(OracleServerSessionManager.OPTION_SHOW_INACTIVE, true);
                }
                return options;
            }

        };
    }
}
