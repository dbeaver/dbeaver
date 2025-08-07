/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.app.standalone.internal;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.Platform;
import org.eclipse.osgi.service.datalocation.Location;
import org.eclipse.ui.internal.e4.compatibility.CompatibilityEditor;
import org.eclipse.ui.internal.menus.MenuHelper;
import org.eclipse.ui.internal.registry.IWorkbenchRegistryConstants;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.utils.xml.XMLException;
import org.jkiss.utils.xml.XMLUtils;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

public final class WorkbenchPatcher {
    private static final Log log = Log.getLog(WorkbenchPatcher.class);

    private WorkbenchPatcher() {
    }

    /**
     * Patches the {@code workbench.xmi} file, updating all view and editor
     * parts' icons to their actual values taken directly from contributed
     * extensions.
     * <p>
     * Does not update the file if no changes were made.
     *
     * @param instance workbench location
     */
    public static void patchWorkbenchXmi(@NotNull Location instance) {
        Path path = getWorkbenchSaveLocation(instance);
        if (path == null || !Files.exists(path)) {
            return;
        }

        try {
            patchWorkbenchXmi(path);
        } catch (Throwable e) {
            log.error("Failed to patch workbench state file: " + path, e);
        }
    }

    private static void patchWorkbenchXmi(@NotNull Path workbenchXmi) throws Exception {
        var documentBuilderFactory = DocumentBuilderFactory.newInstance();
        documentBuilderFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);

        var documentBuilder = documentBuilderFactory.newDocumentBuilder();
        var document = documentBuilder.parse(workbenchXmi.toFile());

        var parts = collectContributedParts();
        var transformed = patchPartIconsRecursively(document, parts);

        if (transformed) {
            var transformerFactory = TransformerFactory.newInstance();
            var transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.STANDALONE, "no");

            try (OutputStream os = Files.newOutputStream(workbenchXmi)) {
                var source = new DOMSource(document);
                var result = new StreamResult(os);

                transformer.transform(source, result);
            }
        }
    }

    private static boolean patchPartIconsRecursively(@NotNull Node node, @NotNull Map<String, PartDescriptor> parts) {
        NodeList children = node.getChildNodes();
        boolean modified = false;

        for (int i = 0; i < children.getLength(); i++) {
            if (!(children.item(i) instanceof Element child)) {
                continue;
            }

            if (child.hasAttribute("elementId") && child.hasAttribute("iconURI")) {
                Attr iconURI = child.getAttributeNode("iconURI");
                String elementId = child.getAttribute("elementId");

                if (elementId.equals(CompatibilityEditor.MODEL_ELEMENT_ID)) {
                    // CompatibilityEditor is not an editor itself
                    // See org.eclipse.ui.internal.WorkbenchPage.createEditorReferenceForPart
                    elementId = extractCompatibilityEditorId(child);
                }

                PartDescriptor part = parts.get(elementId);
                if (part != null && !iconURI.getNodeValue().equals(part.icon())) {
                    log.debug("Replacing icon for part '" + part.id() + "' with '" + part.icon() + "'");
                    iconURI.setNodeValue(part.icon());
                    modified = true;
                }
            }

            modified |= patchPartIconsRecursively(child, parts);
        }

        return modified;
    }

    @Nullable
    private static String extractCompatibilityEditorId(@NotNull Element element) {
        // For explanation behind this logic, see org.eclipse.ui.internal.EditorReference#EditorReference

        Element persistedState = XMLUtils.getChildElement(element, "persistedState");
        if (persistedState == null) {
            return null;
        }

        String key = persistedState.getAttribute("key");
        String value = persistedState.getAttribute("value");
        if (!"memento".equals(key)) {
            return null;
        }

        try {
            var memento = XMLUtils.parseDocument(new StringReader(value));
            var editor = memento.getDocumentElement();
            if (editor.getTagName().equals("editor") && editor.hasAttribute("id")) {
                return editor.getAttribute("id");
            }
        } catch (XMLException e) {
            log.debug("Error parsing editor memento", e);
        }

        return null;
    }

    @NotNull
    private static Map<String, PartDescriptor> collectContributedParts() {
        var registry = Platform.getExtensionRegistry();

        var views = Stream.of(registry.getExtensionPoint("org.eclipse.ui.views").getExtensions())
            .map(IExtension::getConfigurationElements).flatMap(Stream::of)
            .filter(e -> e.getName().equals("view") && e.getAttribute("icon") != null)
            .map(PartDescriptor::of)
            .toList();

        var editors = Stream.of(registry.getExtensionPoint("org.eclipse.ui.editors").getExtensions())
            .map(IExtension::getConfigurationElements).flatMap(Stream::of)
            .filter(e -> e.getName().equals("editor") && e.getAttribute("icon") != null)
            .map(PartDescriptor::of)
            .toList();

        return Stream.concat(views.stream(), editors.stream())
            .collect(Collectors.toMap(
                PartDescriptor::id,
                Function.identity(),
                (a, b) -> b
            ));
    }

    @Nullable
    private static Path getWorkbenchSaveLocation(@NotNull Location instance) {
        try {
            var path = RuntimeUtils.getLocalPathFromURL(instance.getURL());
            return path.resolve(".metadata/.plugins/org.eclipse.e4.workbench/workbench.xmi"); //$NON-NLS-1$
        } catch (IOException e) {
            log.error("Unable to resolve workbench save location: " + instance.getURL(), e);
            return null;
        }
    }

    private record PartDescriptor(@NotNull IConfigurationElement element, @NotNull String id, @NotNull String icon) {
        @NotNull
        static PartDescriptor of(@NotNull IConfigurationElement element) {
            String id = element.getAttribute("id");
            String icon = MenuHelper.getIconURI(element, IWorkbenchRegistryConstants.ATT_ICON);
            return new PartDescriptor(element, id, icon);
        }
    }
}
