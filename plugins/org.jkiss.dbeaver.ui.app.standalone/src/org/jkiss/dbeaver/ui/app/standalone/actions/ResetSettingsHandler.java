/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.app.standalone.internal.CoreApplicationMessages;
import org.jkiss.dbeaver.ui.dialogs.BaseDialog;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

public class ResetSettingsHandler extends AbstractHandler {
    private static final Log log = Log.getLog(ResetSettingsHandler.class);

    private static final String PLUGINS_FOLDER = ".plugins";
    private static final String CORE_RESOURCES_PLUGIN = "org.eclipse.core.resources";

    private static final Option[] OPTIONS = {
        new Option(
            "user_preferences",
            CoreApplicationMessages.reset_settings_option_user_preferences_name,
            CoreApplicationMessages.reset_settings_option_user_preferences_description,
            true,
            path -> !path.startsWith(CORE_RESOURCES_PLUGIN)
        ),
        new Option(
            "workspace_configuration",
            CoreApplicationMessages.reset_settings_option_workspace_configuration_name,
            CoreApplicationMessages.reset_settings_option_workspace_configuration_description,
            false,
            path -> path.startsWith(CORE_RESOURCES_PLUGIN)
        )
    };

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        final ResetSettingsDialog dialog = new ResetSettingsDialog(HandlerUtil.getActiveShell(event));

        if (dialog.open() != IDialogConstants.OK_ID) {
            return null;
        }

        final Set<String> options = dialog.options;
        final IWorkbench workbench = PlatformUI.getWorkbench();

        if (workbench.restart()) {
            final Path path = DBWorkbench.getPlatform().getWorkspace().getMetadataFolder().resolve(PLUGINS_FOLDER);

            if (Files.notExists(path) || !Files.isDirectory(path)) {
                return null;
            }

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    Files.walkFileTree(path, new SimpleFileVisitor<>() {
                        @Override
                        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                            log.trace("Deleting " + file);

                            try {
                                Files.delete(file);
                            } catch (IOException e) {
                                log.trace("Unable to delete " + file + ":" + e.getMessage());
                            }

                            return FileVisitResult.CONTINUE;
                        }

                        @Override
                        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                            if (dir.endsWith(PLUGINS_FOLDER)) {
                                return FileVisitResult.CONTINUE;
                            }

                            for (Option option : OPTIONS) {
                                if (options.contains(option.id) && option.predicate.test(path.relativize(dir))) {
                                    return FileVisitResult.CONTINUE;
                                }
                            }

                            return FileVisitResult.SKIP_SUBTREE;
                        }

                        @Override
                        public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
                            log.trace("Deleting " + dir);

                            try {
                                Files.delete(dir);
                            } catch (IOException e) {
                                log.trace("Unable to delete " + dir + ":" + e.getMessage());
                            }

                            return FileVisitResult.CONTINUE;
                        }
                    });
                } catch (IOException e) {
                    log.error("Error walking plugin settings", e);
                }
            }));
        }

        return null;
    }

    private static class ResetSettingsDialog extends BaseDialog {
        private final Set<String> options = new HashSet<>();

        public ResetSettingsDialog(@NotNull Shell shell) {
            super(shell, CoreApplicationMessages.reset_settings_dialog_title, null);
            setShellStyle(SWT.DIALOG_TRIM);
        }

        @Override
        protected Composite createDialogArea(Composite parent) {
            final Composite composite = super.createDialogArea(parent);

            UIUtils.createLabel(composite, CoreApplicationMessages.reset_settings_dialog_message);

            final Group group = UIUtils.createControlGroup(
                composite, CoreApplicationMessages.reset_settings_dialog_options, 1, GridData.FILL_BOTH, 0);

            final SelectionListener listener = SelectionListener.widgetSelectedAdapter(e -> {
                final Button checkbox = (Button) e.widget;
                final Option option = (Option) checkbox.getData();

                if (checkbox.getSelection()) {
                    options.add(option.id);
                } else {
                    options.remove(option.id);
                }

                UIUtils.asyncExec(() -> {
                    final Button button = getButton(IDialogConstants.OK_ID);

                    if (button != null) {
                        button.setEnabled(!options.isEmpty());
                    }
                });
            });

            final int indent = computeCheckboxIndent(group);

            for (Option option : ResetSettingsHandler.OPTIONS) {
                final Button checkbox = UIUtils.createCheckbox(group, option.name, option.description, option.checked, 1);
                checkbox.setData(option);
                checkbox.addSelectionListener(listener);
                checkbox.notifyListeners(SWT.Selection, new Event());

                final Label label = UIUtils.createLabel(group, option.description);
                label.setLayoutData(GridDataFactory.swtDefaults().indent(indent, 0).create());
            }

            return composite;
        }

        @Override
        protected void createButtonsForButtonBar(Composite parent) {
            createButton(parent, IDialogConstants.OK_ID, CoreApplicationMessages.button_apply_and_restart, true)
                .setEnabled(false);
            createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
        }

        private static int computeCheckboxIndent(@NotNull Composite parent) {
            final Label measurementLabel = UIUtils.createLabel(parent, "X ");
            final Button measurementCheckbox = UIUtils.createCheckbox(parent, "X", false);

            parent.layout(true, true);

            try {
                return measurementCheckbox.getSize().x - measurementLabel.getSize().x;
            } finally {
                measurementLabel.dispose();
                measurementCheckbox.dispose();
            }
        }
    }

    private static class Option {
        private final String id;
        private final String name;
        private final String description;
        private final boolean checked;
        private final Predicate<Path> predicate;

        public Option(@NotNull String id, @NotNull String name, @Nullable String description, boolean checked, @NotNull Predicate<Path> predicate) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.checked = checked;
            this.predicate = predicate;
        }
    }
}
