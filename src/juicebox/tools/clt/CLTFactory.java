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

import juicebox.HiCGlobals;
import juicebox.tools.clt.juicer.*;


/**
 * Factory for command line tools to call different functions
 *
 * @author Muhammad Shamim
 * @since 1/30/2015
 */
public class CLTFactory {

    // Commenting some out because we're not going to release all these when we release CLT
    private final static String[] commandLineToolUsages = {
            APA.getBasicUsage(),
            Arrowhead.getBasicUsage(),
            HiCCUPS.getBasicUsage(),
            HiCCUPSDiff.getBasicUsage(),
            APAvsDistance.getBasicUsage()
    };

    public static void generalUsage() {
        System.out.println("Juicer Tools Version " + HiCGlobals.versionNum);
        System.out.println("Usage:");
        for (String usage : commandLineToolUsages) {
            System.out.println("\t" + usage);
        }
        System.out.println("\t" + "-h, --help print help");
        System.out.println("\t" + "-v, --verbose verbose mode");
        System.out.println("\t" + "-V, --version print version");
        System.out.println("Type juicer_tools <commandName> for more detailed usage instructions");
    }

    public static JuicerCLT getCLTCommand(String cmd) {

        cmd = cmd.toLowerCase();
        if (cmd.equals("apa")) {
            return new APA();
        } else if (cmd.equals("compare")) {
            return new CompareLists();
        } else if (cmd.equals("arrowhead")) {
            return new Arrowhead();
        } else if (cmd.equals("hiccups")) {
            return new HiCCUPS();
        } else if (cmd.equals("loop_domains")) {
            return new LoopDomains();
        } else if (cmd.equals("motifs")) {
            return new MotifFinder();
        } else if (cmd.equals("hiccupsdiff")) {
            return new HiCCUPSDiff();
        } else if (cmd.equals("apa_vs_distance")) { //Todo check if okay
            return new APAvsDistance();
        }


        return null;
    }
}
