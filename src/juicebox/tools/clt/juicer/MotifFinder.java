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

package juicebox.tools.clt.juicer;

import javastraw.feature1D.GenomeWide1DList;
import javastraw.feature2D.Feature2DList;
import javastraw.feature2D.Feature2DParser;
import javastraw.reader.basics.ChromosomeHandler;
import javastraw.reader.basics.ChromosomeTools;
import juicebox.data.MotifAnchor;
import juicebox.data.MotifAnchorParser;
import juicebox.data.MotifAnchorTools;
import juicebox.tools.clt.CommandLineParserForJuicer;
import juicebox.tools.clt.JuicerCLT;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by muhammadsaadshamim on 9/4/15.
 */
public class MotifFinder extends JuicerCLT {

    private final List<String> tierOneFiles = new ArrayList<>();
    private final List<String> tierTwoFiles = new ArrayList<>();
    private final List<String> tierThreeFiles = new ArrayList<>();
    private String outputPath;
    private String loopListPath;
    private String genomeID;
    private List<String> proteinsForUniqueMotifPaths, proteinsForInferredMotifPaths;
    private String globalMotifListPath;
    private GenomeWide1DList<MotifAnchor> genomeWideAnchorsList = new GenomeWide1DList<>();

    public MotifFinder() {
        super("motifs <genomeID> <bed_file_dir> <looplist> [custom_global_motif_list]");
        MotifAnchor.uniquenessShouldSupercedeConvergentRule = true;
    }

    private static GenomeWide1DList<MotifAnchor> getIntersectionOfBEDFiles(ChromosomeHandler handler, List<String> bedFiles) {
        GenomeWide1DList<MotifAnchor> proteins = MotifAnchorParser.loadFromBEDFile(handler, bedFiles.get(0));
        for (int i = 1; i < bedFiles.size(); i++) {
            GenomeWide1DList<MotifAnchor> nextProteinList = MotifAnchorParser.loadFromBEDFile(handler, bedFiles.get(i));
            MotifAnchorTools.intersectLists(proteins, nextProteinList, false);
        }
        return proteins;
    }

    @Override
    protected void readJuicerArguments(String[] args, CommandLineParserForJuicer juicerParser) {
        if (args.length != 4 && args.length != 5) {
            this.printUsageAndExit();
        }

        int i = 1;
        genomeID = args[i++];
        String bedFileDirPath = args[i++];
        loopListPath = args[i++];
        if (args.length == 5) {
            globalMotifListPath = args[i++];
        }

        if (loopListPath.endsWith(".txt")) {
            outputPath = loopListPath.substring(0, loopListPath.length() - 4) + "_with_motifs.bedpe";
        } else if (loopListPath.endsWith(".bedpe")) {
            outputPath = loopListPath.substring(0, loopListPath.length() - 6) + "_with_motifs.bedpe";
        } else {
            outputPath = loopListPath + "_with_motifs.bedpe";
        }

        try {
            retrieveAllBEDFiles(bedFileDirPath);
        } catch (Exception e) {
            System.err.println("Unable to locate BED files");
            System.err.println("All BED files should include the '.bed' extension");
            System.err.println("BED files for locating unique motifs should be located in " + bedFileDirPath + "/unique/*.bed");
            System.err.println("BED files for locating inferred motifs should be located in " + bedFileDirPath + "/inferred/*.bed");
            System.exit(54);
        }

        // TODO add specific chromosome option (i.e. use givenChromosomes)
    }

    @Override
    public void run() {
        ChromosomeHandler handler = ChromosomeTools.loadChromosomes(genomeID);

        Feature2DList features = Feature2DParser.loadFeatures(loopListPath, handler, true, null, true);

        findUniqueMotifs(handler, features);

        findInferredMotifs(handler, features);

        features.exportFeatureList(new File(outputPath), false, Feature2DList.ListFormat.NA);
        System.out.println("Motif Finder complete");
    }

    private void findInferredMotifs(ChromosomeHandler handler, Feature2DList features) {
        /*
        1. identify all peak loci that overlap multiple CTCF peaks in the track CTCF_collapsed.narrowpeak.

        2. split peak loci from step 1 by whether they are upstream peak loci or downstream peak loci.

        3. For upstream peak loci, find the best motif match in each of the overlapping CTCF peaks from step 1.
        If there is only 1 overlapping CTCF peak with a best match motif that is a forward motif, retain that
        motif as the inferred motif for the peak locus.

        4. For downstream peak loci, find the best motif match in each of the overlapping CTCF peaks from step 1.
        If there is only 1 overlapping CTCF peak with a best match motif that is a reverse motif,
        retain that motif as the inferred motif for the peak locus.

        Previously on Motif Finder:
        fourth step: the 1-d peak tracks provided in (iii) are intersected.

        fifth step: the 1-d peak track from step 4 are intersected with the genomewide motif list (best motif match)
        and split into a forward motif track and a reverse motif track.

        sixth step: upstream peak loci that did not have a unique motif are intersected with the forward motif
        track from step 5, and for each peak locus if the peak locus has only one forward motif, that is an
        inferred mapping (these motifs are outputted as 'i'). downstream peak loci that did not have a unique
        motif are intersected with the reverse motif track from step 5, and for each peak locus if the peak
        locus has only one reverse motif, that is an inferred mapping (these motifs are outputted as 'i').
        Peak loci that form loops in both directions are ignored.
         */

        GenomeWide1DList<MotifAnchor> inferredProteins = getIntersectionOfBEDFiles(handler, proteinsForInferredMotifPaths);
        GenomeWide1DList<MotifAnchor> featureAnchors = MotifAnchorTools.extractAnchorsFromIntrachromosomalFeatures(features, true, handler);

        GenomeWide1DList<MotifAnchor> globalAnchors = retrieveFreshMotifs();
        GenomeWide1DList<MotifAnchor> upStreamAnchors = MotifAnchorTools.extractDirectionalAnchors(featureAnchors, true);
        MotifAnchorTools.retainProteinsInLocus(inferredProteins, upStreamAnchors, false, true);
        MotifAnchorTools.retainBestMotifsInLocus(globalAnchors, inferredProteins);
        MotifAnchorTools.updateOriginalFeatures(globalAnchors, false, 1);

        // reset
        inferredProteins = getIntersectionOfBEDFiles(handler, proteinsForInferredMotifPaths);
        globalAnchors = retrieveFreshMotifs();

        GenomeWide1DList<MotifAnchor> downStreamAnchors = MotifAnchorTools.extractDirectionalAnchors(featureAnchors, false);
        MotifAnchorTools.retainProteinsInLocus(inferredProteins, downStreamAnchors, false, true);
        MotifAnchorTools.retainBestMotifsInLocus(globalAnchors, inferredProteins);
        MotifAnchorTools.updateOriginalFeatures(globalAnchors, false, 1);
        MotifAnchorTools.updateOriginalFeatures(globalAnchors, false, -1);
    }

    private void setUpThreeTieredFiltration() {
        for (String filename : proteinsForUniqueMotifPaths) {
            String nameLC = filename.toLowerCase();
            if (nameLC.contains("ctcf") || nameLC.startsWith("1")) {
                tierOneFiles.add(filename);
            } else if (nameLC.contains("rad21") || nameLC.startsWith("2")) {
                tierTwoFiles.add(filename);
            } else {
                // smc3, and anything else
                tierThreeFiles.add(filename);
            }
        }
    }

    private GenomeWide1DList<MotifAnchor> getThreeTierFilteredProteinTrack(ChromosomeHandler handler,
                                                                           GenomeWide1DList<MotifAnchor> baseList) {

        if (tierOneFiles.size() > 0) {
            GenomeWide1DList<MotifAnchor> tierOneProteins = getIntersectionOfBEDFiles(handler, tierOneFiles);
            MotifAnchorTools.retainProteinsInLocus(tierOneProteins, baseList, true, true);

            if (tierTwoFiles.size() > 0) {
                GenomeWide1DList<MotifAnchor> tierTwoProteins = getIntersectionOfBEDFiles(handler, tierTwoFiles);
                if (tierTwoProteins.size() > 0) {
                    MotifAnchorTools.preservativeIntersectLists(tierOneProteins, tierTwoProteins, false);
                }
            }

            if (tierThreeFiles.size() > 0) {
                GenomeWide1DList<MotifAnchor> tierThreeProteins = getIntersectionOfBEDFiles(handler, tierThreeFiles);
                if (tierThreeProteins.size() > 0) {
                    MotifAnchorTools.preservativeIntersectLists(tierOneProteins, tierThreeProteins, false);
                }
            }

            return tierOneProteins;
        } else {
            // no files
            System.err.println("No CTCF files provided");
            System.exit(55);
        }
        return null;
    }

    private void findUniqueMotifs(ChromosomeHandler handler, Feature2DList features) {

        /*
         1. take in loop list

         2. convert to anchor list by splitting out each anchor of each loop, removing duplicates,
         merging adjacent anchors, and expanding anchors to 15kb if they are less than 15kb --->
         (should be 12,903 peak loci using GEO GM12878 loop list)

         3. intersecting peak loci with CTCF track, retaining CTCF sites that are unique in a peak
         locus ---> (should be 8,011 CTCF sites using CTCF_collasped.narrowpeak track and 12,903 peak
         loci from step 2)

         4 (optional). intersecting unique CTCF sites with RAD21 sites and retaining sites that overlap a RAD21--->
         (should be 7,727 sites left using 8,011 from above and RAD21_collapsed.narrowPeak track)

         5. (optional). intersection unique CTCF+RAD21 sites with SMC3 sites and retaining sites that
         overlap an SMC3. ---> (should be 7,599 sites left using 7,727 sites from above and SMC3 track)

         6. intersecting list from latest step of {3,4,5} with motif list and retaining best
         (positive match score) motifs in each of the sites from latest step of {3,4,5}
         (Throw out sites that do not have a positive match score; if a huge number of sites
         are getting filtered here, we may want to increase the p-val threshold parameter on FIMO).
         */
        setUpThreeTieredFiltration();
        GenomeWide1DList<MotifAnchor> featureAnchors = MotifAnchorTools.extractAnchorsFromIntrachromosomalFeatures(features,
                false, handler);
        GenomeWide1DList<MotifAnchor> threeTierFilteredProteins = getThreeTierFilteredProteinTrack(handler, featureAnchors);

        GenomeWide1DList<MotifAnchor> globalAnchors = retrieveFreshMotifs();
        //MotifAnchorTools.intersectLists(threeTierFilteredProteins,globalAnchors, true);

        MotifAnchorTools.retainBestMotifsInLocus(globalAnchors, threeTierFilteredProteins);
        MotifAnchorTools.updateOriginalFeatures(globalAnchors, true, 0);
    }

    private GenomeWide1DList<MotifAnchor> retrieveFreshMotifs() {
        if (genomeWideAnchorsList.size() < 10) {
            GenomeWide1DList<MotifAnchor> anchors;
            if (globalMotifListPath == null || globalMotifListPath.length() < 1) {
                anchors = MotifAnchorParser.loadMotifsFromGenomeID(genomeID, null);
            } else {
                if (globalMotifListPath.contains("http")) {
                    // url
                    anchors = MotifAnchorParser.loadMotifsFromURL(globalMotifListPath, genomeID, null);
                } else {
                    // local file
                    anchors = MotifAnchorParser.loadMotifsFromLocalFile(globalMotifListPath, genomeID, null);
                }
            }
            genomeWideAnchorsList = new GenomeWide1DList<>(anchors);
            return anchors;
        } else {
            return new GenomeWide1DList<>(genomeWideAnchorsList);
        }
    }

    private void retrieveAllBEDFiles(String path) throws IOException {
        File bedFileDir = new File(path);
        if (bedFileDir.exists()) {
            String uniqueBEDFilesPath = path + "/unique";
            String inferredBEDFilesPath = path + "/inferred";

            // if the '/' was already included
            if (path.endsWith("/")) {
                uniqueBEDFilesPath = path + "unique";
                inferredBEDFilesPath = path + "inferred";
            }

            proteinsForUniqueMotifPaths = retrieveBEDFilesByExtensionInFolder(uniqueBEDFilesPath, "Unique");
            proteinsForInferredMotifPaths = retrieveBEDFilesByExtensionInFolder(inferredBEDFilesPath, "Inferred");
        } else {
            throw new IOException("BED files directory not valid");
        }
    }

    private List<String> retrieveBEDFilesByExtensionInFolder(String directoryPath, String description) throws IOException {

        List<String> bedFiles = new ArrayList<>();

        File folder = new File(directoryPath);
        File[] listOfFiles = folder.listFiles();

        for (File file : listOfFiles) {
            if (file.isFile()) {
                String path = file.getAbsolutePath();
                if (path.endsWith(".bed")) {
                    bedFiles.add(path);
                }
            }
        }

        if (bedFiles.size() < 1) {
            throw new IOException(description + " BED files not found");
        }

        return bedFiles;
    }
}
