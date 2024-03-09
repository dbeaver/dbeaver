///*
// * DBeaver - Universal Database Manager
// * Copyright (C) 2010-2024 DBeaver Corp and others
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *     http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//package org.jkiss.dbeaver.model.sql.analyzer;
//
//import org.jkiss.code.NotNull;
//import org.jkiss.dbeaver.Log;
//import org.jkiss.dbeaver.model.lsm.LSMAnalyzer;
//import org.jkiss.dbeaver.model.lsm.sql.dialect.LSMDialectRegistry;
//import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
//import org.jkiss.dbeaver.model.sql.SQLScriptElement;
//import org.jkiss.dbeaver.model.sql.completion.SQLCompletionRequest;
//import org.jkiss.dbeaver.model.sql.semantics.SQLDocumentSyntaxContext;
//import org.jkiss.dbeaver.model.sql.semantics.SQLDocumentSyntaxContext.ScriptItemAtOffset;
//import org.jkiss.dbeaver.model.sql.semantics.SQLQueryModelRecognizer;
//import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryDataContext;
//import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryDataContext.KnownSourcesInfo;
//import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryPureResultTupleContext;
//import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryResultTupleContext.SQLQueryResultColumn;
//import org.jkiss.dbeaver.model.sql.semantics.context.SQLQuerySyntaxContext;
//import org.jkiss.dbeaver.model.sql.semantics.model.SQLQueryNodeModel;
//import org.jkiss.dbeaver.model.sql.semantics.model.SQLQuerySelectionModel;
//import org.jkiss.dbeaver.model.stm.*;
//import org.jkiss.utils.CommonUtils;
//
//import java.io.StringReader;
//import java.util.Collections;
//import java.util.List;
//import java.util.Map;
//import java.util.Map.Entry;
//import java.util.TreeMap;
//import java.util.stream.Collectors;
//
///**
// * Internal implementation of references analyzer
// */
//public class TableReferencesAnalyzerNew implements TableReferencesAnalyzer {
//
//    private static final Log log = Log.getLog(TableReferencesAnalyzerNew.class);
//
//    private Map<String, String> tableReferences = null;
//    private final SQLCompletionRequest request;
//    private final DBRProgressMonitor monitor;
//
//    public TableReferencesAnalyzerNew(@NotNull SQLCompletionRequest request, DBRProgressMonitor monitor) {
//        this.request = request;
//        this.monitor = monitor;
//    }
//    
//    private void prepareTableReferencesIfNeeded() {
//        if (tableReferences == null) {
//            SQLQuerySelectionModel queryModel = null;
//            int offset = this.request.getDocumentOffset();
//            
//            SQLDocumentSyntaxContext docSyntaxContext = this.request.getContext().getSyntaxContext();
//            if (docSyntaxContext != null) {
//                ScriptItemAtOffset scriptItemAtOffset = docSyntaxContext.findScriptItem(offset); 
//                if (scriptItemAtOffset != null) {
//                    queryModel = scriptItemAtOffset.item.getQueryModel();
//                    offset -= scriptItemAtOffset.offset;
//                }
//            }
//            
//            if (queryModel == null){
//                SQLQueryModelRecognizer recognizer = new SQLQueryModelRecognizer(this.request.getContext().getExecutionContext(), true);
//                SQLScriptElement activeQuery = this.request.getActiveQuery();
//                queryModel = recognizer.recognizeQuery(activeQuery.getOriginalText(), this.monitor);
//                offset -= activeQuery.getOffset();
//            }
//            
//            if (queryModel != null && queryModel.getResultSource() != null) {
//                SQLQueryNodeModel modelNode = queryModel.findNodeContaining(offset);
//                if (modelNode != null) {
//                    SQLQueryDataContext queryContext = modelNode.getDataContext();
//                    System.out.println("Columns: {");
//                    for (SQLQueryResultColumn column: queryContext.getColumnsList()) {
//                        System.out.println("  " + column.symbol.getName());
//                    }
//                    System.out.println("}");
//                    if (queryContext != null) {
//                        KnownSourcesInfo sources = queryContext.getKnwonSources();
//                        this.tableReferences = sources.obtainAliasesByTableName();
//                    }
//                }
//            }
//            
//            if (this.tableReferences == null) {
//                this.tableReferences = Collections.emptyMap();
//            }
//        }
//        
//        System.out.println("Known table refs {");
//        for (Entry<String, String> kv: this.tableReferences.entrySet()) {
//            System.out.println("  " + kv.getKey() + " as " + kv.getValue());
//        }
//        System.out.println("}");
//    }
//
//    @NotNull
//    @Override
//    public Map<String, String> getFilteredTableReferences(@NotNull String tableAlias, boolean allowPartialMatch) {
//        Map<String, String> result;
//        this.prepareTableReferencesIfNeeded();
//        if (CommonUtils.isNotEmpty(tableAlias) && this.tableReferences.size() > 0) {
//            result = this.tableReferences.entrySet().stream()
//                .filter(r -> allowPartialMatch
//                    ? r.getValue() != null && CommonUtils.startsWithIgnoreCase(r.getValue(), tableAlias)
//                    : r.getValue() != null && r.getValue().equalsIgnoreCase(tableAlias))
//                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
//        } else {
//            result = this.tableReferences;
//        }
//        return result;
//    }
//
//    @NotNull
//    @Override
//    public Map<String, String> getTableAliasesFromQuery() {
//        try {
//            this.prepareTableReferencesIfNeeded();
//        } catch (Exception e) {
//            log.error(e.getMessage(), e);
//        }
//        return this.tableReferences;
//    }
//}
