/*
Copyright 2014-2016 Intel Corporation

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package org.moe.executable_builder.helpers;


import java.io.*;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class XCodeProjectFormatter {

    final static String XCODE_PROJECT_EXTENSION = ".pbxproj";

    public XCodeProjectFormatter() {

    }

    public boolean removeShellScripts(final String xcodeProjectDirPath, final String projectName, final String projectDirPath) {
        String xCodeProjectDefaultPath = xcodeProjectDirPath + "/" + projectName + ".xcodeproj" + "/" + "project" + XCODE_PROJECT_EXTENSION;
        System.out.println("removeShellScripts: " + xCodeProjectDefaultPath);
        File xCodeProject = new File(xCodeProjectDefaultPath);

        if (xCodeProject.isFile() && xCodeProject.exists()) {
            cutStringWithInLines("Begin PBXShellScriptBuildPhase section", "End PBXShellScriptBuildPhase section", xCodeProject);
        } else {
            xCodeProject = findXCodeProject(projectDirPath);
            if (xCodeProject != null)
                cutStringWithInLines("Begin PBXShellScriptBuildPhase section", "End PBXShellScriptBuildPhase section", xCodeProject);
            else throw new RuntimeException("Couldn't find xCode project");
        }
        return findShellScript("ShellScript", xCodeProject);
    }

    private File findXCodeProject(String projectDirPath) {
        File projectDirectory = new File(projectDirPath);
        if (projectDirectory.exists() && projectDirectory.isDirectory()) {

            LinkedList<File> queue = new LinkedList<File>();
            queue.push(projectDirectory);

            while (!queue.isEmpty()) {
                File currentDirectory = queue.pop();
                File[] filesList = currentDirectory.listFiles();

                for (File file : filesList) {
                    if (file.isDirectory()) {
                        queue.push(file);
                    } else {
                        if (file.getName().endsWith(XCODE_PROJECT_EXTENSION)) return file;
                    }
                }
            }

        } else {
            throw new RuntimeException("XCode project directory is invalid (not exist), couldn't find xCode project");
        }

        return null;
    }

    private void cutLinesWithStrings(String[] prohibitedStringsArray, File sourceFile) {
        List<String> lines = new ArrayList<String>();
        String line;

        try {
            FileReader fileReader = new FileReader(sourceFile);
            BufferedReader bufferedReader = new BufferedReader(fileReader);

            while ((line = bufferedReader.readLine()) != null) {
                if (isStringContainsAtLeastOneString(line, prohibitedStringsArray))
                    continue;
                lines.add(line);
            }
            fileReader.close();
            bufferedReader.close();

            FileWriter fileWriter = new FileWriter(sourceFile);
            BufferedWriter out = new BufferedWriter(fileWriter);
            for (String s : lines) {
                out.write(s);
                out.newLine();
            }
            out.flush();
            out.close();

        } catch (Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException(ex);
        } finally {

        }
    }

    public void cutStringWithInLines(String startLine, String endLine, File sourceFile) {
        List<String> lines = new ArrayList<String>();
        String line;

        boolean shellScriptBlock = false;

        try {
            FileReader fileReader = new FileReader(sourceFile);
            BufferedReader bufferedReader = new BufferedReader(fileReader);

            while ((line = bufferedReader.readLine()) != null) {
                if (isStringContainsAtLeastOneString(line, new String[]{startLine})) {
                    shellScriptBlock = true;
                    continue;
                } else if (isStringContainsAtLeastOneString(line, new String[]{endLine})) {
                    shellScriptBlock = false;
                    continue;
                } else if (shellScriptBlock) {
                    continue;
                }


                lines.add(line);
            }
            fileReader.close();
            bufferedReader.close();

            FileWriter fileWriter = new FileWriter(sourceFile);
            BufferedWriter out = new BufferedWriter(fileWriter);
            for (String s : lines) {
                out.write(s);
                out.newLine();
            }
            out.flush();
            out.close();

        } catch (Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException(ex);
        } finally {

        }
    }

    public boolean findShellScript(String lineToFind, File sourceFile) {
        String line;

        boolean shellScriptBlock = false;

        try {
            FileReader fileReader = new FileReader(sourceFile);
            BufferedReader bufferedReader = new BufferedReader(fileReader);

            while ((line = bufferedReader.readLine()) != null) {
                if (isStringContainsAtLeastOneString(line, new String[]{lineToFind})) {
                    shellScriptBlock = true;
                    break;
                }
            }
            fileReader.close();
            bufferedReader.close();
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException(ex);
        } finally {
        }
        return shellScriptBlock;
    }

    private boolean isStringContainsAtLeastOneString(String source, String[] stringsToFind) {

        for (String str : stringsToFind) {
            if (source.contains(str)) {
                return true;
            }
        }

        return false;
    }
}

