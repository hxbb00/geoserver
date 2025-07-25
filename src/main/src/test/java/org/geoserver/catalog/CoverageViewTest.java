/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2014 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import javax.media.jai.PlanarImage;
import javax.xml.namespace.QName;
import org.geoserver.catalog.CoverageView.CompositionType;
import org.geoserver.catalog.CoverageView.CoverageBand;
import org.geoserver.catalog.CoverageView.EnvelopeCompositionType;
import org.geoserver.catalog.CoverageView.InputCoverageBand;
import org.geoserver.catalog.CoverageView.SelectedResolution;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.data.test.TestData;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geotools.api.coverage.grid.GridCoverage;
import org.geotools.api.coverage.grid.GridCoverageReader;
import org.geotools.api.data.Query;
import org.geotools.api.feature.type.Name;
import org.geotools.api.filter.Filter;
import org.geotools.api.filter.FilterFactory;
import org.geotools.api.geometry.Bounds;
import org.geotools.api.parameter.GeneralParameterValue;
import org.geotools.api.parameter.ParameterValue;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridEnvelope2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.grid.io.GranuleSource;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.coverage.grid.io.StructuredGridCoverage2DReader;
import org.geotools.coverage.grid.io.footprint.FootprintBehavior;
import org.geotools.data.DataUtilities;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.NameImpl;
import org.geotools.feature.visitor.MinVisitor;
import org.geotools.gce.imagemosaic.ImageMosaicFormat;
import org.geotools.geometry.GeneralBounds;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.image.ImageWorker;
import org.geotools.image.util.ImageUtilities;
import org.geotools.parameter.DefaultParameterDescriptor;
import org.geotools.referencing.CRS;
import org.geotools.referencing.operation.transform.AffineTransform2D;
import org.junit.Before;
import org.junit.Test;

public class CoverageViewTest extends GeoServerSystemTestSupport {

    private static final String RGB_IR_VIEW = "RgbIrView";
    private static final String BANDS_FLAGS_VIEW = "BandsFlagsView";
    private static final String NDVI_VIEW = "NDVI";
    private static final String NDVI_VIEW2 = "NDVI2";
    private static final String NDVI_MASK = "NDVIMASK";
    private static final String BGR_VIEW = "BGR";
    private static final double ERROR = 1E-5;
    protected static QName WATTEMP = new QName(MockData.SF_URI, "watertemp", MockData.SF_PREFIX);
    protected static QName S2REDUCED = new QName(MockData.SF_URI, "s2reduced", MockData.SF_PREFIX);
    protected static QName IR_RGB = new QName(MockData.SF_URI, "ir-rgb", MockData.SF_PREFIX);
    protected static QName BANDS_FLAGS = new QName(MockData.SF_URI, "bands-flags", MockData.SF_PREFIX);
    protected static QName S2MASK = new QName(MockData.WCS_URI, "s2mask", MockData.WCS_PREFIX);

    @Before
    public void cleanupCatalog() {
        // attempt to solve intermittend build failure
        getGeoServer().reset();
    }

    static CoordinateReferenceSystem UTM32N;

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        testData.setUpRasterLayer(WATTEMP, "watertemp.zip", null, null, TestData.class);
        testData.setUpRasterLayer(S2REDUCED, "s2reduced.zip", null, null, TestData.class);
        testData.setUpRasterLayer(S2MASK, "s2mask.zip", null, null, TestData.class);
        testData.setUpRasterLayer(IR_RGB, "ir-rgb.zip", null, null, TestData.class);
        testData.setUpRasterLayer(BANDS_FLAGS, "bands-flags.zip", null, null, TestData.class);

        UTM32N = CRS.decode("EPSG:32632", true);
    }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        // setup the coverage view
        final Catalog cat = getCatalog();
        configureIROnCatalog(cat);
        configureBandsFlagsOnCatalog(cat);
        configureNDVIonCatalog(cat);
        configureNDVI2(cat);
        configureNDVIMaskOnCatalog(cat);
        configureBGRonCatalogWithJiffle(cat);
    }

    private void configureIROnCatalog(Catalog cat) throws Exception {
        final CoverageStoreInfo storeInfo = cat.getCoverageStoreByName("ir-rgb");
        final CoverageView coverageView = buildRgbIRView();
        final CatalogBuilder builder = new CatalogBuilder(cat);
        builder.setStore(storeInfo);

        final CoverageInfo coverageInfo = coverageView.createCoverageInfo(RGB_IR_VIEW, storeInfo, builder);
        coverageInfo.getParameters().put("USE_JAI_IMAGEREAD", "false");
        coverageInfo.getDimensions().get(0).setName("Red");
        coverageInfo.getDimensions().get(1).setName("Green");
        coverageInfo.getDimensions().get(2).setName("Blue");
        coverageInfo.getDimensions().get(3).setName("Infrared");
        cat.add(coverageInfo);
    }

    private void configureBandsFlagsOnCatalog(Catalog cat) throws Exception {
        final CoverageStoreInfo storeInfo = cat.getCoverageStoreByName("bands-flags");
        final CoverageView coverageView = buildBandsFlagsView();
        final CatalogBuilder builder = new CatalogBuilder(cat);
        builder.setStore(storeInfo);

        final CoverageInfo coverageInfo = coverageView.createCoverageInfo(BANDS_FLAGS_VIEW, storeInfo, builder);
        coverageInfo.getParameters().put("USE_JAI_IMAGEREAD", "false");
        cat.add(coverageInfo);
    }

    private void configureNDVIonCatalog(Catalog cat) throws Exception {
        final CoverageStoreInfo storeInfo = cat.getCoverageStoreByName("s2reduced");
        InputCoverageBand ib04 = new InputCoverageBand("B04", "0");
        InputCoverageBand ib08 = new InputCoverageBand("B08", "0");
        final CoverageBand ndvi = new CoverageBand(Arrays.asList(ib04, ib08), "NDVI", 0, CompositionType.JIFFLE);
        final CoverageView coverageView = new CoverageView(NDVI_VIEW, Collections.singletonList(ndvi));
        coverageView.setOutputName("NDVI");
        coverageView.setDefinition("NDVI = (B08 - B04) / (B04 + B08);");
        coverageView.setCompositionType(CompositionType.JIFFLE);
        final CatalogBuilder builder = new CatalogBuilder(cat);
        builder.setStore(storeInfo);
        final CoverageInfo coverageInfo = coverageView.createCoverageInfo(NDVI_VIEW, storeInfo, builder);
        cat.add(coverageInfo);
    }

    private void configureNDVIMaskOnCatalog(Catalog cat) throws Exception {
        final CoverageStoreInfo storeInfo = cat.getCoverageStoreByName("s2mask");
        InputCoverageBand mask16 = new InputCoverageBand("MASK16", "0");
        InputCoverageBand ib04 = new InputCoverageBand("B04", "0");
        InputCoverageBand ib08 = new InputCoverageBand("B08", "0");
        final CoverageBand ndvi =
                new CoverageBand(Arrays.asList(mask16, ib04, ib08), "NDVI", 0, CompositionType.JIFFLE);
        final CoverageView coverageView = new CoverageView(NDVI_MASK, Collections.singletonList(ndvi));
        coverageView.setOutputName("NDVI");
        coverageView.setDefinition(
                "if (MASK16 == 0) {" + "NDVI = null;" + "} else {" + "NDVI = (B08 - B04) / (B04 + B08);}");
        coverageView.setCompositionType(CompositionType.JIFFLE);
        final CatalogBuilder builder = new CatalogBuilder(cat);
        builder.setStore(storeInfo);
        final CoverageInfo coverageInfo = coverageView.createCoverageInfo(NDVI_MASK, storeInfo, builder);
        cat.add(coverageInfo);
    }

    private void configureNDVI2(Catalog cat) throws Exception {
        final CoverageStoreInfo storeInfo = cat.getCoverageStoreByName("s2reduced");
        InputCoverageBand ib04 = new InputCoverageBand("B04", "0");
        InputCoverageBand ib08 = new InputCoverageBand("B08", "0");
        // set bands in a different order
        final CoverageBand ndvi = new CoverageBand(Arrays.asList(ib08, ib04), "NDVI2", 0, CompositionType.JIFFLE);
        final CoverageView coverageView = new CoverageView(NDVI_VIEW2, Collections.singletonList(ndvi));
        coverageView.setOutputName("NDVI2");
        coverageView.setDefinition("A=B08; B=B04; NDVI2=(A-B)/(B+A);");
        coverageView.setCompositionType(CompositionType.JIFFLE);
        final CatalogBuilder builder = new CatalogBuilder(cat);
        builder.setStore(storeInfo);
        final CoverageInfo coverageInfo = coverageView.createCoverageInfo(NDVI_VIEW2, storeInfo, builder);
        cat.add(coverageInfo);
    }

    private void configureBGRonCatalogWithJiffle(Catalog cat) throws Exception {
        // Let's do bands swap with jiffle
        final CoverageStoreInfo storeInfo = cat.getCoverageStoreByName("ir-rgb");
        List<InputCoverageBand> icbs = Arrays.asList(new InputCoverageBand("rgb", "0"));

        final CoverageBand b = new CoverageBand(icbs, "bgr@0", 0, CompositionType.JIFFLE);
        final CoverageBand g = new CoverageBand(icbs, "bgr@1", 0, CompositionType.JIFFLE);
        final CoverageBand r = new CoverageBand(icbs, "bgr@2", 0, CompositionType.JIFFLE);

        final CoverageView coverageView = new CoverageView(BGR_VIEW, Arrays.asList(b, g, r));
        coverageView.setCompositionType(CompositionType.JIFFLE);
        coverageView.setOutputName("bgr");
        coverageView.setDefinition("bgr[0]=rgb[2];bgr[1]=rgb[1];bgr[2]=rgb[0];");
        final CatalogBuilder builder = new CatalogBuilder(cat);
        builder.setStore(storeInfo);
        final CoverageInfo coverageInfo = coverageView.createCoverageInfo(BGR_VIEW, storeInfo, builder);
        cat.add(coverageInfo);
    }

    private CoverageView buildRgbIRView() {
        final CoverageBand rBand = new CoverageBand(
                Arrays.asList(new InputCoverageBand("rgb", "0")), "rband", 0, CompositionType.BAND_SELECT);
        final CoverageBand gBand = new CoverageBand(
                Arrays.asList(new InputCoverageBand("rgb", "1")), "gband", 1, CompositionType.BAND_SELECT);
        final CoverageBand bBand = new CoverageBand(
                Arrays.asList(new InputCoverageBand("rgb", "2")), "bband", 2, CompositionType.BAND_SELECT);
        final CoverageBand irBand = new CoverageBand(
                Collections.singletonList(new InputCoverageBand("ir", "0")), "irband", 3, CompositionType.BAND_SELECT);
        final CoverageView coverageView = new CoverageView(RGB_IR_VIEW, Arrays.asList(rBand, gBand, bBand, irBand));
        // old coverage views deserialize with null in these fields, force it to test backwards
        // compatibility
        coverageView.setEnvelopeCompositionType(null);
        coverageView.setSelectedResolution(null);
        return coverageView;
    }

    private CoverageView buildBandsFlagsView() {
        String[] sources = {
            "SWIR", "VNIR", "QUALITY_CLASSES", "QUALITY_CLOUD", "QUALITY_CLOUDSHADOW", "QUALITY_HAZE", "QUALITY_SNOW"
        };

        List<CoverageBand> bands = new ArrayList<>();
        for (String source : sources) {
            if (source.startsWith("QUALITY_")) {
                CoverageBand band = new CoverageBand(
                        Arrays.asList(new InputCoverageBand(source, "0")), source, 0, CompositionType.BAND_SELECT);
                bands.add(band);
            } else {
                for (int i = 0; i < 3; i++) {
                    CoverageBand band = new CoverageBand(
                            Arrays.asList(new InputCoverageBand(source, "" + i)),
                            source + "_" + i,
                            i,
                            CompositionType.BAND_SELECT);
                    bands.add(band);
                }
            }
        }

        final CoverageView coverageView = new CoverageView(BANDS_FLAGS_VIEW, bands);
        return coverageView;
    }

    @Test
    public void testPreserveCoverageBandNames() throws Exception {
        final Catalog cat = getCatalog();
        final CoverageStoreInfo storeInfo = cat.getCoverageStoreByName("ir-rgb");
        final CoverageView coverageView = buildRgbIRView();
        final CatalogBuilder builder = new CatalogBuilder(cat);
        builder.setStore(storeInfo);

        final CoverageInfo coverageInfo = coverageView.createCoverageInfo(RGB_IR_VIEW, storeInfo, builder);
        List<CoverageDimensionInfo> dimensions = coverageInfo.getDimensions();
        assertEquals("rband", dimensions.get(0).getName());
        assertEquals("gband", dimensions.get(1).getName());
        assertEquals("bband", dimensions.get(2).getName());
        assertEquals("irband", dimensions.get(3).getName());
    }

    /** */
    @Test
    public void testCoverageView() throws Exception {
        final Catalog cat = getCatalog();
        final CoverageStoreInfo storeInfo = cat.getCoverageStoreByName("watertemp");

        final InputCoverageBand band = new InputCoverageBand("watertemp", "0");
        final CoverageBand outputBand =
                new CoverageBand(Collections.singletonList(band), "watertemp@0", 0, CompositionType.BAND_SELECT);
        final CoverageView coverageView = new CoverageView("waterView", Collections.singletonList(outputBand));
        final CatalogBuilder builder = new CatalogBuilder(cat);
        builder.setStore(storeInfo);

        final CoverageInfo coverageInfo = coverageView.createCoverageInfo("waterView", storeInfo, builder);
        coverageInfo.getParameters().put("USE_JAI_IMAGEREAD", "false");
        cat.add(coverageInfo);
        final MetadataMap metadata = coverageInfo.getMetadata();
        final CoverageView metadataCoverageView = (CoverageView) metadata.get(CoverageView.COVERAGE_VIEW);
        assertEquals(metadataCoverageView, coverageView);

        final ResourcePool resPool = cat.getResourcePool();
        final ReferencedEnvelope bbox = coverageInfo.getLatLonBoundingBox();
        final GridCoverage coverage = resPool.getGridCoverage(coverageInfo, "waterView", bbox, null);
        assertEquals(1, coverage.getNumSampleDimensions());

        disposeCoverage(coverage);
        final GridCoverageReader reader = resPool.getGridCoverageReader(coverageInfo, null);
        reader.dispose();
    }

    private void disposeCoverage(GridCoverage coverage) {
        if (coverage != null) {
            RenderedImage ri = coverage.getRenderedImage();
            if (coverage instanceof GridCoverage2D) {
                ((GridCoverage2D) coverage).dispose(true);
            }
            if (ri instanceof PlanarImage) {
                ImageUtilities.disposePlanarImageChain((PlanarImage) ri);
            }
        }
    }

    /** */
    @Test
    public void testBands() throws Exception {

        // Test input bands
        final InputCoverageBand u = new InputCoverageBand("u-component", "0");
        final InputCoverageBand v = new InputCoverageBand("u-component", "0");
        assertEquals(u, v);

        final InputCoverageBand empty = new InputCoverageBand();
        v.setCoverageName("v-component");
        v.setBand("1");
        assertNotEquals(u, v);
        assertNotEquals(u, empty);

        // Test output bands
        final CoverageBand outputBandU =
                new CoverageBand(Collections.singletonList(u), "u@1", 0, CompositionType.BAND_SELECT);

        final CoverageBand outputBandV = new CoverageBand();
        outputBandV.setInputCoverageBands(Collections.singletonList(v));
        outputBandV.setDefinition("v@0");
        assertNotEquals(outputBandU, outputBandV);

        // Test compositions
        CompositionType defaultComposition = CompositionType.getDefault();
        assertEquals("Band Selection", defaultComposition.displayValue());
        assertEquals("BAND_SELECT", defaultComposition.toValue());

        // Test coverage views
        final List<CoverageBand> bands = new ArrayList<>();
        bands.add(outputBandU);
        bands.add(outputBandV);

        final CoverageView coverageView = new CoverageView("wind", bands);
        final CoverageView sameViewDifferentName = new CoverageView();
        sameViewDifferentName.setName("winds");
        sameViewDifferentName.setCoverageBands(bands);
        assertNotEquals(coverageView, sameViewDifferentName);
        assertEquals(defaultComposition, coverageView.getCompositionType());

        assertEquals(coverageView.getBand(1), outputBandV);
        assertEquals(outputBandU, coverageView.getBands("u-component").get(0));
        assertEquals(2, coverageView.getSize());
        assertEquals(2, coverageView.getCoverageBands().size());
        assertEquals("wind", coverageView.getName());
    }

    @Test
    public void testNDVIJiffle() throws Exception {
        assertNdvi("NDVI");
    }

    @Test
    public void testNDVI2Jiffle() throws Exception {
        assertNdvi("NDVI2");
    }

    private void assertNdvi(String viewName) throws Exception {
        GridCoverage2D solidCoverage = null;
        GridCoverage b04Coverage = null;
        GridCoverage b08Coverage = null;
        try {
            Catalog cat = getCatalog();
            CoverageInfo coverageInfo = cat.getCoverageByName(viewName);
            final ResourcePool rp = cat.getResourcePool();
            GridCoverageReader reader = rp.getGridCoverageReader(coverageInfo, viewName, null);
            solidCoverage = (GridCoverage2D) reader.read(viewName);
            assertBandNames(solidCoverage, viewName);
            GridCoverageReader rb04 = rp.getGridCoverageReader(cat.getCoverageByName("B04"), "B04", null);
            GridCoverageReader rb08 = rp.getGridCoverageReader(cat.getCoverageByName("B08"), "B08", null);
            b04Coverage = rb04.read("B04");
            b08Coverage = rb08.read("B08");

            RenderedImage ri = solidCoverage.getRenderedImage();
            Raster raster = ri.getData();
            Raster b04 = b04Coverage.getRenderedImage().getData();
            Raster b08 = b08Coverage.getRenderedImage().getData();
            double d;
            double ndvi;
            double b04v;
            double b08v;
            for (int j = ri.getMinY(); j < raster.getHeight(); j++) {
                for (int i = ri.getMinX(); i < raster.getWidth(); i++) {
                    d = raster.getSampleDouble(i, j, 0);
                    b04v = b04.getSample(i, j, 0) & 0xFF;
                    b08v = b08.getSample(i, j, 0) & 0xFF;
                    ndvi = (b08v - b04v) / (b08v + b04v);
                    assertEquals(d, ndvi, ERROR);
                }
            }
        } finally {
            disposeCoverage(solidCoverage);
            disposeCoverage(b04Coverage);
            disposeCoverage(b08Coverage);
        }
    }

    @Test
    public void testBGRBandSwapWithJiffle() throws Exception {
        Catalog cat = getCatalog();
        CoverageInfo coverageInfo = cat.getCoverageByName(BGR_VIEW);
        final ResourcePool rp = cat.getResourcePool();
        GridCoverageReader reader = rp.getGridCoverageReader(coverageInfo, BGR_VIEW, null);
        GridCoverage solidCoverage = reader.read(BGR_VIEW);
        coverageInfo = cat.getCoverageByName("rgb");
        GridCoverageReader rgbReader = rp.getGridCoverageReader(coverageInfo, "rgb", null);
        GridCoverage rgbCoverage = rgbReader.read("rgb");
        try {
            RenderedImage ri = solidCoverage.getRenderedImage();
            Raster raster = solidCoverage.getRenderedImage().getData();
            Raster rgb = rgbCoverage.getRenderedImage().getData();
            double rgbArray[] = new double[3];
            double bgrArray[] = new double[3];
            for (int j = ri.getMinY(); j < raster.getHeight(); j++) {
                for (int i = ri.getMinX(); i < raster.getWidth(); i++) {
                    raster.getPixel(i, j, bgrArray);
                    rgb.getPixel(i, j, rgbArray);
                    // Assert the bands get swapped
                    assertEquals(rgbArray[0], bgrArray[2], ERROR);
                    assertEquals(rgbArray[1], bgrArray[1], ERROR);
                    assertEquals(rgbArray[2], bgrArray[0], ERROR);
                }
            }
        } finally {
            disposeCoverage(solidCoverage);
            disposeCoverage(rgbCoverage);
        }
    }

    @Test
    public void testRGBIrToRGB() throws IOException {
        Catalog cat = getCatalog();
        CoverageInfo coverageInfo = cat.getCoverageByName(RGB_IR_VIEW);
        final ResourcePool rp = cat.getResourcePool();
        GridCoverageReader reader = rp.getGridCoverageReader(coverageInfo, RGB_IR_VIEW, null);

        // no transparency due to footprint
        GeneralParameterValue[] params = buildFootprintBandParams(FootprintBehavior.None, new int[] {0, 1, 2});
        GridCoverage solidCoverage = reader.read(params);
        try {
            // System.out.println(solidCoverage);
            assertBandNames(solidCoverage, "Red", "Green", "Blue");
        } finally {
            disposeCoverage(solidCoverage);
        }

        // dynamic tx due to footprint
        params = buildFootprintBandParams(FootprintBehavior.Transparent, new int[] {0, 1, 2});
        GridCoverage txCoverage = reader.read(params);
        try {
            // System.out.println(txCoverage);
            assertBandNames(txCoverage, "Red", "Green", "Blue", "ALPHA_BAND");
        } finally {
            disposeCoverage(txCoverage);
        }
    }

    @Test
    public void testRGBIrToIr() throws IOException {
        Catalog cat = getCatalog();
        CoverageInfo coverageInfo = cat.getCoverageByName(RGB_IR_VIEW);
        final ResourcePool rp = cat.getResourcePool();
        GridCoverageReader reader = rp.getGridCoverageReader(coverageInfo, RGB_IR_VIEW, null);

        // get IR, no transparency due to footprint
        GeneralParameterValue[] params = buildFootprintBandParams(FootprintBehavior.None, new int[] {3});
        GridCoverage solidCoverage = reader.read(RGB_IR_VIEW, params);
        try {
            // System.out.println(solidCoverage);
            assertBandNames(solidCoverage, "Infrared");
        } finally {
            disposeCoverage(solidCoverage);
        }

        // get IR, dynamic tx due to footprint
        params = buildFootprintBandParams(FootprintBehavior.Transparent, new int[] {3});
        GridCoverage txCoverage = reader.read(RGB_IR_VIEW, params);
        try {
            // System.out.println(txCoverage);
            assertBandNames(txCoverage, "Infrared", "ALPHA_BAND");
        } finally {
            disposeCoverage(txCoverage);
        }
    }

    @Test
    public void testRGBIrToIrGB() throws IOException {
        Catalog cat = getCatalog();
        CoverageInfo coverageInfo = cat.getCoverageByName(RGB_IR_VIEW);
        final ResourcePool rp = cat.getResourcePool();
        GridCoverageReader reader = rp.getGridCoverageReader(coverageInfo, RGB_IR_VIEW, null);

        // get IR, no transparency due to footprint
        GeneralParameterValue[] params = buildFootprintBandParams(FootprintBehavior.None, new int[] {3, 1, 2});
        GridCoverage solidCoverage = reader.read(RGB_IR_VIEW, params);
        try {
            // System.out.println(solidCoverage);
            assertBandNames(solidCoverage, "Infrared", "Green", "Blue");
        } finally {
            disposeCoverage(solidCoverage);
        }

        // get IR, dynamic tx due to footprint
        params = buildFootprintBandParams(FootprintBehavior.Transparent, new int[] {3, 1, 2});
        GridCoverage txCoverage = reader.read(RGB_IR_VIEW, params);
        try {
            // System.out.println(txCoverage);
            assertBandNames(txCoverage, "Infrared", "Green", "Blue", "ALPHA_BAND");
        } finally {
            disposeCoverage(txCoverage);
        }
    }

    @Test
    public void testRGBIrToRed() throws IOException {
        Catalog cat = getCatalog();
        CoverageInfo coverageInfo = cat.getCoverageByName(RGB_IR_VIEW);
        final ResourcePool rp = cat.getResourcePool();
        GridCoverageReader reader = rp.getGridCoverageReader(coverageInfo, RGB_IR_VIEW, null);

        // get IR, no transparency due to footprint
        GeneralParameterValue[] params = buildFootprintBandParams(FootprintBehavior.None, new int[] {0});
        GridCoverage solidCoverage = reader.read(RGB_IR_VIEW, params);
        try {
            // System.out.println(solidCoverage);
            assertBandNames(solidCoverage, "Red");
        } finally {
            disposeCoverage(solidCoverage);
        }

        // get IR, dynamic tx due to footprint
        params = buildFootprintBandParams(FootprintBehavior.Transparent, new int[] {0});
        GridCoverage txCoverage = reader.read(RGB_IR_VIEW, params);
        try {
            // System.out.println(txCoverage);
            assertBandNames(txCoverage, "Red", "ALPHA_BAND");
        } finally {
            disposeCoverage(txCoverage);
        }
    }

    private void assertBandNames(GridCoverage coverage, String... bandNames) {
        assertEquals(bandNames.length, coverage.getNumSampleDimensions());
        for (int i = 0; i < bandNames.length; i++) {
            String expectedName = bandNames[i];
            String actualName = coverage.getSampleDimension(i).getDescription().toString();
            assertEquals(expectedName, actualName);
        }
    }

    private GeneralParameterValue[] buildFootprintBandParams(FootprintBehavior footprintBehavior, int[] bands) {
        final List<ParameterValue<?>> parameters = new ArrayList<>();
        parameters.add(new DefaultParameterDescriptor<>(
                        AbstractGridFormat.FOOTPRINT_BEHAVIOR.getName().toString(),
                        AbstractGridFormat.FOOTPRINT_BEHAVIOR.getValueClass(),
                        null,
                        footprintBehavior.name())
                .createValue());
        parameters.add(new DefaultParameterDescriptor<>(
                        AbstractGridFormat.BANDS.getName().toString(),
                        AbstractGridFormat.BANDS.getValueClass(),
                        null,
                        bands)
                .createValue());
        return parameters.toArray(new GeneralParameterValue[parameters.size()]);
    }

    /** Tests a heterogeneous view without setting any extra configuration (falling back on defaults) */
    @Test
    public void testHeterogeneousViewDefaults() throws Exception {
        assertHeterogeneousViewDefaults(null);
    }

    /** Same as {@link #testHeterogeneousViewDefaults()} but with multithraeded reading */
    @Test
    public void testHeterogeneousViewDefaultsMT() throws Exception {
        ParameterValue<Boolean> mtParameter = ImageMosaicFormat.ALLOW_MULTITHREADING.createValue();
        mtParameter.setValue(true);
        assertHeterogeneousViewDefaults(new GeneralParameterValue[] {mtParameter});
    }

    private void assertHeterogeneousViewDefaults(GeneralParameterValue[] readParams) throws Exception {
        CoverageInfo info = buildHeterogeneousResolutionView(
                "s2AllBandsDefaults",
                cv -> {},
                "B01",
                "B02",
                "B03",
                "B04",
                "B05",
                "B06",
                "B07",
                "B08",
                "B09",
                "B10",
                "B11",
                "B12");
        GridCoverage2D coverage = null;
        try {
            // default resolution policy is "best"
            GridCoverage2DReader reader = (GridCoverage2DReader) info.getGridCoverageReader(null, null);
            assertEquals(1007, reader.getResolutionLevels()[0][0], 1);
            assertEquals(1007, reader.getResolutionLevels()[0][1], 1);

            // default envelope policy is "union"
            GeneralBounds envelope = reader.getOriginalEnvelope();
            assertEquals(399960, envelope.getMinimum(0), 1);
            assertEquals(5190240, envelope.getMinimum(1), 1);
            assertEquals(509760, envelope.getMaximum(0), 1);
            assertEquals(5300040, envelope.getMaximum(1), 1);

            // read the full coverage to verify it's consistent
            coverage = reader.read(readParams);
            assertCoverageResolution(coverage, 1007, 1007);

            assertEquals(coverage.getEnvelope(), envelope);
        } finally {
            getCatalog().remove(info);
            if (coverage != null) {
                coverage.dispose(true);
            }
        }
    }

    /** Tests a heterogeneous view without setting any extra configuration (falling back on defaults) */
    @Test
    public void testHeterogeneousViewIntersectionEnvelope() throws Exception {
        CoverageInfo info = buildHeterogeneousResolutionView(
                "s2AllBandsIntersection",
                cv -> {
                    cv.setEnvelopeCompositionType(EnvelopeCompositionType.INTERSECTION);
                },
                "B01",
                "B02",
                "B03",
                "B04",
                "B05",
                "B06",
                "B07",
                "B08",
                "B09",
                "B10",
                "B11",
                "B12");
        GridCoverage2D coverage = null;
        try {
            // default resolution policy is "best"
            GridCoverage2DReader reader = (GridCoverage2DReader) info.getGridCoverageReader(null, null);
            assertEquals(1007, reader.getResolutionLevels()[0][0], 1);
            assertEquals(1007, reader.getResolutionLevels()[0][1], 1);

            // one of the granules has been cut to get a tigheter envelope
            GeneralBounds envelope = reader.getOriginalEnvelope();
            assertEquals(399960, envelope.getMinimum(0), 1);
            assertEquals(5192273, envelope.getMinimum(1), 1);
            assertEquals(507726, envelope.getMaximum(0), 1);
            assertEquals(5300040, envelope.getMaximum(1), 1);

            // checking the coverage it's not particularly useful as it does not get cut,
            // the bounds are just metadata
            coverage = reader.read();
            assertCoverageResolution(coverage, 1007, 1007);
            Bounds coverageEnvelope = coverage.getEnvelope();
            assertEquals(399960, coverageEnvelope.getMinimum(0), 1);
            assertEquals(5190240, coverageEnvelope.getMinimum(1), 1);
            assertEquals(509760, coverageEnvelope.getMaximum(0), 1);
            assertEquals(5300040, coverageEnvelope.getMaximum(1), 1);

        } finally {
            getCatalog().remove(info);
            if (coverage != null) {
                coverage.dispose(true);
            }
        }
    }

    @Test
    public void testHeterogeneousViewResolutionLowest() throws Exception {
        CoverageInfo info = buildHeterogeneousResolutionView(
                "s2AllBandsLowest",
                cv -> {
                    cv.setSelectedResolution(SelectedResolution.WORST);
                },
                "B01",
                "B02",
                "B03",
                "B04",
                "B05",
                "B06",
                "B07",
                "B08",
                "B09",
                "B10",
                "B11",
                "B12");
        GridCoverage2D coverage = null;
        try {
            GridCoverage2DReader reader = (GridCoverage2DReader) info.getGridCoverageReader(null, null);
            assertEquals(6100, reader.getResolutionLevels()[0][0], 1);
            assertEquals(6100, reader.getResolutionLevels()[0][1], 1);

            // no point checking the coverage, this is again just metadata, just smoke testing
            // the read will work
            coverage = reader.read();
        } finally {
            getCatalog().remove(info);
            if (coverage != null) {
                coverage.dispose(true);
            }
        }
    }

    private void assertCoverageResolution(GridCoverage2D coverage, double resX, double resY) {
        AffineTransform2D mt = (AffineTransform2D) coverage.getGridGeometry().getGridToCRS2D();
        assertEquals(resX, mt.getScaleX(), 1);
        assertEquals(resY, Math.abs(mt.getScaleY()), 1);
    }

    /** Hit the view outside its bounds, should return null */
    @Test
    public void testHeterogeneousViewOutsideBounds() throws Exception {
        CoverageInfo info = buildHeterogeneousResolutionView(
                "s2AllBandsOutsideBounds",
                cv -> {},
                "B01",
                "B02",
                "B03",
                "B04",
                "B05",
                "B06",
                "B07",
                "B08",
                "B09",
                "B10",
                "B11",
                "B12");
        GridCoverage2D coverage = null;
        try {
            ParameterValue<GridGeometry2D> gg = AbstractGridFormat.READ_GRIDGEOMETRY2D.createValue();
            gg.setValue(new GridGeometry2D(
                    new GridEnvelope2D(0, 0, 10, 10), new ReferencedEnvelope(0, 1000, 0, 1000, UTM32N)));
            GridCoverage2DReader reader = (GridCoverage2DReader) info.getGridCoverageReader(null, null);
            coverage = reader.read(gg);
            assertNull(coverage);
        } finally {
            getCatalog().remove(info);
            if (coverage != null) {
                coverage.dispose(true);
            }
        }
    }

    @Test
    public void testHeterogeneousViewBandSelectionBestResolution() throws Exception {
        CoverageInfo info = buildHeterogeneousResolutionView(
                "s2AllBandsBest",
                cv -> {
                    // use the default: BEST
                },
                "B01",
                "B02",
                "B03",
                "B04",
                "B05",
                "B06",
                "B07",
                "B08",
                "B09",
                "B10",
                "B11",
                "B12");

        // check band resolutions with specific band selections
        checkBandSelectionResolution(info, new int[] {0}, 6100, 6100);
        checkBandSelectionResolution(info, new int[] {0, 1}, 1007, 1007);
        checkBandSelectionResolution(info, new int[] {0, 5}, 2033, 2033);
        checkBandSelectionResolution(info, new int[] {5, 8, 1}, 1007, 1007);
        checkBandSelectionResolution(info, new int[] {1, 8, 5}, 1007, 1007);
    }

    @Test
    public void testHeterogeneousViewBandSelectionWorstResolution() throws Exception {
        CoverageInfo info = buildHeterogeneousResolutionView(
                "s2AllBandsWorst",
                cv -> {
                    cv.setSelectedResolution(SelectedResolution.WORST);
                },
                "B01",
                "B02",
                "B03",
                "B04",
                "B05",
                "B06",
                "B07",
                "B08",
                "B09",
                "B10",
                "B11",
                "B12");

        // check band resolutions with specific band selections
        checkBandSelectionResolution(info, new int[] {0}, 6100, 6100);
        checkBandSelectionResolution(info, new int[] {0, 1}, 6100, 6100);
        checkBandSelectionResolution(info, new int[] {0, 5}, 6100, 6100);
        checkBandSelectionResolution(info, new int[] {5, 8, 1}, 6100, 6100);
        checkBandSelectionResolution(info, new int[] {5, 8, 1}, 6100, 6100);
        checkBandSelectionResolution(info, new int[] {1}, 1007, 1007);
        checkBandSelectionResolution(info, new int[] {1, 5}, 2033, 2033);
    }

    public void checkBandSelectionResolution(
            CoverageInfo info, int[] bands, double expectedResolutionX, double expectedResolutionY) throws IOException {
        GridCoverage2D coverage = null;
        try {
            GridCoverage2DReader reader = (GridCoverage2DReader) info.getGridCoverageReader(null, null);

            ParameterValue<int[]> bandsValue = AbstractGridFormat.BANDS.createValue();
            bandsValue.setValue(bands);
            coverage = reader.read(bandsValue);
            assertNotNull(coverage);
            assertCoverageResolution(coverage, expectedResolutionX, expectedResolutionY);
        } finally {
            getCatalog().remove(info);
            if (coverage != null) {
                coverage.dispose(true);
            }
        }
    }

    private CoverageInfo buildHeterogeneousResolutionView(
            String name, Consumer<CoverageView> viewCustomizer, String... coverageNames) throws Exception {
        List<CoverageBand> bands = new ArrayList<>();
        int bandIdx = 0;
        for (String coverageName : coverageNames) {
            CoverageBand band =
                    new CoverageBand(Arrays.asList(new InputCoverageBand(coverageName, "0")), coverageName, bandIdx++);
            bands.add(band);
        }
        final CoverageView coverageView = new CoverageView(name, bands);
        viewCustomizer.accept(coverageView);

        final Catalog cat = getCatalog();
        final CoverageStoreInfo storeInfo = cat.getCoverageStoreByName("s2reduced");
        final CatalogBuilder builder = new CatalogBuilder(cat);
        builder.setStore(storeInfo);

        final CoverageInfo coverageInfo = coverageView.createCoverageInfo(name, storeInfo, builder);
        coverageInfo.getParameters().put("USE_JAI_IMAGEREAD", "false");
        cat.add(coverageInfo);

        return cat.getCoverage(coverageInfo.getId());
    }

    @Test
    public void testCoverageViewGranuleSource() throws Exception {
        final String VIEW_NAME = "view";
        CoverageInfo info = buildHeterogeneousResolutionView(
                VIEW_NAME,
                cv -> {
                    cv.setSelectedResolution(SelectedResolution.BEST);
                },
                "B02",
                "B03",
                "B04");

        StructuredGridCoverage2DReader reader = (StructuredGridCoverage2DReader) info.getGridCoverageReader(null, null);
        GranuleSource source = reader.getGranules(VIEW_NAME, true);
        Query query = new Query(VIEW_NAME);
        // used to throw exception here
        SimpleFeatureCollection granules = source.getGranules(query);
        // just check we can pull data from it
        DataUtilities.first(granules);
        // there are three bands, so three granules making up the coverage
        assertEquals(3, granules.size());
    }

    @Test
    public void testCoverageViewGranuleSourceAggregation() throws Exception {
        final String VIEW_NAME = "viewAggregate";
        CoverageInfo info = buildHeterogeneousResolutionView(
                VIEW_NAME,
                cv -> {
                    cv.setSelectedResolution(SelectedResolution.BEST);
                },
                "B02",
                "B03",
                "B04",
                "B01");

        StructuredGridCoverage2DReader reader = (StructuredGridCoverage2DReader) info.getGridCoverageReader(null, null);
        GranuleSource source = reader.getGranules(VIEW_NAME, true);
        Query query = new Query(VIEW_NAME);
        // used to throw exception here
        SimpleFeatureCollection granules = source.getGranules(query);
        MinVisitor visitor = new MinVisitor("location");
        granules.accepts(visitor, null);
        assertEquals("20170410T103021026Z_fullres_CC2.1989_T32TMT_B01.tif", visitor.getMin());
    }

    @Test
    public void testBandsFlagsView() throws Exception {
        // creation in the setup would have failed before the fix for
        // [GEOT-6168] CoverageView setup fails if one of the source bands has an indexed color
        // model

        CoverageInfo info = getCatalog().getCoverageByName(BANDS_FLAGS_VIEW);
        GridCoverageReader reader = info.getGridCoverageReader(null, null);
        GridCoverage2D coverage = (GridCoverage2D) reader.read();
        assertEquals(11, coverage.getRenderedImage().getSampleModel().getNumBands());
        coverage.dispose(true);
    }

    @Test
    public void testMissingBandsFill() throws Exception {
        GridCoverage2D solidCoverage = null;
        GridCoverage b04Coverage = null;
        GridCoverage b08Coverage = null;
        try {
            Catalog cat = getCatalog();
            CoverageInfo coverageInfo = cat.getCoverageByName(NDVI_MASK);
            final ResourcePool rp = cat.getResourcePool();
            // The dataset has 3 bands (B04,B08,MASK16) with extension tif
            // and 2 bands (B04,B08) with extension tiff
            // The Jiffle coverageView is like this:
            // if (MASK16 == 0) {;
            //     NDVI = null;
            // } else {;
            //   NDVI = (B08 - B04) / (B04 + B08);
            // }

            // We are going to put a filter to only return tiff images (so that MASK16 will be missing)
            // and then we will check the coverageView computation

            // First, without filling enabled. ImageMosaic dryrun will identify that there are granules
            // on that area that are not matching the filter so it will return null instead of a blank
            // response
            GridCoverageReader reader = rp.getGridCoverageReader(coverageInfo, NDVI_MASK, null);
            ParameterValue<Filter> filterParam = ImageMosaicFormat.FILTER.createValue();
            FilterFactory ff = CommonFactoryFinder.getFilterFactory();
            Filter locationFilter = ff.like(ff.property("location"), "*.tiff");
            filterParam.setValue(locationFilter);

            solidCoverage = (GridCoverage2D) reader.read(NDVI_MASK, filterParam);
            // Returned null coverage since there was a missing band.
            assertNull(solidCoverage);

            // Then, we are going to enable the fillMissingBands in the coverageView
            MetadataMap metadata = coverageInfo.getMetadata();
            CoverageView view = metadata.get("COVERAGE_VIEW", CoverageView.class);
            view.setFillMissingBands(true);
            cat.save(coverageInfo);

            // Let's repeat the read now
            reader = rp.getGridCoverageReader(coverageInfo, NDVI_MASK, null);
            solidCoverage = (GridCoverage2D) reader.read(NDVI_MASK, filterParam);
            assertNotNull(solidCoverage);
            assertBandNames(solidCoverage, "NDVI");

            Name b04Name = new NameImpl(MockData.WCS_PREFIX, "B04");
            Name b08Name = new NameImpl(MockData.WCS_PREFIX, "B08");

            // Make sure NDVI has been computed properly
            GridCoverageReader rb04 = rp.getGridCoverageReader(cat.getCoverageByName(b04Name), "B04", null);
            GridCoverageReader rb08 = rp.getGridCoverageReader(cat.getCoverageByName(b08Name), "B08", null);
            b04Coverage = rb04.read("B04", filterParam);
            b08Coverage = rb08.read("B08", filterParam);

            RenderedImage ri = solidCoverage.getRenderedImage();
            ImageWorker iw = new ImageWorker(ri);
            double[] maxs = iw.getMaximums();
            double[] mins = iw.getMinimums();
            assertEquals(maxs[0], 0.986842, ERROR);
            assertEquals(mins[0], -0.931034, ERROR);

            Raster raster = ri.getData();
            Raster b04 = b04Coverage.getRenderedImage().getData();
            Raster b08 = b08Coverage.getRenderedImage().getData();
            double d;
            double ndvi;
            double b04v;
            double b08v;

            for (int j = ri.getMinY(); j < raster.getHeight(); j++) {
                for (int i = ri.getMinX(); i < raster.getWidth(); i++) {
                    d = raster.getSampleDouble(i, j, 0);
                    if (!Double.isNaN(d)) {
                        b04v = b04.getSample(i, j, 0) & 0xFF;
                        b08v = b08.getSample(i, j, 0) & 0xFF;
                        ndvi = (b08v - b04v) / (b08v + b04v);
                        assertEquals(d, ndvi, ERROR);
                    }
                }
            }
        } finally {
            disposeCoverage(solidCoverage);
            disposeCoverage(b04Coverage);
            disposeCoverage(b08Coverage);
        }
    }
}
