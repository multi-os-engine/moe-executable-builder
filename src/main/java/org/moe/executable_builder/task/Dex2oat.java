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

import org.moe.common.exec.ExecOutputCollector;
import org.moe.common.defaults.Dex2OatDefaults;
import org.moe.common.exec.SimpleExec;
import org.moe.common.sdk.MOESDK;
import org.moe.common.utils.FileUtil;
import org.moe.common.variant.ArchitectureVariant;
import org.moe.common.variant.ModeVariant;
import org.moe.common.variant.TargetVariant;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

public class Dex2oat extends BaseTask {

    static final private String[] BACKENDS = new String[]{"Quick"};
    static final private String INPUT_DIR = "dex";

    /*
    Task inputs
    */
    private String archFamily;
    private long base;
    private File imageClasses;
    private Collection<File> inputFiles;
    private File dex2oatExec;

    /*
    Task outputs
     */
    private File destImage;
    private File destOat;
    private File log;


    public Dex2oat(MOESDK sdk,
                   String modulePath,
                   String sourceSet,
                   ModeVariant mode,
                   String[] dexFiles,
                   ArchitectureVariant architectureVariant,
                   TargetVariant targetVariant) throws IOException {

        FileUtil.checkFile(new File(modulePath));
        this.archFamily = architectureVariant.getFamilyName();

        final String buildPath = modulePath + File.separator + "build";
        final String outPath = buildPath + File.separator +
                BaseTask.MOE + File.separator +
                sourceSet + File.separator +
                "xcode" + File.separator +
                mode.getName() + "-" + targetVariant.getPlatformName();

        new File(outPath).mkdirs();

        inputFiles = new ArrayList<File>();

        for (String dexFile : dexFiles) {
            File inputFile = new File(buildPath, BaseTask.MOE + "/" + sourceSet + "/" + mode.getName() + "/" + dexFile + ".jar");
            inputFiles.add(FileUtil.checkFile(inputFile));
        }
        for (File mainDexFile : sdk.getBindings().getMainDexFiles()) {
            inputFiles.add(FileUtil.checkFile(mainDexFile));
        }

        dex2oatExec = FileUtil.checkFile(sdk.getTools().dex2OatExec());
        imageClasses = FileUtil.checkFile(sdk.getTools().preloadedClasses());


        this.base = Dex2OatDefaults.getDefaultBaseForArchFamily(archFamily);

        this.destImage = new File(outPath, architectureVariant.getArchName() + ".art");
        this.destOat = new File(outPath, architectureVariant.getArchName() + ".oat");
        this.log = new File(outPath, "dex2oat.log");
    }

    private String getCompilerBackend() {
        return BACKENDS[0];
    }

    @Override
    void launch() {
        try {
            SimpleExec exec = SimpleExec.getExec(dex2oatExec.getAbsolutePath());

            ArrayList<String> args = exec.getArguments();

            // Set target options
            args.add("--instruction-set=" + archFamily);
            args.add("--base=0x" + Long.toHexString(base));

            // Set compiler backend
            args.add("--compiler-backend=" + getCompilerBackend());

            // Set files
            args.add("--image=" + destImage.getAbsolutePath());
            args.add("--image-classes=" + imageClasses.getAbsolutePath());
            args.add("--oat-file=" + destOat.getAbsolutePath());

            // Set inputs
            StringBuilder dexFiles = new StringBuilder();
            for (File inputFile : inputFiles) {
                if (dexFiles.length() > 0) {
                    dexFiles.append(':');
                }
                dexFiles.append(inputFile.getAbsolutePath());
            }
            args.add("--dex-file=" + dexFiles);

            // TODO: implement logger to file. (and console?)

            System.out.println(ExecOutputCollector.collect(exec));
        } catch (Exception e) {
            System.err.println("Dex2oat failed: " + e.getMessage());
        }
    }
}
