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

package juicebox.track.feature;

import javastraw.feature2D.Feature2DList;
import juicebox.tools.utils.juicer.arrowhead.ArrowheadScoreList;
import juicebox.tools.utils.juicer.arrowhead.HighScore;

import java.util.List;

/**
 * Created by muhammadsaadshamim on 6/1/15.
 */
public class JuicerToolsFeature2DParser {

    public static Feature2DList parseHighScoreList(int chrIndex, String chrName, int resolution, List<HighScore> binnedScores) {
        Feature2DList feature2DList = new Feature2DList();

        for (HighScore score : binnedScores) {
            feature2DList.add(chrIndex, chrIndex, score.toFeature2D(chrName, resolution));
        }

        return feature2DList;
    }

    public static Feature2DList parseArrowheadScoreList(int chrIndex, String chrName,
                                                        ArrowheadScoreList scoreList) {
        Feature2DList feature2DList = new Feature2DList();
        feature2DList.add(scoreList.toFeature2DList(chrIndex, chrName));
        return feature2DList;
    }
}