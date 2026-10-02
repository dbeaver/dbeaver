/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jkiss.dbeaver.ext.cdata.ui;

import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.cdata.model.CDataConnectionHierarchy;
import org.jkiss.dbeaver.ext.cdata.model.CDataConnectionHierarchy.Property;
import org.jkiss.dbeaver.ext.cdata.ui.internal.CDataUIMessages;
import org.jkiss.dbeaver.ui.UIUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Consumer;

final class CDataConnectionEditor {
    private final Composite general;
    private final Composite authentication;
    private final Consumer<Property> changeListener;
    private Control passwordOptionsControl;
    private final List<Label> generalLabels = new ArrayList<>();
    private CDataConnectionHierarchy hierarchy;
    private boolean rebuildPending;
    private boolean passwordEnabled = true;
    private boolean credentialsPromptMode;

    CDataConnectionEditor(@NotNull Composite general, @NotNull Composite authentication, @NotNull Consumer<Property> changeListener) {
        this.general = general;
        this.authentication = authentication;
        this.changeListener = changeListener;
    }

    void setPasswordOptionsControl(@NotNull Control control) {
        passwordOptionsControl = control;
    }

    void setCredentialsPromptMode(boolean credentialsPromptMode) {
        this.credentialsPromptMode = credentialsPromptMode;
    }

    void setHierarchy(@Nullable CDataConnectionHierarchy hierarchy) {
        this.hierarchy = hierarchy;
        rebuild();
    }

    void setPasswordEnabled(boolean enabled) {
        passwordEnabled = enabled;
        updatePasswordEnabled(general);
        updatePasswordEnabled(authentication);
    }

    private void updatePasswordEnabled(Composite parent) {
        for (Control control : parent.getChildren()) {
            if (Boolean.TRUE.equals(control.getData("secret"))) {
                control.setEnabled(passwordEnabled);
            }
        }
    }

    private void rebuild() {
        UIUtils.disposeChildControls(general);
        UIUtils.disposeChildControls(authentication);
        generalLabels.clear();
        if (hierarchy != null) {
            if (!credentialsPromptMode) {
                createGeneralFields();
            }
            List<Property> properties = credentialsPromptMode ?
                hierarchy.getCredentialProperties() : hierarchy.getAuthenticationProperties();
            for (Property property : properties) {
                createField(authentication, property);
            }
        }
        alignGeneralLabels();
        layoutPasswordOptions();
        general.getParent().layout(true, true);
        authentication.getParent().layout(true, true);
        updateScrollSize(general);
    }

    private void createGeneralFields() {
        List<Property> properties = hierarchy.getGeneralProperties();
        Property server = properties.stream()
            .filter(property -> property.visible() && (property.name().equalsIgnoreCase("Server")
                || property.name().equalsIgnoreCase("Host")))
            .findFirst().orElse(null);
        Property port = properties.stream()
            .filter(property -> property.visible() && property.name().equalsIgnoreCase("Port"))
            .findFirst().orElse(null);
        for (Property property : properties) {
            if (property == port && server != null) {
                continue;
            }
            boolean paired = property == server && port != null;
            Control input = createField(general, property);
            if (input != null) {
                ((GridData) input.getLayoutData()).horizontalSpan = paired ? 1 : 3;
            }
            if (paired) {
                createField(general, port);
            }
        }
    }

    private void alignGeneralLabels() {
        List<Label> labels = new ArrayList<>(generalLabels);
        for (Control control : general.getParent().getChildren()) {
            if (control instanceof Label label && control.getLayoutData() instanceof GridData data && data.horizontalSpan == 1) {
                labels.add(label);
            }
        }
        int width = labels.stream().mapToInt(label -> label.computeSize(SWT.DEFAULT, SWT.DEFAULT).x).max().orElse(0);
        labels.forEach(label -> ((GridData) label.getLayoutData()).widthHint = width);
    }

    private void layoutPasswordOptions() {
        if (passwordOptionsControl == null || passwordOptionsControl.isDisposed()) {
            return;
        }
        int labelWidth = 0;
        boolean hasPassword = false;
        for (Control control : authentication.getChildren()) {
            if (control instanceof Label label) {
                labelWidth = Math.max(labelWidth, label.computeSize(SWT.DEFAULT, SWT.DEFAULT).x);
            }
            hasPassword |= Boolean.TRUE.equals(control.getData("secret"));
        }
        for (Control control : general.getChildren()) {
            hasPassword |= Boolean.TRUE.equals(control.getData("secret"));
        }
        GridData data = new GridData(SWT.LEFT, SWT.CENTER, false, false);
        data.horizontalIndent = labelWidth + ((GridLayout) authentication.getLayout()).horizontalSpacing;
        if (credentialsPromptMode) {
            data.verticalIndent = UIUtils.getFontHeight(passwordOptionsControl) / 2;
        }
        passwordOptionsControl.setLayoutData(data);
        UIUtils.setControlVisible(passwordOptionsControl, hasPassword);
    }

    private void updateScrollSize(Composite content) {
        ScrolledComposite scroll = UIUtils.getParentOfType(content, ScrolledComposite.class);
        if (scroll != null && scroll.getContent() != null) {
            scroll.setMinSize(scroll.getContent().computeSize(SWT.DEFAULT, SWT.DEFAULT));
        }
    }

    @Nullable
    private Control createField(Composite parent, Property property) {
        if (!property.visible()) {
            return null;
        }
        String title = property.name().equalsIgnoreCase("AuthScheme") ? CDataUIMessages.connection_editor_auth_model : property.label();
        Label label = UIUtils.createControlLabel(parent, title);
        label.setToolTipText(property.description());
        if (parent == general && !property.name().equalsIgnoreCase("Port")) {
            generalLabels.add(label);
        }
        String value = hierarchy.getValue(property);
        Control input;
        if (!property.choices().isEmpty() && !property.password()) {
            Combo combo = new Combo(parent, SWT.DROP_DOWN | SWT.READ_ONLY);
            combo.setItems(property.choices().toArray(String[]::new));
            if (combo.indexOf(value) < 0) {
                combo.add(value);
            }
            combo.setText(value);
            combo.addListener(SWT.Selection, event -> changed(property, combo.getText()));
            input = combo;
        } else if ("Boolean".equalsIgnoreCase(property.type())) {
            Button checkbox = new Button(parent, SWT.CHECK);
            checkbox.setSelection(Boolean.parseBoolean(value));
            checkbox.addListener(SWT.Selection, event -> changed(property, Boolean.toString(checkbox.getSelection())));
            input = checkbox;
        } else {
            Text text = new Text(parent, SWT.BORDER | (property.password() ? SWT.PASSWORD : SWT.NONE));
            text.setText(value);
            text.setMessage(property.placeholder());
            text.addModifyListener(event -> changed(property, text.getText()));
            input = text;
        }
        input.setData(property.name());
        input.setData("secret", property.password());
        input.setToolTipText(property.description());
        if (property.password()) {
            input.setEnabled(passwordEnabled);
        }
        boolean compact = parent == authentication || property.name().equalsIgnoreCase("Port");
        GridData data = new GridData(compact ? SWT.LEFT : SWT.FILL, SWT.CENTER, !compact, false);
        if (property.name().equalsIgnoreCase("Port")) {
            data.widthHint = UIUtils.getFontHeight(input) * 7;
        } else if (parent == authentication) {
            data.widthHint = UIUtils.getFontHeight(input) * 20;
        } else {
            data.widthHint = 150;
        }
        input.setLayoutData(data);
        return input;
    }

    private void changed(Property property, String value) {
        hierarchy.setValue(property, value, false);
        changeListener.accept(property);
        if (!property.rules().isEmpty() && !rebuildPending) {
            rebuildPending = true;
            general.getDisplay().asyncExec(() -> {
                rebuildPending = false;
                if (!general.isDisposed()) {
                    rebuild();
                    changeListener.accept(null);
                }
            });
        }
    }

    @Nullable
    String getValidationError() {
        if (hierarchy == null) {
            return null;
        }
        Map<String, String> properties = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        hierarchy.getBasicProperties().forEach(property -> properties.put(property.name(), hierarchy.getValue(property)));
        for (var group : hierarchy.getAdvancedGroups()) {
            for (Property property : group.properties()) {
                String value = properties.get(property.name());
                if (value != null && !value.isEmpty()) {
                    try {
                        switch (property.type()) {
                            case "Integer" -> Integer.parseInt(value);
                            case "Long" -> Long.parseLong(value);
                            case "Number" -> new BigDecimal(value);
                            default -> {
                            }
                        }
                    } catch (NumberFormatException e) {
                        return NLS.bind(CDataUIMessages.connection_editor_invalid_number, property.label());
                    }
                }
            }
        }
        return null;
    }
}
