/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.project;

import net.sf.jkiss.utils.Base64;
import net.sf.jkiss.utils.xml.XMLBuilder;
import net.sf.jkiss.utils.xml.XMLException;
import net.sf.jkiss.utils.xml.XMLUtils;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.registry.DataSourceRegistry;
import org.jkiss.dbeaver.utils.ContentUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Bookmark storage
 */
public class BookmarkStorage {

    private String title;
    private String description;
    private Image image;
    private String dataSourceId;
    private List<String> dataSourcePath;

    public BookmarkStorage(IFile file, boolean loadImage) throws DBException, CoreException
    {
        this.title = file.getFullPath().removeFileExtension().lastSegment();
        try {
            final InputStream contents = file.getContents(true);
            try {
                final Document document = XMLUtils.parseDocument(contents);
                final Element root = document.getDocumentElement();
                this.title = root.getAttribute("title");
                this.description = root.getAttribute("description");
                this.dataSourceId = root.getAttribute("data-source");
                if (dataSourceId == null) {
                    throw new DBException("Data source ID missing in bookmark definition");
                }
                this.dataSourcePath = new ArrayList<String>();
                final Element[] pathElements = XMLUtils.getChildElementList(root, "path");
                for (Element elem : pathElements) {
                    this.dataSourcePath.add(XMLUtils.getElementBody(elem));
                }
                if (loadImage) {
                    Element imgElement = XMLUtils.getChildElement(root, "image");
                    if (imgElement != null) {
                        String imgString = XMLUtils.getElementBody(imgElement);
                        final byte[] imgBytes = Base64.decode(imgString);
                        ImageLoader loader = new ImageLoader();
                        this.image = new Image(null, loader.load(new ByteArrayInputStream(imgBytes))[0]);
                    }
                }
            } finally {
                ContentUtils.close(contents);
            }
        } catch (XMLException e) {
            throw new DBException(e);
        }
    }

    BookmarkStorage(String title, String description, Image image, String dataSourceId, List<String> dataSourcePath)
    {
        this.title = title;
        this.description = description;
        this.image = image;
        this.dataSourceId = dataSourceId;
        this.dataSourcePath = dataSourcePath;
    }

    public void dispose()
    {
        if (image != null) {
            image.dispose();
        }
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

    public Image getImage()
    {
        return image;
    }

    public String getDataSourceId()
    {
        return dataSourceId;
    }

    public Collection<String> getDataSourcePath()
    {
        return dataSourcePath;
    }

    public ByteArrayInputStream serialize() throws IOException
    {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream(5000);
        XMLBuilder xml = new XMLBuilder(buffer, ContentUtils.getDefaultFileEncoding());
        xml.startElement("bookmark");
        xml.addAttribute("title", title);
        if (description != null) {
            xml.addAttribute("description", description);
        }
        xml.addAttribute("data-source", dataSourceId);
        for (String path : dataSourcePath) {
            xml.startElement("path");
            xml.addText(path);
            xml.endElement();
        }

        {
            xml.startElement("image");

            ImageLoader loader = new ImageLoader();
            loader.data = new ImageData[] {image.getImageData()};
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
