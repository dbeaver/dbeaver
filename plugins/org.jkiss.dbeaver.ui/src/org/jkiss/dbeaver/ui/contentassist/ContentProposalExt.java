package org.jkiss.dbeaver.ui.contentassist;

import org.eclipse.jface.fieldassist.ContentProposal;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.DBPImageProvider;

public class ContentProposalExt extends ContentProposal implements DBPImageProvider {

    private DBPImage image;

    public ContentProposalExt(String content) {
        super(content);
    }

    public ContentProposalExt(String content, String description) {
        super(content, description);
    }

    public ContentProposalExt(String content, String label, String description) {
        super(content, label, description);
    }

    public ContentProposalExt(String content, String label, String description, int cursorPosition) {
        super(content, label, description, cursorPosition);
    }

    public ContentProposalExt(String content, String label, String description, DBPImage image) {
        super(content, label, description);
        this.image = image;
    }

    public ContentProposalExt(String content, String label, String description, int cursorPosition, DBPImage image) {
        super(content, label, description, cursorPosition);
        this.image = image;
    }

    @Nullable
    @Override
    public DBPImage getObjectImage() {
        return image;
    }

    public void setObjectImage(DBPImage image) {
        this.image = image;
    }

}
