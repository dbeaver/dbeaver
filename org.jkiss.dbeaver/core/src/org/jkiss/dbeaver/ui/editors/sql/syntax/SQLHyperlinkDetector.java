/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.editors.sql.syntax;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.text.*;
import org.eclipse.jface.text.hyperlink.AbstractHyperlinkDetector;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.model.struct.DBSEntityContainer;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSStructureAssistant;
import org.jkiss.dbeaver.model.struct.DBSTablePath;
import org.jkiss.dbeaver.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.runtime.jobs.DataSourceJob;
import org.jkiss.dbeaver.ui.DBIcon;
import org.jkiss.dbeaver.ui.editors.entity.EntityHyperlink;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditor;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;


/**
 * SQLHyperlinkDetector
 */
public class SQLHyperlinkDetector extends AbstractHyperlinkDetector
{
    static final Log log = LogFactory.getLog(SQLHyperlinkDetector.class);

    private SQLEditor editor;
    private Map<String, TablesFinderJob> linksCache = new WeakHashMap<String, TablesFinderJob>();

    public SQLHyperlinkDetector(SQLEditor editor)
    {
        this.editor = editor;
    }

    public IHyperlink[] detectHyperlinks(ITextViewer textViewer, IRegion region, boolean canShowMultipleHyperlinks)
    {
        if (region == null || textViewer == null || !editor.getDataSourceContainer().isConnected()) {
            return null;
        }

        IDocument document = textViewer.getDocument();
        if (document == null) {
            return null;
        }

        int offset = region.getOffset();

        SQLIdentifierDetector wordDetector = new SQLIdentifierDetector(editor.getSyntaxManager().getCatalogSeparator());
        int docLength = document.getLength();
        int identStart = offset;
        int identEnd = offset;
        int wordStart = -1, wordEnd = -1;
        String identifier, word;
        try {
            if (!wordDetector.isPlainWordPart(document.getChar(offset))) {
                return null;
            }
            while (identStart >= 0) {
                char ch = document.getChar(identStart);
                if (!wordDetector.isWordPart(ch)) {
                    break;
                }
                if (wordStart < 0 && !wordDetector.isPlainWordPart(ch)) {
                    wordStart = identStart + 1;
                }
                identStart--;
            }
            identStart++;
            while (identEnd < docLength) {
                char ch = document.getChar(identEnd);
                if (!wordDetector.isWordPart(ch)) {
                    break;
                }
                if (!wordDetector.isPlainWordPart(ch)) {
                    wordEnd = identEnd;
                }
                identEnd++;
            }
            if (wordStart < 0) wordStart = identStart;
            if (wordEnd < 0) wordEnd = identEnd;
            identifier = document.get(identStart, identEnd - identStart);
            word = document.get(wordStart, wordEnd - wordStart);
        } catch (BadLocationException e) {
            return null;
        }

        if (word.length() == 0) {
            return null;
        }

        if (editor.getSyntaxManager().getKeywordType(identifier.toUpperCase()) == SQLSyntaxManager.KeywordType.KEYWORD) {
            // Skip keywords
            return null;
        }

        // Detect what all this means
        final DBPDataSource dataSource = editor.getDataSource();
        if (dataSource instanceof DBSEntityContainer && dataSource instanceof DBSStructureAssistant) {
            final IRegion wordRegion = new Region(wordStart, wordEnd - wordStart);
            final List<IHyperlink> links = new ArrayList<IHyperlink>();
            final String checkWord = word;
/*
            TablesFinderJob finderJob = this.linksCache.get(word);
            if (finderJob == null) {
                finderJob = new TablesFinderJob(checkWord);
                finderJob.schedule();
            }
            finderJob.join();

            if (objects == null) {

            }
*/
            try {
                DBRRunnableWithProgress objLoader = new DBRRunnableWithProgress() {
                    public void run(DBRProgressMonitor monitor)
                        throws InvocationTargetException, InterruptedException
                    {
                        monitor.beginTask("Find tables", 1);
                        try {
                            final List<DBSTablePath> pathList = ((DBSStructureAssistant) editor.getDataSource()).findTableNames(
                                monitor, checkWord, 10);
                            if (!pathList.isEmpty()) {
                                for (DBSTablePath path : pathList) {
                                    DBSObject object = DBUtils.getTableByPath(monitor, (DBSEntityContainer) dataSource, path);
                                    if (object != null) {
                                        links.add(new EntityHyperlink(object, wordRegion));
                                    }
                                }
                            }
                        }
                        catch (DBException e) {
                            throw new InvocationTargetException(e);
                        }
                        finally {
                            monitor.done();
                        }
                    }
                };
                // Run it with dummy monitor
                // Using detached thread (job) or running with progress service breaks hyperlinks
                // TODO: use real progress monitor
                objLoader.run(VoidProgressMonitor.INSTANCE);
            }
            catch (InvocationTargetException e) {
                log.error(e.getTargetException());
            }
            catch (InterruptedException e) {
                // do nothing
            }

            if (links.isEmpty()) {
                return null;
            }
            return links.toArray(new IHyperlink[links.size()]);
        }
        return null;
    }

    private class TablesFinderJob extends DataSourceJob {

        private String word;
        private List<DBSObject> objects = new ArrayList<DBSObject>();

        protected TablesFinderJob(String word)
        {
            super("Find table names for '" + word + "'", DBIcon.SQL_EXECUTE.getImageDescriptor(), editor.getDataSource());
            this.word = word;
        }

        @Override
        protected IStatus run(DBRProgressMonitor monitor)
        {
            final List<DBSTablePath> pathList;
            try {
                pathList = ((DBSStructureAssistant) getDataSource()).findTableNames(monitor, word, 10);
                if (!pathList.isEmpty()) {
                    for (DBSTablePath path : pathList) {
                        DBSObject object = DBUtils.getTableByPath(monitor, (DBSEntityContainer) getDataSource(), path);
                        if (object != null) {
                            objects.add(object);
                        }
                    }
                }
            } catch (DBException e) {
                log.warn(e);
            }
            return Status.OK_STATUS;
        }

        public List<DBSObject> getObjects()
        {
            return objects;
        }
    }

}