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

package org.moe.executable_builder.task;

import org.moe.common.exec.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class IBTool extends BaseTask {

    private static String defaulResourceFolder = "resources";
    private static String srcFolder = "src";
    private static String defaultStoryBoardName = "MainUI.storyboard";

    private String sourceSet = "";
    private String modulePath = "";

    public IBTool(String sourceSet, String modulePath) {
        this.sourceSet = sourceSet;
        this.modulePath = modulePath;
    }

    @Override
    void launch() {

        StringBuilder mainUIStoryboardPath = new StringBuilder();
        mainUIStoryboardPath.append(modulePath);
        mainUIStoryboardPath.append(File.separator);
        mainUIStoryboardPath.append(srcFolder);
        mainUIStoryboardPath.append(File.separator);
        mainUIStoryboardPath.append(sourceSet);
        mainUIStoryboardPath.append(File.separator );
        mainUIStoryboardPath.append(defaulResourceFolder);
        mainUIStoryboardPath.append(File.separator);
        mainUIStoryboardPath.append(defaultStoryBoardName);

        File storyboardFile = new File(mainUIStoryboardPath.toString());
        if(!storyboardFile.exists()) return;

        SimpleExec exec = SimpleExec.getExec("ibtool");

        ArrayList<String> args = exec.getArguments();


        args.add(storyboardFile.getAbsolutePath());
        args.add("--write");

        args.add(storyboardFile.getAbsolutePath());
        args.add("--update-frames");

        args.add("--errors");
        args.add("--warnings");
        args.add("--notices");


        System.out.print("UITransformer IBTool check \n");
        try {
            final ExecRunner runner = exec.getRunner();
            runner.setListener(new ExecRunnerBase.ExecRunnerListener() {
                final String newLine = System.getProperty("line.separator");

                @Override
                public void stdout(String line) {
                    System.out.print(line + newLine);
                }

                @Override
                public void stderr(String line) {
                    System.err.print(line + newLine);
                }
            });

            int returnCode = runner.run(new IKillListener() {
                @Override
                public boolean needsKill() {
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                        System.out.println("IBTool thread has been finished ");
                    }
                    return true;
                }
            });
            if (returnCode != 0) {
                System.err.println("Failed " + exec.getExecPath());
                System.exit(returnCode);
            }
        } catch (IOException e) {
            System.err.println("IPA build failed: " + e.getMessage());
            System.exit(1);
        }
    }
}
