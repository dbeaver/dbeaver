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

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ext.cdata.model.CDataConnectionHierarchy;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.preferences.DBPPropertyDescriptor;
import org.jkiss.dbeaver.runtime.properties.PropertySourceCustom;
import org.jkiss.dbeaver.ui.dialogs.connection.ConnectionPageAbstract;
import org.jkiss.dbeaver.ui.dialogs.connection.ConnectionPropertiesControl;
import org.jkiss.dbeaver.ui.dialogs.connection.DriverPropertiesDialogPage;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Supplier;

final class CDataDriverPropertiesDialogPage extends DriverPropertiesDialogPage {
    private final Supplier<CDataConnectionHierarchy> hierarchySupplier;
    private final Map<String, Object> originalValues = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    private final Set<String> editableProperties = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);

    CDataDriverPropertiesDialogPage(ConnectionPageAbstract hostPage, Supplier<CDataConnectionHierarchy> hierarchySupplier) {
        super(hostPage);
        this.hierarchySupplier = hierarchySupplier;
    }

    @Override
    protected void loadPropertyValues(@NotNull PropertySourceCustom source) {
        originalValues.clear();
        originalValues.putAll(source.getPropertyValues());
        editableProperties.clear();
        for (var property : source.getProperties()) {
            editableProperties.add(property.getId());
        }
        var hierarchy = hierarchySupplier.get();
        if (hierarchy != null) {
            Map<String, Object> defaults = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
            var credentialProperties = hierarchy.getCredentialProperties();
            for (var property : hierarchy.getAdvancedSecretProperties()) {
                if (!editableProperties.contains(property.name())) {
                    continue;
                }
                defaults.put(property.name(), property.defaultValue());
                String value = hierarchy.getValue(property);
                if (!value.isEmpty() || credentialProperties.contains(property)) {
                    originalValues.remove(property.name());
                    originalValues.put(property.name(), value);
                }
            }
            source.addDefaultValues(defaults);
            source.setValues(originalValues);
        }
    }

    @Override
    protected void savePropertyValues(@NotNull PropertySourceCustom source, @NotNull DBPConnectionConfiguration configuration) {
        super.savePropertyValues(source, configuration);
        var hierarchy = hierarchySupplier.get();
        if (hierarchy != null) {
            Map<String, Object> editedValues = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
            editedValues.putAll(source.getPropertyValues());
            for (var property : hierarchy.getAdvancedSecretProperties()) {
                String name = property.name();
                if (editableProperties.contains(name) && !Objects.equals(originalValues.get(name), editedValues.get(name))) {
                    String value = Objects.toString(editedValues.get(name), property.defaultValue());
                    // an absent property after reset must override the secret retained by the main page
                    hierarchy.setValue(property, value, true);
                    configuration.getProperties().keySet().removeIf(name::equalsIgnoreCase);
                    configuration.getProperties().put(name, value);
                }
            }
        }
    }

    @NotNull
    @Override
    protected ConnectionPropertiesControl createPropertiesControl(@NotNull Composite parent) {
        return new ConnectionPropertiesControl(parent, SWT.NONE) {
            @Override
            protected boolean isHidePropertyValue(@NotNull DBPPropertyDescriptor property) {
                var hierarchy = hierarchySupplier.get();
                return hierarchy != null && hierarchy.isSecretProperty(property.getId()) || super.isHidePropertyValue(property);
            }
        };
    }
}
