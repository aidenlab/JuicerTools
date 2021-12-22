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

package juicebox.tools.utils.juicer.arrowhead;

import javastraw.feature2D.Feature2D;
import javastraw.feature2D.Feature2DList;
import javastraw.reader.basics.Chromosome;
import juicebox.tools.utils.common.MatrixTools;
import org.apache.commons.math3.linear.RealMatrix;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by muhammadsaadshamim on 7/20/15.
 */
public class ArrowheadScoreList {

    private int resolution = -1;
    private List<ArrowheadScore> arrowheadScores = new ArrayList<>();

    public ArrowheadScoreList(int resolution) {
        this.resolution = resolution;
    }

    public ArrowheadScoreList(Feature2DList features, Chromosome chr, int resolution) {
        this.resolution = resolution;
        if (features.getNumTotalFeatures() > 0) {
            for (Feature2D feature : features.get(chr.getIndex(), chr.getIndex())) {
                arrowheadScores.add(convertToArrowheadScore(feature));
            }
        }
    }

    private ArrowheadScore convertToArrowheadScore(Feature2D feature) {
        return new ArrowheadScore(new long[]{feature.getStart1(), feature.getEnd1(),
                feature.getStart2(), feature.getEnd2()});
    }

    public ArrowheadScoreList deepCopy() {
        ArrowheadScoreList copy = new ArrowheadScoreList(resolution);
        for (ArrowheadScore data : arrowheadScores) {
            copy.arrowheadScores.add(new ArrowheadScore(data));
        }
        return copy;
    }

    public ArrowheadScoreList updateActiveIndexScores(RealMatrix blockScore, int limStart, int limEnd) {

        setActiveListElements(limStart, limEnd);

        ArrowheadScoreList scoredList = new ArrowheadScoreList(resolution);

        for (ArrowheadScore score : arrowheadScores) {
            if (score.isActive) {
                int[] transformedIndices = scaleAndTranslateIndices(score.indices, resolution, limStart);
                score.updateScore(MatrixTools.calculateMax(MatrixTools.getSubMatrix(blockScore, transformedIndices)));
                scoredList.arrowheadScores.add(new ArrowheadScore(score));
            }
        }

        return scoredList;
    }

    private int[] scaleAndTranslateIndices(long[] indices, int resolution, int limStart) {
		int[] transformedIndices = new int[indices.length];
		for (int i = 0; i < indices.length; i++) {
			// casting should be ok for Arrowhead
			transformedIndices[i] = (int) (indices[i] / resolution - limStart);
		}
		return transformedIndices;
	}

    private void setActiveListElements(int limStart, int limEnd) {
        for (ArrowheadScore score : arrowheadScores) {
            score.isActive = false;
        }

        for (ArrowheadScore score : arrowheadScores) {
            if (score.isWithin(limStart, limEnd, resolution)) {
                score.isActive = true;
            }
        }
    }

    public void addAll(ArrowheadScoreList arrowheadScoreList) {
        arrowheadScores.addAll(arrowheadScoreList.arrowheadScores);
    }

    public void mergeScores() {
        List<ArrowheadScore> mergedScores = new ArrayList<>();

        for (ArrowheadScore aScore : arrowheadScores) {
            boolean valueNotFound = true;
            for (ArrowheadScore mScore : mergedScores) {
                if (aScore.equivalentTo(mScore)) {
                    mScore.updateScore(aScore.score);
                    valueNotFound = false;
                    break;
                }
            }

            if (valueNotFound) {
                mergedScores.add(aScore);
            }
        }
        arrowheadScores = mergedScores;
    }

    public Feature2DList toFeature2DList(int chrIndex, String chrName) {
        Feature2DList feature2DList = new Feature2DList();
        for (ArrowheadScore score : arrowheadScores) {
            feature2DList.add(chrIndex, chrIndex, score.toFeature2D(chrName));
        }
        return feature2DList;
    }
}


