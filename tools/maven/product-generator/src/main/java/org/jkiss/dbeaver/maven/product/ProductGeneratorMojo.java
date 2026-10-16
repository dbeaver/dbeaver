/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
package org.jkiss.dbeaver.maven.product;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;

@Mojo(name = "generate-product", defaultPhase = LifecyclePhase.GENERATE_RESOURCES, threadSafe = true)
public class ProductGeneratorMojo extends AbstractMojo {
    @Parameter(property = "product.sourceDirectory", defaultValue = "${project.basedir}", required = true)
    private File sourceDirectory;

    @Parameter(
        property = "product.outputDirectory",
        defaultValue = "${project.build.directory}/generated-products",
        required = true
    )
    private File outputDirectory;

    @Parameter(property = "product.buildDirectory", defaultValue = "${project.build.directory}", required = true)
    private File buildDirectory;

    @Override
    public void execute() throws MojoExecutionException {
        try {
            int generated = new ProductGenerator(getClass().getClassLoader()).generate(
                sourceDirectory.toPath(),
                outputDirectory.toPath(),
                buildDirectory.toPath()
            );
            if (generated > 0) {
                getLog().info("Generated " + generated + " product descriptor(s) in " + outputDirectory);
            } else {
                getLog().debug("No product descriptors found in " + sourceDirectory);
            }
        } catch (ProductGenerationException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }
}
