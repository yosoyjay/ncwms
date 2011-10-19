/*
 * Copyright (c) 2007 The University of Reading
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the University of Reading, nor the names of the
 *    authors or contributors may be used to endorse or promote products
 *    derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package uk.ac.rdg.resc.ncwms.util;

import java.io.File;
import java.io.IOException;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.geotoolkit.referencing.CRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import uk.ac.rdg.resc.edal.Extent;
import uk.ac.rdg.resc.edal.coverage.GridCoverage2D;
import uk.ac.rdg.resc.edal.coverage.grid.RegularGrid;
import uk.ac.rdg.resc.edal.coverage.grid.TimeAxis;
import uk.ac.rdg.resc.edal.coverage.grid.VerticalAxis;
import uk.ac.rdg.resc.edal.coverage.grid.impl.RegularGridImpl;
import uk.ac.rdg.resc.edal.feature.GridSeriesFeature;
import uk.ac.rdg.resc.edal.geometry.BoundingBox;
import uk.ac.rdg.resc.edal.geometry.impl.BoundingBoxImpl;
import uk.ac.rdg.resc.edal.position.TimePosition;
import uk.ac.rdg.resc.edal.position.Vector2D;
import uk.ac.rdg.resc.edal.position.impl.TimePositionImpl;
import uk.ac.rdg.resc.edal.util.Extents;
import uk.ac.rdg.resc.edal.util.TimeUtils;
import uk.ac.rdg.resc.ncwms.controller.GetMapDataRequest;
import uk.ac.rdg.resc.ncwms.exceptions.InvalidCrsException;
import uk.ac.rdg.resc.ncwms.exceptions.InvalidDimensionValueException;
import uk.ac.rdg.resc.ncwms.exceptions.WmsException;

/**
 * <p>Collection of static utility methods that are useful in the WMS application.</p>
 *
 * <p>Through the taglib definition /WEB-INF/taglib/wmsUtils.tld, these functions
 * are also available as JSP2.0 functions. For example:</p>
 * <code>
 * <%@taglib uri="/WEB-INF/taglib/wmsUtils" prefix="utils"%>
 * </code>
 *
 * @author Jon Blower
 */
public class WmsUtils
{
    /**
     * The versions of the WMS standard that this server supports
     */
    public static final Set<String> SUPPORTED_VERSIONS = new HashSet<String>();

    private static final String EMPTY_STRING = "";

    // Patterns are immutable and therefore thread-safe.
    private static final Pattern MULTIPLE_WHITESPACE = Pattern.compile("\\s+");

    static
    {
        SUPPORTED_VERSIONS.add("1.1.1");
        SUPPORTED_VERSIONS.add("1.3.0");
    }

    /** Private constructor to prevent direct instantiation */
    private WmsUtils() { throw new AssertionError(); }

    /**
     * Creates a directory, throwing an Exception if it could not be created and
     * it does not already exist.
     */
    public static void createDirectory(File dir) throws Exception
    {
        if (dir.exists())
        {
            if (dir.isDirectory())
            {
                return;
            }
            else
            {
                throw new Exception(dir.getPath() + 
                    " already exists but it is a regular file");
            }
        }
        else
        {
            boolean created = dir.mkdirs();
            if (!created)
            {
                throw new Exception("Could not create directory "
                    + dir.getPath());
            }
        }
    }
    
    /**
     * Creates a unique name for a Layer (for display in the Capabilities
     * document) based on a dataset ID and a Layer ID that is unique within a
     * dataset.
     * @todo doesn't belong in generic WmsUtils: specific to ncWMS
     */
    public static String createUniqueLayerName(String datasetId, String layerId)
    {
        return datasetId + "/" + layerId;
    }
    
    /**
     * Converts a string of the form "x1,y1,x2,y2" into a bounding box of four
     * doubles.
     * @throws WmsException if the format of the bounding box is invalid
     */
    public static double[] parseBbox(String bboxStr, boolean lonFirst) throws WmsException
    {
        String[] bboxEls = bboxStr.split(",");
        // Check the validity of the bounding box
        if (bboxEls.length != 4)
        {
            throw new WmsException("Invalid bounding box format: need four elements");
        }
        double[] bbox = new double[4];
        try
        {
            if(lonFirst){
                bbox[0] = Double.parseDouble(bboxEls[0]);
                bbox[1] = Double.parseDouble(bboxEls[1]);
                bbox[2] = Double.parseDouble(bboxEls[2]);
                bbox[3] = Double.parseDouble(bboxEls[3]);
            } else {
                bbox[0] = Double.parseDouble(bboxEls[1]);
                bbox[1] = Double.parseDouble(bboxEls[0]);
                bbox[2] = Double.parseDouble(bboxEls[3]);
                bbox[3] = Double.parseDouble(bboxEls[2]);
            }
        }
        catch(NumberFormatException nfe)
        {
            throw new WmsException("Invalid bounding box format: all elements must be numeric");
        }
        if (bbox[0] >= bbox[2] || bbox[1] >= bbox[3])
        {
            throw new WmsException("Invalid bounding box format");
        }
        return bbox;
    }

    /**
     * Calculates the magnitude of the vector components given in the provided
     * Lists.  The two lists must be of the same length.  For any element in the
     * component lists, if either east or north is null, the magnitude will also
     * be null.
     * @return a List of the magnitudes calculated from the components.
     */
    public static List<Float> getMagnitudes(List<Float> eastData, List<Float> northData)
    {
        if (eastData == null || northData == null) throw new NullPointerException();
        if (eastData.size() != northData.size())
        {
            throw new IllegalArgumentException("east and north data components must be the same length");
        }
        List<Float> mag = new ArrayList<Float>(eastData.size());
        for (int i = 0; i < eastData.size(); i++)
        {
            Float east = eastData.get(i);
            Float north = northData.get(i);
            Float val = null;
            if (east != null && north != null)
            {
                val = (float)Math.sqrt(east * east + north * north);
            }
            mag.add(val);
        }
        if (mag.size() != eastData.size()) throw new AssertionError();
        return mag;
    }
    
    /**
     * @return true if the given location represents an OPeNDAP dataset.
     * This method simply checks to see if the location string starts with "http://",
     * "https://" or "dods://".
     */
    public static boolean isOpendapLocation(String location)
    {
        return location.startsWith("http://") || location.startsWith("dods://")
            || location.startsWith("https://");
    }
    
    /**
     * @return true if the given location represents an NcML aggregation. dataset.
     * This method simply checks to see if the location string ends with ".xml"
     * or ".ncml", following the same procedure as the Java NetCDF library.
     */
    public static boolean isNcmlAggregation(String location)
    {
        return location.endsWith(".xml") || location.endsWith(".ncml");
    }

    /**
     * Estimate the range of values in this layer by reading a sample of data
     * from the default time and elevation.  Works for both Scalar and Vector
     * layers.
     * @return
     * @throws IOException if there was an error reading from the source data
     */
    public static Extent<Float> estimateValueRange(GridSeriesFeature<?> feature) throws IOException
    {
        List<Float> dataSample = readDataSample(feature);
        return Extents.findMinMax(dataSample);
    }

    private static List<Float> readDataSample(GridSeriesFeature<?> feature) throws IOException {
        final GridCoverage2D<?> coverage = feature.extractHorizontalGrid(
                getClosestToCurrentTime(feature.getCoverage().getDomain().getTimeAxis()),
                getUppermostElevation(feature),
                new RegularGridImpl(feature.getCoverage().getDomain().getHorizontalGrid()
                        .getCoordinateExtent(), 100, 100));
        // Read a low-resolution grid of data covering the entire spatial
        // extent
        return new AbstractList<Float>() {
            @SuppressWarnings("unchecked")
            @Override
            public Float get(int index) {
                Class<?> clazz = coverage.getRangeMetadata(null).getValueType();
                if(clazz == Float.class){
                    return (Float) coverage.getValues().get(index);
                } else if(clazz == Vector2D.class){
                    return ((Vector2D<Float>) coverage.getValues().get(index)).getMagnitude();
                } else {
                    return null;
                }
            }
            
            @Override
            public int size() {
                return (int) coverage.size();
            }
        };
    }

    /**
     * Replaces instances of duplicated whitespace with a single space.
     */
    public static String removeDuplicatedWhiteSpace(String theString)
    {
        return MULTIPLE_WHITESPACE.matcher(theString).replaceAll(" ");
    }

    /**
     * Returns the RuntimeException name. This used in 'displayDefaultException.jsp'
     * to show the exception name, to go around the use of '${exception.class.name}' where 
     * the word 'class' is deemed as Java keyword by Tomcat 7.0 
     *  
     */
    public static String getExceptionName(Exception e)
    {
        return e.getClass().getName();
    }

    /** Adds leading zeros and removes trailing zeros as appropriate, to make
     * the given number of milliseconds suitable for placing after a decimal
     * point (e.g. 500 becomes 5, 5 becomes 005). */
    private static String addOrRemoveZeros(long millis)
    {
        if (millis == 0) return "";
        String s = Long.toString(millis);
        if (millis < 10) return "00" + s;

        if (millis < 100) s = "0" + s;
        // Now remove all trailing zeros
        while (s.endsWith("0"))
        {
            s = s.substring(0, s.length() - 1);
        }
        return s;
    }

    /**
     * Finds a {@link CoordinateReferenceSystem} with the given code, forcing
     * longitude-first axis order.
     * @param crsCode The code for the CRS
     * @return a coordinate reference system with the longitude axis first
     * @throws InvalidCrsException if a CRS matching the code cannot be found
     * @throws NullPointerException if {@code crsCode} is null
     */
    public static CoordinateReferenceSystem getCrs(String crsCode) throws InvalidCrsException
    {
        if (crsCode == null) throw new NullPointerException("CRS code cannot be null");
        try
        {
            // the "true" means "force longitude first"
            return CRS.decode(crsCode, true);
        }
        catch(Exception e)
        {
            throw new InvalidCrsException(crsCode);
        }
    }

    /**
     * Gets a {@link RegularGrid} representing the image requested by a client
     * in a GetMap operation
     * @param dr Object representing a GetMap request
     * @return a RegularGrid representing the requested image
     */
    public static RegularGrid getImageGrid(GetMapDataRequest dr)
            throws InvalidCrsException
    {
        CoordinateReferenceSystem crs = getCrs(dr.getCrsCode());
        BoundingBox bbox = new BoundingBoxImpl(dr.getBbox(), crs);
        return new RegularGridImpl(bbox, dr.getWidth(), dr.getHeight());
    }

    /**
     * Returns an ArrayList of null values of the given length
     */
    public static ArrayList<Float> nullArrayList(int n)
    {
        ArrayList<Float> list = new ArrayList<Float>(n);
        Collections.fill(list, null);
        return list;
    }
    
    public static TimePosition getClosestToCurrentTime(TimeAxis tAxis){
        if (tAxis == null) return null; // no time axis
        int index = TimeUtils.findTimeIndex(tAxis.getCoordinateValues(), new TimePositionImpl());
        if (index < 0) {
            // We can calculate the insertion point
            int insertionPoint = -(index + 1); 
            // We set the index to the most recent past time
            if (insertionPoint > 0) index = insertionPoint - 1; // The most recent past time
            else index = 0; // All DateTimes on the axis are in the future, so we take the earliest
        }
        
        return tAxis.getCoordinateValue(index);
    }

    public static double getUppermostElevation(GridSeriesFeature<?> feature){
        VerticalAxis vAxis = feature.getCoverage().getDomain().getVerticalAxis();
        // We must access the elevation values via the accessor method in case
        // subclasses override it.
        if (vAxis == null) {
            return Double.NaN;
        }

        if (vAxis.getVerticalCrs().isPressure()) {
            // The vertical axis is pressure. The default (closest to the
            // surface)
            // is therefore the maximum value.
            return Collections.max(vAxis.getCoordinateValues());
        } else {
            // The vertical axis represents linear height, so we find which
            // value is closest to zero (the surface), i.e. the smallest
            // absolute value
            return Collections.min(vAxis.getCoordinateValues(), new Comparator<Double>() {
                @Override public int compare(Double d1, Double d2) {
                    return Double.compare(Math.abs(d1), Math.abs(d2));
                }
            });
        }
    }
}