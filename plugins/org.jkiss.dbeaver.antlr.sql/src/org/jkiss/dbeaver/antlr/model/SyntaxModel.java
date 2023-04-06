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
package org.jkiss.dbeaver.antlr.model;

import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.misc.Interval;
import org.antlr.v4.runtime.tree.*;
import org.jkiss.dbeaver.antlr.model.internal.CustomXPathFunctionResolver;
import org.jkiss.dbeaver.antlr.model.internal.CustomXPathModelNodeBase;
import org.jkiss.dbeaver.antlr.model.internal.FieldTypeKind;
import org.jkiss.dbeaver.antlr.model.internal.LiteralTypeInfo;
import org.jkiss.dbeaver.antlr.model.internal.NodeFieldInfo;
import org.jkiss.dbeaver.antlr.model.internal.NodeFieldInfo.SubnodeInfo;
import org.jkiss.dbeaver.antlr.model.internal.NodeTypeInfo;
import org.jkiss.dbeaver.antlr.model.internal.NodesList;
import org.jkiss.dbeaver.antlr.model.internal.TreeRuleNode.SubnodesList;
import org.w3c.dom.*;

import java.io.StringWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import javax.xml.namespace.QName;
import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.*;
import javax.xml.xpath.XPathEvaluationResult.XPathResultType;


public class SyntaxModel {

    private final Parser parser;
    private final Map<String, NodeTypeInfo> nodeTypeByRuleName;
    private final Map<String, LiteralTypeInfo> literalTypeByRuleName;
    
    private final XPath xpath;
    
    public SyntaxModel(Parser parser) {
        this.parser = parser;
        this.nodeTypeByRuleName = new HashMap<>();
        this.literalTypeByRuleName = new HashMap<>();

        XPathFactory xf = XPathFactory.newInstance();
        this.xpath = xf.newXPath();
        xpath.setXPathFunctionResolver(new CustomXPathFunctionResolver(xpath));
    }

    private AbstractSyntaxNode instantiateAndFill(NodeTypeInfo typeInfo, CustomXPathModelNodeBase nodeInfo) {
        try {
            if (nodeInfo.getModel() == null) {
                nodeInfo.setModel(typeInfo.ctor.newInstance());
            }
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
            throw new RuntimeException("Failed to instantiate " + typeInfo.type.getName(), ex);
        }
        
        Set<CustomXPathModelNodeBase> subnodes = new HashSet<CustomXPathModelNodeBase>(5);
        for (var field : typeInfo.fields.values()) {
            subnodes.clear();
            for (var expr : field.termExprs) {
                try {
                    XPathEvaluationResult<?> value = expr.evaluateExpression(nodeInfo);
                    this.bindValue(nodeInfo, field, value);
                } catch (XPathExpressionException e) {
                    // TODO collect error info
                    e.printStackTrace();
                }
            }
            if (field.kind == FieldTypeKind.Array || field.kind == FieldTypeKind.List) {
                for (var subnodeInfo : field.subnodesInfo) {
                    boolean tryDescedants = subnodeInfo.lookupMode == SyntaxSubnodeLookupMode.DEPTH_FIRST;
                    try {
                        if (subnodeInfo.scopeExpr != null) {
                            XPathEvaluationResult<?> scopeOrSubnode = subnodeInfo.scopeExpr.evaluateExpression(nodeInfo);
                            if (scopeOrSubnode.type() == XPathResultType.NODESET && scopeOrSubnode.value() instanceof XPathNodes) {
                                for (var scopeSubnode : (XPathNodes) scopeOrSubnode.value()) {
                                    if (scopeSubnode instanceof CustomXPathModelNodeBase) {
                                        mapSubtrees((CustomXPathModelNodeBase) scopeSubnode, subnodeInfo.subnodeType, true, tryDescedants, subnodes);
                                    }
                                }
                            } else if (scopeOrSubnode.type() == XPathResultType.NODE && scopeOrSubnode.value() instanceof CustomXPathModelNodeBase) {
                                mapSubtrees((CustomXPathModelNodeBase) scopeOrSubnode.value(), subnodeInfo.subnodeType, true, tryDescedants, subnodes);
                            }
                        } else {
                            if (tryDescedants) {
                                mapSubtrees(nodeInfo, subnodeInfo.subnodeType, false, true, subnodes);
                            } else {
                                for (var candidateSubnode : nodeInfo.getSubnodes().getCollection()) {
                                    mapSubtrees(candidateSubnode, subnodeInfo.subnodeType, true, false, subnodes);
                                }
                            }
                        }
                    } catch (XPathExpressionException e) {
                        // TODO collect error info
                        e.printStackTrace();
                    }
                }
                List<Node> orderedSubnodes = subnodes.stream()
                    .sorted(Comparator.comparingInt(a -> a.getModel().getStartPosition()))
                    .collect(Collectors.toList());
                this.bindValue(nodeInfo, field, orderedSubnodes);
            } else {
                for (var subnodeInfo : field.subnodesInfo) {
                    boolean tryDescedants = subnodeInfo.lookupMode == SyntaxSubnodeLookupMode.DEPTH_FIRST;
                    try {
                        AbstractSyntaxNode subnode = null;
                        if (subnodeInfo.scopeExpr != null) {
                            XPathEvaluationResult<?> scopeOrSubnode = subnodeInfo.scopeExpr.evaluateExpression(nodeInfo);
                            if (scopeOrSubnode.type() == XPathResultType.NODESET && scopeOrSubnode.value() instanceof XPathNodes) {
                                for (var scopeSubnode : (XPathNodes) scopeOrSubnode.value()) {
                                    if (scopeSubnode instanceof CustomXPathModelNodeBase) {
                                        subnode = mapSubtree((CustomXPathModelNodeBase) scopeSubnode, subnodeInfo.subnodeType, true, tryDescedants);
                                        if (subnode != null) {
                                            break;
                                        }
                                    }
                                }
                            } else if (scopeOrSubnode.type() == XPathResultType.NODE && scopeOrSubnode.value() instanceof CustomXPathModelNodeBase) {
                                subnode = mapSubtree((CustomXPathModelNodeBase) scopeOrSubnode.value(), subnodeInfo.subnodeType, true, tryDescedants);
                            }
                        } else {
                            if (tryDescedants) {
                                subnode = mapSubtree(nodeInfo, subnodeInfo.subnodeType, false, true);
                            } else {
                                for (var candidateSubnode : nodeInfo.getSubnodes().getCollection()) {
                                    mapSubtrees(candidateSubnode, subnodeInfo.subnodeType, true, false, subnodes);
                                }
                            }
                        }
                        if (subnode != null) {
                            this.bindRawValue(nodeInfo, field, subnode);
                            break;
                        }
                    } catch (XPathExpressionException e) {
                        // TODO collect error info
                        e.printStackTrace();
                    }
                }
            }
        }
        
        AbstractSyntaxNode model = nodeInfo.getModel(); 
        if (nodeInfo instanceof SyntaxTree) {
            SyntaxTree snode = (SyntaxTree) nodeInfo;
            model.setStartPosition(snode.getSourceInterval().a);
            model.setEndPosition(snode.getSourceInterval().b);
        }
        return model;
    }   
    
    private Object mapLiteralValue(CustomXPathModelNodeBase nodeInfo, Class<?> type) {
        SyntaxLiteral ruleAnnotation = type.getAnnotation(SyntaxLiteral.class);
        LiteralTypeInfo typeInfo = literalTypeByRuleName.get(ruleAnnotation.name());
        
        if (typeInfo != null) {
            try {
                String str = typeInfo.stringExpr == null ? nodeInfo.getTextContent() : typeInfo.stringExpr.evaluateExpression(nodeInfo, String.class);
                if (str != null && str.length() > 0) {
                    // System.out.println(str + " | " + nodeInfo.getNodeValue());
                    Object value = typeInfo.valuesByName.get(typeInfo.isCaseSensitive ? str : str.toUpperCase());
                    return value;
                }
            } catch (XPathExpressionException e) {
                e.printStackTrace(); // TODO collect errors
            }
            
            for (var literalCase : typeInfo.exprByValue.entrySet()) {
                try {
                    Object value = literalCase.getValue().evaluateExpression(nodeInfo, Boolean.class);
                    if (Boolean.TRUE.equals(value)) {
                        return literalCase.getKey();
                    }
                } catch (XPathExpressionException e) {
                    e.printStackTrace(); // TODO collect errors
                }
            }
        }

        return null;
    }

    private void bindValue(CustomXPathModelNodeBase nodeInfo, NodeFieldInfo fieldInfo, List<Node> subnodes) {
        try {
            this.bindValueImpl(nodeInfo, fieldInfo, new XPathEvaluationResult<XPathNodes>() {
                @Override
                public XPathResultType type() { return XPathResultType.NODESET; }

                @Override
                public XPathNodes value() {
                    return new XPathNodes() {
                        @Override
                        public int size() { return subnodes.size(); }
                        @Override
                        public Iterator<Node> iterator() { return subnodes.iterator(); }
                        @Override
                        public Node get(int index) throws XPathException { return subnodes.get(index); }
                    };
                }
            });
        } catch (IllegalArgumentException | IllegalAccessException ex) {
            throw new RuntimeException("Failed to bind " + fieldInfo.info.getName(), ex); // TODO collect errors
        }            
    }

    private void bindValue(CustomXPathModelNodeBase nodeInfo, NodeFieldInfo fieldInfo, CustomXPathModelNodeBase subnode) {
        try {
            this.bindValueImpl(nodeInfo, fieldInfo, new XPathEvaluationResult<Node>() {
                @Override
                public XPathResultType type() { return XPathResultType.NODE; }
                @Override
                public Node value() { return subnode; }
            });
        } catch (IllegalArgumentException | IllegalAccessException ex) {
            throw new RuntimeException("Failed to bind " + fieldInfo.info.getName(), ex); // TODO collect errors
        }            
    }
    
    private void bindValue(CustomXPathModelNodeBase nodeInfo, NodeFieldInfo fieldInfo, XPathEvaluationResult<?> xvalue) {
        try {
            this.bindValueImpl(nodeInfo, fieldInfo, xvalue);
        } catch (IllegalArgumentException | IllegalAccessException ex) {
            throw new RuntimeException("Failed to bind " + fieldInfo.info.getName(), ex); // TODO collect errors
        }            
    }

    private Object getValue(NodeFieldInfo fieldInfo, AbstractSyntaxNode model) {
        try {
            return fieldInfo.info.get(model);
        } catch (IllegalArgumentException | IllegalAccessException ex) {
            throw new RuntimeException("Failed to bind " + fieldInfo.info.getName(), ex); // TODO collect errors
        }
    }
    
    private String getScalarString(XPathEvaluationResult<?> xvalue) {
        try {
            switch (xvalue.type()) {
                case NODE:
                    @SuppressWarnings("unchecked") 
                    CustomXPathModelNodeBase nodeInfo = (CustomXPathModelNodeBase) xvalue.value();
                    return nodeInfo.getNodeValue();
                case NODESET:
                    XPathNodes nodes = (XPathNodes) xvalue.value();
                    int count = nodes.size();
                    if (count == 0) {
                        return null;
                    } else if (count == 1) {
                        return nodes.get(0).getNodeValue().toString();
                    } else {
                        // TODO capture ambiguous binding error
                        return nodes.get(0).getNodeValue().toString();
                    }
                default:
                    if (xvalue.value() != null) {
                        return xvalue.value().toString();
                    } else {
                        return null;
                    }
            }
        } catch (XPathException ex) {
            ex.printStackTrace(); // TODO capture error
            return null;
        }
    }
    
    private void bindValueImpl(CustomXPathModelNodeBase nodeInfo, NodeFieldInfo fieldInfo, XPathEvaluationResult<?> xvalue) throws IllegalArgumentException, IllegalAccessException {
        Object value;
        switch (fieldInfo.kind) {
            case Object:
            case Array:
            case List:
//                if (subnode.model != null) {
//                    value = subnode.model;
//                    break;
//                }
                switch (xvalue.type()) {
                    case NODE:
                    case NODESET:
                        value = xvalue.value();
                        break;
                    default:
                        throw new RuntimeException("Not supported");
                }
                break;
            case String: value = getScalarString(xvalue); break;
            case Byte: value = Byte.parseByte(getScalarString(xvalue)); break;
            case Short: value = Short.parseShort(getScalarString(xvalue)); break;
            case Int: value = Integer.parseInt(getScalarString(xvalue)); break;
            case Long: value = Long.parseLong(getScalarString(xvalue)); break;
            case Bool: value = Boolean.parseBoolean(getScalarString(xvalue)); break;
            case Float: value = Float.parseFloat(getScalarString(xvalue)); break;
            case Double: value = Double.parseDouble(getScalarString(xvalue)); break;
            case Enum:
                switch (xvalue.type()) {
                    case NODE: 
                        value = mapLiteralValue((CustomXPathModelNodeBase) xvalue.value(), fieldInfo.info.getType());
                        break;
                    case NODESET:
                        XPathNodes nodes = (XPathNodes) xvalue.value();
                        if (nodes.size() == 1) {
                            try {
                                value = mapLiteralValue((CustomXPathModelNodeBase) nodes.get(0), fieldInfo.info.getType());
                            } catch (XPathException e) {
                                throw new RuntimeException(e); // TODO collect errors
                            }
                        } else {                                
                            value = null;
                        }
                        break;
                    default:
                        throw new RuntimeException("Not supported");
                }
                break;
            default: throw new RuntimeException("Unexpected syntax model field kind " + fieldInfo.kind);
        }
        
        this.bindRawValueImpl(nodeInfo, fieldInfo, xvalue);
    }
    
    private void bindRawValue(CustomXPathModelNodeBase nodeInfo, NodeFieldInfo fieldInfo, Object value) {
        try {
            this.bindRawValueImpl(nodeInfo, fieldInfo, value);
        } catch (IllegalArgumentException | IllegalAccessException ex) {
            throw new RuntimeException("Failed to bind raw " + fieldInfo.info.getName(), ex); // TODO collect errors
        }            
    }
    
    private void bindRawValueImpl(CustomXPathModelNodeBase nodeInfo, NodeFieldInfo fieldInfo, Object value) throws IllegalArgumentException, IllegalAccessException {       
        switch (fieldInfo.kind) {
            case Object: {
                if (value instanceof CustomXPathModelNodeBase) {
                    @SuppressWarnings("unchecked")
                    CustomXPathModelNodeBase subnodeInfo = (CustomXPathModelNodeBase) value;
                    if (subnodeInfo.getModel() != null) {
                        fieldInfo.info.set(nodeInfo.getModel(), subnodeInfo.getModel());
                    }
                } else {
                    fieldInfo.info.set(nodeInfo.getModel(), value);
                }
                break;
            }
            case Array: { // TODO
//                int index;
//                Object newArr, oldArr = fieldInfo.info.get(nodeInfo.model);
//                Class<?> itemType = fieldInfo.info.getType().getComponentType();
//                if (value != null) {
//                    index = Array.getLength(oldArr);
//                    newArr = Array.newInstance(itemType, index + 1);
//                    System.arraycopy(oldArr, 0, newArr, 0, index);
//                } else {
//                    index = 0;
//                    newArr = Array.newInstance(itemType, 1);
//                }
//                Array.set(newArr, index, value);
//                fieldInfo.info.set(nodeInfo.model, newArr);
//                break;
                throw new UnsupportedOperationException("TODO");
            }
            case List: {
                @SuppressWarnings("unchecked")
                List<Object> list = (List<Object>) fieldInfo.info.get(nodeInfo.getModel());
                if (list == null) {
                    fieldInfo.info.set(nodeInfo.getModel(), list = new ArrayList<>());
                } else {
                    list.clear();
                }
                if (value instanceof XPathNodes) {
                    XPathNodes nodes = (XPathNodes) value;
                    if (list instanceof ArrayList<?>) {
                        ((ArrayList<?>) list).ensureCapacity(nodes.size());
                    }
                    for (var xnode: nodes) {
                        if (xnode instanceof CustomXPathModelNodeBase) {
                            @SuppressWarnings("unchecked")
                            CustomXPathModelNodeBase subnodeInfo = (CustomXPathModelNodeBase) xnode;
                            if (subnodeInfo.getModel() != null) {
                                list.add(subnodeInfo.getModel());
                            }
                        }
                    }
                } else if (value instanceof CustomXPathModelNodeBase) {
                    @SuppressWarnings("unchecked")
                    CustomXPathModelNodeBase subnodeInfo = (CustomXPathModelNodeBase) value;
                    if (subnodeInfo.getModel() != null) {
                        list.add(subnodeInfo.getModel());
                    }
                } else {
                    list.add(value);
                }
                break;
            }
            default:
                fieldInfo.info.set(nodeInfo.getModel(), value);
                break;
        }
    }
    
    private void mapSubtrees(CustomXPathModelNodeBase nodeInfo, Class<? extends AbstractSyntaxNode> type,  boolean tryExact, boolean tryDescedants, Set<CustomXPathModelNodeBase> subnodes) {
        SyntaxNode ruleAnnotation = type.getAnnotation(SyntaxNode.class);
        NodeTypeInfo typeInfo = nodeTypeByRuleName.get(ruleAnnotation.name());
        
        if (typeInfo != null) {
            if (tryExact && nodeInfo.getLocalName().equals(ruleAnnotation.name())) {
                if (subnodes.add(nodeInfo)) {
                    this.instantiateAndFill(typeInfo, nodeInfo);
                }
                return;
            }
            
            if (tryDescedants) {
                NodesList<CustomXPathModelNodeBase> childNodes = nodeInfo.findDescedantLayerByName(typeInfo.ruleName);
                for (CustomXPathModelNodeBase childNode : childNodes) {
                    if (subnodes.add(childNode)) {
                        this.instantiateAndFill(typeInfo, childNode);
                    }
                }
            }
        }
    }
    
    private AbstractSyntaxNode mapSubtree(CustomXPathModelNodeBase nodeInfo, Class<? extends AbstractSyntaxNode> type,  boolean tryExact, boolean tryDescedants) {
        SyntaxNode ruleAnnotation = type.getAnnotation(SyntaxNode.class);
        NodeTypeInfo typeInfo = nodeTypeByRuleName.get(ruleAnnotation.name());
        
        if (typeInfo != null) {
            if (tryExact && nodeInfo.getLocalName().equals(ruleAnnotation.name())) {
                return this.instantiateAndFill(typeInfo, nodeInfo);
            }
            
            if (tryDescedants) {
                CustomXPathModelNodeBase childNode = nodeInfo.findFirstDescedantByName(typeInfo.ruleName);
                if (childNode != null) {
                    return this.instantiateAndFill(typeInfo, childNode);
                }
            }
        }
        return null;
    }
    
    private CustomXPathModelNodeBase prepareTree(Tree root) {
        if (!(root instanceof CustomXPathModelNodeBase)) {
            throw new UnsupportedOperationException();
        }
        CustomXPathModelNodeBase rootNode = (CustomXPathModelNodeBase)root;
        if (rootNode.getIndex() < 0) {
            rootNode.fixup(parser, 0);
        }
        return rootNode;
    }
    
    public String toXml(Tree root) throws XMLStreamException, FactoryConfigurationError, TransformerException {
        CustomXPathModelNodeBase rootInfo = prepareTree(root);
        TransformerFactory transFactory = TransformerFactory.newInstance();
        Transformer transformer = transFactory.newTransformer();
        StringWriter buffer = new StringWriter();
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.transform(new DOMSource(rootInfo), new StreamResult(buffer));
        return buffer.toString();
    }
    
    public <T extends AbstractSyntaxNode> SyntaxModelMappingResult<T> map(Tree root, Class<T> type) {
        CustomXPathModelNodeBase rootInfo = prepareTree(root);
        AbstractSyntaxNode modelNode = this.mapSubtree(rootInfo, type, true, true);
        if (modelNode != null) {
            @SuppressWarnings("unchecked")
            T result = (T) modelNode;
            return new SyntaxModelMappingResult<T>(result);
        } else {
            return new SyntaxModelMappingResult<T>(null);
        }
    }

    private void appendIndent(StringBuilder sb, int indent) {
        for (int i = 0; i < indent; i++) {
            sb.append("\t");
        }
    }
    
    public String stringify(AbstractSyntaxNode model) {
        StringBuilder sb = new StringBuilder();
        stringifyImpl(model, sb, 0);
        return sb.toString();
    }
    
    private void stringifyImpl(AbstractSyntaxNode model, StringBuilder sb, int indent) {
        this.appendIndent(sb, indent);
        sb.append("{");
        indent++;
        NodeTypeInfo typeInfo = this.nodeTypeByRuleName.get(model.getName());
        int n = 0;
        {
            sb.append("\n");
            this.appendIndent(sb, indent);
            sb.append("\"_position\": ").append(model.getStartPosition());
            sb.append(",").append("\n");
            this.appendIndent(sb, indent);
            sb.append("\"_type\": \"").append(model.getClass().getName()).append("\"");;
            sb.append(",").append("\n");
            this.appendIndent(sb, indent);
            sb.append("\"_ruleName\": \"").append(model.getName()).append("\"");
            if (typeInfo == null) {
                sb.append(",").append("\n");
                this.appendIndent(sb, indent);
                sb.append("\"_error\": \"").append("syntax model type info not found").append("\"");
            }
            n += 2;
        }
        if (typeInfo != null) {
            for (NodeFieldInfo field : typeInfo.getFields()) {
                Object value = this.getValue(field, model);
                if (n > 0) {
                    sb.append(",");
                }
                sb.append("\n");
                this.appendIndent(sb, indent);
                sb.append("\"").append(field.getName()).append("\"").append(": ");
                if (value == null) {
                    sb.append("null");
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
                        case Object:
                            sb.append("\n");
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
    
    private static <T> int maxValue(T[] arr, ToIntFunction<T> accessor, int def) {
        int result;
        if (arr == null || arr.length == 0) {
            result = def;
        } else {
            result = accessor.applyAsInt(arr[0]);
            for (int i = 1; i < arr.length; i++) {
                result = Math.max(result, accessor.applyAsInt(arr[i]));
            }
        }
        return result;
    }
    
    @FunctionalInterface
    private interface ThrowableFunction<T, R> {
        R apply(T obj) throws Throwable;
    }
    
    private <T, R, E extends Throwable> Function<T, R> captureExceptionInfo(Class<E> exceptionType, ThrowableFunction<T, R> mapper) {
        return o -> {
            try { return mapper.apply(o); }
            catch (Throwable ex) {
                if (exceptionType.isInstance(ex)) {
                    // TODO collect errors
                    ex.printStackTrace();
                    return null;
                } else {
                    throw new RuntimeException(ex);
                }
            }
        };
    }
    
    private void introduceEnum(Class<?> type) throws XPathExpressionException { // TODO collect errors
        SyntaxLiteral literalAnnotation = type.getAnnotation(SyntaxLiteral.class);
        if (literalAnnotation == null) {
            throw new RuntimeException("Type " + type.getName() + " is not marked as syntax ruleName!"); // TODO collect errors
        } else if (!type.isEnum()) {
            throw new RuntimeException("Type " + type.getName() + " is not a enum while marked as syntax literal!"); // TODO collect errors
        }
        
        var existing = literalTypeByRuleName.get(literalAnnotation.name());
        if (existing == null) {
            // var values = type.getEnumConstants();
            var enumEntries = Stream.of(type.getFields()).filter(f -> f.isEnumConstant()).map(captureExceptionInfo(IllegalAccessException.class, f -> new Object() {
               public final Field field = f;
               public final Object value = f.get(null);
               public final String name = f.getName();
               public final String upperCasedName = f.getName().toUpperCase();
               public final SyntaxLiteralCase literalCaseAnnotation = f.getAnnotation(SyntaxLiteralCase.class);
            })).collect(Collectors.toList());
            
            var countOfDefaultCasedNames = enumEntries.stream().map(e -> e.name).collect(Collectors.toSet()).size(); 
            var countOfUpperCasedNames = enumEntries.stream().map(e -> e.upperCasedName).collect(Collectors.toSet()).size();
            var isCaseSensitive = countOfDefaultCasedNames != countOfUpperCasedNames;
            var valuesByName = isCaseSensitive ? enumEntries.stream().collect(Collectors.toMap(e -> e.name, e -> e.value))
                                               : enumEntries.stream().collect(Collectors.toMap(e -> e.upperCasedName, e -> e.value));
            
            var exprByValue = enumEntries.stream()
                    .filter(e -> e.literalCaseAnnotation != null && e.literalCaseAnnotation.xcondition().length() > 0)
                    .collect(Collectors.toMap(
                            e -> e.value, 
                            captureExceptionInfo(XPathExpressionException.class, e -> xpath.compile(e.literalCaseAnnotation.xcondition())),
                            (x, y) -> { throw new IllegalStateException("Duplicated enum values"); /* should never happen*/ },
                            LinkedHashMap::new
                    ));
            var stringExpr = literalAnnotation.xstring().length() > 0 ? xpath.compile(literalAnnotation.xstring()) : null; // TODO collect errors
            literalTypeByRuleName.put(literalAnnotation.name(), new LiteralTypeInfo(literalAnnotation.name(), type, stringExpr, exprByValue, valuesByName, isCaseSensitive));
        } else if (!existing.type.equals(type)) {
            throw new RuntimeException("Ambiguous syntax literal: both " + type.getName() + " and " + existing.type.getName() + "  are marked with the same name " + literalAnnotation.name()); // TODO collect errors
        } else {
            // already registered, so nothing 
        }
    }

    public <T extends AbstractSyntaxNode> void introduce(Class<T> modelType) {        
        List<String> errors = new LinkedList<>();
        
        Set<Class<? extends AbstractSyntaxNode>> processedTypes = new HashSet<>();
        
        Queue<Class<? extends AbstractSyntaxNode>> queue = new LinkedList<>();
        queue.add(modelType);
        while (!queue.isEmpty()) {
            Class<? extends AbstractSyntaxNode> type = queue.remove();
            if (!processedTypes.add(type)) {
                continue;
            }
            SyntaxNode ruleAnnotation = type.getAnnotation(SyntaxNode.class);
            if (ruleAnnotation == null) {
                errors.add("Type " + type.getName() + " is not marked as syntax ruleName!");
                continue;
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
                FieldTypeKind kind = FieldTypeKind.resolveModelFieldKind(field.info.getType());
                if (field.subnodeSpecs.length > 0 && kind.isTerm) {
                    throw new RuntimeException("Field of terminal value kind cannot be bound with complex subnode type");
                }
                
                List<XPathExpression> termExprs = new ArrayList<>(field.termSpecs.length);
                List<SubnodeInfo> subnodeExprs = new ArrayList<>(field.subnodeSpecs.length);
                for (var termSpec : field.termSpecs) {
                    try {
                        termExprs.add(xpath.compile(termSpec.xpath()));
                        if (field.info.getType().isEnum()) {
                            introduceEnum(field.info.getType());
                        }
                    } catch (XPathExpressionException e) {
                        throw new RuntimeException(e);
                    }
                }
                for (var subnodeSpec : field.subnodeSpecs) {
                    Class fieldType = subnodeSpec.type() == null  || subnodeSpec.type().equals(AbstractSyntaxNode.class) 
                        ? field.info.getType() : subnodeSpec.type();
                    try {
                        XPathExpression scopeExpr = subnodeSpec.xpath() != null && subnodeSpec.xpath().length() > 0 ? xpath.compile(subnodeSpec.xpath()) : null;  
                        subnodeExprs.add(new SubnodeInfo(scopeExpr, fieldType, subnodeSpec.lookup()));
                        queue.add(fieldType);
                    } catch (XPathExpressionException e) {
                        throw new RuntimeException(e);
                    }
                }
                
                modelFields.put(field.info.getName(), new NodeFieldInfo(kind, field.info, termExprs, subnodeExprs));
            }
            
            Constructor<? extends AbstractSyntaxNode> ctor;
            try {
                ctor = type.getConstructor();
            } catch (Throwable ex) {
                errors.add("Failed to resolve default contructor for syntax model type " + type.getName());
                continue;
            }

            String ruleName = ruleAnnotation.name() != null && ruleAnnotation.name().length() > 0 ? ruleAnnotation.name() : type.getName();
            nodeTypeByRuleName.put(ruleName, new NodeTypeInfo(ruleName, type, ctor, modelFields));
        }
    }
}
