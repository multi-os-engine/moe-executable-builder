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

package org.moe.executable_builder;

import org.moe.common.constants.ProductType;
import org.moe.common.sdk.MOESDK;
import org.moe.common.variant.ArchitectureVariant;
import org.moe.common.variant.ModeVariant;
import org.moe.common.variant.TargetVariant;
import org.moe.executable_builder.task.*;
import org.apache.commons.cli.*;

import java.io.IOException;
import java.util.Collection;


public class Main {

    public static final String OPTION_DEX_FILES = "dex_files";
    public static final String OPTION_MOE_MODULE_PATH = "module_path";
    public static final String OPTION_MODE_VARIANT = "mode_variant";
    public static final String OPTION_PLATFORM_NAME = "platform_name";
    public static final String OPTION_SOURCE_SET = "source_set";
    public static final String OPTION_PROVISIONING_PROFILE = "prov_profile";
    public static final String OPTION_SIGNING_IDENTITY = "sign_identity";
    public static final String PRODUCT_TYPE = "product_type";

    public static void main(String args[]) {

        //big TODO: handle exceptions and return code from application
        // TODO: Decide: use or not to use File.separator

        MOESDK sdk = new MOESDK();

        // create the command line parser
        CommandLineParser parser = new DefaultParser();

        // TODO: read in separate place: method or class
        Options options = new Options();

        Option opt_modulePath = Option.builder(OPTION_MOE_MODULE_PATH)
                .hasArg()
                .required(true)
                .desc("Absolute path to unzipped MOE module to build")
                .build();

        Option opt_dexFiles = Option.builder(OPTION_DEX_FILES)
                .hasArg()
                .valueSeparator(',')
                .required(true)
                .desc("Comma-separated dex files for dex2oat input. Not includes moe-core.dex and moe-ios-retro.jar")
                .build();

        Option opt_modeVariant = Option.builder(OPTION_MODE_VARIANT)
                .hasArg()
                .required(true)
                .desc("Release or Debug ")
                .build();

        Option opt_platformName = Option.builder(OPTION_PLATFORM_NAME)
                .hasArg()
                .required(true)
                .desc("iphoneos or iphonesimulator")
                .build();

        Option opt_sourceSet = Option.builder(OPTION_SOURCE_SET)
                .hasArg()
                .required(true)
                .desc("main or test")
                .build();

        Option opt_provisioningProfile = Option.builder(OPTION_PROVISIONING_PROFILE)
                .hasArg()
                .required(false)
                .desc("/Users/<user_name>/Library/MobileDevice/Provisioning Profiles/<ID>.mobileprovision")
                .build();

        Option opt_signingIdentity = Option.builder(OPTION_SIGNING_IDENTITY)
                .hasArg()
                .required(false)
                .desc("iPhone Developer | iPhone Distributor : <developer name> (<developer ID>)")
                .build();

        Option opt_productType = Option.builder(PRODUCT_TYPE)
                .hasArg()
                .required(false)
                .desc("app | ipa")
                .build();

        options.addOption(opt_dexFiles);
        options.addOption(opt_modulePath);
        options.addOption(opt_modeVariant);
        options.addOption(opt_platformName);
        options.addOption(opt_sourceSet);
        options.addOption(opt_provisioningProfile);
        options.addOption(opt_signingIdentity);
        options.addOption(opt_productType);


        String[] dexFilesArray = null;
        String modulePath = null, mode = null, platform = null, sourceSet = null;
        String provisioningProfile = null, signingIdentity = null;
        ProductType productType = ProductType.app;
        try {

            CommandLine line = parser.parse(options, args);

            dexFilesArray = line.getOptionValues(OPTION_DEX_FILES);
            modulePath = line.getOptionValue(OPTION_MOE_MODULE_PATH);
            mode = line.getOptionValue(OPTION_MODE_VARIANT);
            platform = line.getOptionValue(OPTION_PLATFORM_NAME);
            sourceSet = line.getOptionValue(OPTION_SOURCE_SET);
            provisioningProfile = line.getOptionValue(OPTION_PROVISIONING_PROFILE);
            signingIdentity = line.getOptionValue(OPTION_SIGNING_IDENTITY);
            productType = ProductType.valueOf(line.getOptionValue(PRODUCT_TYPE));

        } catch (ParseException exp) {
            System.out.println("Unexpected exception:" + exp.getMessage());

            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("java -jar moeExecutableBuilder.jar", options, true);

            System.exit(1);
        }

        ModeVariant modeVariant = null;
        TargetVariant targetVariant = null;
        try {
            modeVariant = ModeVariant.getModeVariant(mode);
            targetVariant = TargetVariant.getTargetVariantByPlatformName(platform);

        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
        Collection<ArchitectureVariant> architectures = ArchitectureVariant.getSupportedArchitectureVariants(targetVariant);

        TaskManager taskManager = new TaskManager();

        try {

            for (ArchitectureVariant arch : architectures) {
                Dex2oat dex2oat = new Dex2oat(
                        sdk,
                        modulePath,
                        sourceSet,
                        modeVariant,
                        dexFilesArray,
                        arch,
                        targetVariant
                );
                taskManager.addTask(dex2oat);
            }

            if ((provisioningProfile == null) || provisioningProfile.isEmpty()) {
                System.err.print("ProvisioningProfile is null or empty.");
                System.exit(1);
            }
            if ((signingIdentity == null) || signingIdentity.isEmpty()) {
                System.err.print("SigningIdentity is null or empty.");
                System.exit(1);
            }

            IBTool ibTask = new IBTool(sourceSet, modulePath);
            taskManager.addTask(ibTask);

            AppBuild buildApp = new AppBuild(modulePath, modeVariant, targetVariant, signingIdentity, provisioningProfile);
            taskManager.addTask(buildApp);

            if (productType == ProductType.ipa) {
                IpaBuild buildIpa = new IpaBuild(buildApp.getAppFile(), buildApp.getOutPath(), provisioningProfile, signingIdentity);
                taskManager.addTask(buildIpa);
            }

        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }

        // TODO: is worst to implement mechanism of task dependencies?
        taskManager.runAll();
    }
}
