package org.jkiss.dbeaver.model.lsm.interfaces;

import java.util.Collection;
import java.util.concurrent.Future;

import org.jkiss.dbeaver.model.lsm.mapping.AbstractSyntaxNode;

public interface LSMDialect {
    
    Collection<LSMAnalysisCase<? extends LSMNode, ? extends AbstractSyntaxNode>> getSupportedCases();

    <T extends LSMNode> LSMAnalysisCase<T, ? extends AbstractSyntaxNode> findAnalysisCase(Class<T> expectedModelType);
    
    <T extends LSMNode> Future<LSMAnalysis<T>> prepareAnalysis(LSMSource source, LSMAnalysisCase<T, ? extends AbstractSyntaxNode> analysisCase);
    
}
