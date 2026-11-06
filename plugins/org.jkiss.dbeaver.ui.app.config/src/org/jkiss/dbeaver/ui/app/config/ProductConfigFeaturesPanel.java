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
package org.jkiss.dbeaver.ui.app.config;

import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.config.ProductConfigFeatureDescriptor;
import org.jkiss.dbeaver.model.config.ProductConfigRegistry;
import org.jkiss.dbeaver.ui.UITextUtils;
import org.jkiss.dbeaver.ui.app.config.nls.ProductConfigMessages;
import org.jkiss.dbeaver.ui.forms.*;
import org.jkiss.utils.StringUtils;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

public final class ProductConfigFeaturesPanel {
    private static final int TEXT_WIDTH_HINT = 300;

    private final boolean featureUpdate;
    private final List<ProductConfigFeatureDescriptor> availableFeatures = ProductConfigRegistry.getInstance().getAvailableFeatures();
    private final Map<ProductConfigFeatureDescriptor, UIObservable<Boolean>> features = new LinkedHashMap<>();
    private final Set<ProductConfigFeatureDescriptor> newFeatures = new HashSet<>();
    private final Map<ProductConfigFeatureDescriptor, Boolean> savedFeatures = new LinkedHashMap<>();

    public ProductConfigFeaturesPanel(boolean featureUpdate) {
        this.featureUpdate = featureUpdate;
        var registry = ProductConfigRegistry.getInstance();
        var configurableFeatures = featureUpdate ? availableFeatures : registry.getFeatures();
        for (ProductConfigFeatureDescriptor descriptor : configurableFeatures) {
            boolean enabled = registry.isFeatureEnabled(descriptor);
            features.put(descriptor, UIObservable.of(enabled));
            savedFeatures.put(descriptor, enabled);
            if (featureUpdate && registry.isFeatureNew(descriptor)) {
                newFeatures.add(descriptor);
            }
        }
    }

    public boolean applySettings() {
        boolean restartRequired = false;
        var registry = ProductConfigRegistry.getInstance();
        for (Map.Entry<ProductConfigFeatureDescriptor, UIObservable<Boolean>> entry : features.entrySet()) {
            boolean enabled = entry.getValue().get();
            if (registry.isFeatureEnabled(entry.getKey()) != enabled) {
                restartRequired = true;
            }
            registry.setFeatureEnabled(entry.getKey(), enabled);
            savedFeatures.put(entry.getKey(), enabled);
        }
        return restartRequired;
    }

    public void reloadSettings() {
        var registry = ProductConfigRegistry.getInstance();
        for (Map.Entry<ProductConfigFeatureDescriptor, UIObservable<Boolean>> entry : features.entrySet()) {
            boolean enabled = registry.isFeatureEnabled(entry.getKey());
            boolean saved = savedFeatures.get(entry.getKey());
            // retain pending preference edits when the wizard leaves the saved value unchanged
            if (enabled != saved || entry.getValue().get() == saved) {
                entry.getValue().set(enabled);
            }
            savedFeatures.put(entry.getKey(), enabled);
        }
    }

    public void resetToDefaults() {
        for (ProductConfigFeatureDescriptor feature : availableFeatures) {
            features.get(feature).set(feature.isEnabledByDefault());
        }
    }

    @NotNull
    public Control createControl(@NotNull Composite parent, @NotNull String hint) {
        return UIPanelBuilder.build(parent, buildPanel(hint, true));
    }

    @NotNull
    public Control createPreferenceControl(@NotNull Composite parent, @NotNull String hint, @NotNull Runnable openConfiguration) {
        return UIPanelBuilder.build(parent, pb -> pb
            .accept(buildPanel(hint, false))
            .row(rb -> rb.button(ProductConfigMessages.preferences_open_product_configuration, event -> openConfiguration.run())));
    }

    @NotNull
    private Consumer<UIPanelBuilder> buildPanel(@NotNull String hint, boolean scrollFeatures) {
        return pb -> {
            pb.margins(scrollFeatures ? 10 : 0, scrollFeatures ? 10 : 0)
                .row(rb -> rb
                    .label(lb -> lb
                        .text(featureUpdate
                            ? ProductConfigMessages.features_update_header
                            : ProductConfigMessages.features_list_header)
                        .wrap()
                        .hint(TEXT_WIDTH_HINT, SWT.DEFAULT)
                        .align(UIAlignX.FILL)
                        .grow(UIGrowX.ALWAYS)));
            if (scrollFeatures) {
                pb.row(rb -> rb.scrolledPanel(false, true, pb1 -> pb1
                    .align(UIAlignX.FILL, UIAlignY.FILL)
                    .grow(UIGrowX.ALWAYS, UIGrowY.ALWAYS)
                    .indent(buildFeatureControls())));
            } else {
                pb.indent(buildFeatureControls());
            }
            pb.row(rb -> rb
                .label(lb -> lb
                    .image(DBIcon.SMALL_INFO)
                    .align(UIAlignY.TOP))
                .label(lb -> lb
                    .text(hint)
                    .wrap()
                    .hint(TEXT_WIDTH_HINT, SWT.DEFAULT)
                    .align(UIAlignX.FILL)
                    .grow(UIGrowX.ALWAYS)));
        };
    }

    @NotNull
    private Consumer<UIPanelBuilder> buildFeatureControls() {
        return pb -> {
            for (ProductConfigFeatureDescriptor descriptor : availableFeatures) {
                boolean isNew = newFeatures.contains(descriptor);
                String label = isNew
                    ? NLS.bind(ProductConfigMessages.features_new_label, descriptor.getLabel())
                    : descriptor.getLabel();
                pb.row(rb -> rb.checkBox(label, bb -> {
                    bb.tooltip(StringUtils.wrap(descriptor.getDescription(), UITextUtils.TOOLTIP_WRAP_LENGTH))
                        .selected(features.get(descriptor));
                    if (isNew) {
                        bb.font(JFaceResources.getFontRegistry().getBold(JFaceResources.DEFAULT_FONT));
                    }
                }));
            }
        };
    }
}
