package org.jkiss.dbeaver.ui.editors.content.parts;

import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IEditorActionBarContributor;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.editors.text.TextEditor;
import org.jkiss.dbeaver.ext.IContentEditorPart;
import org.jkiss.dbeaver.ext.ui.IDataSourceUser;
import org.jkiss.dbeaver.ui.DBIcon;
import org.jkiss.dbeaver.ui.preferences.PrefConstants;

/**
 * LOB text editor
 */
public class ContentTextEditorPart extends TextEditor implements IContentEditorPart {

    private IEditorPart contentEditor;

    public ContentTextEditorPart() {
        setDocumentProvider(new ContentTextDocumentProvider());
    }

    public void initPart(IEditorPart contentEditor)
    {
        this.contentEditor = contentEditor;
    }

    public IEditorActionBarContributor getActionBarContributor()
    {
        return null;
    }

    public String getContentTypeTitle()
    {
        return "Text";
    }

    public Image getContentTypeImage()
    {
        return DBIcon.TEXT.getImage();
    }

    public String getPreferedMimeType()
    {
        return "text";
    }

    public long getMaxContentLength()
    {
        if (contentEditor instanceof IDataSourceUser) {
            return ((IDataSourceUser)contentEditor).getDataSourceContainer().getPreferenceStore().getInt(PrefConstants.RS_EDIT_MAX_TEXT_SIZE);
        }
        return 10 * 1024 * 1024;
    }

    /**
     * Always return true cos' text editor can load any binary content
     * @return
     */
    public boolean isContentValid()
    {
        return true;
    }
}
