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
package org.jkiss.dbeaver.model.lsm.mapping.internal;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.xml.namespace.QName;
import javax.xml.xpath.*;

public class XFunctionResolver implements XPathFunctionResolver {

    @FunctionalInterface
    private interface MyXPathFunction {
        @Nullable
        Object evaluate(@NotNull List<?> args) throws XPathExpressionException;
    }

    @NotNull
    private static java.util.Map.Entry<String, XPathFunction> xfunction(@NotNull String name, @Nullable MyXPathFunction impl) {
        return java.util.Map.entry(name, args -> {
            try {
                if (impl == null) {
                    throw new IllegalArgumentException("Can't evaluate function " + name);
                }
                return impl.evaluate(args);
            } catch (XPathExpressionException ex) {
                throw new XPathFunctionException(ex);
            }
        });
    }

    @NotNull
    private final Map<String, XPathFunction> functionByName = Map.ofEntries(
        xfunction("echo", args -> {
            for (Object o : args) {
                if (o instanceof NodeList) {
                    NodeList nodeList = (NodeList) o;
                    if (nodeList.getLength() == 0) {
                        System.out.println("[]");
                    } else {
                        System.out.println(
                            CustomXPathUtils.streamOf(nodeList).map(n -> "  " + n.getLocalName() + ": \"" + n.getNodeValue() + "\"")
                                .collect(Collectors.joining(",\n", "[\n", "\n]"))
                        );
                    }
                } else if (o instanceof Node) {
                    Node node = (Node) o;
                    System.out.println(node.getLocalName() + ": \"" + node.getNodeValue() + "\"");
                } else {
                    System.out.println(o);
                }
            }
            return args.size() > 0 ? args.get(0) : null;
        }),
        xfunction("rootOf", args -> {
            if (args.size() > 0 && args.get(0) instanceof NodeList) {
                NodeList nodeList = (NodeList) args.get(0);
                if (nodeList.getLength() > 0) {
                    Node node = nodeList.item(0);
                    while (node.getParentNode() != null) {
                        node = node.getParentNode();
                    }
                    return node;
                } else {
                    return null;
                }
            } else {
                return null;
            }
        }),
        xfunction("flatten", args -> {
            final String signatureDescr = "flatten(roots:NodeList, stepExpr:String, justLeaves:bool = false, incudeRoot:bool = ture)";
            if (args.size() < 2) {
                throw new XPathFunctionException("At least two arguments required for " + signatureDescr);
            } else if (args.size() > 4) {
                throw new XPathFunctionException("No more than four arguments required for " + signatureDescr);
            } else {
                NodeList roots = (NodeList) args.get(0);
                XPathExpression stepExpr = prepareExpr(args.get(1).toString());
                boolean justLeaves = args.size() > 2 ? (Boolean) args.get(2) : false;
                boolean includeRoot = args.size() > 3 ? (Boolean) args.get(3) : true;
                
                NodesList<Node> result = new NodesList<>(); 
                if (includeRoot && !justLeaves) {
                    result.ensureCapacity(roots.getLength());
                }

                for (Node root : CustomXPathUtils.iterableOf(roots)) {
                    if (includeRoot && !justLeaves) {
                        result.add(root);
                    } else {
                        CustomXPathUtils.flattenExclusiveImpl(root, stepExpr, justLeaves, result);
                    }
                }
                
                return result;
            }
        }),
        xfunction("joinStrings", args -> {
            final String signatureDescr = "joinStrings(separator:String, nodes...:NodeList)";
            if (args.size() < 2) {
                throw new XPathFunctionException("At least two arguments required for " + signatureDescr);
            } else {
                StringBuilder sb = new StringBuilder();
                String separator = args.get(0).toString();
                int count = 0;
                for (int i = 1; i < args.size(); i++) {
                    for (Node node : CustomXPathUtils.iterableOf((NodeList) args.get(i))) {
                        if (count > 0) {
                            sb.append(separator);
                        }
                        sb.append(node.getTextContent());
                        count++;
                    }
                }
                return sb.toString();
            }
        })
    );
    
    private final XPath xpath;
    private final Map<String, XPathExpression> exprs = new HashMap<>();
    
    public XFunctionResolver(XPath xpath) {
        this.xpath = xpath;
    }
    
    private XPathExpression prepareExpr(String exprStr) throws XPathExpressionException {
        XPathExpression expr = exprs.get(exprStr);
        if (expr == null) {
            expr = xpath.compile(exprStr); 
            exprs.put(exprStr, expr);
        }
        return expr;
    }
    
    @Override
    public XPathFunction resolveFunction(QName functionName, int arity) {
        return functionByName.get(functionName.getLocalPart());
    }
    
}
