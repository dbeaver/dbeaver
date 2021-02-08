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

package org.jkiss.dbeaver.ui.resources.bookmarks;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.ui.DBIconBinary;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.Base64;
import org.jkiss.utils.xml.XMLBuilder;
import org.jkiss.utils.xml.XMLException;
import org.jkiss.utils.xml.XMLUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Bookmark storage
 */
public class BookmarkStorage {

    public static final String ATTR_TITLE = "title"; //NON-NLS-1
    public static final String ATTR_DESCRIPTION = "description"; //NON-NLS-1
    public static final String ATTR_DATA_SOURCE = "data-source"; //NON-NLS-1
    public static final String TAG_PATH = "path"; //NON-NLS-1
    public static final String TAG_IMAGE = "image"; //NON-NLS-1
    public static final String TAG_BOOKMARK = "bookmark"; //NON-NLS-1
    private String title;
    private String description;
    private DBPImage image;
    private String dataSourceId;
    private List<String> dataSourcePath;

    public BookmarkStorage(IFile file, boolean loadImage) throws DBException, CoreException
    {
        this.title = file.getFullPath().removeFileExtension().lastSegment();
        try (InputStream contents = file.getContents(true)) {
            final Document document = XMLUtils.parseDocument(contents);
            final Element root = document.getDocumentElement();
            this.title = root.getAttribute(ATTR_TITLE);
            this.description = root.getAttribute(ATTR_DESCRIPTION);
            this.dataSourceId = root.getAttribute(ATTR_DATA_SOURCE);
            if (dataSourceId == null) {
                throw new DBException("Data source ID missing in bookmark definition");
            }
            this.dataSourcePath = new ArrayList<>();
            for (Element elem : XMLUtils.getChildElementList(root, TAG_PATH)) {
                this.dataSourcePath.add(XMLUtils.getElementBody(elem));
            }
            if (loadImage) {
                Element imgElement = XMLUtils.getChildElement(root, TAG_IMAGE);
                if (imgElement != null) {
                    String imgString = XMLUtils.getElementBody(imgElement);
                    final byte[] imgBytes = Base64.decode(imgString);
                    ImageLoader loader = new ImageLoader();
                    this.image = new DBIconBinary(
                        dataSourcePath.toString(),
                        loader.load(new ByteArrayInputStream(imgBytes))[0]);
                }
            }
        } catch (XMLException e) {
            throw new DBException("Error reading bookmarks storage", e);
        } catch (IOException e) {
            throw new DBException("IO Error reading bookmarks storage", e);
        }
    }

    BookmarkStorage(String title, String description, DBPImage image, String dataSourceId, List<String> dataSourcePath)
    {
        this.title = title;
        this.description = description;
        this.image = image;
        this.dataSourceId = dataSourceId;
        this.dataSourcePath = dataSourcePath;
    }

    public void dispose()
    {
    }

    public String getTitle()
    {
        return title;
    }

    public void setTitle(String title)
    {
        this.title = title;
    }

    public String getDescription()
    {
        return description;
    }

    public DBPImage getImage()
    {
        return image;
    }

    public void setImage(DBPImage image)
    {
        this.image = image;
    }

    public String getDataSourceId()
    {
        return dataSourceId;
    }

    public List<String> getDataSourcePath()
    {
        return dataSourcePath;
    }

    public ByteArrayInputStream serialize() throws IOException
    {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream(5000);
        XMLBuilder xml = new XMLBuilder(buffer, GeneralUtils.getDefaultFileEncoding());
        xml.startElement(TAG_BOOKMARK);
        xml.addAttribute(ATTR_TITLE, title);
        if (description != null) {
            xml.addAttribute(ATTR_DESCRIPTION, description);
        }
        xml.addAttribute(ATTR_DATA_SOURCE, dataSourceId);
        for (String path : dataSourcePath) {
            xml.startElement(TAG_PATH);
            xml.addText(path);
            xml.endElement();
        }

        {
            xml.startElement(TAG_IMAGE);

            Image realImage = DBeaverIcons.getImage(this.image);
            ImageLoader loader = new ImageLoader();
            loader.data = new ImageData[] {realImage.getImageData()};
            ByteArrayOutputStream imageBuffer = new ByteArrayOutputStream(5000);
            loader.save(imageBuffer, SWT.IMAGE_PNG);
            xml.addText(Base64.encode(imageBuffer.toByteArray()));

            xml.endElement();
        }

        xml.endElement();
        xml.flush();
        return new ByteArrayInputStream(buffer.toByteArray());
    }

}
