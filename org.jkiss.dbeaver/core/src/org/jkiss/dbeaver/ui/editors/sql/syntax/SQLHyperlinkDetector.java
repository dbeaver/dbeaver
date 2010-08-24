/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.editors.sql.syntax;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.hyperlink.AbstractHyperlinkDetector;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.model.struct.DBSEntityContainer;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSStructureAssistant;
import org.jkiss.dbeaver.model.struct.DBSTablePath;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.ui.editors.entity.EntityHyperlink;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditor;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;


/**
 * SQLHyperlinkDetector
 */
public class SQLHyperlinkDetector extends AbstractHyperlinkDetector
{
    static final Log log = LogFactory.getLog(SQLHyperlinkDetector.class);

    private SQLEditor editor;

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

        SQLIdentifierDetector wordDetector = new SQLIdentifierDetector(editor.getSyntaxManager());
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
                if (!wordDetector.isPlainWordPart(ch)) {
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

        if (editor.getSyntaxManager().getKeywordType(identifier.toUpperCase()) != null) {
            // Skip keywords
            return null;
        }
        if (word.length() == 0) {
            return null;
        }

        // Detect what all this means
        final DBPDataSource dataSource = editor.getDataSource();
        if (dataSource instanceof DBSEntityContainer && dataSource instanceof DBSStructureAssistant) {
            final IRegion wordRegion = new Region(wordStart, wordEnd - wordStart);
            final List<IHyperlink> links = new ArrayList<IHyperlink>();
            final String checkWord = word;
            try {
                DBRRunnableWithProgress objLoader = new DBRRunnableWithProgress() {
                    public void run(DBRProgressMonitor monitor)
                        throws InvocationTargetException, InterruptedException
                    {
                        monitor.beginTask("Find tables", 1);
                        try {
                            final List<DBSTablePath> pathList = ((DBSStructureAssistant) editor.getDataSource()).findTableNames(
                                monitor, checkWord, 2);
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

}