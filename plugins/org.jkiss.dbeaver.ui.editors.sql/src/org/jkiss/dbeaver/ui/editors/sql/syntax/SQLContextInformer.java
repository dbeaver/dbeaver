/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.editors.sql.syntax;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.swt.widgets.Display;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.app.DBPDataSourceRegistry;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCExecutionContextDefaults;
import org.jkiss.dbeaver.model.impl.DBObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.impl.struct.DirectObjectReference;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.model.sql.SQLSyntaxManager;
import org.jkiss.dbeaver.model.sql.parser.SQLIdentifierDetector;
import org.jkiss.dbeaver.model.sql.parser.SQLWordPartDetector;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.runtime.jobs.DataSourceJob;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditorBase;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

import java.util.*;


/**
 * SQLContextInformer
 */
public class SQLContextInformer
{
    private static final Log log = Log.getLog(SQLContextInformer.class);

    private static final Map<String, Map<String, ObjectLookupCache>> LINKS_CACHE = new HashMap<>();

    private final SQLEditorBase editor;
    private SQLSyntaxManager syntaxManager;
    private SQLIdentifierDetector.WordRegion wordRegion;

    private String[] keywords;
    private DBPKeywordType keywordType;
    private List<DBSObjectReference> objectReferences;

    private static class ObjectLookupCache {
        List<DBSObjectReference> references;
        boolean loading = true;
    }

    public SQLContextInformer(SQLEditorBase editor, SQLSyntaxManager syntaxManager)
    {
        this.editor = editor;
        this.syntaxManager = syntaxManager;
    }

    public SQLEditorBase getEditor() {
        return editor;
    }

    public void refresh(SQLSyntaxManager syntaxManager) {
        this.syntaxManager = syntaxManager;
        DBPDataSource dataSource = editor.getDataSource();
        if (dataSource != null) {
            synchronized (LINKS_CACHE) {
                LINKS_CACHE.remove(dataSource.getContainer().getId());
            }
        }

    }

    public SQLIdentifierDetector.WordRegion getWordRegion() {
        return wordRegion;
    }

    public String[] getKeywords() {
        return keywords;
    }

    public DBPKeywordType getKeywordType() {
        return keywordType;
    }

    public synchronized List<DBSObjectReference> getObjectReferences() {
        return objectReferences;
    }

    public synchronized boolean hasObjects() {
        return !CommonUtils.isEmpty(objectReferences);
    }

    public void searchInformation(IRegion region)
    {
        ITextViewer textViewer = editor.getTextViewer();
        final DBCExecutionContext executionContext = editor.getExecutionContext();
        if (region == null || textViewer == null || executionContext == null) {
            return;
        }

        IDocument document = textViewer.getDocument();
        if (document == null) {
            return;
        }

        SQLWordPartDetector wordDetector = new SQLWordPartDetector(document, syntaxManager, region.getOffset());
        wordRegion = wordDetector.detectIdentifier(document, region);

        if (wordRegion.word.length() == 0) {
            return;
        }

        String fullName = wordRegion.identifier;
        String tableName = wordRegion.word;
        boolean caseSensitive = false;
        if (wordDetector.isQuoted(tableName)) {
            tableName = DBUtils.getUnQuotedIdentifier(tableName, syntaxManager.getIdentifierQuoteStrings());
            caseSensitive = true;
        }
        String[] containerNames = null;
        if (!CommonUtils.equalObjects(fullName, tableName)) {
            int divPos = fullName.indexOf(syntaxManager.getStructSeparator());
            if (divPos != -1) {
                String[] parts = wordDetector.splitIdentifier(fullName);
                tableName = parts[parts.length - 1];
                containerNames = ArrayUtils.remove(String.class, parts, parts.length - 1);
                for (int i = 0; i < containerNames.length; i++) {
                    if (wordDetector.isQuoted(containerNames[i])) {
                        containerNames[i] = DBUtils.getUnQuotedIdentifier(containerNames[i], syntaxManager.getIdentifierQuoteStrings());
                    }
                    containerNames[i] = DBObjectNameCaseTransformer.transformName(editor.getDataSource(), containerNames[i]);
                }
                if (wordDetector.isQuoted(tableName)) {
                    tableName = DBUtils.getUnQuotedIdentifier(tableName, syntaxManager.getIdentifierQuoteStrings());
                }
            } else {
                // Full name could be quoted
                if (wordDetector.isQuoted(fullName)) {
                    String unquotedName = DBUtils.getUnQuotedIdentifier(tableName, syntaxManager.getIdentifierQuoteStrings());
                    if (unquotedName.equals(tableName)) {
                        caseSensitive = true;
                    }
                }
            }
        }

        final SQLDialect dialect = syntaxManager.getDialect();

        keywordType = dialect.getKeywordType(fullName);
        if (keywordType == DBPKeywordType.KEYWORD && region.getLength() > 1) {
            // It is a keyword = let's use whole selection
            try {
                fullName = document.get(region.getOffset(), region.getLength());
            } catch (BadLocationException e) {
                log.warn(e);
            }
        }
        keywords = new String[] { fullName };
        if (keywordType == DBPKeywordType.KEYWORD || keywordType == DBPKeywordType.FUNCTION) {
            // Skip keywords
            return;
        }

        final Map<String, ObjectLookupCache> contextCache = getLinksCache();
        if (contextCache == null) {
            return;
        }
        ObjectLookupCache tlc = contextCache.get(fullName);
        if (tlc == null) {
            // Start new word finder job
            tlc = new ObjectLookupCache();
            contextCache.put(fullName, tlc);

            DBSStructureAssistant structureAssistant = DBUtils.getAdapter(DBSStructureAssistant.class, editor.getDataSource());
            TablesFinderJob job = new TablesFinderJob(executionContext, structureAssistant, containerNames, tableName, caseSensitive, tlc);
            job.schedule();
        }
        if (tlc.loading) {
            // Wait for 1000ms maximum
            for (int i = 0; i < 20; i++) {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    // interrupted - just go further
                    break;
                }
                if (!tlc.loading) {
                    break;
                }
                Display.getCurrent().readAndDispatch();
            }
        }
        if (!tlc.loading) {
            synchronized (this) {
                objectReferences = tlc.references;
            }
        }
    }

    private Map<String, ObjectLookupCache> getLinksCache() {
        DBPDataSource dataSource = this.editor.getDataSource();
        if (dataSource == null) {
            return null;
        }
        final DBPDataSourceContainer container = dataSource.getContainer();
        synchronized (LINKS_CACHE) {
            Map<String, ObjectLookupCache> cacheMap = LINKS_CACHE.get(container.getId());
            if (cacheMap == null) {
                cacheMap = new HashMap<>();
                LINKS_CACHE.put(container.getId(), cacheMap);

                DBPDataSourceRegistry registry = container.getRegistry();

                // Register disconnect listener
                DBPEventListener dbpEventListener = new DBPEventListener() {
                    @Override
                    public void handleDataSourceEvent(DBPEvent event) {
                        if (event.getAction() == DBPEvent.Action.OBJECT_UPDATE && Boolean.FALSE.equals(event.getEnabled())) {
                            synchronized (LINKS_CACHE) {
                                LINKS_CACHE.remove(container.getId());
                                registry.removeDataSourceListener(this);
                            }
                        }
                    }
                };
                registry.addDataSourceListener(dbpEventListener);
                editor.getEditorControl().addDisposeListener(e -> registry.removeDataSourceListener(dbpEventListener));
            }
            return cacheMap;
        }
    }

    private class TablesFinderJob extends DataSourceJob {

        private final DBSStructureAssistant structureAssistant;
        private final String[] containerNames;
        private final String objectName;
        private final ObjectLookupCache cache;
        private final boolean caseSensitive;

        protected TablesFinderJob(@NotNull DBCExecutionContext executionContext, @Nullable DBSStructureAssistant structureAssistant, @Nullable String[] containerNames, @NotNull String objectName, boolean caseSensitive, @NotNull ObjectLookupCache cache)
        {
            super("Find object '" + objectName + "'", executionContext);
            this.structureAssistant = structureAssistant;
            // Transform container name case
            this.containerNames = containerNames;
            this.objectName = objectName;
            this.caseSensitive = caseSensitive;
            this.cache = cache;
            setUser(false);
            setSystem(true);
        }

        @Override
        protected IStatus run(DBRProgressMonitor monitor)
        {
            monitor.beginTask("Read metadata information", 1);
            cache.references = new ArrayList<>();
            try {

                DBSObjectContainer container = null;
                if (!ArrayUtils.isEmpty(containerNames)) {
                    DBSObjectContainer dsContainer = DBUtils.getAdapter(DBSObjectContainer.class, getExecutionContext().getDataSource());
                    if (dsContainer != null) {
                        DBCExecutionContextDefaults contextDefaults = getExecutionContext().getContextDefaults();

                        DBSObject childContainer = dsContainer.getChild(monitor, containerNames[0]);
                        if (childContainer == null) {
                             if (contextDefaults != null) {
                                 if (contextDefaults.getDefaultCatalog() != null) {
                                     childContainer = contextDefaults.getDefaultCatalog().getChild(monitor, containerNames[0]);
                                 }
                             }
                        }
                        if (childContainer instanceof DBSObjectContainer) {
                            container = (DBSObjectContainer) childContainer;
                        } else {
                            // Check in selected object
                            if (childContainer == null && structureAssistant != null) {
                                // Container is not direct child of schema/catalog. Let's try struct assistant
                                DBCExecutionContext executionContext = editor.getExecutionContext();
                                if (executionContext != null) {
                                    final List<DBSObjectReference> objReferences = structureAssistant.findObjectsByMask(monitor, executionContext, null, structureAssistant.getAutoCompleteObjectTypes(), containerNames[0], false, true, 1);
                                    if (objReferences.size() == 1) {
                                        childContainer = objReferences.get(0).resolveObject(monitor);
                                    }
                                    if (childContainer == null) {
                                        return Status.CANCEL_STATUS;
                                    }
                                }
                            }
                            if (childContainer instanceof DBSObjectContainer) {
                                container = (DBSObjectContainer) childContainer;
                            }
                        }
                    }
                }
                if (container != null) {
                    if (containerNames.length > 1) {
                        // We have multiple containers. They MUST combine a unique
                        // path to the object
                        for (int i = 1; i < containerNames.length; i++) {
                            DBSObject childContainer = container.getChild(monitor, containerNames[i]);
                            if (childContainer instanceof DBSObjectContainer) {
                                container = (DBSObjectContainer) childContainer;
                            } else {
                                break;
                            }
                        }
                    } else {
                        // We have a container. But maybe it is a wrong one -
                        // this may happen if database supports multiple nested containers (catalog+schema+?)
                        // and schema name is the same as catalog name.
                        // So let's try to get nested container because we always need the deepest one.
                        DBSObject childContainer = container.getChild(monitor, containerNames[0]);
                        if (childContainer instanceof DBSObjectContainer) {
                            // Yep - this is it
                            container = (DBSObjectContainer) childContainer;
                        }
                    }
                }

                DBSObject targetObject = null;
                if (container != null) {
                    final String fixedName = DBObjectNameCaseTransformer.transformName(getExecutionContext().getDataSource(), objectName);
                    if (fixedName != null) {
                        targetObject = container.getChild(monitor, fixedName);
                    }
                }
                if (targetObject != null) {
                    cache.references.add(new DirectObjectReference(container, null, targetObject));
                } else if (structureAssistant != null) {
                    DBSObjectType[] objectTypes = structureAssistant.getHyperlinkObjectTypes();
                    DBCExecutionContext executionContext = editor.getExecutionContext();
                    if (executionContext != null) {
                        Collection<DBSObjectReference> objects = structureAssistant.findObjectsByMask(monitor, executionContext, container, objectTypes, objectName, caseSensitive, false, 10);
                        if (!CommonUtils.isEmpty(objects)) {
                            cache.references.addAll(objects);
                        }
                    }
                }
            } catch (DBException e) {
                log.warn(e);
            }
            finally {
                cache.loading = false;
                monitor.done();
            }
            return Status.OK_STATUS;
        }
    }

}