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

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.lsm.mapping.AbstractSyntaxNode.BindingInfo;
import org.jkiss.dbeaver.model.lsm.mapping.internal.*;
import org.w3c.dom.Node;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.stream.Collectors;
import javax.xml.xpath.XPathEvaluationResult;
import javax.xml.xpath.XPathEvaluationResult.XPathResultType;
import javax.xml.xpath.XPathException;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathNodes;

public class SyntaxModelMappingSession {
    
    private final ModelErrorsCollection errors = new ModelErrorsCollection();
    private final SyntaxModel modelInfo;
    
    public SyntaxModelMappingSession(SyntaxModel modelInfo) {
        this.modelInfo = modelInfo;
    }
    
    private static boolean isEmptyValue(XPathEvaluationResult<?> xvalue) {
        Object value = xvalue.value();
        if (value == null) {
            return true;
        }
        switch (xvalue.type()) {
            case BOOLEAN:
            case NUMBER:
            case STRING:
                if (value instanceof String) {
                    return ((String) value).length() < 1;
                }
                break;
            case ANY:
            case NODE:
            case NODESET:
                if (value instanceof XPathNodes) {
                    XPathNodes nodes = (XPathNodes) value;
                    return nodes.size() < 1;
                }
                break;
            default: throw new UnsupportedOperationException("Unexpected xpath value type " + xvalue.type());
        }
        return false;
    }

    private static XTreeNodeBase tryGetNode(XPathEvaluationResult<?> xvalue) throws XPathException {
        Object value = xvalue.value();
        if (value instanceof XTreeNodeBase) {
            return (XTreeNodeBase) value;
        } else if (value instanceof XPathNodes) {
            XPathNodes nodes = (XPathNodes) value;
            if (nodes.size() == 1) {
                return (XTreeNodeBase) nodes.get(0);
            }
        }
        return null;
    }
    
    private AbstractSyntaxNode instantiateAndFill(@NotNull NodeTypeInfo typeInfo, @NotNull XTreeNodeBase nodeInfo) {
        try {
            if (nodeInfo.getModel() == null) {
                AbstractSyntaxNode model = typeInfo.ctor.newInstance();
                model.setAstNode(nodeInfo);
                nodeInfo.setModel(model);
            }
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
            errors.add(ex, "Failed to instantiate syntax model node of type " + typeInfo.type.getName());
            return null;
        }

        Set<XTreeNodeBase> subnodes = new HashSet<>(5);
        for (var field : typeInfo.fields.values()) {
            subnodes.clear();
            for (var expr : field.termExprs) {
                try {
                    XPathEvaluationResult<?> value = expr.evaluateExpression(nodeInfo);
                    if (!isEmptyValue(value)) {
                        this.bindValue(nodeInfo, field, value);
                        XTreeNodeBase valueNode = tryGetNode(value);
                        if (valueNode != null) {
                            nodeInfo.getModel().appendBinding(new BindingInfo(field, null, valueNode));
                        }
                    }
                } catch (XPathException e) {
                    errors.add(e, "Failed to evaluate syntax model term expression for field "
                        + field.getFieldName() + " of type " + field.getDeclaringClassName());
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
                                    if (scopeSubnode instanceof XTreeNodeBase) {
                                    	var subnodeTypeInfo = subnodeInfo.getNodeTypeInfo();
                                        mapSubtrees((XTreeNodeBase) scopeSubnode, subnodeTypeInfo, true, tryDescedants, subnodes);
                                    }
                                }
                            } else if (scopeOrSubnode.type() == XPathResultType.NODE && scopeOrSubnode.value() instanceof XTreeNodeBase) {
                                var subnodeTypeInfo = subnodeInfo.getNodeTypeInfo();
                                mapSubtrees((XTreeNodeBase) scopeOrSubnode.value(), subnodeTypeInfo, true, tryDescedants, subnodes);
                            }
                        } else {
                            if (tryDescedants) {
                                mapSubtrees(nodeInfo, subnodeInfo.getNodeTypeInfo(), false, true, subnodes);
                            } else {
                                for (var candidateSubnode : nodeInfo.getSubnodes().getCollection()) {
                                    mapSubtrees(candidateSubnode, subnodeInfo.getNodeTypeInfo(), true, false, subnodes);
                                }
                            }
                        }
                    } catch (XPathExpressionException e) {
                        errors.add(e, "Failed to evaluate syntax model subnode scope expression for subnode "
                            + subnodeInfo.subnodeType.getName() + " of field "
                            + field.getFieldName() + " of type " + field.getDeclaringClassName());
                    }
                }
                List<XTreeNodeBase> orderedSubnodes = subnodes.stream()
                    .sorted(Comparator.comparingInt(a -> a.getModel().getStartPosition()))
                    .collect(Collectors.toList());
                this.bindValue(nodeInfo, field, orderedSubnodes);
                orderedSubnodes.forEach(n -> nodeInfo.getModel().appendBinding(new BindingInfo(field, n.getModel(), n)));
            } else {
                for (var subnodeInfo : field.subnodesInfo) {
                    boolean tryDescedants = subnodeInfo.lookupMode == SyntaxSubnodeLookupMode.DEPTH_FIRST;
                    try {
                        AbstractSyntaxNode subnode = null;
                        if (subnodeInfo.scopeExpr != null) {
                            XPathEvaluationResult<?> scopeOrSubnode = subnodeInfo.scopeExpr.evaluateExpression(nodeInfo);
                            if (scopeOrSubnode.type() == XPathResultType.NODESET && scopeOrSubnode.value() instanceof XPathNodes) {
                                for (var scopeSubnode : (XPathNodes) scopeOrSubnode.value()) {
                                    if (scopeSubnode instanceof XTreeNodeBase) {
                                        var subnodeTypeInfo = subnodeInfo.getNodeTypeInfo();
                                        subnode = mapSubtree((XTreeNodeBase) scopeSubnode, subnodeTypeInfo, true, tryDescedants);
                                        if (subnode != null) {
                                            break;
                                        }
                                    }
                                }
                            } else if (scopeOrSubnode.type() == XPathResultType.NODE && scopeOrSubnode.value() instanceof XTreeNodeBase) {
                                var subnodeTypeInfo = subnodeInfo.getNodeTypeInfo();
                                subnode = mapSubtree((XTreeNodeBase) scopeOrSubnode.value(), subnodeTypeInfo, true, tryDescedants);
                            }
                        } else {
                            if (tryDescedants) {
                                subnode = mapSubtree(nodeInfo, subnodeInfo.getNodeTypeInfo(), false, true);
                            } else {
                                for (var candidateSubnode : nodeInfo.getSubnodes().getCollection()) {
                                    mapSubtrees(candidateSubnode, subnodeInfo.getNodeTypeInfo(), true, false, subnodes);
                                }
                            }
                        }
                        if (subnode != null) {
                            this.bindRawValue(nodeInfo, field, subnode);
                            nodeInfo.getModel().appendBinding(new BindingInfo(field, subnode, subnode.getAstNode()));
                            break;
                        }
                    } catch (XPathExpressionException e) {
                        errors.add(e, "Failed to evaluate syntax model subnode scope expression for subnode "
                            + subnodeInfo.subnodeType.getName() + " of field " + field.getFieldName()
                            + " of type " + field.getDeclaringClassName());
                    }
                }
            }
        }
        
        return nodeInfo.getModel(); 
    }   
    
    private Object mapLiteralValue(XTreeNodeBase nodeInfo, LiteralTypeInfo typeInfo) {

        if (typeInfo != null) {
            try {
                String str = typeInfo.stringExpr == null ? nodeInfo.getTextContent()
                    : typeInfo.stringExpr.evaluateExpression(nodeInfo, String.class);
                if (str != null && str.length() > 0) {
                    // System.out.println(str + " | " + nodeInfo.getNodeValue());
                    Object value = typeInfo.valuesByName.get(typeInfo.isCaseSensitive ? str : str.toUpperCase());
                    return value;
                }
            } catch (XPathExpressionException e) {
                errors.add(e, "Failed to evaluate syntax model literal expression of type " + typeInfo.type.getName());
            }
            
            for (var literalCase : typeInfo.exprByValue.entrySet()) {
                try {
                    Object value = literalCase.getValue().evaluateExpression(nodeInfo, Boolean.class);
                    if (Boolean.TRUE.equals(value)) {
                        return literalCase.getKey();
                    }
                } catch (XPathExpressionException e) {
                    errors.add(e, "Failed to evaluate syntax model literal case condition expression for case " + literalCase.getKey()
                        + " of type " + typeInfo.type.getName());
                }
            }
        }

        return null;
    }

    private void bindValue(XTreeNodeBase nodeInfo, NodeFieldInfo fieldInfo, List<XTreeNodeBase> subnodes) {
        this.bindValue(nodeInfo, fieldInfo, new XPathEvaluationResult<XPathNodes>() {
            @Override
            public XPathResultType type() {
                return XPathResultType.NODESET;
            }

            @Override
            public XPathNodes value() {
                return new XPathNodes() {
                    @Override
                    public int size() {
                        return subnodes.size();
                    }

                    @SuppressWarnings("unchecked")
                    @Override
                    public Iterator<Node> iterator() {
                        return (Iterator<Node>) (Iterator<?>) subnodes.iterator();
                    }

                    @Override
                    public Node get(int index) {
                        return subnodes.get(index);
                    }
                };
            }
        });      
    }

    private void bindValue(XTreeNodeBase nodeInfo, NodeFieldInfo fieldInfo, XPathEvaluationResult<?> xvalue) {
        Object value;
        switch (fieldInfo.kind) {
            case Object:
            case Array:
            case List:
                switch (xvalue.type()) {
                    case NODE:
                    case NODESET:
                        value = xvalue.value();
                        break;
                    default:
                        throw new UnsupportedOperationException("Not supported");
                }
                break;
            case LiteralList:
                switch (xvalue.type()) {
                    case STRING:
                    case ANY:
                    case NODE:
                    case NODESET:
                        value = xvalue.value();
                        break;
                    default:
                        throw new UnsupportedOperationException("Not supported value type for binding: " + xvalue.type());
                }
                break;
            case String:
                value = getScalarString(fieldInfo, xvalue);
                break;
            case Byte:
                value = Byte.parseByte(Objects.requireNonNull(getScalarString(fieldInfo, xvalue)));
                break;
            case Short:
                value = Short.parseShort(Objects.requireNonNull(getScalarString(fieldInfo, xvalue)));
                break;
            case Int:
                value = Integer.parseInt(Objects.requireNonNull(getScalarString(fieldInfo, xvalue)));
                break;
            case Long:
                value = Long.parseLong(Objects.requireNonNull(getScalarString(fieldInfo, xvalue)));
                break;
            case Bool:
                value = Boolean.parseBoolean(getScalarString(fieldInfo, xvalue));
                break;
            case Float:
                value = Float.parseFloat(Objects.requireNonNull(getScalarString(fieldInfo, xvalue)));
                break;
            case Double:
                value = Double.parseDouble(Objects.requireNonNull(getScalarString(fieldInfo, xvalue)));
                break;
            case Enum:
                switch (xvalue.type()) {
                    case NODE:
                        value = mapLiteralValue((XTreeNodeBase) xvalue.value(), fieldInfo.getLiteralTypeInfo());
                        break;
                    case NODESET:
                        XPathNodes nodes = (XPathNodes) xvalue.value();
                        if (nodes.size() == 1) {
                            try {
                                value = mapLiteralValue((XTreeNodeBase) nodes.get(0), fieldInfo.getLiteralTypeInfo());
                            } catch (XPathException e) {
                                errors.add(e, "Failed to bind raw value to field " + fieldInfo.getFieldName()
                                        +  " of type " + fieldInfo.getDeclaringClassName());
                                value = null;
                            }
                        } else {
                            value = null;
                        }
                        break;
                    default:
                        throw new UnsupportedOperationException("Not supported");
                }
                break;
            default: throw new UnsupportedOperationException("Unexpected syntax model field kind " + fieldInfo.kind);
        }

        if (value != null) {
            this.bindRawValue(nodeInfo, fieldInfo, value);
        }
    }

    @Nullable
    private String getScalarString(@NotNull NodeFieldInfo fieldInfo,  @NotNull XPathEvaluationResult<?> xvalue) {
        try {
            switch (xvalue.type()) {
                case NODE:
                    XTreeNodeBase nodeInfo = (XTreeNodeBase) xvalue.value();
                    return nodeInfo.getNodeValue();
                case NODESET:
                    XPathNodes nodes = (XPathNodes) xvalue.value();
                    int count = nodes.size();
                    if (count == 0) {
                        return null;
                    } else if (count == 1) {
                        return nodes.get(0).getNodeValue();
                    } else {
                        errors.add("Ambiguous resolution of syntax model value expression for field " + fieldInfo.getFieldName()
                            + " of type " + fieldInfo.getDeclaringClassName());
                        return nodes.get(0).getNodeValue();
                    }
                default:
                    if (xvalue.value() != null) {
                        return xvalue.value().toString();
                    } else {
                        return null;
                    }
            }
        } catch (XPathException ex) {
            errors.add(ex, "Failed to evaluate syntax model scalar value expression for field " + fieldInfo.getFieldName()
                + " of type " + fieldInfo.getDeclaringClassName());
            return null;
        }
    }

    
    private void bindRawValue(@NotNull XTreeNodeBase nodeInfo, @NotNull NodeFieldInfo fieldInfo, @NotNull Object value) {
        try {
            this.bindRawValueImpl(nodeInfo, fieldInfo, value);
        } catch (IllegalArgumentException | IllegalAccessException ex) {
            errors.add(ex, "Failed to bind raw value to field " + fieldInfo.getFieldName()
                +  " of type " + fieldInfo.getDeclaringClassName());
        }
    }
    
    private void bindRawValueImpl(
        @NotNull XTreeNodeBase nodeInfo,
        @NotNull NodeFieldInfo fieldInfo,
        @Nullable Object value
    ) throws IllegalArgumentException, IllegalAccessException {
        switch (fieldInfo.kind) {
            case Object: {
                if (value instanceof XTreeNodeBase) {
                    XTreeNodeBase subnodeInfo = (XTreeNodeBase) value;
                    if (subnodeInfo.getModel() != null) {
                        fieldInfo.setValue(nodeInfo.getModel(), subnodeInfo.getModel());
                    }
                } else {
                    fieldInfo.setValue(nodeInfo.getModel(), value);
                }
                break;
            }
            case Array: { // TODO
                /* int index;
                Object newArr, oldArr = fieldInfo.info.get(nodeInfo.model);
                Class<?> itemType = fieldInfo.info.getType().getComponentType();
                if (value != null) {
                    index = Array.getLength(oldArr);
                    newArr = Array.newInstance(itemType, index + 1);
                    System.arraycopy(oldArr, 0, newArr, 0, index);
                } else {
                    index = 0;
                    newArr = Array.newInstance(itemType, 1);
                }
                Array.set(newArr, index, value);
                fieldInfo.info.set(nodeInfo.model, newArr);
                break;*/
                throw new UnsupportedOperationException("Arrays binding for syntax model is not implemented yet");
            }
            case List: {
                @SuppressWarnings("unchecked")
                List<Object> list = (List<Object>) fieldInfo.getValue(nodeInfo.getModel());
                if (list == null) {
                    fieldInfo.setValue(nodeInfo.getModel(), list = new ArrayList<>());
                } else {
                    list.clear();
                }
                if (value instanceof XPathNodes) {
                    XPathNodes nodes = (XPathNodes) value;
                    if (list instanceof ArrayList<?>) {
                        ((ArrayList<?>) list).ensureCapacity(nodes.size());
                    }
                    for (var xnode : nodes) {
                        if (xnode instanceof XTreeNodeBase) {
                            XTreeNodeBase subnodeInfo = (XTreeNodeBase) xnode;
                            if (subnodeInfo.getModel() != null) {
                                list.add(subnodeInfo.getModel());
                            }
                        }
                    }
                } else if (value instanceof XTreeNodeBase) {
                    XTreeNodeBase subnodeInfo = (XTreeNodeBase) value;
                    if (subnodeInfo.getModel() != null) {
                        list.add(subnodeInfo.getModel());
                    }
                } else {
                    list.add(value);
                }
                break;
            }
            case LiteralList: {
                @SuppressWarnings("unchecked")
                List<Object> list = (List<Object>) fieldInfo.getValue(nodeInfo.getModel());
                if (list == null) {
                    fieldInfo.setValue(nodeInfo.getModel(), list = new ArrayList<>());
                } else {
                    list.clear();
                }
                if (value instanceof String) {
                    list.add(value);
                } else if (value instanceof XPathNodes) {
                    XPathNodes nodes = (XPathNodes) value;
                    if (list instanceof ArrayList<?>) {
                        ((ArrayList<?>) list).ensureCapacity(nodes.size());
                    }
                    for (var xnode : nodes) {
                        if (xnode instanceof XTreeNodeBase) {
                            XTreeNodeBase subnodeInfo = (XTreeNodeBase) xnode;
                            list.add(subnodeInfo.getNodeValue());
                        }
                    }
                } else if (value instanceof XTreeNodeBase) {
                    XTreeNodeBase subnodeInfo = (XTreeNodeBase) value;
                    list.add(subnodeInfo.getNodeValue());
                } else {
                    list.add(value);
                }
                break;
            }
            default:
                fieldInfo.setValue(nodeInfo.getModel(), value);
                break;
        }
    }
    
    private void mapSubtrees(
        @NotNull XTreeNodeBase nodeInfo,
        @NotNull NodeTypeInfo typeInfo, 
        boolean tryExact,
        boolean tryDescedants,
        @NotNull Set<XTreeNodeBase> subnodes
    ) {

        if (typeInfo != null) {
            if (tryExact && nodeInfo.getLocalName().equals(typeInfo.ruleName)) {
                if (subnodes.add(nodeInfo)) {
                    this.instantiateAndFill(typeInfo, nodeInfo);
                }
                return;
            }
            
            if (tryDescedants) {
                NodesList<XTreeNodeBase> childNodes = nodeInfo.findDescedantLayerByName(typeInfo.ruleName);
                for (XTreeNodeBase childNode : childNodes) {
                    if (subnodes.add(childNode)) {
                        this.instantiateAndFill(typeInfo, childNode);
                    }
                }
            }
        }
    }
    
    private AbstractSyntaxNode mapSubtree(XTreeNodeBase nodeInfo, NodeTypeInfo typeInfo,  boolean tryExact, boolean tryDescedants) {

        if (typeInfo != null) {
            if (tryExact && nodeInfo.getLocalName().equals(typeInfo.ruleName)) {
                return this.instantiateAndFill(typeInfo, nodeInfo);
            }
            
            if (tryDescedants) {
                XTreeNodeBase childNode = nodeInfo.findFirstDescedantByName(typeInfo.ruleName);
                if (childNode != null) {
                    return this.instantiateAndFill(typeInfo, childNode);
                }
            }
        }
        return null;
    }

    public <T extends AbstractSyntaxNode> SyntaxModelMappingResult<T> map(XTreeNodeBase rootInfo, Class<T> type) {
        NodeTypeInfo typeInfo = modelInfo.findNodeTypeInfo(type);
        if (typeInfo == null) {
            errors.add("Failed to find syntax model node info for type " + type.getName() + ". Consider introducing it to the model.");
        }
        AbstractSyntaxNode modelNode = this.mapSubtree(rootInfo, typeInfo, true, true);
        if (modelNode != null) {
            @SuppressWarnings("unchecked")
            T result = (T) modelNode;
            return new SyntaxModelMappingResult<>(errors, result);
        } else {
            return new SyntaxModelMappingResult<>(errors, null);
        }    
    }
}
