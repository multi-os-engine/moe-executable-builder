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


import org.moe.common.exec.ExecRunner;
import org.moe.common.exec.ExecRunnerBase;
import org.moe.common.exec.SimpleExec;
import org.moe.common.utils.FileUtil;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class IpaBuild extends BaseTask {

    /*
    Task inputs
     */

    private final String buildDate = new SimpleDateFormat("yyyy.MM.dd_HH.mm.ss").format(new Date());

    private File inputApp;

    private String provisioningProfile;

    private String signingIdentity;

    /*
    Task outputs
     */

    private File outputIpa;

    private String outPath;

    private File log;

    public IpaBuild(File appFile, String outPath, String provisioningProfile, String signingIdentity) throws IOException {

        inputApp = appFile;
        this.outPath = outPath;

        String targetName = FilenameUtils.removeExtension(appFile.getName());
        String appDir = appFile.getParentFile().getAbsolutePath();
        outputIpa = new File(appDir + "/" + targetName + ".ipa");

        this.provisioningProfile = provisioningProfile;
        this.signingIdentity = signingIdentity;

        log = new File(outPath, "ipaBuild-" + buildDate + ".log");
        log.createNewFile();
    }

    @Override
    void launch() {

        try {
            FileUtil.checkFile(inputApp);
            FileUtil.checkFile(new File(outPath));
        } catch (IOException e) {
            e.printStackTrace();
        }

        SimpleExec exec = SimpleExec.getExec("xcrun");
        
        ArrayList<String> args = exec.getArguments();

        args.add("-sdk");
        args.add("iphoneos");
        args.add("PackageApplication");
        args.add("-v");
        args.add(inputApp.getAbsolutePath());
        args.add("-o");
        args.add(outputIpa.getAbsolutePath());

        FileOutputStream ostream = null;
        try {
            ostream = new FileOutputStream(log);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        final FileOutputStream finalOstream = ostream;
        assert finalOstream != null;

        // TODO: implement ExecRunnerWithLog class
        try {
            final ExecRunner runner = exec.getRunner();
            runner.setListener(new ExecRunnerBase.ExecRunnerListener() {
                final String newLine = System.getProperty("line.separator");

                @Override
                public void stdout(String line) {
                    try {
                        finalOstream.write((line + newLine).getBytes());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void stderr(String line) {
                    try {
                        finalOstream.write((line + newLine).getBytes());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
            int returnCode = runner.run(null);
            if (returnCode != 0) {
                System.err.println("Failed " + exec.getExecPath());
                System.exit(returnCode);
            }
        } catch (IOException e) {
            System.err.println("IPA build failed: " + e.getMessage());
            System.exit(1);
        }

        try {
            finalOstream.flush();
            finalOstream.close();
        } catch (IOException ignored) {
        }
    }
}
