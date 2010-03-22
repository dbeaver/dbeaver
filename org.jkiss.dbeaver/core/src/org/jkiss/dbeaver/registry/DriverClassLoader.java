package org.jkiss.dbeaver.registry;

import java.net.URL;
import java.net.URLClassLoader;

/**
 * DriverClassLoader
 */
public class DriverClassLoader extends URLClassLoader
{
    public DriverClassLoader(URL[] urls, ClassLoader parent)
    {
        super(urls, parent);
    }
}
