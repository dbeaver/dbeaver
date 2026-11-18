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
package org.jkiss.dbeaver.ui.controls;

import jakarta.annotation.PostConstruct;
import org.eclipse.core.expressions.Expression;
import org.eclipse.core.expressions.ExpressionInfo;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.services.IEvaluationReference;
import org.eclipse.ui.services.IEvaluationService;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.impl.AbstractDescriptor;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceListener;
import org.jkiss.dbeaver.registry.RegistryConstants;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.IStatusBarContributor;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;

/** Hosts ordered status-bar items, activating their classes only when their expressions are true. */
public class DBeaverStatusBarTrim {
    private static final Log log = Log.getLog(DBeaverStatusBarTrim.class);
    static final String EXTENSION_ID = "org.jkiss.dbeaver.statusBar";

    @PostConstruct
    public void createControls(@NotNull Composite parent) {
        Composite bar = UIUtils.createPlaceholder(parent, 1, 0);
        List<ItemDescriptor> descriptors = new ArrayList<>();
        for (var config : Platform.getExtensionRegistry().getConfigurationElementsFor(EXTENSION_ID)) {
            if (!"item".equals(config.getName())) {
                continue;
            }
            ItemDescriptor descriptor = new ItemDescriptor(config);
            if (descriptor.id.isEmpty()) {
                log.warn("Status bar item ID is missing");
                continue;
            }
            if (config.getChildren("enabledWhen").length > 0 && descriptor.enablement == null) {
                log.warn("Invalid enablement for status bar item '" + descriptor.id + "'");
                continue;
            }
            descriptors.add(descriptor);
        }
        descriptors.sort(Comparator.comparingInt(ItemDescriptor::getOrder).thenComparing(ItemDescriptor::getId));
        GridLayout layout = (GridLayout) bar.getLayout();
        layout.numColumns = Math.max(1, descriptors.size());
        layout.marginLeft = 5;
        layout.horizontalSpacing = 8;

        IEvaluationService evaluation = PlatformUI.getWorkbench().getService(IEvaluationService.class);
        List<IEvaluationReference> references = new ArrayList<>();
        var properties = new LinkedHashSet<String>();
        for (ItemDescriptor descriptor : descriptors) {
            Composite slot = UIUtils.createPlaceholder(bar, 1, 0);
            slot.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, true));
            UIUtils.setControlVisible(slot, false);
            Item item = new Item(descriptor, slot);
            if (descriptor.enablement == null) {
                item.update(true);
            } else {
                ExpressionInfo info = new ExpressionInfo();
                descriptor.enablement.collectExpressionInfo(info);
                for (String property : info.getAccessedPropertyNames()) {
                    properties.add(property);
                }
                references.add(evaluation.addEvaluationListener(descriptor.enablement,
                    event -> UIUtils.asyncExec(() -> item.update(Boolean.TRUE.equals(event.getNewValue()))),
                    IEvaluationService.RESULT));
            }
        }
        var store = DBWorkbench.getPlatform().getPreferenceStore();
        DBPPreferenceListener listener = event -> UIUtils.asyncExec(() -> {
            if (!bar.isDisposed()) {
                properties.forEach(evaluation::requestEvaluation);
            }
        });
        store.addPropertyChangeListener(listener);
        bar.addDisposeListener(event -> {
            store.removePropertyChangeListener(listener);
            references.forEach(evaluation::removeEvaluationListener);
        });
    }

    private static final class ItemDescriptor extends AbstractDescriptor {
        private final String id;
        private final int order;
        private final ObjectType type;
        @Nullable
        private final Expression enablement;

        private ItemDescriptor(@NotNull IConfigurationElement config) {
            super(config);
            id = CommonUtils.notEmpty(config.getAttribute(RegistryConstants.ATTR_ID));
            order = CommonUtils.toInt(config.getAttribute(RegistryConstants.ATTR_ORDER), Integer.MAX_VALUE);
            type = new ObjectType(config.getAttribute(RegistryConstants.ATTR_CLASS));
            enablement = getEnablementExpression(config);
        }

        @NotNull
        private String getId() {
            return id;
        }

        private int getOrder() {
            return order;
        }

        @NotNull
        private IStatusBarContributor createContributor() throws DBException {
            return type.createInstance(IStatusBarContributor.class);
        }
    }

    private static final class Item {
        private final ItemDescriptor descriptor;
        private final Composite slot;
        @Nullable
        private Composite content;

        private Item(@NotNull ItemDescriptor descriptor, @NotNull Composite slot) {
            this.descriptor = descriptor;
            this.slot = slot;
        }

        private void update(boolean enabled) {
            if (slot.isDisposed()) {
                return;
            }
            if (enabled && content == null) {
                content = UIUtils.createPlaceholder(slot, 1, 0);
                content.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, true));
                try {
                    descriptor.createContributor().createControls(content);
                } catch (DBException | RuntimeException e) {
                    content.dispose();
                    content = null;
                    log.error("Cannot create status bar item '" + descriptor.id + "'", e);
                }
            } else if (!enabled && content != null) {
                content.dispose();
                content = null;
            }
            UIUtils.setControlVisible(slot, content != null);
            slot.getParent().layout(true, true);
            slot.getParent().requestLayout();
        }
    }
}
