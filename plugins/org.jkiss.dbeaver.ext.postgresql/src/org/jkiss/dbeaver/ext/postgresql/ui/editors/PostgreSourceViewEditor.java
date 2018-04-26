/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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

package org.jkiss.dbeaver.ext.postgresql.ui.editors;

import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.jface.action.ControlContribution;
import org.eclipse.jface.action.IContributionManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.jkiss.dbeaver.ext.postgresql.PostgreActivator;
import org.jkiss.dbeaver.ext.postgresql.PostgreConstants;
import org.jkiss.dbeaver.ext.postgresql.model.*;
import org.jkiss.dbeaver.model.DBPScriptObject;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.IDatabaseEditorInput;
import org.jkiss.dbeaver.ui.editors.sql.SQLSourceViewer;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;

import java.util.Map;

/**
 * PostgreSourceViewEditor
 */
public class PostgreSourceViewEditor extends SQLSourceViewer<PostgreScriptObject> {
    
    private static final String TOPIC_DEBUGGER_SOURCE = GeneralUtils.encodeTopic(DBPScriptObject.OPTION_DEBUGGER_SOURCE);
    private Button omitHeaderCheck;
    private Boolean showPermissions;
    private boolean showColumnComments = true;

    private IEventBroker eventBroker;
    private TopicEventHandler topicEventHandler = new TopicEventHandler();

    public PostgreSourceViewEditor()
    {
    }

    private class TopicEventHandler implements EventHandler {

        @Override
        public void handleEvent(Event event) {
            String topic = event.getTopic();
            if (TOPIC_DEBUGGER_SOURCE.equals(topic)) {
                Object data = event.getProperty(IEventBroker.DATA);
                if (data instanceof String) {
                    String nodePath = (String) data;
                    IDatabaseEditorInput editorInput = getEditorInput();
                    if (nodePath.equals(editorInput.getNavigatorNode().getNodeItemPath())) {
                        Object omitValue = editorInput.getAttribute(DBPScriptObject.OPTION_DEBUGGER_SOURCE);
                        boolean omitHeader = Boolean.parseBoolean(String.valueOf(omitValue));
                        if (!omitHeader) {
                            setOmitHeader(true);
                        }
                    }
                }
            }
        }

    }

    @Override
    public void dispose() {
        if (eventBroker != null) {
            eventBroker.unsubscribe(topicEventHandler);
            eventBroker = null;
        }
        super.dispose();
    }

    @Override
    protected boolean isReadOnly()
    {
        PostgreScriptObject sourceObject = getSourceObject();
        if (sourceObject instanceof PostgreProcedure || sourceObject instanceof PostgreTrigger) {
            return false;
        }
        return true;
    }

    public boolean getShowPermissions() {
        // By default permissions enabled only for tables
        return showPermissions != null ? showPermissions : getSourceObject() instanceof PostgreTableBase;
    }

    @Override
    protected void setSourceText(DBRProgressMonitor monitor, String sourceText)
    {
        getEditorInput().getPropertySource().setPropertyValue(monitor, "objectDefinitionText", sourceText);
    }

    @Override
    protected void contributeEditorCommands(IContributionManager contributionManager)
    {
        super.contributeEditorCommands(contributionManager);
        if (eventBroker == null) {
            eventBroker = PostgreActivator.getDefault().getEventBroker();
            eventBroker.subscribe(TOPIC_DEBUGGER_SOURCE, topicEventHandler);
        }
        PostgreScriptObject sourceObject = getSourceObject();
        if (sourceObject instanceof PostgreProcedure) {
            contributionManager.add(new Separator());
            contributionManager.add(new ControlContribution("ProcedureDebugSource") {
                @Override
                protected Control createControl(Composite parent) {
                    omitHeaderCheck = UIUtils.createCheckbox(parent, "Omit procedure header", "Show only procedure body without auto-generated header", false, 0);
                    Object omitValue = getEditorInput().getAttribute(DBPScriptObject.OPTION_DEBUGGER_SOURCE);
                    boolean omitHeader = Boolean.parseBoolean(String.valueOf(omitValue));
                    omitHeaderCheck.setSelection(omitHeader);
                    omitHeaderCheck.addSelectionListener(new SelectionAdapter() {
                        @Override
                        public void widgetSelected(SelectionEvent e) {
                            boolean omitHeader = omitHeaderCheck.getSelection();
                            getEditorInput().setAttribute(DBPScriptObject.OPTION_DEBUGGER_SOURCE, omitHeader);
                            refreshPart(PostgreSourceViewEditor.this, true);
                        }
                    });
                    return omitHeaderCheck;
                }
            });
        }
        if (sourceObject instanceof PostgrePermissionsOwner) {
            contributionManager.add(new Separator());
            contributionManager.add(new ControlContribution("PGDDLShowPermissions") {
                @Override
                protected Control createControl(Composite parent) {
                    Button showPermissionsCheck = UIUtils.createCheckbox(parent, "Show permissions", "Show permission grants", getShowPermissions(), 0);
                    showPermissionsCheck.addSelectionListener(new SelectionAdapter() {
                        @Override
                        public void widgetSelected(SelectionEvent e) {
                            showPermissions = showPermissionsCheck.getSelection();
                            refreshPart(PostgreSourceViewEditor.this, true);
                        }
                    });
                    return showPermissionsCheck;
                }
            });
        }
        if (sourceObject instanceof PostgreTableBase) {
            contributionManager.add(new ControlContribution("PGDDLShowColumnComments") {
                @Override
                protected Control createControl(Composite parent) {
                    Button showColumnCommentsCheck = UIUtils.createCheckbox(parent, "Show comments", "Show column comments in column definition", true, 0);
                    showColumnCommentsCheck.addSelectionListener(new SelectionAdapter() {
                        @Override
                        public void widgetSelected(SelectionEvent e) {
                            showColumnComments = showColumnCommentsCheck.getSelection();
                            refreshPart(PostgreSourceViewEditor.this, true);
                        }
                    });
                    return showColumnCommentsCheck;
                }
            });
        }
    }

    private void setOmitHeader(boolean omitHeader) {
        Display.getDefault().syncExec(new Runnable() {
            
            @Override
            public void run() {
                getEditorInput().setAttribute(DBPScriptObject.OPTION_DEBUGGER_SOURCE, omitHeader);
                omitHeaderCheck.setSelection(omitHeader);
                refreshPart(this, true);
            }
        });
    }

    @Override
    protected Map<String, Object> getSourceOptions() {
        Map<String, Object> options = super.getSourceOptions();
        Object omitValue = getEditorInput().getAttribute(DBPScriptObject.OPTION_DEBUGGER_SOURCE);
        boolean omitHeader = Boolean.parseBoolean(String.valueOf(omitValue));
        options.put(DBPScriptObject.OPTION_DEBUGGER_SOURCE, omitHeader);
        options.put(PostgreConstants.OPTION_DDL_SHOW_PERMISSIONS, getShowPermissions());
        options.put(PostgreConstants.OPTION_DDL_SHOW_COLUMN_COMMENTS, showColumnComments);
        return options;
    }
}

