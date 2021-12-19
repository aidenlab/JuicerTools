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

import javastraw.feature2D.Feature2D;
import javastraw.feature2D.Feature2DList;
import javastraw.feature2D.Feature2DParser;
import javastraw.reader.basics.ChromosomeHandler;
import net.sf.jsi.SpatialIndex;
import net.sf.jsi.rtree.RTree;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles 2D features such as domains and peaks
 * Created by sa501428 on 8/6/15.
 */
public class Feature2DHandler {

    //private static final float MAX_DIST_NEIGHBOR = 1000f;
    private final Map<String, SpatialIndex> featureRTrees = new HashMap<>();
    protected Feature2DList loopList;
    private String path = null;

    public Feature2DHandler() {
        loopList = new Feature2DList();
        clearLists();
    }

    protected void clearLists() {
        loopList = new Feature2DList();
        featureRTrees.clear();
    }

    protected void remakeRTree() {
        featureRTrees.clear();

        loopList.processLists((key, features) -> {

            SpatialIndex si = new RTree();
            si.init(null);
            for (int i = 0; i < features.size(); i++) {
                Feature2D feature = features.get(i);
                si.add(new net.sf.jsi.Rectangle((float) feature.getStart1(), (float) feature.getStart2(),
                        (float) feature.getEnd1(), (float) feature.getEnd2()), i);
            }
            featureRTrees.put(key, si);
        });
        //}
    }

    public void setLoopList(String path, ChromosomeHandler chromosomeHandler) {
        ArrayList<String> attributes;
        if (this.path == null) {
            this.path = path;
            Feature2DList newList = Feature2DParser.loadFeatures(path, chromosomeHandler, true, null, false);
            attributes = newList.extractSingleFeature().getAttributeKeys();
            loopList = newList;
            Map<String, String> defaultAttributes = new HashMap<>(); //creates defaultAttributes map
            for (String attribute : attributes) {
                defaultAttributes.put(attribute, null);
            }
            loopList.setDefaultAttributes(defaultAttributes);
        }

        remakeRTree();
    }

    public List<Feature2D> getContainedFeatures(int chrIdx1, int chrIdx2, net.sf.jsi.Rectangle currentWindow) {
        final List<Feature2D> foundFeatures = new ArrayList<>();
        final String key = Feature2DList.getKey(chrIdx1, chrIdx2);

        if (featureRTrees.containsKey(key)) {
            // a procedure whose execute() method will be called with the results
            featureRTrees.get(key).contains(
                    currentWindow,      // the window in which we want to find all rectangles
                    i -> {
                        Feature2D feature = loopList.get(key).get(i);
                        //System.out.println(feature.getChr1() + "\t" + feature.getStart1() + "\t" + feature.getStart2());
                        foundFeatures.add(feature);
                        return true;              // return true here to continue receiving results
                    }
            );
        } else {
            System.err.println("returning all; key " + key + " not found contained");
            List<Feature2D> features = loopList.get(key);
            if (features != null) foundFeatures.addAll(features);
        }

        return foundFeatures;
    }
}
