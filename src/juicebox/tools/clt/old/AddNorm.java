/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2011-2021 Broad Institute, Aiden Lab, Rice University, Baylor College of Medicine
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
 */

package juicebox.tools.clt.old;

import juicebox.HiCGlobals;
import juicebox.tools.clt.CommandLineParser;
import juicebox.tools.clt.JuiceboxCLT;
import juicebox.tools.utils.norm.CustomNormVectorFileHandler;
import juicebox.tools.utils.norm.MultithreadedNormalizationVectorUpdater;
import juicebox.tools.utils.norm.NormalizationVectorUpdater;
import juicebox.windowui.NormalizationHandler;
import juicebox.windowui.NormalizationType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class AddNorm extends JuiceboxCLT {

    private boolean noFragNorm = false;
    private String inputVectorFile = null;
    private int genomeWideResolution = -100;
    private String file;
    private final List<NormalizationType> normalizationTypes = new ArrayList<>();
    private Map<NormalizationType, Integer> resolutionsToBuildTo;
    protected static int numCPUThreads = 1;

    public AddNorm() {
        super(getBasicUsage()+"\n"
                + "           : -d use intra chromosome (diagonal) [false]\n"
                + "           : -F don't calculate normalization for fragment-delimited maps [false]\n"
                + "           : -w <int> calculate genome-wide resolution on all resolutions >= input resolution [not set]\n"
                + " Above options ignored if input_vector_file present\n"
                + "           : -k normalizations to include\n"
                + "           : -r resolutions for respective normalizations to build to\n"
                + "           : -j number of CPU threads to use\n"
        );
    }

    public static String getBasicUsage() {
        return "addNorm <input_HiC_file> [input_vector_file]";
    }

    public static Map<NormalizationType, Integer> defaultHashMapForResToBuildTo(List<NormalizationType> normalizationTypes) {
        HashMap<NormalizationType, Integer> map = new HashMap<>();
        for (NormalizationType norm : normalizationTypes) {
            map.put(norm, NormalizationHandler.getIdealResolutionLimit(norm));
        }
        return map;
    }

    @Override
    public void readArguments(String[] args, CommandLineParser parser) {
        if (parser.getHelpOption()) {
            printUsageAndExit();
        }

        if (args.length == 3) {
            inputVectorFile = args[2];
        } else if (args.length != 2) {
            printUsageAndExit();
        }
        noFragNorm = parser.getNoFragNormOption();
        HiCGlobals.SAVE_CONTACT_RECORDS_IN_RAM = parser.getPutAllContactsIntoRAM();
        genomeWideResolution = parser.getGenomeWideOption();
        normalizationTypes.addAll(parser.getAllNormalizationTypesOption());
        resolutionsToBuildTo = defaultHashMapForResToBuildTo(normalizationTypes);

        List<String> resolutions = parser.getResolutionOption();
        if (resolutions != null && resolutions.size() > 0) {
            if (resolutions.size() != normalizationTypes.size()) {
                System.err.println("Error: Number of resolutions and normalizations need to be the same");
                System.exit(0);
            }

            for (int k = 0; k < resolutions.size(); k++) {
                NormalizationType normType = normalizationTypes.get(k);
                try {
                    int resVal = Integer.parseInt(resolutions.get(k));
                    resolutionsToBuildTo.put(normType, resVal);
                } catch (Exception e) {
                    resolutionsToBuildTo.put(normType, NormalizationHandler.getIdealResolutionLimit(normType));
                }
            }
        }

        updateNumberOfCPUThreads(parser);

        file = args[1];
    }

    @Override
    public void run() {
        HiCGlobals.allowDynamicBlockIndex = false;
        try {
            if (inputVectorFile != null) {
                CustomNormVectorFileHandler.updateHicFile(file, inputVectorFile);
            }
            else if (numCPUThreads==1){
                (new NormalizationVectorUpdater()).updateHicFile(file, normalizationTypes, resolutionsToBuildTo, genomeWideResolution, noFragNorm);
            }
            else {
                MultithreadedNormalizationVectorUpdater updater = new MultithreadedNormalizationVectorUpdater();
                updater.setNumCPUThreads(numCPUThreads);
                updater.updateHicFile(file,normalizationTypes,resolutionsToBuildTo, genomeWideResolution, noFragNorm);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected void updateNumberOfCPUThreads(CommandLineParser juicerParser) {
        int numThreads = juicerParser.getNumThreads();
        if (numThreads > 0) {
            numCPUThreads = numThreads;
        } else if (numThreads < 0) {
            numCPUThreads = Runtime.getRuntime().availableProcessors();
        } else {
            numCPUThreads = 1;
        }
        System.out.println("Using " + numCPUThreads + " CPU thread(s)");
    }
}