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

import org.moe.common.developer.ProvisioningProfile;
import org.moe.common.exec.ExecRunner;
import org.moe.common.exec.ExecRunnerBase;
import org.moe.common.exec.SimpleExec;
import org.moe.common.utils.FileUtil;
import org.moe.common.variant.ModeVariant;
import org.moe.common.variant.TargetVariant;
import org.moe.executable_builder.helpers.XCodeProjectFormatter;

import java.io.*;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class AppBuild extends BaseTask {

    /*
    Task inputs
     */

    private final String buildDate = new SimpleDateFormat("yyyy.MM.dd_HH.mm.ss").format(new Date());

    private String modulePath;

    private String buildPath;

    private String targetName;

    private String configuration;

    private String sdk;

    private File xcodeProjectFile;

    private List<String> additionalParameters = new ArrayList<String>();

    private File provisioningProfile;

    private String signingIdentity;

    /*
    Task outputs
     */

    private File dstRoot;

    private File objRoot;

    private File symRoot;

    private File sharedPrecompsDir;

    private File log;

    public AppBuild(String modulePath, ModeVariant modeVariant, TargetVariant targetVariant, String signingIdentity, String provisioningProfilePath) throws IOException {

        this.modulePath = modulePath;
        FileUtil.checkFile(new File(this.modulePath));

        this.buildPath = modulePath + File.separator + "build";
        FileUtil.checkFile(new File(this.buildPath));

        File xcodeProjectDir;
        try {
            xcodeProjectDir = new File(modulePath, "xcode");
            FileUtil.checkFile(xcodeProjectDir);
        } catch (Exception e) {
            xcodeProjectDir = new File(modulePath, "build/xcode");
            FileUtil.checkFile(xcodeProjectDir);
        }

        ArrayList<String> xcodeProjectFileNames = FileUtil.getFileNamesListByExtension(xcodeProjectDir, "xcodeproj", FileUtil.SearchTarget.DIRECTORIES);
        if (xcodeProjectFileNames.size() > 1) {
            System.out.println("More than one project files were found. The first one will be selected!");
        }
        targetName = xcodeProjectFileNames.get(0);

        xcodeProjectFile = new File(xcodeProjectDir.getAbsolutePath() + "/" + targetName + ".xcodeproj");
        FileUtil.checkFile(xcodeProjectFile);

        configuration = modeVariant.getName();
        sdk = targetVariant.getPlatformName();

        final String outPath = buildPath + File.separator + MOE + File.separator + "xcodebuild";
        new File(outPath).mkdirs();

        dstRoot = new File(outPath, "dst");
        dstRoot.mkdirs();

        objRoot = new File(outPath, "obj");
        objRoot.mkdirs();

        symRoot = new File(outPath, "sym");
        symRoot.mkdirs();

        sharedPrecompsDir = new File(outPath, "sym");
        sharedPrecompsDir.mkdirs();

        this.provisioningProfile = null;
        if ((provisioningProfilePath != null) && !provisioningProfilePath.isEmpty()) {
            this.provisioningProfile = new File(provisioningProfilePath);
            if (!this.provisioningProfile.exists()) {
                this.provisioningProfile = null;
            } else {
                File dst = new File(System.getProperty("user.home") + "/Library/MobileDevice/Provisioning Profiles/" + this.provisioningProfile.getName());
                System.out.println(dst.getAbsolutePath());

                if (!dst.getParentFile().exists()) {
                    dst.getParentFile().mkdirs();
                }
                System.out.println("After create.");
                FileChannel inputChannel = null;
                FileChannel outputChannel = null;
                try {
                    inputChannel = new FileInputStream(this.provisioningProfile).getChannel();
                    outputChannel = new FileOutputStream(dst).getChannel();
                    outputChannel.transferFrom(inputChannel, 0, inputChannel.size());
                } catch (Exception e) {
                    System.out.println(e.getMessage());
                } finally {
                    inputChannel.close();
                    outputChannel.close();
                }
                if (dst.exists()) {
                    this.provisioningProfile = dst;
                }
            }
        }

        this.signingIdentity = null;
        if ((signingIdentity != null) && !signingIdentity.isEmpty()) {
            this.signingIdentity = signingIdentity;
        }

        log = new File(modulePath + "/build/logs", "xcodebuild-" + buildDate + ".log");
        if (!log.getParentFile().exists()) {
            log.getParentFile().mkdirs();
        }
        log.createNewFile();
    }

    @Override
    void launch() {
        removeCustomScript();
        launchXcodeBuild();
    }

    private void removeCustomScript() {
        XCodeProjectFormatter formatter = new XCodeProjectFormatter();
        boolean isFoundShellScript = formatter.removeShellScripts(xcodeProjectFile.getParent(), targetName, modulePath);
        if (isFoundShellScript) {
            System.err.print("Error: ShellScript section in Xcode project is prohibited");
            System.exit(1);
        } else {
            System.out.println("Custom Script was removed!");
        }
    }

    private void launchXcodeBuild() {
        SimpleExec exec = SimpleExec.getExec("xcodebuild");

        ArrayList<String> args = exec.getArguments();

        // Set target options
        args.add("-target");
        args.add(targetName);

        args.add("-configuration");
        args.add(configuration);

        args.add("-sdk");
        args.add(sdk);

        args.add("-project");
        args.add(xcodeProjectFile.getAbsolutePath());

        for (String param : additionalParameters) {
            args.add(param);
        }

        args.add("DSTROOT=" + dstRoot.getAbsolutePath());
        args.add("OBJROOT=" + objRoot.getAbsolutePath());
        args.add("SYMROOT=" + symRoot.getAbsolutePath());
        args.add("SHARED_PRECOMPS_DIR=" + sharedPrecompsDir.getAbsolutePath());


        if ((this.provisioningProfile != null) && (this.signingIdentity != null)) {
            String uuid = null;
            try {
                uuid = ProvisioningProfile.getUUIDFromProfile(this.provisioningProfile);
                args.add("PROVISIONING_PROFILE=" + uuid);
            } catch (Exception e) {
                System.out.println("Invalid provisioning profile! Default one will be chosen!");
            }
            if (uuid != null) {
                args.add("CODE_SIGN_IDENTITY=" + this.signingIdentity);
            }
        }

        FileOutputStream ostream = null;

        try {
            ostream = new FileOutputStream(log);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        // TODO: implement ExecRunnerWithLog class
        final FileOutputStream finalOstream = ostream;
        assert finalOstream != null;

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
            System.err.println("App build failed: " + e.getMessage());
            System.exit(1);
        }

        try {
            finalOstream.flush();
            finalOstream.close();
        } catch (IOException ignored) {
        }

        if (!this.provisioningProfile.delete()){
            System.out.println(provisioningProfile.getAbsolutePath() + " was not deleted!");
        }
    }

    public File getAppFile() {
        return new File(symRoot.getAbsolutePath() + "/" + configuration + "-" + sdk + "/" + targetName + ".app");
    }

    public String getOutPath() {
        return buildPath + File.separator + MOE + File.separator + "xcodebuild";
    }

}
