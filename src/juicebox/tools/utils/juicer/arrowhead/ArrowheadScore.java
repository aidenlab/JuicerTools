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

import java.awt.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Container class for Arrowhead blocks
 */
public class ArrowheadScore {
    final long[] indices = new long[4];
	double score = Double.NaN;
    boolean isActive = false;
	
	public ArrowheadScore(long[] indices) {
		System.arraycopy(indices, 0, this.indices, 0, 4);
	}

    /**
     * use for deep copying
     *
     * @param arrowheadScore
     */
    public ArrowheadScore(ArrowheadScore arrowheadScore) {
        System.arraycopy(arrowheadScore.indices, 0, this.indices, 0, 4);
        this.score = arrowheadScore.score;
        this.isActive = arrowheadScore.isActive;
    }

    public void updateScore(double score) {
        if (Double.isNaN(this.score))
            this.score = score;
        else if (!Double.isNaN(score))
            this.score = Math.max(score, this.score);
    }

    /**
     * @param limStart
     * @param limEnd
     * @param resolution
     * @return true if block is fully contained within given bounds
     */
    public boolean isWithin(int limStart, int limEnd, int resolution) {
		boolean containedInBounds = true;
		for (long index : indices) {
			long scaledIndex = index / resolution;
			containedInBounds = containedInBounds && scaledIndex >= limStart && scaledIndex <= limEnd;
		}
		return containedInBounds;
	}

    public boolean equivalentTo(ArrowheadScore mScore) {
        return Arrays.equals(indices, mScore.indices);
    }

    public Feature2D toFeature2D(String chrName) {
        Map<String, String> attributes = new HashMap<>();
        attributes.put("score", Double.toString(score));
        return new Feature2D(Feature2D.FeatureType.DOMAIN, chrName, indices[0], indices[1],
                chrName, indices[2], indices[3], Color.yellow, attributes);
    }
}
