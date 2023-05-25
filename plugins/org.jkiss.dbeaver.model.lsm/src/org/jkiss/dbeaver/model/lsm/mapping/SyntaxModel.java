/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.lsm.mapping;

import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.misc.Pair;
import org.antlr.v4.runtime.tree.Tree;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.lsm.mapping.AbstractSyntaxNode.BindingInfo;
import org.jkiss.dbeaver.model.lsm.mapping.internal.*;
import org.jkiss.dbeaver.model.lsm.mapping.internal.NodeFieldInfo.SubnodeInfo;

import java.io.StringWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.xml.stream.FactoryConfigurationError;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.*;

public class SyntaxModel {

    private final Parser parser;
    private final Map<String, NodeTypeInfo> nodeTypeByRuleName = new HashMap<>();
    private final Map<Class<?>, NodeTypeInfo> nodeTypeByClass = new HashMap<>();
    private final Map<String, LiteralTypeInfo> literalTypeByRuleName = new HashMap<>();
    private final Map<Class<?>, LiteralTypeInfo> literalTypeByClass = new HashMap<>();
    
    private final XPath xpath;
    
    public SyntaxModel(@NotNull Parser parser) {
        this.parser = parser;

        XPathFactory xf = XPathFactory.newInstance();
        this.xpath = xf.newXPath();
        xpath.setXPathFunctionResolver(new XFunctionResolver(xpath));
    }

    @NotNull
    private XTreeNodeBase prepareTree(@NotNull Tree root) {
        if (!(root instanceof XTreeNodeBase)) {
            throw new IllegalArgumentException("Failed to prepare syntax model due to unsupported syntax tree typeing." +
                "Consider using adapted grammar with correct superClass and contextSuperClass options.");
        }
        
        XTreeNodeBase rootNode = (XTreeNodeBase) root;
        if (rootNode.getIndex() < 0) {
            rootNode.fixup(parser, 0);
        }
        return rootNode;
    }

    @NotNull
    public String toXml(@NotNull Tree root) throws FactoryConfigurationError, TransformerException {
        XTreeNodeBase rootInfo = prepareTree(root);
        TransformerFactory transFactory = TransformerFactory.newInstance();
        Transformer transformer = transFactory.newTransformer();
        StringWriter buffer = new StringWriter();
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.transform(new DOMSource(rootInfo), new StreamResult(buffer));
        return buffer.toString();
    }

    @NotNull
    public <T extends AbstractSyntaxNode> SyntaxModelMappingResult<T> map(@NotNull Tree root, @NotNull Class<T> type) {
        XTreeNodeBase rootInfo = prepareTree(root);
        SyntaxModelMappingSession mappingSession = new SyntaxModelMappingSession(this);
        return mappingSession.map(rootInfo, type);
    }

    private void appendIndent(@NotNull StringBuilder sb, int indent) {
        sb.append("\t".repeat(Math.max(0, indent)));
    }

    @NotNull
    public String stringify(@NotNull AbstractSyntaxNode model) {
        StringBuilder sb = new StringBuilder();
        stringifyImpl(model, sb, 0);
        return sb.toString();
    }
    
    private void stringifyImpl(@NotNull AbstractSyntaxNode model, @NotNull StringBuilder sb, int indent) {
        this.appendIndent(sb, indent);
        sb.append("{");
        indent++;
        NodeTypeInfo typeInfo = this.nodeTypeByRuleName.get(model.getName());
        int n = 0;
        {
            sb.append("\n");
            this.appendIndent(sb, indent);
            sb.append("\"_sourceInterval\": \"").append(model.getAstNode().getSourceInterval()).append("\"");
            sb.append(",").append("\n");
            this.appendIndent(sb, indent);
            sb.append("\"_realInterval\": \"").append(model.getAstNode().getRealInterval()).append("\"");
            sb.append(",").append("\n");
            this.appendIndent(sb, indent);
            sb.append("\"_type\": \"").append(model.getClass().getName()).append("\"");
            sb.append(",").append("\n");
            this.appendIndent(sb, indent);
            sb.append("\"_ruleName\": \"").append(model.getName()).append("\"");
            sb.append(",").append("\n");
            this.appendIndent(sb, indent);
            sb.append("\"_bindings\": ");
            indent++;
            sb.append("[");
            int m = 0;
            for (BindingInfo binding : model.getBindings()) {
                if (m > 0) {
                    sb.append(",");
                }
                sb.append("{\n");
                indent++;
                this.appendIndent(sb, indent);
                sb.append("\"").append("sourceInterval").append("\"");
                sb.append(": ");
                sb.append("\"").append(binding.astNode.getSourceInterval()).append("\"");
                sb.append(",\n");
                this.appendIndent(sb, indent);
                sb.append("\"").append("realInterval").append("\"");
                sb.append(": ");
                sb.append("\"").append(binding.astNode.getRealInterval()).append("\"");
                sb.append(",\n");
                this.appendIndent(sb, indent);
                sb.append("\"").append("text").append("\"");
                sb.append(": ");
                sb.append("\"").append(binding.astNode.getTextContent().replace("\n", "\\n ").replace("\r", "\\r")).append("\"");
                sb.append(",\n");
                this.appendIndent(sb, indent);
                sb.append("\"").append("model").append("\"");
                sb.append(": ");
                sb.append("\"").append(binding.field.getDeclaringClassName()).append(".").append(binding.field.getFieldName()).append("\"");
                sb.append(",\n");
                this.appendIndent(sb, indent);
                sb.append("\"").append("node").append("\"");
                sb.append(": ");
                sb.append("\"");
                sb.append(binding.astNode.getFullPathName().substring(model.getAstNode().getFullPathName().length()));
                sb.append("\"");
                sb.append("\n");
                indent--;
                this.appendIndent(sb, indent);
                sb.append("}");
                m++;
            }
            indent--;
            sb.append("\n");
            this.appendIndent(sb, indent);
            sb.append("]");
            if (typeInfo == null) {
                sb.append(",").append("\n");
                this.appendIndent(sb, indent);
                sb.append("\"_error\": \"").append("syntax model type info not found").append("\"");
            }
            n += 2;
        }
        if (typeInfo != null) {
            for (NodeFieldInfo field : typeInfo.getFields()) {
                Object value;
                try {
                    value = field.getValue(model);
                } catch (IllegalArgumentException | IllegalAccessException ex) {
                    value = ex;
                }
                if (n > 0) {
                    sb.append(",");
                }
                sb.append("\n");
                this.appendIndent(sb, indent);
                sb.append("\"").append(field.getFieldName()).append("\"").append(": ");
                if (value == null) {
                    sb.append("null");
                } else if (value instanceof Throwable) {
                    this.appendIndent(sb, indent);
                    sb.append("{");
                    indent++;
                    sb.append("\n");
                    this.appendIndent(sb, indent);
                    sb.append("\"_type\": \"").append(value.getClass().getName()).append("\"");
                    sb.append(",").append("\n");
                    this.appendIndent(sb, indent);
                    sb.append("\"_error\": \"").append(value).append("\"");
                    indent--;
                    sb.append("\n");
                    this.appendIndent(sb, indent);
                    sb.append("}");
                    sb.append("\n");
                } else {
                    switch (field.kind) {
                        case Enum:
                        case String:
                            sb.append('"').append(value.toString().replace("\"", "\\\"")).append('"');
                            break;
                        case Byte:
                        case Short:
                        case Int:
                        case Long:
                        case Bool:
                        case Float:
                        case Double:
                            sb.append(value);
                            break;
                        case LiteralList: {
                            indent++;
                            sb.append("[");
                            int m = 0;
                            for (Object item : (Iterable<?>) value) {
                                if (m > 0) {
                                    sb.append(",");
                                }
                                sb.append("\n");
                                this.appendIndent(sb, indent);
                                sb.append("\"");
                                sb.append(item.toString());
                                sb.append("\"");
                                m++;
                            }
                            indent--;
                            sb.append("\n");
                            this.appendIndent(sb, indent);
                            sb.append("]");
                            break;
                        }
                        case Object:
                            this.stringifyImpl((AbstractSyntaxNode) value, sb, indent);
                            break;
                        case Array:
                        case List: {
                            indent++;
                            sb.append("[");
                            int m = 0;
                            for (Object item : (Iterable<?>) value) {
                                if (m > 0) {
                                    sb.append(",");
                                }
                                sb.append("\n");
                                this.stringifyImpl((AbstractSyntaxNode) item, sb, indent); // TODO consider trivial item
                                m++;
                            }
                            indent--;
                            sb.append("\n");
                            this.appendIndent(sb, indent);
                            sb.append("]");
                            break;
                        }
                        default: throw new RuntimeException("Unexpected syntax model field kind " + field.kind);
                    }
                }
                n++;
            }
        }
        
        indent--;
        sb.append("\n");
        this.appendIndent(sb, indent);
        sb.append("}");
    }
    
    @FunctionalInterface
    private interface ThrowableFunction<T, R> {
        R apply(T obj) throws Throwable;
    }
    
    private <T, R> Function<T, R> captureExceptionInfo(ThrowableFunction<T, R> mapper, BiConsumer<T, Throwable> handler) {
        return o -> {
            try {
                return mapper.apply(o);
            } catch (Throwable ex) {
                handler.accept(o, ex);
                return null;
            }
        };
    }
    
    private void introduceEnum(Class<?> type, ModelErrorsCollection errors) {
        SyntaxLiteral literalAnnotation = type.getAnnotation(SyntaxLiteral.class);
        if (literalAnnotation == null) {
            errors.add("Type " + type.getName() + " is not marked as syntax ruleName!");
        } else if (!type.isEnum()) {
            errors.add("Type " + type.getName() + " is not a enum while marked as syntax literal!");
        }

        assert literalAnnotation != null;
        var existing = literalTypeByRuleName.get(literalAnnotation.name());
        if (existing == null) {
            var enumEntries = Stream.of(type.getFields()).filter(Field::isEnumConstant).map(captureExceptionInfo(
                    f -> new Object() {
                        public final Object value = f.get(null);
                        public final String name = f.getName();
                        public final String upperCasedName = f.getName().toUpperCase();
                        public final SyntaxLiteralCase literalCaseAnnotation = f.getAnnotation(SyntaxLiteralCase.class);
                    }, 
                    (f, ex) -> errors.add(ex, "Failed to introduce model enum case " + f.getName() + " of type " + type.getName())
            )).collect(Collectors.toList());
            
            var countOfDefaultCasedNames = enumEntries.stream().map(e -> e.name).collect(Collectors.toSet()).size(); 
            var countOfUpperCasedNames = enumEntries.stream().map(e -> e.upperCasedName).collect(Collectors.toSet()).size();
            var isCaseSensitive = countOfDefaultCasedNames != countOfUpperCasedNames;
            var valuesByName = isCaseSensitive ? enumEntries.stream().collect(Collectors.toMap(e -> e.name, e -> e.value))
                                               : enumEntries.stream().collect(Collectors.toMap(e -> e.upperCasedName, e -> e.value));
            
            var exprByValue = enumEntries.stream()
                .filter(e -> e.literalCaseAnnotation != null && e.literalCaseAnnotation.xcondition().length() > 0)
                .collect(Collectors.toMap(
                    e -> e.value, 
                    captureExceptionInfo(
                        e -> xpath.compile(e.literalCaseAnnotation.xcondition()), // TODO collect raw string too 
                        (f, ex) -> errors.add(ex, "Failed to prepare condition xpath for enum case "
                            + f.name + " of type " + type.getName())
                    ),
                    (x, y) -> {
                        throw new IllegalStateException("Duplicated enum values"); // should never happen
                    },
                    LinkedHashMap::new
                ));
            try {
                var xstringExpr = literalAnnotation.xstring().length() > 0
                    ? xpath.compile(literalAnnotation.xstring())
                    : null; // TODO collect raw string too
                LiteralTypeInfo literalTypeInfo = new LiteralTypeInfo(
                    literalAnnotation.name(), type, xstringExpr, exprByValue, valuesByName, isCaseSensitive);
                literalTypeByRuleName.put(literalAnnotation.name(), literalTypeInfo);
                literalTypeByClass.put(type, literalTypeInfo);
            } catch (XPathException ex) {
                errors.add(ex, "Failed to prepare xstring xpath exprssion for literal of type " + type.getName());
            }
        } else if (!existing.type.equals(type)) {
            errors.add("Ambiguous syntax literal: both " + type.getName() + " and " + existing.type.getName() +
                "  are marked with the same name " + literalAnnotation.name());
        } else {
            // already registered, so nothing to do 
        }
    }

    public <T extends AbstractSyntaxNode> ModelErrorsCollection introduce(Class<T> modelType) {
        ModelErrorsCollection errors = new ModelErrorsCollection();
        
        Set<Class<?>> processedTypes = new HashSet<>();
        
        LinkedList<NodeFieldInfo> fieldsToFixup = new LinkedList<>();
        Queue<Pair<Field, Class<? extends AbstractSyntaxNode>>> queue = new LinkedList<>();
        queue.add(new Pair<>(null, modelType));
        while (!queue.isEmpty()) {
            Pair<Field, Class<? extends AbstractSyntaxNode>> entry = queue.remove();
            Class<? extends AbstractSyntaxNode> type = entry.b;
            
            if (!processedTypes.add(type)) {
                continue;
            }
            
            SyntaxNode ruleAnnotation = type.getAnnotation(SyntaxNode.class);
            if (ruleAnnotation == null) {
                String referrent = entry.a == null ? " "
                    : (" referenced from field " + entry.a.getName() + " of type " + entry.a.getDeclaringClass().getName() + " ");
                errors.add("Type " + type.getName() + referrent + " is not marked as syntax node with SyntaxNode annotation");
                continue;
            }
            if (!AbstractSyntaxNode.class.isAssignableFrom(type)) {
                String referrent = entry.a == null ? " "
                    : (" referenced from field " + entry.a.getName() + " of type " + entry.a.getDeclaringClass().getName() + " ");
                errors.add("Type " + type.getName() + referrent + " is not a subclass of AbstractSyntaxNode "
                    + "and cannot be supported as part of syntax model");
            }
            
            var fields = Stream.of(type.getFields()).map(f -> new Object() {
                public final Field info = f;
                public final SyntaxSubnode[] subnodeSpecs = f.getAnnotationsByType(SyntaxSubnode.class);
                public final SyntaxTerm[] termSpecs = f.getAnnotationsByType(SyntaxTerm.class);
            }).filter(
                f -> !Modifier.isStatic(f.info.getModifiers())
            ).collect(Collectors.toList());
            Map<String, NodeFieldInfo> modelFields = new HashMap<>(fields.size());
            
            for (var field : fields) {
                Class<?> fieldType = field.info.getType();
                boolean expectsSubnode = field.subnodeSpecs.length > 0;
                boolean expectsLiteral = field.termSpecs.length > 0;
                FieldTypeKind kind;
                if (expectsSubnode && expectsLiteral) {
                    errors.add("Field of terminal value kind cannot be bound with complex subnode type");
                }
                if (expectsSubnode) {
                    kind = FieldTypeKind.resolveModelSubnodeFieldKind(fieldType);
                } else {
                    kind = FieldTypeKind.resolveModelLiteralFieldKind(fieldType);
                }
                
                List<XPathExpression> termExprs = new ArrayList<>(field.termSpecs.length);
                List<SubnodeInfo> subnodeExprs = new ArrayList<>(field.subnodeSpecs.length);
                for (var termSpec : field.termSpecs) {
                    try {
                        termExprs.add(xpath.compile(termSpec.xpath())); // TODO collect raw string too
                        if (fieldType.isEnum()) {
                            if (processedTypes.add(fieldType)) {
                                introduceEnum(fieldType, errors);
                            }
                        }
                    } catch (XPathExpressionException e) {
                        errors.add(e, "Failed to prepare literal xpath exprssion for field "
                            + field.info.getName() + " of type " + type.getName());
                    }
                }
                for (var subnodeSpec : field.subnodeSpecs) {
                    Class<? extends AbstractSyntaxNode> subnodeType;
                    if (subnodeSpec.type() == null || subnodeSpec.type().equals(AbstractSyntaxNode.class)) {
                        if (AbstractSyntaxNode.class.isAssignableFrom(fieldType)) {
                            @SuppressWarnings("unchecked")
                            Class<? extends AbstractSyntaxNode> ft = (Class<? extends AbstractSyntaxNode>) fieldType;
                            subnodeType = ft;
                        } else {
                            errors.add("Failed to resolve subnode type for field " + field.info.getName() + " of type " + type.getName()
                                + ": either " + fieldType.getName() + " should be a subclass of AbstractSyntaxNode"
                                + " or explicit target type required for subnode annotation");
                            continue;
                        }
                    } else {
                        subnodeType = subnodeSpec.type();
                    }
                    
                    try {
                        XPathExpression scopeExpr = subnodeSpec.xpath() != null && subnodeSpec.xpath().length() > 0
                            ? xpath.compile(subnodeSpec.xpath()) : null; // TODO collect raw string too
                        subnodeExprs.add(new SubnodeInfo(scopeExpr, subnodeType, subnodeSpec.lookup()));
                        queue.add(new Pair<>(field.info, subnodeType));
                    } catch (XPathExpressionException ex) {
                        errors.add(ex, "Failed to prepare subnode xpath exprssion for field "
                            + field.info.getName() + " of type " + type.getName());
                    }
                }
                
                NodeFieldInfo fieldInfo = new NodeFieldInfo(kind, field.info, termExprs, subnodeExprs); 
                modelFields.put(field.info.getName(), fieldInfo);
                fieldsToFixup.addLast(fieldInfo);
            }
            
            Constructor<? extends AbstractSyntaxNode> ctor;
            try {
                ctor = type.getConstructor();
            } catch (Throwable ex) {
                errors.add(ex, "Failed to resolve default contructor for syntax model type " + type.getName());
                continue;
            }

            String ruleName = ruleAnnotation.name() != null && ruleAnnotation.name().length() > 0 ? ruleAnnotation.name() : type.getName();
            NodeTypeInfo nodeTypeInfo = new NodeTypeInfo(ruleName, type, ctor, modelFields);
            nodeTypeByRuleName.put(ruleName, nodeTypeInfo);
            nodeTypeByClass.put(type, nodeTypeInfo);
        }
        
        
        for (NodeFieldInfo fieldInfo : fieldsToFixup) {
            fieldInfo.fixup(this);
        }
        
        return errors;
    }

    public NodeTypeInfo findNodeTypeInfo(Class<?> type) {
        return this.nodeTypeByClass.get(type);
    }

    public LiteralTypeInfo findLiteralTypeInfo(Class<?> type) {
        return this.literalTypeByClass.get(type);
    }
}
