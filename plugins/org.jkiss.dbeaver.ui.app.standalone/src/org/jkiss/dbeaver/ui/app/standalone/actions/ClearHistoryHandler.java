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
package org.jkiss.dbeaver.ui.app.standalone.actions;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.impl.AbstractDescriptor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.app.standalone.internal.CoreApplicationMessages;
import org.jkiss.dbeaver.ui.dialogs.BaseDialog;

import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class ClearHistoryHandler extends AbstractHandler {
    private static final String EXTENSION_ID = "org.jkiss.dbeaver.clearHistoryHandler";

    private final Map<String, HandlerDescriptor> descriptors = new LinkedHashMap<>();

    public ClearHistoryHandler() {
        for (IConfigurationElement element : Platform.getExtensionRegistry().getConfigurationElementsFor(EXTENSION_ID)) {
            if (HandlerDescriptor.ELEMENT_ID.equals(element.getName())) {
                final HandlerDescriptor descriptor = new HandlerDescriptor(element);
                this.descriptors.put(descriptor.id, descriptor);
            }
        }
    }

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        final ClearHistoryDialog dialog = new ClearHistoryDialog(HandlerUtil.getActiveShell(event));

        if (dialog.open() != IDialogConstants.OK_ID) {
            return null;
        }

        if (PlatformUI.getWorkbench().restart()) {
            for (String option : dialog.options) {
                final HandlerDescriptor descriptor = descriptors.get(option);

                try {
                    UIUtils.runInProgressDialog(descriptor.getHandler());
                } catch (Exception e) {
                    DBWorkbench.getPlatformUI().showError(
                        CoreApplicationMessages.clear_history_error_title,
                        NLS.bind(CoreApplicationMessages.clear_history_error_message, descriptor.name),
                        e instanceof InvocationTargetException ? ((InvocationTargetException) e).getTargetException() : e
                    );
                }
            }
        }

        return null;
    }

    private class ClearHistoryDialog extends BaseDialog {
        private final Set<String> options = new HashSet<>();

        public ClearHistoryDialog(@NotNull Shell shell) {
            super(shell, CoreApplicationMessages.clear_history_dialog_title, null);
            setShellStyle(SWT.DIALOG_TRIM);
        }

        @Override
        protected Composite createDialogArea(Composite parent) {
            final Composite composite = super.createDialogArea(parent);

            UIUtils.createLabel(composite, CoreApplicationMessages.clear_history_dialog_message);

            final Group group = UIUtils.createControlGroup(
                composite, CoreApplicationMessages.clear_history_dialog_options, 1, GridData.FILL_BOTH, 0);

            final SelectionListener listener = new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    final Button checkbox = (Button) e.widget;
                    final HandlerDescriptor descriptor = (HandlerDescriptor) checkbox.getData();

                    if (checkbox.getSelection()) {
                        options.add(descriptor.id);
                    } else {
                        options.remove(descriptor.id);
                    }

                    final Button button = getButton(IDialogConstants.OK_ID);

                    if (button != null) {
                        button.setEnabled(!options.isEmpty());
                    }
                }
            };

            for (HandlerDescriptor descriptor : descriptors.values()) {
                final Button checkbox = UIUtils.createCheckbox(group, descriptor.name, descriptor.description, false, 1);
                checkbox.setData(descriptor);
                checkbox.addSelectionListener(listener);
            }

            return composite;
        }

        @Override
        protected void createButtonsForButtonBar(Composite parent) {
            createButton(parent, IDialogConstants.OK_ID, CoreApplicationMessages.button_apply_and_restart, true).setEnabled(false);
            createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
        }
    }

    private static class HandlerDescriptor extends AbstractDescriptor {
        public static final String ELEMENT_ID = "handler";

        private final String id;
        private final String name;
        private final String description;
        private final ObjectType handler;

        public HandlerDescriptor(@NotNull IConfigurationElement config) {
            super(config);

            this.id = config.getAttribute("id");
            this.name = config.getAttribute("name");
            this.description = config.getAttribute("description");
            this.handler = new ObjectType(config.getAttribute("handler"));
        }

        @NotNull
        public DBRRunnableWithProgress getHandler() throws DBException {
            handler.checkObjectClass(DBRRunnableWithProgress.class);
            try {
                return handler
                    .getObjectClass(DBRRunnableWithProgress.class)
                    .getDeclaredConstructor()
                    .newInstance();
            } catch (Throwable e) {
                throw new DBException("Can't create instance", e);
            }
        }
    }
}
