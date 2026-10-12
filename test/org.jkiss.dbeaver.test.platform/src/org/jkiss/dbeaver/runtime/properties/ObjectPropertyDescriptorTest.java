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
package org.jkiss.dbeaver.runtime.properties;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

import java.lang.reflect.Method;
import java.util.ListResourceBundle;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class ObjectPropertyDescriptorTest {
    private MockedStatic<FrameworkUtil> framework;
    private MockedStatic<RuntimeUtils> runtime;

    @BeforeEach
    public void setUp() {
        framework = Mockito.mockStatic(FrameworkUtil.class, Mockito.withSettings().mockMaker("mock-maker-inline"));
        runtime = Mockito.mockStatic(RuntimeUtils.class, Mockito.withSettings().mockMaker("mock-maker-inline"));
    }

    @AfterEach
    public void tearDown() {
        runtime.close();
        framework.close();
    }

    @Test
    public void sharedOwnerSuppliesNamesDescriptionsAndHintsWithoutChangingPropertyMetadata() throws Exception {
        register(SharedProperty.class, "en", Map.of());
        register(SharedOwner.class, "en", Map.of(
            key(SharedOwner.class, "name"), "Shared name",
            key(SharedOwner.class, "description"), "Shared description",
            key(SharedOwner.class, "hint"), "Shared hint"
        ));

        ObjectPropertyDescriptor descriptor = descriptor(SharedProperty.class, "en", true);
        assertEquals("Shared name", descriptor.getDisplayName());
        assertEquals("Shared description", descriptor.getDescription());
        assertEquals("Shared hint", descriptor.getHint());
        assertEquals("value", descriptor.getId());
        assertEquals("storedValue", descriptor.getKeyName());
        assertEquals(3, descriptor.getOrderNumber());
        assertTrue(descriptor.isRequired());
        assertTrue(descriptor.isPassword());
        assertEquals(SharedProperty.class, descriptor.getDeclaringClass());
    }

    @Test
    public void classSpecificMessagesOverrideSharedMessagesIndependently() throws Exception {
        register(SharedProperty.class, "en", Map.of(key(SharedProperty.class, "hint"), "Specific hint"));
        register(SharedOwner.class, "en", Map.of(
            key(SharedOwner.class, "name"), "Shared name",
            key(SharedOwner.class, "hint"), "Shared hint"
        ));

        ObjectPropertyDescriptor descriptor = descriptor(SharedProperty.class, "en", true);
        assertEquals("Shared name", descriptor.getDisplayName());
        assertEquals("Specific hint", descriptor.getHint());
        assertEquals("Shared name", descriptor.getDescription());

        register(SharedProperty.class, "en", Map.of(
            key(SharedProperty.class, "name"), "Specific name",
            key(SharedProperty.class, "description"), "Specific description",
            key(SharedProperty.class, "hint"), "Specific hint"
        ));
        descriptor = descriptor(SharedProperty.class, "en", true);
        assertEquals("Specific name", descriptor.getDisplayName());
        assertEquals("Specific description", descriptor.getDescription());
        assertEquals("Specific hint", descriptor.getHint());
    }

    @Test
    public void sharedMessagesUseTheRequestedLocale() throws Exception {
        register(SharedProperty.class, "ru", Map.of());
        register(SharedOwner.class, "ru", Map.of(key(SharedOwner.class, "name"), "Общее имя"));

        assertEquals("Общее имя", descriptor(SharedProperty.class, "ru", true).getDisplayName());
    }

    @Test
    public void sharedOwnerMayBelongToTheSameBundle() throws Exception {
        Bundle bundle = register(SharedProperty.class, "en", Map.of(key(SharedOwner.class, "name"), "Shared name"));
        framework.when(() -> FrameworkUtil.getBundle(SharedOwner.class)).thenReturn(bundle);

        assertEquals("Shared name", descriptor(SharedProperty.class, "en", true).getDisplayName());
    }

    @Test
    public void missingSharedMessagesKeepExistingDefaults() throws Exception {
        register(SharedProperty.class, "en", Map.of());
        register(SharedOwner.class, "en", Map.of());

        ObjectPropertyDescriptor descriptor = descriptor(SharedProperty.class, "en", true);
        assertEquals("value", descriptor.getDisplayName());
        assertEquals("value", descriptor.getDescription());
        assertNull(descriptor.getHint());
    }

    @Test
    public void unavailableSharedBundleKeepsExistingDefaults() throws Exception {
        register(SharedProperty.class, "en", Map.of());

        assertEquals("value", descriptor(SharedProperty.class, "en", true).getDisplayName());
    }

    @Test
    public void missingSharedMessagesStillFallBackToParentTranslations() throws Exception {
        register(InheritedProperty.class, "en", Map.of());
        register(SharedOwner.class, "en", Map.of());
        register(ParentProperty.class, "en", Map.of(key(ParentProperty.class, "name"), "Parent name"));

        assertEquals("Parent name", descriptor(InheritedProperty.class, "en", true).getDisplayName());
    }

    @Test
    public void propertiesWithoutSharedOwnerKeepTheirOriginalLookup() throws Exception {
        register(LocalProperty.class, "en", Map.of(key(LocalProperty.class, "name"), "Local name"));

        assertEquals(void.class, LocalProperty.class.getMethod("getValue").getAnnotation(Property.class).localizationOwner());
        assertEquals("Local name", descriptor(LocalProperty.class, "en", true).getDisplayName());
        framework.verify(() -> FrameworkUtil.getBundle(SharedOwner.class), Mockito.never());
    }

    @Test
    public void literalNamesDoNotConsultSharedTranslations() throws Exception {
        assertEquals("Literal name", descriptor(LiteralProperty.class, "en", true).getDisplayName());
        runtime.verifyNoInteractions();
        framework.verifyNoInteractions();
    }

    @Test
    public void nonLocalizedDescriptorsDoNotLoadResourceBundles() throws Exception {
        assertEquals("value", descriptor(SharedProperty.class, "en", false).getDisplayName());
        runtime.verifyNoInteractions();
        framework.verifyNoInteractions();
    }

    @NotNull
    private Bundle register(@NotNull Class<?> owner, @NotNull String locale, @NotNull Map<String, String> messages) {
        Bundle bundle = Mockito.mock(Bundle.class);
        framework.when(() -> FrameworkUtil.getBundle(owner)).thenReturn(bundle);
        runtime.when(() -> RuntimeUtils.getBundleLocalization(bundle, locale)).thenReturn(new ListResourceBundle() {
            @NotNull
            @Override
            protected Object[][] getContents() {
                return messages.entrySet().stream().map(entry -> new Object[]{entry.getKey(), entry.getValue()})
                    .toArray(Object[][]::new);
            }
        });
        return bundle;
    }

    @NotNull
    private static String key(@NotNull Class<?> owner, @NotNull String type) {
        return "meta." + owner.getName() + ".value." + type;
    }

    @NotNull
    private static ObjectPropertyDescriptor descriptor(@NotNull Class<?> owner, @NotNull String locale, boolean localized)
        throws NoSuchMethodException {
        Method getter = owner.getMethod("getValue");
        return new ObjectPropertyDescriptor(null, null, getter.getAnnotation(Property.class), getter, locale, localized);
    }

    public static class SharedOwner {
    }

    public static class SharedProperty {
        @Property(order = 3, required = true, password = true, keyName = "storedValue", localizationOwner = SharedOwner.class)
        @Nullable
        public String getValue() {
            return null;
        }
    }

    public static class LocalProperty {
        @Property
        @Nullable
        public String getValue() {
            return null;
        }
    }

    public static class LiteralProperty {
        @Property(name = "Literal name", localizationOwner = SharedOwner.class)
        @Nullable
        public String getValue() {
            return null;
        }
    }

    public static class ParentProperty {
        @Property
        @Nullable
        public String getValue() {
            return null;
        }
    }

    public static class InheritedProperty extends ParentProperty {
        @Override
        @Property(localizationOwner = SharedOwner.class)
        @Nullable
        public String getValue() {
            return null;
        }
    }
}
