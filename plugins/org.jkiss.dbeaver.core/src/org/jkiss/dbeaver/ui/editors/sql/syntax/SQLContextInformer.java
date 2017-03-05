/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.widgets.Display;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPKeywordType;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.DBObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.impl.struct.DirectObjectReference;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLSyntaxManager;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.runtime.jobs.DataSourceJob;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditorBase;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

import java.util.*;


/**
 * SQLContextInformer
 */
class SQLContextInformer
{
    private static final Log log = Log.getLog(SQLContextInformer.class);

    private final SQLEditorBase editor;
    private SQLSyntaxManager syntaxManager;
    private SQLIdentifierDetector.WordRegion wordRegion;

    private static class ObjectLookupCache {
        List<DBSObjectReference> references;
        boolean loading = true;
    }

    private static final Map<SQLEditorBase, Map<String, ObjectLookupCache>> linksCache = new HashMap<>();

    private String keyword;
    private DBPKeywordType keywordType;
    private List<DBSObjectReference> objectReferences;

    public SQLContextInformer(SQLEditorBase editor, SQLSyntaxManager syntaxManager)
    {
        this.editor = editor;
        this.syntaxManager = syntaxManager;
    }

    public SQLEditorBase getEditor() {
        return editor;
    }

    public SQLIdentifierDetector.WordRegion getWordRegion() {
        return wordRegion;
    }

    public String getKeyword() {
        return keyword;
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
        initEditorCache();

        ITextViewer textViewer = editor.getTextViewer();
        final DBCExecutionContext executionContext = editor.getExecutionContext();
        if (region == null || textViewer == null || executionContext == null) {
            return;
        }

        IDocument document = textViewer.getDocument();
        if (document == null) {
            return;
        }

        SQLIdentifierDetector wordDetector = new SQLIdentifierDetector(syntaxManager.getStructSeparator(), syntaxManager.getQuoteSymbol());
        wordRegion = wordDetector.detectIdentifier(document, region);

        if (wordRegion.word.length() == 0) {
            return;
        }

        String fullName = wordRegion.identifier;
        String tableName = wordRegion.word;
        boolean caseSensitive = false;
        if (wordDetector.isQuoted(tableName)) {
            tableName = DBUtils.getUnQuotedIdentifier(tableName, syntaxManager.getQuoteSymbol());
            caseSensitive = true;
        }
        String[] containerNames = null;
        if (!CommonUtils.equalObjects(fullName, tableName)) {
            int divPos = fullName.indexOf(syntaxManager.getStructSeparator());
            if (divPos != -1) {
                String[] parts = ArrayUtils.toArray(String.class, CommonUtils.splitString(fullName, syntaxManager.getStructSeparator()));
                tableName = parts[parts.length - 1];
                containerNames = ArrayUtils.remove(String.class, parts, parts.length - 1);
                for (int i = 0; i < containerNames.length; i++) {
                    if (wordDetector.isQuoted(containerNames[i])) {
                        containerNames[i] = DBUtils.getUnQuotedIdentifier(containerNames[i], syntaxManager.getQuoteSymbol());
                    }
                    containerNames[i] = DBObjectNameCaseTransformer.transformName(editor.getDataSource(), containerNames[i]);
                }
                if (wordDetector.isQuoted(tableName)) {
                    tableName = DBUtils.getUnQuotedIdentifier(tableName, syntaxManager.getQuoteSymbol());
                }
            } else {
                // Full name could be quoted
                if (wordDetector.isQuoted(fullName)) {
                    String unquotedName = DBUtils.getUnQuotedIdentifier(tableName, syntaxManager.getQuoteSymbol());
                    if (unquotedName.equals(tableName)) {
                        caseSensitive = true;
                    }
                }
            }
        }

        keywordType = syntaxManager.getDialect().getKeywordType(fullName);
        keyword = fullName;
        if (keywordType == DBPKeywordType.KEYWORD || keywordType == DBPKeywordType.FUNCTION) {
            // Skip keywords
            return;
        }
        DBSStructureAssistant structureAssistant = DBUtils.getAdapter(DBSStructureAssistant.class, editor.getDataSource());
        if (structureAssistant == null) {
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

    private void initEditorCache() {
        // Register cache for specified editor
        boolean dupEditor;
        synchronized (linksCache) {
            dupEditor = linksCache.containsKey(this.editor);
            if (!dupEditor) {
                linksCache.put(this.editor, new HashMap<String, ObjectLookupCache>());
            }
        }
        if (!dupEditor) {
            editor.getTextViewer().getControl().addDisposeListener(new DisposeListener() {
                @Override
                public void widgetDisposed(DisposeEvent e) {
                    synchronized (linksCache) {
                        linksCache.remove(SQLContextInformer.this.editor);
                    }
                }
            });
        }
    }

    private Map<String, ObjectLookupCache> getLinksCache() {
        synchronized (linksCache) {
            return linksCache.get(this.editor);
        }
    }

    public void dispose()
    {
    }

    private class TablesFinderJob extends DataSourceJob {

        private final DBSStructureAssistant structureAssistant;
        private final String[] containerNames;
        private final String objectName;
        private final ObjectLookupCache cache;
        private final boolean caseSensitive;

        protected TablesFinderJob(@NotNull DBCExecutionContext executionContext, @NotNull DBSStructureAssistant structureAssistant, @Nullable String[] containerNames, @NotNull String objectName, boolean caseSensitive, @NotNull ObjectLookupCache cache)
        {
            super("Find object '" + objectName + "'", DBeaverIcons.getImageDescriptor(UIIcon.SQL_EXECUTE), executionContext);
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
            cache.references = new ArrayList<>();
            try {

                DBSObjectContainer container = null;
                if (!ArrayUtils.isEmpty(containerNames)) {
                    DBSObjectContainer dsContainer = DBUtils.getAdapter(DBSObjectContainer.class, getExecutionContext().getDataSource());
                    if (dsContainer != null) {
                        DBSObject childContainer = dsContainer.getChild(monitor, containerNames[0]);
                        if (childContainer instanceof DBSObjectContainer) {
                            container = (DBSObjectContainer) childContainer;
                        } else {
                            // Check in selected object
                            DBSObjectSelector dsSelector = DBUtils.getAdapter(DBSObjectSelector.class, getExecutionContext().getDataSource());
                            if (dsSelector != null) {
                                DBSObject curCatalog = dsSelector.getDefaultObject();
                                if (curCatalog instanceof DBSObjectContainer) {
                                    childContainer = ((DBSObjectContainer)curCatalog).getChild(monitor, containerNames[0]);
                                }
                            }
                            if (childContainer == null) {
                                // Container is not direct child of schema/catalog. Let's try struct assistant
                                final List<DBSObjectReference> objReferences = structureAssistant.findObjectsByMask(monitor, null, structureAssistant.getAutoCompleteObjectTypes(), containerNames[0], false, true, 1);
                                if (objReferences.size() == 1) {
                                    childContainer = objReferences.get(0).resolveObject(monitor);
                                }
                                if (childContainer == null) {
                                    return Status.CANCEL_STATUS;
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
                } else {
                    DBSObjectType[] objectTypes = structureAssistant.getHyperlinkObjectTypes();
                    Collection<DBSObjectReference> objects = structureAssistant.findObjectsByMask(monitor, container, objectTypes, objectName, caseSensitive, false, 10);
                    if (!CommonUtils.isEmpty(objects)) {
                        cache.references.addAll(objects);
                    }
                }
            } catch (DBException e) {
                log.warn(e);
            }
            finally {
                cache.loading = false;
            }
            return Status.OK_STATUS;
        }
    }

}