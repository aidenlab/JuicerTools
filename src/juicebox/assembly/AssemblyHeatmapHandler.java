/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2011-2017 Broad Institute, Aiden Lab
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
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
 */

package juicebox.assembly;

import juicebox.HiCGlobals;
import juicebox.data.Block;
import juicebox.data.ContactRecord;
import juicebox.gui.SuperAdapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by muhammadsaadshamim on 4/17/17.
 */
public class AssemblyHeatmapHandler {

    private static SuperAdapter superAdapter;
    private static List<Scaffold> listOfOSortedAggregateScaffolds = new ArrayList<>();

    public static void setListOfOSortedAggregateScaffolds(List<Scaffold> listOfAggregateScaffolds) {
        AssemblyHeatmapHandler.listOfOSortedAggregateScaffolds = new ArrayList<>(listOfAggregateScaffolds);
        Collections.sort(listOfOSortedAggregateScaffolds, Scaffold.originalStateComparator);
    }

    public static SuperAdapter getSuperAdapter() {
        return AssemblyHeatmapHandler.superAdapter;
    }

    public static void setSuperAdapter(SuperAdapter superAdapter) {
        AssemblyHeatmapHandler.superAdapter = superAdapter;
    }

    public static Block modifyBlock(Block block, String key, int binSize, int chr1Idx, int chr2Idx, AssemblyScaffoldHandler aFragHandler) {
        List<ContactRecord> alteredContacts = new ArrayList<>();
        for (ContactRecord record : block.getContactRecords()) {

            int alteredAsmBinX = getAlteredAsmBin(record.getBinX(), binSize);
            int alteredAsmBinY = getAlteredAsmBin(record.getBinY(), binSize);

            if (alteredAsmBinX == -1 || alteredAsmBinY == -1) {
                alteredContacts.add(record);
            } else {
                if (alteredAsmBinX > alteredAsmBinY) {
                    alteredContacts.add(new ContactRecord(
                            alteredAsmBinY,
                            alteredAsmBinX, record.getCounts()));
                } else {
                    alteredContacts.add(new ContactRecord(
                            alteredAsmBinX,
                            alteredAsmBinY, record.getCounts()));
                }
            }
        }
        block = new Block(block.getNumber(), alteredContacts, key);
        return block;
    }


    private static int getAlteredAsmBin(int binValue, int binSize) {

        long originalBinCenterCoordinate = (long) ((binValue + 1 / 2) * HiCGlobals.hicMapScale * binSize);
        long currentBinCenterCoordinate;
        Scaffold aggregateScaffold = lookUpOriginalAggregateScaffold(originalBinCenterCoordinate);
        if (aggregateScaffold == null) {
            return -1;
        } else {
            if (!aggregateScaffold.getInvertedVsInitial()) {
                currentBinCenterCoordinate = (aggregateScaffold.getCurrentStart() + originalBinCenterCoordinate - aggregateScaffold.getOriginalStart());
            } else {
                currentBinCenterCoordinate = (aggregateScaffold.getCurrentStart() - originalBinCenterCoordinate + aggregateScaffold.getOriginalEnd());
            }
            return (int) ((currentBinCenterCoordinate / (HiCGlobals.hicMapScale * binSize)) - 1 / 2);
        }
    }

    public static Scaffold lookUpOriginalAggregateScaffold(long genomicPos) {
        Scaffold tmp = new Scaffold("tmp", 1, 1);
        tmp.setOriginalStart(genomicPos);
        int idx = Collections.binarySearch(listOfOSortedAggregateScaffolds, tmp, Scaffold.originalStateComparator);
        if (-idx - 2 >= 0)
            return listOfOSortedAggregateScaffolds.get(-idx - 2);
        else
            return null;

    }
}