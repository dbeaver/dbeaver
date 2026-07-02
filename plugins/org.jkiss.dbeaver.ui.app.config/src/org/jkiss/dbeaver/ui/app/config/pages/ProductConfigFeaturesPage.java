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
import org.jkiss.dbeaver.model.config.ProductConfigFeatureDescriptor;
import org.jkiss.dbeaver.model.config.ProductConfigFeatureRegistry;
import org.jkiss.dbeaver.ui.app.config.nls.ProductConfigMessages;
import org.jkiss.dbeaver.ui.forms.*;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class ProductConfigFeaturesPage extends ProductConfigWizardPage {
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
        var registry = ProductConfigFeatureRegistry.getInstance();
        for (ProductConfigFeatureDescriptor descriptor : registry.getFeatures()) {
            features.put(descriptor, UIObservable.of(registry.isFeatureEnabled(descriptor)));
        }
    }

    @Override
    public void applySettings() {
        var registry = ProductConfigFeatureRegistry.getInstance();
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
                    .align(UIAlignX.FILL)
                    .grow(UIGrowX.ALWAYS)))
            .row(rb -> rb.scrolledPanel(false, true, pb1 -> pb1
                .align(UIAlignX.FILL, UIAlignY.FILL)
                .grow(UIGrowX.ALWAYS, UIGrowY.ALWAYS)
                .indent(pb2 -> {
                    for (ProductConfigFeatureDescriptor descriptor : ProductConfigFeatureRegistry.getInstance().getFeatures()) {
                        pb2.row(rb1 -> rb1.checkBox(descriptor.getLabel(), features.get(descriptor)));
                    }
                })
            ));
    }
}
