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
import org.jkiss.dbeaver.model.data.DBDDisplayFormat;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.DBObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.impl.struct.DirectObjectReference;
import org.jkiss.dbeaver.model.preferences.DBPPropertyDescriptor;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.model.runtime.SystemJob;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.model.sql.SQLHelpProvider;
import org.jkiss.dbeaver.model.sql.SQLHelpTopic;
import org.jkiss.dbeaver.model.sql.SQLSyntaxManager;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.runtime.jobs.DataSourceJob;
import org.jkiss.dbeaver.runtime.properties.PropertyCollector;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditorBase;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;


/**
 * SQLContextInformer
 */
public class SQLContextInformer
{
    private static final Log log = Log.getLog(SQLContextInformer.class);

    private static final Map<DBPDataSourceContainer, Map<String, ObjectLookupCache>> LINKS_CACHE = new HashMap<>();

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
            tableName = DBUtils.getUnQuotedIdentifier(tableName, syntaxManager.getQuoteStrings());
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
                        containerNames[i] = DBUtils.getUnQuotedIdentifier(containerNames[i], syntaxManager.getQuoteStrings());
                    }
                    containerNames[i] = DBObjectNameCaseTransformer.transformName(editor.getDataSource(), containerNames[i]);
                }
                if (wordDetector.isQuoted(tableName)) {
                    tableName = DBUtils.getUnQuotedIdentifier(tableName, syntaxManager.getQuoteStrings());
                }
            } else {
                // Full name could be quoted
                if (wordDetector.isQuoted(fullName)) {
                    String unquotedName = DBUtils.getUnQuotedIdentifier(tableName, syntaxManager.getQuoteStrings());
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

    private Map<String, ObjectLookupCache> getLinksCache() {
        DBPDataSource dataSource = this.editor.getDataSource();
        if (dataSource == null) {
            return null;
        }
        final DBPDataSourceContainer container = dataSource.getContainer();
        synchronized (LINKS_CACHE) {
            Map<String, ObjectLookupCache> cacheMap = LINKS_CACHE.get(container);
            if (cacheMap == null) {
                cacheMap = new HashMap<>();
                LINKS_CACHE.put(container, cacheMap);

                // Register disconnect listener
                container.getRegistry().addDataSourceListener(new DBPEventListener() {
                    @Override
                    public void handleDataSourceEvent(DBPEvent event) {
                        if (event.getAction() == DBPEvent.Action.OBJECT_UPDATE && Boolean.FALSE.equals(event.getEnabled())) {
                            synchronized (LINKS_CACHE) {
                                LINKS_CACHE.remove(container);
                                container.getRegistry().removeDataSourceListener(this);
                            }
                        }
                    }
                });
            }
            return cacheMap;
        }
    }

    public static String makeObjectDescription(@Nullable DBRProgressMonitor monitor, DBPNamedObject object, boolean html) {
        final PropertiesReader reader = new PropertiesReader(object, html);

        if (monitor == null) {
            SystemJob searchJob = new SystemJob("Extract object properties info", reader);
            searchJob.schedule();
            UIUtils.waitJobCompletion(searchJob);
        } else {
            reader.run(monitor);
        }
        return reader.getPropertiesInfo();
    }

    public static String readAdditionalProposalInfo(@Nullable DBRProgressMonitor monitor, final DBPDataSource dataSource, DBPNamedObject object, final String[] keywords, final DBPKeywordType keywordType) {
        if (object != null) {
            return makeObjectDescription(monitor, object, true);
        } else if (keywordType != null && dataSource != null) {
            HelpReader helpReader = new HelpReader(dataSource, keywordType, keywords);
            if (monitor == null) {
                SystemJob searchJob = new SystemJob("Read help topic", helpReader);
                searchJob.schedule();
                UIUtils.waitJobCompletion(searchJob);
            } else {
                helpReader.run(monitor);
            }

            return helpReader.info;
        } else {
            return keywords.length == 0 ? null : keywords[0];
        }
    }

    private static String readDataSourceHelp(DBRProgressMonitor monitor, DBPDataSource dataSource, DBPKeywordType keywordType, String keyword) {
        final SQLHelpProvider helpProvider = DBUtils.getAdapter(SQLHelpProvider.class, dataSource);
        if (helpProvider == null) {
            return null;
        }
        final SQLHelpTopic helpTopic = helpProvider.findHelpTopic(monitor, keyword, keywordType);
        if (helpTopic == null) {
            return null;
        }
        if (!CommonUtils.isEmpty(helpTopic.getContents())) {
            return helpTopic.getContents();
        } else if (!CommonUtils.isEmpty(helpTopic.getUrl())) {
            return "<a href=\"" + helpTopic.getUrl() + "\">" + keyword + "</a>";
        } else {
            return null;
        }
    }

    private static class PropertiesReader implements DBRRunnableWithProgress {

        private final StringBuilder info = new StringBuilder();
        private final DBPNamedObject object;
        private final boolean html;

        public PropertiesReader(DBPNamedObject object, boolean html) {
            this.object = object;
            this.html = html;
        }

/*
        boolean hasRemoteProperties() {
            for (DBPPropertyDescriptor descriptor : collector.getPropertyDescriptors2()) {
                if (descriptor.isRemote()) {
                    return true;
                }
            }
            return false;
        }
*/

        String getPropertiesInfo() {
            return info.toString();
        }

        @Override
        public void run(DBRProgressMonitor monitor) {
            DBPNamedObject targetObject = object;
            if (object instanceof DBSObjectReference) {
                try {
                    targetObject = ((DBSObjectReference) object).resolveObject(monitor);
                } catch (DBException e) {
                    StringWriter buf = new StringWriter();
                    e.printStackTrace(new PrintWriter(buf, true));
                    info.append(buf.toString());
                }
            }
            PropertyCollector collector = new PropertyCollector(targetObject, false);
            collector.collectProperties();

            for (DBPPropertyDescriptor descriptor : collector.getPropertyDescriptors2()) {
                Object propValue = collector.getPropertyValue(monitor, descriptor.getId());
                if (propValue == null) {
                    continue;
                }
                String propString;
                if (propValue instanceof DBPNamedObject) {
                    propString = ((DBPNamedObject) propValue).getName();
                } else {
                    propString = DBValueFormatting.getDefaultValueDisplayString(propValue, DBDDisplayFormat.UI);
                }
                if (CommonUtils.isEmpty(propString)) {
                    continue;
                }
                if (html) {
                    info.append("<b>").append(descriptor.getDisplayName()).append(":  </b>");
                    info.append(propString);
                    info.append("<br>");
                } else {
                    info.append(descriptor.getDisplayName()).append(": ").append(propString).append("\n");
                }
            }
        }

    }

    private static class HelpReader implements DBRRunnableWithProgress {
        private final DBPDataSource dataSource;
        private final DBPKeywordType keywordType;
        private final String[] keywords;
        private String info;

        public HelpReader(DBPDataSource dataSource, DBPKeywordType keywordType, String[] keywords) {
            this.dataSource = dataSource;
            this.keywordType = keywordType;
            this.keywords = keywords;
        }

        @Override
        public void run(DBRProgressMonitor monitor) {
            for (String keyword : keywords) {
                info = readDataSourceHelp(monitor, dataSource, keywordType, keyword);
                if (info != null) {
                    break;
                }
            }
            if (CommonUtils.isEmpty(info)) {
                info = "<b>" + keywords[0] + "</b> (" + keywordType.name() + ")";
            }
        }
    }

    private class TablesFinderJob extends DataSourceJob {

        private final DBSStructureAssistant structureAssistant;
        private final String[] containerNames;
        private final String objectName;
        private final ObjectLookupCache cache;
        private final boolean caseSensitive;

        protected TablesFinderJob(@NotNull DBCExecutionContext executionContext, @NotNull DBSStructureAssistant structureAssistant, @Nullable String[] containerNames, @NotNull String objectName, boolean caseSensitive, @NotNull ObjectLookupCache cache)
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