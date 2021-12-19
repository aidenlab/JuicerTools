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

package juicebox.data;

import javastraw.feature1D.Feature1D;
import javastraw.feature2D.Feature2D;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Created by muhammadsaadshamim on 9/28/15.
 */
public class MotifAnchor extends Feature1D implements Comparable<MotifAnchor> {

    public static boolean uniquenessShouldSupercedeConvergentRule = true;
    private static int posCount = 0;
    private static int negCount = 0;
    // critical components of a motif anchor
    private final String chr;
    // references to original features if applicable
    private final List<Feature2D> originalFeatures1 = new ArrayList<>();
    private final List<Feature2D> originalFeatures2 = new ArrayList<>();
    private boolean strand;
	private long x1, x2;
	// fimo output loaded as attributes
	private boolean fimoAttributesHaveBeenInitialized = false;
	private double score = 0, pValue, qValue;
	private String sequence;
	private String name = "";
	
	/**
	 * Inititalize anchor given parameters (e.g. from BED file)
	 *
	 * @param chr
	 * @param x1
	 * @param x2
	 */
	public MotifAnchor(String chr, long x1, long x2) {
		this.chr = chr;
		if (x1 <= x2) {
			// x1 < x2
			this.x1 = x1;
			this.x2 = x2;
		} else {
			System.err.println("Improperly formatted Motif file: chr " + chr + " x1 " + x1 + " x2 " + x2);
		}
	}

    public MotifAnchor(String chr, int x1, int x2, String name) {
        this(chr, x1, x2);
        this.name = name;
    }
	
	/**
	 * Inititalize anchor given parameters (e.g. from feature list)
	 *
	 * @param chrIndex
	 * @param x1
	 * @param x2
	 * @param originalFeatures1
	 * @param originalFeatures2
	 */
	public MotifAnchor(String chrIndex, long x1, long x2, List<Feature2D> originalFeatures1, List<Feature2D> originalFeatures2) {
		this(chrIndex, x1, x2);
		this.originalFeatures1.addAll(originalFeatures1);
		this.originalFeatures2.addAll(originalFeatures2);
	}

    @Override
    public String getKey() {
        return "" + chr;
    }

    @Override
    public Feature1D deepClone() {
        MotifAnchor clone = new MotifAnchor(chr, x1, x2, originalFeatures1, originalFeatures2);
        clone.name = name;
        if (fimoAttributesHaveBeenInitialized) {
            clone.setFIMOAttributes(score, pValue, qValue, strand, sequence);
        }

        return clone;
    }

    /**
     * @return chromosome name
     */
    public String getChr() {
        return chr;
    }
	
	/**
	 * @return start point
	 */
	public long getX1() {
		return x1;
	}
	
	/**
	 * @return end point
	 */
	public long getX2() {
		return x2;
	}

    /**
     * @return width of this anchor
     */
    public int getWidth() {
		return (int) (x2 - x1);
    }

    /**
     * Expand this anchor (symmetrically) by the width given
     *
     * @param width
     */
    public void widenMargins(int width) {
        x1 = x1 - width / 2;
        x2 = x2 + width / 2;
    }
	
	/**
	 * @param x
	 * @return true if x is within bounds of anchor
	 */
	public boolean contains(long x) {
		return x >= x1 && x <= x2;
	}

    /**
     * @param anchor
     * @return true if this is strictly left of given anchor
     */
    public boolean isStrictlyToTheLeftOf(MotifAnchor anchor) {
        return x2 < anchor.x1;
    }

    /**
     * @param anchor
     * @return true if this is strictly right of given anchor
     */
    public boolean isStrictlyToTheRightOf(MotifAnchor anchor) {
        return anchor.x2 < x1;
    }

    /**
     * @param anchor
     * @return true if given anchor overlaps at either edge with this anchor
     */
    public boolean hasOverlapWith(MotifAnchor anchor) {
        return chr.equalsIgnoreCase(anchor.chr)
                && (this.contains(anchor.x1) || this.contains(anchor.x2) || anchor.contains(x1) || anchor.contains(x2));
    }

    public void mergeWith(MotifAnchor anchor) {
        if (chr.equalsIgnoreCase(anchor.chr)) {
            x1 = Math.min(x1, anchor.x1);
            x2 = Math.max(x2, anchor.x2);
            addFeatureReferencesFrom(anchor);
        } else {
            System.err.println("Attempted to merge anchors on different chromosomes");
            System.err.println(this + " & " + anchor);
        }
    }

    @Override
    public String toString() {
        return chr + "\t" + x1 + "\t" + x2;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof MotifAnchor) {
            MotifAnchor o = (MotifAnchor) obj;
            return chr.equalsIgnoreCase(o.chr) && x1 == o.x1 && x2 == o.x2;
        }
        return false;
    }

    @Override
    public int hashCode() {
		return Objects.hash(x2, chr, x1);
    }

    @Override
    public int compareTo(MotifAnchor o) {
        if (chr.equalsIgnoreCase(o.chr)) {
            if (x1 == o.x1) {
                if (x2 == o.x2 && sequence != null && o.sequence != null) {
                    return sequence.compareTo(o.sequence);
                }
				return Long.compare(x2, o.x2);
            }
			return Long.compare(x1, o.x1);
        }
        return chr.compareTo(o.chr);
    }

    public void setFIMOAttributes(double score, double pValue, double qValue, boolean strand, String sequence) {
        this.score = score;
        this.pValue = pValue;
        this.qValue = qValue;
        this.strand = strand;
        this.sequence = sequence;

        fimoAttributesHaveBeenInitialized = true;
    }

    public double getScore() {
        return score;
    }

    public boolean hasFIMOAttributes() {
        return fimoAttributesHaveBeenInitialized;
    }

    public void addFIMOAttributesFrom(MotifAnchor anchor) {
        setFIMOAttributes(anchor.score, anchor.pValue, anchor.qValue, anchor.strand, anchor.sequence);
    }

    public void addFeatureReferencesFrom(MotifAnchor anchor) {
        originalFeatures1.addAll(anchor.originalFeatures1);
        originalFeatures2.addAll(anchor.originalFeatures2);
    }

    public void updateOriginalFeatures(boolean uniqueStatus, int specificStatus) {
        if ((originalFeatures1.size() > 0 || originalFeatures2.size() > 0)) {
            if (fimoAttributesHaveBeenInitialized) {
                if (specificStatus == 1) {
                    for (Feature2D feature : originalFeatures1) {
                        if (feature instanceof Feature2DWithMotif) {
                            if (strand || uniqueStatus) {
                                posCount++;
                                ((Feature2DWithMotif) feature).updateMotifData(strand, uniqueStatus, sequence, x1, x2, true, score);
                            }
                        }
                    }
                } else if (specificStatus == -1) {
                    for (Feature2D feature : originalFeatures2) {
                        if (feature instanceof Feature2DWithMotif) {
                            if (!strand || uniqueStatus) {
                                negCount++;
                                ((Feature2DWithMotif) feature).updateMotifData(strand, uniqueStatus, sequence, x1, x2, false, score);
                            }
                        }
                    }
                } else {
                    for (Feature2D feature : originalFeatures1) {
                        if (feature instanceof Feature2DWithMotif) {
                            if (strand || uniqueStatus) {
                                posCount++;
                                ((Feature2DWithMotif) feature).updateMotifData(strand, uniqueStatus, sequence, x1, x2, true, score);
                            }
                        }
                    }
                    for (Feature2D feature : originalFeatures2) {
                        if (feature instanceof Feature2DWithMotif) {
                            if (!strand || uniqueStatus) {
                                negCount++;
                                ((Feature2DWithMotif) feature).updateMotifData(strand, uniqueStatus, sequence, x1, x2, false, score);
                            }
                        }
                    }
                }
            } else {
                System.err.println("Attempting to assign motifs on incomplete anchor");
            }
        }
    }

    public String getSequence() {
        return sequence;
    }

    public List<Feature2D> getOriginalFeatures1() {
        return originalFeatures1;
    }

    public List<Feature2D> getOriginalFeatures2() {
        return originalFeatures2;
    }

    public boolean isDirectionalAnchor(boolean direction) {
        if (direction) {
            return originalFeatures1.size() > 0 && originalFeatures2.size() == 0;
        } else {
            return originalFeatures2.size() > 0 && originalFeatures1.size() == 0;
        }
    }

    /**
     * @return true if positive strand, false if negative strand
     */
    public boolean getStrand() {
        return strand;
    }

    public String getName() {
        return name;
    }
}