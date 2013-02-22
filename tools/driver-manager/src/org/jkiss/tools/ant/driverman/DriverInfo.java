package org.jkiss.tools.ant.driverman;

import org.jkiss.utils.CommonUtils;

import java.io.File;
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
    private String category;
    private List<String> files = new ArrayList<String>();

    DriverInfo(File path, Properties properties)
    {
        this.path = path;
        this.id = path.getName().toLowerCase();
        this.pluginId = properties.getProperty("id", id);
        this.name = properties.getProperty("name", id);
        this.version = properties.getProperty("version", "1.0.0");
        this.vendor = properties.getProperty("vendor", "Unknown");
        this.description = properties.getProperty("description", this.name + " database driver");
        this.license = properties.getProperty("license");
        if (!CommonUtils.isEmpty(license)) {
            this.files.add(license);
        }
        for (int i = 1; ; i++) {
            String file = properties.getProperty("file" + i);
            if (file == null) {
                break;
            }
            this.files.add(file);
        }
    }

    public String getPluginID()
    {
        return "org.jkiss.dbeaver.driver." + pluginId;
    }

    public String getFeatureID()
    {
        return "org.jkiss.dbeaver.driver." + pluginId + ".feature";
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

    public String getCategory()
    {
        return category;
    }

    public void setCategory(String category)
    {
        while (category.startsWith("/")) category = category.substring(1);
        while (category.endsWith("/")) category = category.substring(0, category.length() - 1);
        this.category = category;
    }
}
