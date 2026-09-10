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
package org.jkiss.dbeaver.maven.product;

import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

final class ProductGenerator {
    static final Pattern MARKER_PATTERN = Pattern.compile("dbeaver-launch-parameters\\s*:\\s*([a-zA-Z0-9_.-]+(?:\\s*,\\s*[a-zA-Z0-9_.-]+)*)");
    private static final Pattern MARKER_COMMENT_PATTERN = Pattern.compile(
        "(?m)^(\\h*)<!--\\s*" + MARKER_PATTERN.pattern() + "\\s*-->"
    );
    private static final String PARAMETERS_RESOURCE_ROOT = "launch-parameters/";
    private static final List<String> ICON_ATTRIBUTES = List.of("icon", "path");

    private final ClassLoader classLoader;

    ProductGenerator(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    int generate(Path sourceDirectory, Path outputDirectory, Path buildDirectory) throws ProductGenerationException {
        try {
            Files.createDirectories(outputDirectory);
            List<Path> productFiles;
            try (Stream<Path> files = Files.list(sourceDirectory)) {
                productFiles = files
                    .filter(path -> path.getFileName().toString().endsWith(".product"))
                    .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                    .toList();
            }
            for (Path productFile : productFiles) {
                generateProduct(productFile, outputDirectory, buildDirectory);
            }
            copyPublisherMetadata(sourceDirectory, outputDirectory);
            return productFiles.size();
        } catch (IOException e) {
            throw new ProductGenerationException("Error generating product descriptors from " + sourceDirectory, e);
        }
    }

    private static void copyPublisherMetadata(Path sourceDirectory, Path outputDirectory) throws IOException {
        try (Stream<Path> files = Files.list(sourceDirectory)) {
            for (Path metadataFile : files
                .filter(path -> path.getFileName().toString().endsWith(".p2.inf"))
                .toList()) {
                Files.copy(
                    metadataFile,
                    outputDirectory.resolve(metadataFile.getFileName()),
                    StandardCopyOption.REPLACE_EXISTING
                );
            }
        }
    }

    private void generateProduct(Path productFile, Path outputDirectory, Path buildDirectory)
        throws ProductGenerationException {
        try {
            String template = Files.readString(productFile, StandardCharsets.UTF_8);
            Document document = parseXml(template, productFile);
            validateMarkers(document, productFile);
            copyLauncherResources(document, productFile.getParent(), outputDirectory, buildDirectory);

            String generated = expandMarkers(template, productFile);
            parseXml(generated, productFile);
            Files.writeString(outputDirectory.resolve(productFile.getFileName()), generated, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new ProductGenerationException("Error generating product descriptor " + productFile, e);
        }
    }

    private String expandMarkers(String template, Path productFile) throws ProductGenerationException {
        Matcher matcher = MARKER_COMMENT_PATTERN.matcher(template);
        StringBuilder result = new StringBuilder(template.length());
        String lineSeparator = template.contains("\r\n") ? "\r\n" : "\n";
        int markerCount = 0;
        while (matcher.find()) {
            markerCount++;
            String indentation = matcher.group(1);
            String replacement = loadParameterSets(matcher.group(2), indentation, lineSeparator, productFile);
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);
        if (markerCount == 0) {
            throw new ProductGenerationException("No launch parameter marker found in " + productFile);
        }
        return result.toString();
    }

    private String loadParameterSets(String parameterSets, String indentation, String lineSeparator, Path productFile)
        throws ProductGenerationException {
        List<String> arguments = new ArrayList<>();
        for (String parameterSet : parameterSets.split(",")) {
            String name = parameterSet.trim();
            String resourceName = PARAMETERS_RESOURCE_ROOT + name + ".ini";
            try (InputStream stream = classLoader.getResourceAsStream(resourceName)) {
                if (stream == null) {
                    throw new ProductGenerationException(
                        "Unknown launch parameter set '" + name + "' in " + productFile
                    );
                }
                for (String line : new String(stream.readAllBytes(), StandardCharsets.UTF_8).lines().toList()) {
                    String argument = line.trim();
                    if (!argument.isEmpty() && !argument.startsWith("#")) {
                        arguments.add(escapeXml(argument));
                    }
                }
            } catch (IOException e) {
                throw new ProductGenerationException("Error reading launch parameter set '" + name + "'", e);
            }
        }
        return indentation + String.join(lineSeparator + indentation, arguments);
    }

    private static void validateMarkers(Document document, Path productFile) throws ProductGenerationException {
        NodeList comments = document.getChildNodes();
        int markerCount = countAndValidateMarkers(comments, productFile);
        if (markerCount == 0) {
            throw new ProductGenerationException("No launch parameter marker found in " + productFile);
        }
    }

    private static int countAndValidateMarkers(NodeList nodes, Path productFile) throws ProductGenerationException {
        int count = 0;
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (node instanceof Comment comment) {
                Matcher marker = MARKER_PATTERN.matcher(comment.getData().trim());
                if (marker.matches()) {
                    if (!(comment.getParentNode() instanceof Element parent) || !"vmArgs".equals(parent.getTagName())) {
                        throw new ProductGenerationException(
                            "Launch parameter marker must be a direct child of <vmArgs> in " + productFile
                        );
                    }
                    count++;
                }
            }
            count += countAndValidateMarkers(node.getChildNodes(), productFile);
        }
        return count;
    }

    private static Document parseXml(String xml, Path productFile) throws ProductGenerationException {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);
            return factory.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
        } catch (Exception e) {
            throw new ProductGenerationException("Invalid product XML in " + productFile, e);
        }
    }

    private static void copyLauncherResources(
        Document document,
        Path sourceDirectory,
        Path outputDirectory,
        Path buildDirectory
    ) throws ProductGenerationException {
        NodeList launchers = document.getElementsByTagName("launcher");
        for (int i = 0; i < launchers.getLength(); i++) {
            copyLauncherResources(launchers.item(i), sourceDirectory, outputDirectory, buildDirectory);
        }
    }

    private static void copyLauncherResources(
        Node node,
        Path sourceDirectory,
        Path outputDirectory,
        Path buildDirectory
    ) throws ProductGenerationException {
        NamedNodeMap attributes = node.getAttributes();
        if (attributes != null) {
            for (String attributeName : ICON_ATTRIBUTES) {
                Node attribute = attributes.getNamedItem(attributeName);
                if (attribute != null && !attribute.getNodeValue().isBlank()) {
                    copyLauncherResource(attribute.getNodeValue(), sourceDirectory, outputDirectory, buildDirectory);
                }
            }
        }
        NodeList children = node.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            copyLauncherResources(children.item(i), sourceDirectory, outputDirectory, buildDirectory);
        }
    }

    private static void copyLauncherResource(
        String resourcePath,
        Path sourceDirectory,
        Path outputDirectory,
        Path buildDirectory
    ) throws ProductGenerationException {
        String relativePath = resourcePath.replace('\\', '/').replaceFirst("^/", "");
        Path source = sourceDirectory.resolve(relativePath).normalize();
        Path target = outputDirectory.resolve(relativePath).normalize();
        if (!target.startsWith(buildDirectory.normalize())) {
            throw new ProductGenerationException("Launcher resource escapes the build directory: " + resourcePath);
        }
        if (!Files.isRegularFile(source)) {
            throw new ProductGenerationException("Launcher resource does not exist: " + source);
        }
        try {
            Files.createDirectories(target.getParent());
            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new ProductGenerationException("Error copying launcher resource " + source, e);
        }
    }

    private static String escapeXml(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
