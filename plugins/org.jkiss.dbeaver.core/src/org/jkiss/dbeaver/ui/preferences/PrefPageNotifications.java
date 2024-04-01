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
package org.jkiss.dbeaver.ui.preferences;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBeaverPreferences;
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.CustomCheckboxCellEditor;
import org.jkiss.dbeaver.ui.controls.CustomComboBoxCellEditor;
import org.jkiss.dbeaver.ui.controls.ListContentProvider;
import org.jkiss.dbeaver.ui.controls.ViewerColumnController;
import org.jkiss.dbeaver.ui.internal.UIMessages;
import org.jkiss.dbeaver.ui.notifications.NotificationSettings;
import org.jkiss.dbeaver.ui.notifications.NotificationUtils;
import org.jkiss.dbeaver.ui.registry.NotificationDescriptor;
import org.jkiss.dbeaver.ui.registry.NotificationRegistry;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class PrefPageNotifications extends AbstractPrefPage implements IWorkbenchPreferencePage {
    private static final String SOUND_BEEP_LABEL = "System beep";
    private static final String SOUND_NONE_LABEL = "No sound";
    private static final String SOUND_FILE_LABEL = "Choose file...";

    private final Map<NotificationDescriptor, NotificationSettings> settings = new HashMap<>();

    private TableViewer viewer;
    private Button enablePopupsCheckbox;
    private Spinner hideDelaySpinner;
    private Button enableSoundsCheckbox;
    private Spinner soundVolumeSpinner;
    private Button longOperationsCheck;
    private Spinner longOperationsTimeout;

    @Override
    public void init(IWorkbench workbench) {
        // nothing to initialize
    }

    @NotNull
    @Override
    protected Control createPreferenceContent(@NotNull Composite parent) {
        final Composite composite = UIUtils.createPlaceholder(parent, 1, 5);

        {
            final DBPPreferenceStore preferences = DBWorkbench.getPlatform().getPreferenceStore();
            final Group group = UIUtils.createControlGroup(
                composite,
                CoreMessages.pref_page_notifications_group_global,
                2,
                GridData.FILL_HORIZONTAL,
                0
            );

            enablePopupsCheckbox = UIUtils.createCheckbox(
                group,
                CoreMessages.pref_page_notifications_enable_notifications_label,
                CoreMessages.pref_page_notifications_enable_notifications_label_tip,
                preferences.getBoolean(ModelPreferences.NOTIFICATIONS_ENABLED),
                2
            );

            hideDelaySpinner = UIUtils.createLabelSpinner(
                group,
                CoreMessages.pref_page_notifications_label_notifications_close_delay,
                preferences.getInt(ModelPreferences.NOTIFICATIONS_CLOSE_DELAY_TIMEOUT), 0, Integer.MAX_VALUE
            );

            enableSoundsCheckbox = UIUtils.createCheckbox(
                group,
                CoreMessages.pref_page_notifications_enable_sounds_label,
                null,
                preferences.getBoolean(ModelPreferences.NOTIFICATIONS_SOUND_ENABLED),
                2
            );

            soundVolumeSpinner = UIUtils.createLabelSpinner(
                group,
                CoreMessages.pref_page_notifications_sound_volume_label,
                preferences.getInt(ModelPreferences.NOTIFICATIONS_SOUND_VOLUME), 1, 100
            );

            longOperationsCheck = UIUtils.createCheckbox(
                group,
                CoreMessages.pref_page_ui_general_label_enable_long_operations,
                CoreMessages.pref_page_ui_general_label_enable_long_operations_tip,
                preferences.getBoolean(DBeaverPreferences.AGENT_LONG_OPERATION_NOTIFY),
                2
            );

            longOperationsTimeout = UIUtils.createLabelSpinner(
                group,
                CoreMessages.pref_page_ui_general_label_long_operation_timeout + UIMessages.label_sec,
                preferences.getInt(DBeaverPreferences.AGENT_LONG_OPERATION_TIMEOUT), 0, Integer.MAX_VALUE
            );
        }

        {
            viewer = new TableViewer(composite, SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION);
            viewer.getTable().setLayoutData(GridDataFactory.fillDefaults().grab(true, true).hint(600, SWT.DEFAULT).create());
            viewer.getTable().setHeaderVisible(true);

            final ViewerColumnController<Object, Object> controller = new ViewerColumnController<>("PrefPageNotifications", viewer);
            controller.addColumn(
                CoreMessages.pref_page_notifications_column_name_label, null,
                SWT.LEFT, true, true,
                new ColumnLabelProvider() {
                    @Override
                    public String getText(Object element) {
                        return ((NotificationDescriptor) element).getName();
                    }

                    @Override
                    public String getToolTipText(Object element) {
                        return ((NotificationDescriptor) element).getDescription();
                    }
                }
            );
            controller.addColumn(
                CoreMessages.pref_page_notifications_column_sound_label, null,
                SWT.LEFT, true, true,
                element -> {
                    final NotificationSettings settings = getSettings((NotificationDescriptor) element);
                    if (!settings.isPlaySound()) {
                        return SOUND_NONE_LABEL;
                    } else if (settings.getSoundFile() == null) {
                        return SOUND_BEEP_LABEL;
                    } else {
                        return settings.getSoundFile().toString();
                    }
                },
                new EditingSupport(viewer) {
                    @Override
                    protected CellEditor getCellEditor(Object element) {
                        final List<String> choices = new ArrayList<>();
                        choices.add(SOUND_BEEP_LABEL);
                        choices.add(SOUND_NONE_LABEL);
                        choices.add(SOUND_FILE_LABEL);

                        return new CustomComboBoxCellEditor(
                            viewer,
                            viewer.getTable(),
                            choices.toArray(new String[0]),
                            SWT.DROP_DOWN | SWT.READ_ONLY
                        );
                    }

                    @Override
                    protected boolean canEdit(Object element) {
                        return true;
                    }

                    @Override
                    protected Object getValue(Object element) {
                        final NotificationSettings settings = getSettings((NotificationDescriptor) element);
                        if (!settings.isPlaySound()) {
                            return SOUND_NONE_LABEL;
                        } else if (settings.getSoundFile() == null) {
                            return SOUND_BEEP_LABEL;
                        } else {
                            return settings.getSoundFile().toString();
                        }
                    }

                    @Override
                    protected void setValue(Object element, Object value) {
                        final NotificationSettings settings = getSettings((NotificationDescriptor) element);

                        switch ((String) value) {
                            case SOUND_BEEP_LABEL:
                                settings.setPlaySound(true);
                                settings.setSoundFile(null);
                                break;
                            case SOUND_NONE_LABEL:
                                settings.setPlaySound(false);
                                settings.setSoundFile(null);
                                break;
                            case SOUND_FILE_LABEL: {
                                final FileDialog dialog = new FileDialog(getShell(), SWT.OPEN);
                                dialog.setText("Choose notification sound file");
                                dialog.setFilterExtensions(new String[]{"*.wav"});
                                dialog.setFilterNames(new String[]{"Waveform Audio File"});

                                if (dialog.open() != null) {
                                    settings.setPlaySound(true);
                                    settings.setSoundFile(new File(dialog.getFilterPath(), dialog.getFileName()));
                                }

                                break;
                            }
                        }

                        viewer.refresh();
                    }
                }
            );
            controller.addBooleanColumn(
                CoreMessages.pref_page_notifications_column_popup_label, null,
                SWT.CENTER, true, true,
                element -> getSettings((NotificationDescriptor) element).isShowPopup(),
                new EditingSupport(viewer) {
                    @Override
                    protected CellEditor getCellEditor(Object element) {
                        return new CustomCheckboxCellEditor(viewer.getTable(), true);
                    }

                    @Override
                    protected boolean canEdit(Object element) {
                        return true;
                    }

                    @Override
                    protected Object getValue(Object element) {
                        return getSettings((NotificationDescriptor) element).isShowPopup();
                    }

                    @Override
                    protected void setValue(Object element, Object value) {
                        getSettings((NotificationDescriptor) element).setShowPopup((boolean) value);
                    }
                }
            );
            controller.createColumns(false);
            controller.sortByColumn(0, SWT.UP);

            viewer.setContentProvider(new ListContentProvider());
            viewer.setInput(NotificationRegistry.getInstance().getNotifications().stream()
                .filter(descriptor -> !descriptor.isHidden())
                .collect(Collectors.toList()));

            UIUtils.asyncExec(() -> UIUtils.packColumns(viewer.getTable(), true));
            ColumnViewerToolTipSupport.enableFor(viewer);
        }

        return composite;
    }

    @Override
    public boolean performOk() {
        for (Map.Entry<NotificationDescriptor, NotificationSettings> entry : settings.entrySet()) {
            NotificationUtils.setNotificationSettings(entry.getKey().getId(), entry.getValue());
        }

        final DBPPreferenceStore preferences = DBWorkbench.getPlatform().getPreferenceStore();

        preferences.setValue(ModelPreferences.NOTIFICATIONS_ENABLED, enablePopupsCheckbox.getSelection());
        preferences.setValue(ModelPreferences.NOTIFICATIONS_CLOSE_DELAY_TIMEOUT, hideDelaySpinner.getSelection());
        preferences.setValue(ModelPreferences.NOTIFICATIONS_SOUND_ENABLED, enableSoundsCheckbox.getSelection());
        preferences.setValue(ModelPreferences.NOTIFICATIONS_SOUND_VOLUME, soundVolumeSpinner.getSelection());
        preferences.setValue(DBeaverPreferences.AGENT_LONG_OPERATION_NOTIFY, longOperationsCheck.getSelection());
        preferences.setValue(DBeaverPreferences.AGENT_LONG_OPERATION_TIMEOUT, longOperationsTimeout.getSelection());

        return super.performOk();
    }

    @Override
    protected void performDefaults() {
        for (Map.Entry<NotificationDescriptor, NotificationSettings> entry : settings.entrySet()) {
            final NotificationDescriptor descriptor = entry.getKey();
            final NotificationSettings settings = entry.getValue();
            settings.setShowPopup(true);
            settings.setPlaySound(descriptor.isSoundEnabled());
            settings.setSoundFile(null);
        }

        final DBPPreferenceStore preferences = DBWorkbench.getPlatform().getPreferenceStore();

        enablePopupsCheckbox.setSelection(preferences.getDefaultBoolean(ModelPreferences.NOTIFICATIONS_ENABLED));
        hideDelaySpinner.setSelection(preferences.getDefaultInt(ModelPreferences.NOTIFICATIONS_CLOSE_DELAY_TIMEOUT));
        enableSoundsCheckbox.setSelection(preferences.getDefaultBoolean(ModelPreferences.NOTIFICATIONS_SOUND_ENABLED));
        soundVolumeSpinner.setSelection(preferences.getDefaultInt(ModelPreferences.NOTIFICATIONS_SOUND_VOLUME));
        longOperationsCheck.setSelection(preferences.getDefaultBoolean(DBeaverPreferences.AGENT_LONG_OPERATION_NOTIFY));
        longOperationsTimeout.setSelection(preferences.getDefaultInt(DBeaverPreferences.AGENT_LONG_OPERATION_TIMEOUT));

        viewer.refresh();

        super.performDefaults();
    }

    @NotNull
    private NotificationSettings getSettings(@NotNull NotificationDescriptor descriptor) {
        return settings.computeIfAbsent(descriptor, d -> NotificationUtils.getNotificationSettings(d.getId()));
    }
}
