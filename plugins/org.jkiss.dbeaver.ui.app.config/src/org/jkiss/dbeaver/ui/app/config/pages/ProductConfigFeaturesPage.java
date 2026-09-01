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
package org.jkiss.dbeaver.ui.app.config.pages;

import org.eclipse.swt.widgets.Composite;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.config.ProductConfigFeatureDescriptor;
import org.jkiss.dbeaver.model.config.ProductConfigRegistry;
import org.jkiss.dbeaver.model.impl.GlobalPropertyTester;
import org.jkiss.dbeaver.ui.UITextUtils;
import org.jkiss.dbeaver.ui.app.config.nls.ProductConfigMessages;
import org.jkiss.dbeaver.ui.forms.*;
import org.jkiss.utils.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class ProductConfigFeaturesPage extends ProductConfigWizardPage {
    private static final String FEATURE_AI = "ai";
    private static final String AI_DISABLED_PROPERTY = "ai.disabled";
    private static final String AI_DISABLED_ENV_VARIABLE = "DBEAVER_AI_DISABLED";
    private static final GlobalPropertyTester GLOBAL_PROPERTY_TESTER = new GlobalPropertyTester();

    private final Map<ProductConfigFeatureDescriptor, UIObservable<Boolean>> features = new HashMap<>();

    public ProductConfigFeaturesPage() {
        super(ProductConfigMessages.features_title, ProductConfigMessages.features_description);
    }

    @Override
    public void createControl(@NotNull Composite parent) {
        setControl(UIPanelBuilder.build(parent, buildPanel()));
    }

    @Override
    public void loadSettings() {
        var registry = ProductConfigRegistry.getInstance();
        for (ProductConfigFeatureDescriptor descriptor : registry.getFeatures()) {
            features.put(descriptor, UIObservable.of(registry.isFeatureEnabled(descriptor)));
        }
    }

    @Override
    public void applySettings() {
        var registry = ProductConfigRegistry.getInstance();
        for (Map.Entry<ProductConfigFeatureDescriptor, UIObservable<Boolean>> entry : features.entrySet()) {
            boolean enabled = entry.getValue().get();
            if (registry.isFeatureEnabled(entry.getKey()) != enabled) {
                getWizard().markForRestart();
            }
            registry.setFeatureEnabled(entry.getKey(), enabled);
        }
    }

    @NotNull
    private Consumer<UIPanelBuilder> buildPanel() {
        return pb -> pb
            .margins(10, 10)
            .accept(buildFeaturesPanel());
    }

    @NotNull
    private Consumer<UIPanelBuilder> buildFeaturesPanel() {
        return pb -> pb
            .row(rb -> rb
                .label(lb -> lb
                    .text(ProductConfigMessages.features_list_header)
                    .wrap()
                    .hint(TEXT_WIDTH_HINT, TEXT_HEIGHT_HINT)
                    .align(UIAlignX.FILL)
                    .grow(UIGrowX.ALWAYS)))
            .row(rb -> rb.scrolledPanel(false, true, pb1 -> pb1
                .align(UIAlignX.FILL, UIAlignY.FILL)
                .grow(UIGrowX.ALWAYS, UIGrowY.ALWAYS)
                .indent(pb2 -> {
                    for (ProductConfigFeatureDescriptor descriptor : ProductConfigRegistry.getInstance().getFeatures()) {
                        if (!isFeatureVisible(descriptor)) {
                            continue;
                        }
                        pb2.row(rb1 -> rb1
                            .checkBox(descriptor.getLabel(), bb -> bb
                                .tooltip(StringUtils.wrap(descriptor.getDescription(), UITextUtils.TOOLTIP_WRAP_LENGTH))
                                .selected(features.get(descriptor))));
                    }
                })))
            .row(rb -> rb
                // TODO introduce a dedicated icon+label control
                .label(lb -> lb
                    .image(DBIcon.SMALL_INFO)
                    .align(UIAlignY.TOP))
                .label(lb -> lb
                    .text(ProductConfigMessages.features_hint)
                    .wrap()
                    .hint(TEXT_WIDTH_HINT, TEXT_HEIGHT_HINT)
                    .align(UIAlignX.FILL)
                    .grow(UIGrowX.ALWAYS)));
    }

    private boolean isFeatureVisible(@NotNull ProductConfigFeatureDescriptor descriptor) {
        return !FEATURE_AI.equals(descriptor.getId()) ||
            !GLOBAL_PROPERTY_TESTER.test(
                descriptor,
                GlobalPropertyTester.PROP_HAS_PREFERENCE,
                new Object[0],
                AI_DISABLED_PROPERTY
            ) && !GLOBAL_PROPERTY_TESTER.test(
                descriptor,
                GlobalPropertyTester.PROP_HAS_ENV_VARIABLE,
                new Object[0],
                AI_DISABLED_ENV_VARIABLE
            );
    }
}
