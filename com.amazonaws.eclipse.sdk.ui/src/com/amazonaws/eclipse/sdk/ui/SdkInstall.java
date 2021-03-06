/*
 * Copyright 2010-2011 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package com.amazonaws.eclipse.sdk.ui;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IJavaProject;

/**
 * Represents an installed version of the AWS SDK for Java. Provides utilities for
 * accessing parts of the AWS SDK for Java and validating the SDK install.
 */
public class SdkInstall {

    /** The library directory within this SDK install */
    private File libDirectory;

    /** The third-party library directory within this SDK install */
    private File thirdPartyDirectory;

    /** The root directory of this SDK install */
    private final File sdkRootDirectory;

    /** File name for pulling properties about sample code */
    private static final String SAMPLE_PROPERTIES_FILENAME = "sample.properties";

    /** File path for SDK version/release info */
    private static final String VERSION_INFO_PROPERTIES_PATH = "com/amazonaws/sdk/versionInfo.properties";


    /**
     * Constructs a new SdkInstall object from the specified SDK install
     * directory.
     *
     * @param sdkRootDirectory
     *            The root directory of an AWS SDK install.
     */
    public SdkInstall(File sdkRootDirectory) {
        this.sdkRootDirectory = sdkRootDirectory;
        libDirectory = new File(sdkRootDirectory, "lib");
        thirdPartyDirectory = new File(sdkRootDirectory, "third-party");
    }

    /**
     * Returns the root directory where this SDK version is installed.
     *
     * @return The root directory where this SDK version is installed.
     */
    public File getRootDirectory() {
        return sdkRootDirectory;
    }

    /**
     * Returns true if this object represents a valid AWS SDK for Java install (i.e.
     * the correct libraries are present).
     *
     * @return True if this object represents a valid AWS SDK for Java install.
     */
    public boolean isValidSdkInstall() {
        return sdkRootDirectory.exists() && libDirectory.exists() && thirdPartyDirectory.exists();
    }

    /**
     * Returns the Jar file containing the SDK classes.
     *
     * @return The Jar file containing the SDK classes.
     */
    public File getSdkJar() throws FileNotFoundException {
        File[] files = libDirectory.listFiles(
                new FilenameFilters.SdkLibraryJarFilenameFilter());
        if (files == null || files.length == 0) {
            throw new FileNotFoundException("Could not find an SDK Jar");
        }
        return files[0];
    }

    /**
     * Returns version release notes pertaining to this particular version of the SDK
     *
     * @return The version notes pertaining to this SDK install.
     */
    public String getVersionNotes() {
        try {
            JarFile jarFile = new JarFile(getSdkJar());
            ZipEntry zipEntry = jarFile.getEntry(VERSION_INFO_PROPERTIES_PATH);

            Properties properties = new Properties();
            properties.load(jarFile.getInputStream(zipEntry));

            if (properties.containsKey("notes")) {
                return properties.getProperty("notes");
            }
        } catch (IOException e) { }

        return "Could not find any version notes for this version of the SDK.";
    }

    /**
     * Returns the URL of the public JavaDoc API Reference for this version of the AWS SDK for Java.
     *
     * @return the URL of the public JavaDoc API Reference for this version of the AWS SDK for Java.
     */
    public String getJavadocURL() {
        try {
            JarFile jarFile = new JarFile(getSdkJar());
            ZipEntry zipEntry = jarFile.getEntry(VERSION_INFO_PROPERTIES_PATH);

            Properties properties = new Properties();
            properties.load(jarFile.getInputStream(zipEntry));

            if (properties.containsKey("javadoc_url")) {
                return properties.getProperty("javadoc_url");
            }
        } catch (IOException e) { }

        return null;
    }

    /**
     * Returns the Jar file containing the SDK source.
     *
     * @return The Jar file containing the SDK source.
     */
    public File getSdkSourceJar() throws FileNotFoundException {
        File[] files = libDirectory.listFiles(
                new FilenameFilters.SdkSourceJarFilenameFilter());
        if (files == null || files.length == 0) {
            throw new FileNotFoundException("Could not find an SDK source Jar");
        }
        return files[0];
    }

    /**
     * Returns a list of all the third-party dependency Jar files for this AWS
     * SDK for Java.
     *
     * @return A list of all the third-party dependency Jar files for this AWS
     *         SDK for Java.
     */
    public List<File> getThirdPartyJars() {
        List<File> thirdPartyJars = new ArrayList<File>();
        // Search each subdirectory of the third-party directory
        // for included third-party jars
        for (File file : thirdPartyDirectory.listFiles()) {
            if (!file.isDirectory()) continue;

            thirdPartyJars.addAll(Arrays.asList(
                    file.listFiles(new FilenameFilters.JarFilenameFilter())));
        }

        return thirdPartyJars;
    }

    /**
     * Returns the version identifier for this SDK install, if known.
     *
     * @return The version identifier for this SDK install, if known.
     */
    public String getVersion() {
        try {
            JarFile jarFile = new JarFile(getSdkJar());
            ZipEntry zipEntry = jarFile.getEntry(VERSION_INFO_PROPERTIES_PATH);

            Properties properties = new Properties();
            properties.load(jarFile.getInputStream(zipEntry));

            return properties.getProperty("version");
        } catch (IOException e) {
            e.printStackTrace();
        }

        return "Unknown";
    }

    /**
     * Returns a list of samples included in this SDK install.
     *
     * @return A list of samples included in this SDK install.
     */
    public List<SdkSample> getSamples() {
        IPath rootDirectoryPath = new Path(getRootDirectory().getAbsolutePath());
        IPath samplesPath = rootDirectoryPath.append("samples");

        File[] sampleDirectories = samplesPath.toFile().listFiles(
                new SdkSampleDirectoryFilter());

        List<SdkSample> samples = new ArrayList<SdkSample>();
        if (sampleDirectories == null || sampleDirectories.length == 0) {
            return samples;
        }

        for (File file : sampleDirectories) {
            FileInputStream inputStream = null;
            try {
                inputStream = new FileInputStream(new File(file, SAMPLE_PROPERTIES_FILENAME));
                Properties properties = new Properties();
                properties.load(inputStream);
                samples.add(new SdkSample(
                        properties.getProperty("name"),
                        properties.getProperty("description"),
                        new Path(file.getAbsolutePath())));
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {inputStream.close();} catch (Exception e) {}
            }
        }

        return samples;
    }

    /**
     * Writes a metadata file to the SDK Plugin root directory specifying which version
     * of the AWS SDK for Java the specified project is using.
     * @param javaProject The project using this SdkInstall.
     * @throws IOException if the plugin root directory could not be written to.
     */
    public void writeMetadataToProject(IJavaProject javaProject) throws IOException {
        SdkProjectMetadata sdkProjectMetadataFile =
            new SdkProjectMetadata(javaProject.getProject());
        sdkProjectMetadataFile.setSdkInstallRootForProject(this.getRootDirectory());
    }

    /*
     * Private Interface
     */

    /**
     * Simple FileFilter implementation that checks a file to see if it is an
     * AWS SDK for Java sample directory, by looking for the properties file
     * describing the sample.
     */
    private static final class SdkSampleDirectoryFilter implements FileFilter {
        /**
         * @see java.io.FileFilter#accept(java.io.File)
         */
        public boolean accept(File pathname) {
            return new File(pathname, SAMPLE_PROPERTIES_FILENAME).exists();
        }
    }

}
