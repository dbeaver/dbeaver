package org.jkiss.tools.ant.driverman;

import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.xml.XMLException;
import org.jkiss.utils.xml.XMLUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Driver information
 */
class DriverInfo {
    private File path;
    private String id;
    private String pluginId;
    private String name;
    private String version;
    private String vendor;
    private String description;
    private String license;
    private List<String> files = new ArrayList<String>();

    DriverInfo(File path)
        throws IllegalArgumentException
    {
        File pluginFile = new File(path, "plugin.xml");
        File metainfFile = new File(path, "META-INF/MANIFEST.MF");
        if (!metainfFile.exists()) {
            throw new IllegalArgumentException("No MANIFEST.MF file");
        }
        if (!pluginFile.exists()) {
            throw new IllegalArgumentException("No plugin.xml file");
        }
        Properties props = new Properties();
        try {
            FileReader propReader = new FileReader(metainfFile);
            props.load(propReader);
            propReader.close();
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }

        this.path = path;
        this.id = path.getName().toLowerCase();
        this.pluginId = props.getProperty("Bundle-SymbolicName", id);
        if (this.pluginId.indexOf(';') != -1) {
            this.pluginId = this.pluginId.substring(0, this.pluginId.indexOf(';'));
        }
        this.name = props.getProperty("Bundle-Name", id);
        this.version = props.getProperty("Bundle-Version", "1.0.0");
        this.vendor = props.getProperty("Bundle-Vendor", "Unknown");
        this.description = props.getProperty("Bundle-Description", this.name + " database driver");

        try {
            Document pluginDocument = XMLUtils.parseDocument(pluginFile);
            NodeList resourceNodes = pluginDocument.getElementsByTagName("resource");
            for (int i = 0; i < resourceNodes.getLength(); i++) {
                Element resourceElement = (Element)resourceNodes.item(i);
                String resourceName = resourceElement.getAttribute("name");
                if (resourceName.toLowerCase().contains("license.txt")) {
                    this.license = resourceName;
                } else {
                    this.files.add(resourceName);
                }
            }
        } catch (XMLException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public String getPluginID()
    {
        return pluginId;
    }

    public String getFeatureID()
    {
        return pluginId;
    }

    public File getPath()
    {
        return path;
    }

    public String getId()
    {
        return id;
    }

    public String getName()
    {
        return name;
    }

    public String getVersion()
    {
        return version;
    }

    public String getVendor()
    {
        return vendor;
    }

    public String getLicense()
    {
        return license;
    }

    public List<String> getFiles()
    {
        return files;
    }

    public String getDescription()
    {
        return description;
    }

    @Override
    public String toString() {
        return pluginId;
    }
}
