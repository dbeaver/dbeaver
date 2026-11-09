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
import org.eclipse.core.runtime.IContributor;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.data.json.JSONUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class ProductConfigRegistryTest {
    @Test
    public void testEmptyRegistryDoesNotRequireConfiguration() throws Exception {
        var registry = registry(Map.of());

        Assertions.assertFalse(registry.hasConfiguredFeatures());
        Assertions.assertFalse(registry.hasNewFeatures());
        Assertions.assertTrue(registry.getAvailableFeatures().isEmpty());
    }

    @Test
    public void testFirstRunOffersAvailableFeatures() throws Exception {
        var registry = registry(Map.of(), feature("available"));

        Assertions.assertFalse(registry.hasConfiguredFeatures());
        Assertions.assertTrue(registry.hasNewFeatures());
        Assertions.assertTrue(registry.isFeatureNew(registry.getFeatures().getFirst()));
    }

    @Test
    public void testFirstRunStillStartsWhenAllFeaturesAreUnavailable() throws Exception {
        var registry = registry(Map.of(), feature("unavailable"));
        setAvailability(registry.getFeatures().getFirst(), () -> false);

        Assertions.assertFalse(registry.hasConfiguredFeatures());
        Assertions.assertTrue(registry.hasNewFeatures());
        Assertions.assertTrue(registry.getAvailableFeatures().isEmpty());
    }

    @Test
    public void testFeatureWithoutAvailabilityTesterIsAvailable() throws Exception {
        var feature = feature("unrestricted");
        Mockito.when(feature.getAttribute("availabilityTester")).thenReturn(null);
        var registry = registry(Map.of(), feature);

        Assertions.assertTrue(registry.hasNewFeatures());
        Assertions.assertEquals(registry.getFeatures(), registry.getAvailableFeatures());
    }

    @Test
    public void testDisabledFeatureIsAlreadyConfigured() throws Exception {
        var registry = registry(Map.of("disabled", false), feature("disabled"));
        var feature = registry.getFeatures().getFirst();

        Assertions.assertTrue(registry.hasConfiguredFeatures());
        Assertions.assertFalse(registry.hasNewFeatures());
        Assertions.assertFalse(registry.isFeatureNew(feature));
        Assertions.assertFalse(registry.isFeatureEnabled(feature));
    }

    @Test
    public void testUnavailableFeatureDoesNotReopenConfiguration() throws Exception {
        var registry = registry(Map.of("known", true), feature("known"), feature("unavailable"));
        setAvailability(registry.getFeatures().getLast(), () -> false);

        Assertions.assertFalse(registry.hasNewFeatures());
        Assertions.assertEquals(List.of("known"), registry.getAvailableFeatures().stream()
            .map(ProductConfigFeatureDescriptor::getId).toList());
    }

    @Test
    public void testFeatureBecomesNewWhenItBecomesAvailable() throws Exception {
        var registry = registry(Map.of("known", true), feature("known"), feature("unavailable"));
        var feature = registry.getFeatures().getLast();
        var available = new AtomicBoolean(false);
        setAvailability(feature, available::get);

        Assertions.assertFalse(registry.hasNewFeatures());
        Assertions.assertTrue(registry.isFeatureNew(feature));

        available.set(true);

        Assertions.assertTrue(registry.hasNewFeatures());
        Assertions.assertTrue(registry.isFeatureNew(feature));
        Assertions.assertEquals(2, registry.getAvailableFeatures().size());
    }

    @Test
    public void testAvailableFeaturesRetainLabelOrder() throws Exception {
        var registry = registry(Map.of("beta", false), feature("gamma"), feature("beta"), feature("alpha"));
        setAvailability(registry.getFeatures().getLast(), () -> false);

        Assertions.assertEquals(List.of("alpha", "beta"), registry.getAvailableFeatures().stream()
            .map(ProductConfigFeatureDescriptor::getId).toList());
        Assertions.assertTrue(registry.hasNewFeatures());
        Assertions.assertTrue(registry.hasConfiguredFeatures());
    }

    @NotNull
    private static ProductConfigRegistry registry(
        @NotNull Map<String, Boolean> configured,
        @NotNull IConfigurationElement... features
    ) throws Exception {
        var extensions = Mockito.mock(IExtensionRegistry.class);
        Mockito.when(extensions.getConfigurationElementsFor("org.jkiss.dbeaver.app.config")).thenReturn(features);
        var constructor = ProductConfigRegistry.class.getDeclaredConstructor(IExtensionRegistry.class);
        constructor.setAccessible(true);
        var registry = constructor.newInstance(extensions);
        for (var feature : registry.getFeatures()) {
            setAvailability(feature, () -> true);
        }
        var stateField = ProductConfigRegistry.class.getDeclaredField("state");
        stateField.setAccessible(true);
        var state = Map.of("features", configured.entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getKey, entry -> Map.of("enabled", entry.getValue()))));
        stateField.set(registry, JSONUtils.GSON.fromJson(JSONUtils.GSON.toJson(state), stateField.getType()));
        return registry;
    }

    @NotNull
    private static IConfigurationElement feature(@NotNull String id) {
        var contributor = Mockito.mock(IContributor.class);
        Mockito.when(contributor.getName()).thenReturn("org.jkiss.dbeaver.test.platform");
        var element = Mockito.mock(IConfigurationElement.class);
        Mockito.when(element.getContributor()).thenReturn(contributor);
        Mockito.when(element.getName()).thenReturn("feature");
        Mockito.when(element.getAttribute("id")).thenReturn(id);
        Mockito.when(element.getAttribute("label")).thenReturn(id);
        Mockito.when(element.getAttribute("availabilityTester")).thenReturn(ProductConfigFeatureAvailabilityTester.class.getName());
        return element;
    }

    private static void setAvailability(
        @NotNull ProductConfigFeatureDescriptor feature,
        @NotNull ProductConfigFeatureAvailabilityTester tester
    ) throws Exception {
        var field = ProductConfigFeatureDescriptor.class.getDeclaredField("availabilityTester");
        field.setAccessible(true);
        field.set(feature, tester);
    }
}
