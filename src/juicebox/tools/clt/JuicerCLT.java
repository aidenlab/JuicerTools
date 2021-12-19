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

package juicebox.tools.clt;


import javastraw.reader.Dataset;
import javastraw.reader.Matrix;
import javastraw.reader.basics.Chromosome;
import javastraw.reader.basics.ChromosomeHandler;
import javastraw.reader.type.HiCZoom;
import javastraw.reader.type.NormalizationHandler;
import javastraw.reader.type.NormalizationType;
import javastraw.tools.HiCFileTools;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by muhammadsaadshamim on 9/21/15.
 */
public abstract class JuicerCLT {

    protected static int numCPUThreads = 1;
    private static String usage;
    protected Dataset dataset = null;
    protected NormalizationType norm = NormalizationHandler.SCALE;
    protected List<String> givenChromosomes = null; //TODO set to protected

    protected JuicerCLT(String usage) {
        setUsage(usage);
    }

    public static String[] splitToList(String nextLine) {
        return nextLine.trim().split("\\s+");
    }

    public static int getAppropriateNumberOfThreads(int numThreads, int defaultNum) {
        if (numThreads > 0) {
            return numThreads;
        } else if (numThreads < 0) {
            return Math.abs(numThreads) * Runtime.getRuntime().availableProcessors();
        } else {
            return defaultNum;
        }
    }

    protected int determineHowManyChromosomesWillActuallyRun(Dataset ds, ChromosomeHandler chromosomeHandler, HiCZoom zoom) {
        int maxProgressStatus = 0;
        for (Chromosome chr : chromosomeHandler.getChromosomeArrayWithoutAllByAll()) {
            Matrix matrix = ds.getMatrix(chr, chr);
            if (matrix == null) continue;
            if (matrix.getZoomData(zoom) == null) continue;
            maxProgressStatus++;
        }
        return maxProgressStatus;
    }

    public void readArguments(String[] args, CommandLineParser parser) {
        CommandLineParserForJuicer juicerParser = (CommandLineParserForJuicer)parser;
        assessIfChromosomesHaveBeenSpecified(juicerParser);
        readJuicerArguments(args, juicerParser);
    }

    protected abstract void readJuicerArguments(String[] args, CommandLineParserForJuicer juicerParser);

    private void assessIfChromosomesHaveBeenSpecified(CommandLineParserForJuicer juicerParser) {
        List<String> possibleChromosomes = juicerParser.getChromosomeListOption();
        if (possibleChromosomes != null && possibleChromosomes.size() > 0) {
            givenChromosomes = new ArrayList<>(possibleChromosomes);
        }
    }

    public String getUsage() {
        return usage;
    }

    private void setUsage(String newUsage) {
        usage = newUsage;
    }

    public abstract void run();

    public void printUsageAndExit() {
        System.out.println("Usage:   juicer_tools " + usage);
        System.exit(0);
    }

    public void printUsageAndExit(int exitcode) {
        System.out.println("Usage:   juicer_tools " + usage);
        System.exit(exitcode);
    }

    protected void setDatasetAndNorm(String files, String normType, boolean allowPrinting) {
        dataset = HiCFileTools.extractDatasetForCLT(files,
                allowPrinting, false);

        norm = dataset.getNormalizationHandler().getNormTypeFromString(normType);
        if (norm == null) {
            System.err.println("Normalization type " + norm + " unrecognized.  Normalization type must be one of \n" +
                    "\"NONE\", \"VC\", \"VC_SQRT\", \"KR\", \"GW_KR\"," +
                    " \"GW_VC\", \"INTER_KR\", \"INTER_VC\", or a custom added normalization.");
            System.exit(16);
        }
    }

    protected void updateNumberOfCPUThreads(CommandLineParser parser, int numDefaultThreads) {
        int numThreads = parser.getNumThreads();
        numCPUThreads = getAppropriateNumberOfThreads(numThreads, numDefaultThreads);
        System.out.println("Using " + numCPUThreads + " CPU thread(s) for primary task");
    }
}
