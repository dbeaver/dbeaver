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
package org.jkiss.dbeaver.registry;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IContributor;
import org.jkiss.code.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.Map;

public class InheritedConfigurationTest {
    @Test
    public void testMissingAttributesUseNearestAncestor() {
        var root = new InheritedConfiguration<>(element("root", "root.bundle", Map.of(
            "rootOnly", "root", "shared", "root"
        )), "root.bundle", null);
        var parent = new InheritedConfiguration<>(
            element("parent", "parent.bundle", Map.of("shared", "parent")), "parent.bundle", root);
        var child = new InheritedConfiguration<>(
            element("child", "child.bundle", Map.of("local", "child")), "child.bundle", parent);

        Assertions.assertEquals("root", child.getAttribute("rootOnly"));
        Assertions.assertEquals("root.bundle", child.getAttributeSource("rootOnly"));
        Assertions.assertEquals("parent", child.getAttribute("shared"));
        Assertions.assertEquals("parent.bundle", child.getAttributeSource("shared"));
        Assertions.assertEquals("child", child.getAttribute("local"));
        Assertions.assertEquals("child.bundle", child.getAttributeSource("local"));
        Assertions.assertNull(child.getAttribute("missing"));
        Assertions.assertEquals("child.bundle", child.getAttributeSource("missing"));
    }

    @Test
    public void testExplicitEmptyFalseAndZeroOverrideAncestors() {
        var parent = new InheritedConfiguration<>(element("parent", "parent.bundle", Map.of(
            "text", "parent", "enabled", "true", "count", "10"
        )), "parent.bundle", null);
        var child = new InheritedConfiguration<>(element("child", "child.bundle", Map.of(
            "text", "", "enabled", "false", "count", "0"
        )), "child.bundle", parent);

        Assertions.assertEquals("", child.getAttribute("text"));
        Assertions.assertEquals("child.bundle", child.getAttributeSource("text"));
        Assertions.assertEquals("false", child.getAttribute("enabled"));
        Assertions.assertEquals("child.bundle", child.getAttributeSource("enabled"));
        Assertions.assertEquals("0", child.getAttribute("count"));
        Assertions.assertEquals("child.bundle", child.getAttributeSource("count"));
    }

    @Test
    public void testNoParentAndMissingAttributes() {
        var config = new InheritedConfiguration<>(
            element("driver", "child.bundle", Map.of("label", "Local")), "child.bundle", null);

        Assertions.assertEquals("Local", config.getAttribute("label"));
        Assertions.assertEquals("child.bundle", config.getAttributeSource("label"));
        Assertions.assertNull(config.getAttribute("missing"));
        Assertions.assertEquals("child.bundle", config.getAttributeSource("missing"));
        Assertions.assertNull(config.getAttribute("icon"));
        Assertions.assertEquals("child.bundle", config.getAttributeSource("icon"));
        Assertions.assertNull(config.getAttribute("class"));
        Assertions.assertEquals("child.bundle", config.getAttributeSource("class"));
    }

    @Test
    public void testRelativeIconAttributesKeepDeclaringSource() {
        var parent = new InheritedConfiguration<>(element("parent", "parent.bundle", Map.of(
            "icon", "icons/parent.svg"
        )), "parent.bundle", null);
        var intermediate = new InheritedConfiguration<>(
            element("middle", "middle.bundle", Map.of()), "middle.bundle", parent);
        var child = new InheritedConfiguration<>(element("child", "child.bundle", Map.of(
            "iconBig", "icons/child.svg"
        )), "child.bundle", intermediate);

        Assertions.assertEquals("icons/parent.svg", child.getAttribute("icon"));
        Assertions.assertEquals("parent.bundle", child.getAttributeSource("icon"));
        Assertions.assertEquals("icons/child.svg", child.getAttribute("iconBig"));
        Assertions.assertEquals("child.bundle", child.getAttributeSource("iconBig"));
    }

    @Test
    public void testPredefinedAndPlatformIconAttributesAndExplicitEmpty() {
        var parent = new InheritedConfiguration<>(element("parent", "parent.bundle", Map.of(
            "icon", "#database_icon_default",
            "iconBig", "platform:/plugin/another.bundle/icons/large.svg",
            "logoImage", "icons/logo.svg"
        )), "parent.bundle", null);
        var child = new InheritedConfiguration<>(
            element("child", "child.bundle", Map.of("logoImage", "")), "child.bundle", parent);

        Assertions.assertEquals("#database_icon_default", child.getAttribute("icon"));
        Assertions.assertEquals("parent.bundle", child.getAttributeSource("icon"));
        Assertions.assertEquals("platform:/plugin/another.bundle/icons/large.svg", child.getAttribute("iconBig"));
        Assertions.assertEquals("parent.bundle", child.getAttributeSource("iconBig"));
        Assertions.assertEquals("", child.getAttribute("logoImage"));
        Assertions.assertEquals("child.bundle", child.getAttributeSource("logoImage"));
    }

    @Test
    public void testInheritedClassAttributeKeepsDeclaringSource() {
        var parent = new InheritedConfiguration<>(element("parent", "org.jkiss.dbeaver.test.platform", Map.of(
            "class", InheritedConfigurationTest.class.getName()
        )), "org.jkiss.dbeaver.test.platform", null);
        var child = new InheritedConfiguration<>(
            element("child", "nonexistent.child.bundle", Map.of()), "nonexistent.child.bundle", parent);

        Assertions.assertEquals(InheritedConfigurationTest.class.getName(), child.getAttribute("class"));
        Assertions.assertEquals("org.jkiss.dbeaver.test.platform", child.getAttributeSource("class"));
    }

    @NotNull
    static IConfigurationElement element(
        @NotNull String name,
        @NotNull String contributor,
        @NotNull Map<String, String> attributes,
        @NotNull IConfigurationElement... children
    ) {
        IConfigurationElement element = Mockito.mock(IConfigurationElement.class);
        IContributor source = () -> contributor;
        Mockito.when(element.getName()).thenReturn(name);
        Mockito.when(element.getContributor()).thenReturn(source);
        Mockito.when(element.getAttribute(Mockito.anyString())).thenAnswer(call -> attributes.get(call.getArgument(0)));
        Mockito.when(element.getChildren()).thenReturn(children);
        Mockito.when(element.getChildren(Mockito.anyString())).thenAnswer(call -> Arrays.stream(children)
            .filter(child -> child.getName().equals(call.getArgument(0)))
            .toArray(IConfigurationElement[]::new));
        return element;
    }
}
