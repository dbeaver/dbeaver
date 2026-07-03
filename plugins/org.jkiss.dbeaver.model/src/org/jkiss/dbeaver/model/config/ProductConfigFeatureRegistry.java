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
package org.jkiss.dbeaver.model.config;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.data.json.JSONUtils;
import org.jkiss.dbeaver.runtime.DBWorkbench;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public final class ProductConfigFeatureRegistry {
    private static final String EXTENSION_ID = "org.jkiss.dbeaver.app.config";
    private static final String CONFIG_FILE = "product-config.json";

    private static final Log log = Log.getLog(ProductConfigFeatureRegistry.class);

    private static ProductConfigFeatureRegistry instance;

    private final List<ProductConfigFeatureDescriptor> features;

    private final Object stateLock = new Object();
    private volatile State state;

    private ProductConfigFeatureRegistry(@NotNull IExtensionRegistry registry) {
        var features = new ArrayList<ProductConfigFeatureDescriptor>();

        for (IConfigurationElement element : registry.getConfigurationElementsFor(EXTENSION_ID)) {
            if ("feature".equals(element.getName())) {
                features.add(new ProductConfigFeatureDescriptor(element));
            } else {
                throw new IllegalStateException("Unknown element " + element.getName());
            }
        }

        this.features = features.stream()
            .sorted(Comparator.comparing(ProductConfigFeatureDescriptor::getLabel))
            .toList();
    }

    @NotNull
    public static synchronized ProductConfigFeatureRegistry getInstance() {
        if (instance == null) {
            instance = new ProductConfigFeatureRegistry(Platform.getExtensionRegistry());
        }
        return instance;
    }

    @NotNull
    public List<ProductConfigFeatureDescriptor> getFeatures() {
        return features;
    }

    public boolean isFeatureEnabled(@NotNull ProductConfigFeatureDescriptor descriptor) {
        var state = currentState();
        var feature = state.features().get(descriptor.getId());
        return feature != null && feature.enabled() || descriptor.isEnabledByDefault();
    }

    public void setFeatureEnabled(@NotNull ProductConfigFeatureDescriptor descriptor, boolean enabled) {
        synchronized (stateLock) {
            state = currentState()
                .withFeature(descriptor.getId(), new State.Feature(enabled));
            saveState(state);
        }
    }

    @NotNull
    private State currentState() {
        if (state == null) {
            synchronized (stateLock) {
                if (state == null) {
                    state = loadState();
                }
            }
        }
        return state;
    }

    @NotNull
    private static State loadState() {
        Path path = DBWorkbench.getPlatform().getLocalConfigurationFile(ProductConfigFeatureRegistry.CONFIG_FILE);
        if (Files.exists(path)) {
            try (var reader = Files.newBufferedReader(path)) {
                return JSONUtils.GSON.fromJson(reader, State.class);
            } catch (Exception e) {
                log.error("Error loading product configuration state from " + path, e);
            }
        }
        return new State(Map.of());
    }

    private static void saveState(@NotNull State state) {
        Path path = DBWorkbench.getPlatform().getLocalConfigurationFile(ProductConfigFeatureRegistry.CONFIG_FILE);
        try (var writer = Files.newBufferedWriter(path)) {
            JSONUtils.GSON.toJson(state, writer);
        } catch (Exception e) {
            log.error("Error saving product configuration state to " + path, e);
        }
    }

    private record State(@NotNull Map<String, Feature> features) {
        State {
            features = Map.copyOf(features);
        }

        @NotNull
        State withFeature(@NotNull String id, @NotNull Feature feature) {
            var newFeatures = new HashMap<>(features);
            newFeatures.put(id, feature);
            return new State(newFeatures);
        }

        private record Feature(boolean enabled) {
        }
    }
}
