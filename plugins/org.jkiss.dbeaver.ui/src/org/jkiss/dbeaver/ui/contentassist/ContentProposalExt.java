/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
