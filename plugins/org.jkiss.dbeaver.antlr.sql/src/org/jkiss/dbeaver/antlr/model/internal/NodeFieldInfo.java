package org.jkiss.dbeaver.antlr.model.internal;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.xml.xpath.XPathEvaluationResult;
import javax.xml.xpath.XPathException;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathNodes;
import javax.xml.xpath.XPathEvaluationResult.XPathResultType;

import org.jkiss.dbeaver.antlr.model.AbstractSyntaxNode;
import org.jkiss.dbeaver.antlr.model.SyntaxSubnodeLookupMode;
import org.w3c.dom.Node;

public class NodeFieldInfo {

    public static class SubnodeInfo {
        public final XPathExpression scopeExpr;
        public final Class<? extends AbstractSyntaxNode> subnodeType;
        public final SyntaxSubnodeLookupMode lookupMode;
        
        public SubnodeInfo(XPathExpression scopeExpr, Class<? extends AbstractSyntaxNode> subnodeType, SyntaxSubnodeLookupMode lookupMode) {
            this.scopeExpr = scopeExpr;
            this.subnodeType = subnodeType;
            this.lookupMode = lookupMode;
        }
    }
    
    public final FieldTypeKind kind;
    public final Field info;
    public final List<XPathExpression> termExprs;
    public final List<SubnodeInfo> subnodesInfo;
    
    public NodeFieldInfo(FieldTypeKind kind, Field info, List<XPathExpression> termExprs, List<SubnodeInfo> subnodesInfo) {
        this.kind = kind;
        this.info = info;
        this.termExprs = Collections.unmodifiableList(termExprs);
        this.subnodesInfo = Collections.unmodifiableList(subnodesInfo);
    }
    
    public String getName() {
        return this.info.getName();
    }
}
