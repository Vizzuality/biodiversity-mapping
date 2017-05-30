package com.vizzuality.he.demo.maps.common.bin;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import no.ecc.vectortile.VectorTileDecoder;
import no.ecc.vectortile.VectorTileEncoder;
import org.codetome.hexameter.core.api.Hexagon;
import org.codetome.hexameter.core.api.HexagonOrientation;
import org.codetome.hexameter.core.api.HexagonalGrid;
import org.codetome.hexameter.core.api.HexagonalGridBuilder;
import org.codetome.hexameter.core.api.HexagonalGridLayout;
import org.codetome.hexameter.core.backport.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.vizzuality.he.demo.maps.common.filter.PointFeatureFilters.META_BIODIVERSITY;

/**
 * A utility to deal with the binning of point based vector tiles into hexagon based vector tiles.
 * <p/>
 * Of critical importance is that the supplied tile has a buffer zone sufficiently large to accommodate the overlap
 * of hexagons across tile boundaries.
 * <p/>
 * See <a href="http://www.redblobgames.com/grids/hexagons">the excellent www.redblobgames.com</a> site for the math
 * surrounding hexagons which was used for this implementation.
 */
public class HexBin {
  private static final Logger LOG = LoggerFactory.getLogger(HexBin.class);
  private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory();
  private static final VectorTileDecoder DECODER = new VectorTileDecoder();


  // we provide a buffer to ensure that we have plenty of hexagon grid around the tile
  // to avoid https://github.com/Hexworks/hexameter/issues/32 and to ensure we have enough room to
  // offset the grid to align at the tile boundaries.
  private static final int NUM_HEX_BUFFER = 8;
  private static final double HEX_GRID_OFFSET_X = 1.5;
  private static final double HEX_GRID_OFFSET_Y = 2;


  static {
    DECODER.setAutoScale(false);
  }

  private final int tileSize;
  private final int hexPerTile;
  private final double radius;
  private final double hexWidth;
  private final double hexHeight;

  /**
   * Constructs a hexagon binner whereby there will be at least minHexPerTile hexagons on each tile, adjusted to ensure
   * that they tessellate properly.
   * @param tileSize The tile size to paint
   * @param minHexPerTile The minimum number of hexagons per tile
   */
  public HexBin(int tileSize, int minHexPerTile) {
    this.tileSize = tileSize;

    // In order to tessellate we need to add an extra 1/2 to the user requested tiles, and round up to an odd number.
    // e.g. 9 will result in 9.5 tiles painted (so would 8)
    minHexPerTile = minHexPerTile%2 == 0 ? minHexPerTile+1 : minHexPerTile; // round up to nearest odd number
    double w = 1.5 * (((double)minHexPerTile + 1) / 2);
    this.hexPerTile = (int) Math.ceil(minHexPerTile);

    hexWidth = tileSize / w;
    radius = hexWidth / 2;
    hexHeight = (Math.sqrt(3)/2) * hexWidth;

    LOG.debug("Radius [{}], width[{}], height[{}]", radius, hexWidth, hexHeight);
  }

  public byte[] bin(byte[] sourceTile, int z, long x, long y) throws IOException {
    VectorTileEncoder encoder = new VectorTileEncoder(tileSize, tileSize/4, false);
    bin(sourceTile, z, x, y, META_BIODIVERSITY, encoder);
    return encoder.encode();
  }

  private void bin(byte[] sourceTile, int z, long x, long y, String layer, VectorTileEncoder encoder) throws IOException {
    VectorTileDecoder.FeatureIterable tile = DECODER.decode(sourceTile, layer);
    Iterable<VectorTileDecoder.Feature> features = () -> tile.iterator();

    HexagonalGrid grid = newGridInstance();

    // Hexagons do not align at boundaries, and therefore we need to determine the offsets to ensure polygons
    // meet correctly across tiles.  The maximum offset is 1.5 cells horizontally and 1 cell vertically due to using
    // flat top tiles.  This is apparent when you see a picture. See http://www.redblobgames.com/grids/hexagons/#basics
    final double gridOffsetX = (x*tileSize)%(1.5*hexWidth);
    final double gridOffsetY = (y*tileSize)%hexHeight;

    LOG.debug("Radius [{}], width[{}], height[{}]", radius, hexWidth, hexHeight);
    LOG.debug("Grid offsets: {},{}", gridOffsetX, gridOffsetY);

    // for each feature returned from the datastore locate its hexagon and store the data on the hexagon
    Set<Hexagon> dataCells = Sets.newHashSet();
    for (VectorTileDecoder.Feature feature : features) {
      double scale = ((double)tileSize) / feature.getExtent(); // adjust for differing tile sizes
      LOG.debug("Scaling from {} to {} with {}", feature.getExtent(), tileSize, scale);
      Geometry geom = feature.getGeometry();
      Preconditions.checkArgument(geom instanceof Point, "Only Point based vector tiles can be binned");
      Point tileLocalXY = (Point) geom;

      // collect the point features into the Hexagons, noting that we apply the scale to compensate for possible
      // different tile sizes
      Hexagon hex = addFeatureInHex(z,
                                    grid,
                                    gridOffsetX,
                                    gridOffsetY,
                                    tileLocalXY.getX() * scale,
                                    tileLocalXY.getY() * scale,
                                    feature,
                                    x,
                                    y);
      if (hex != null) {
        dataCells.add(hex);
      }
    }


    for (Hexagon<HexagonData> hexagon : dataCells) {
      Coordinate[] coordinates = new Coordinate[7];
      int i = 0;
      Collection<org.codetome.hexameter.core.api.Point> points = hexagon.getPoints();
      for (org.codetome.hexameter.core.api.Point point : points) {
        coordinates[i++] = new Coordinate(point.getCoordinateX() - gridOffsetX - hexWidth * HEX_GRID_OFFSET_X,
                                          point.getCoordinateY() - gridOffsetY - hexHeight * HEX_GRID_OFFSET_Y);
      }
      coordinates[6] = coordinates[0]; // close our polygon
      LinearRing linear = GEOMETRY_FACTORY.createLinearRing(coordinates);
      Polygon poly = new Polygon(linear, null, GEOMETRY_FACTORY);

      Map<String, Object> meta = Maps.newHashMap();

      if (hexagon.getSatelliteData().isPresent()) {
        hexagon.getSatelliteData().get().getMetadata().forEach((k,v) -> {
            List<Double> values = (List<Double>)v;
            double total = values.stream().mapToDouble(d -> d.doubleValue()).sum();
            double avg = (total / values.size());
            //LOG.info("{}:{}", k, avg);
            meta.put(k, Math.floor(avg));
        });
      }

      LOG.debug("Coords {},{},{},{},{},{}" + coordinates[0],
               coordinates[1],
               coordinates[2],
               coordinates[3],
               coordinates[4],
               coordinates[5]);
      encoder.addFeature(layer, meta, poly);
    }
  }

  /**
   * Establish the grid, with a sufficient periphery hexagons to allow for adjustments due to tile boundary alignment
   * and the nearest neighbour behaviour of the hexagon lookup (https://github.com/Hexworks/hexameter/issues/32).
   * @return The grid of hexagons
   */
  @VisibleForTesting
  HexagonalGrid newGridInstance() {
    int requiredWidth = hexPerTile + NUM_HEX_BUFFER;
    int requiredHeight = (int) Math.ceil(tileSize / hexHeight) + NUM_HEX_BUFFER;
    LOG.debug("Grid: {}x{}", requiredWidth, requiredHeight);
    return new HexagonalGridBuilder()
      .setGridWidth(requiredWidth)
      .setGridHeight(requiredHeight)
      .setGridLayout(HexagonalGridLayout.RECTANGULAR)
      .setOrientation(HexagonOrientation.FLAT_TOP)
      .setRadius(radius)
      .build();
  }

  /**
   * Adds a feature to the satellite data in the hexagon taking into account the offsets.
   * It should be noted that on a tile with 0 offset, the top left of the tile is actually covered by tile 1,0 (x,y)
   * and numbered on an odd-q vertical layout addressing scheme on http://www.redblobgames.com/grids/hexagons/.
   * @param z the zoom
   * @param hexWidth the width of a hexagon
   * @param hexHeight the height of a hexagon
   * @param minTilePixelX the minimum pixel X of the tile in world space
   * @param minTilePixelY the minimum pixel Y of the tile in world space
   * @param grid the hexagon grid
   * @param gridOffsetX the offset for the hexagon to align with adjacent tiles
   * @param gridOffsetY the offset for the hexagon to align with adjacent tiles
   * @param feature to inspect and add
   * @return the hexagon or null when the hexagon is not on the hex grid or if satellite data is null and it cannot be
   * created.
   */
  private Hexagon addFeatureInHex(
    int z,
    HexagonalGrid grid,
    double gridOffsetX,
    double gridOffsetY,
    double tileLocalX,
    double tileLocalY,
    VectorTileDecoder.Feature feature,
    long x, long y
  ) {

    // and the pixel when on hex grid space, compensating for the offset and 2 hex buffer
    double hexGridLocalX = tileLocalX + gridOffsetX + HEX_GRID_OFFSET_X*hexWidth;
    double hexGridLocalY = tileLocalY + gridOffsetY + HEX_GRID_OFFSET_Y*hexHeight;

    Optional<Hexagon<HexagonData>> hex = grid.getByPixelCoordinate(hexGridLocalX, hexGridLocalY);
    if (hex.isPresent()) {
      Hexagon<HexagonData> hexagon = hex.get();

      if (!hexagon.getSatelliteData().isPresent()) {
        hexagon.setSatelliteData(new HexagonData());
      }

      if (hexagon.getSatelliteData().isPresent()) {
        HexagonData cellData = hexagon.getSatelliteData().get();

        // Store a list of all the values per hexagon so they can be averaged at the end
        feature.getAttributes().forEach((k, v) -> {
          if (!cellData.getMetadata().containsKey(k)) {
            List<Double> l = Lists.newArrayList();
            l.add(Double.valueOf(v.toString()));
            cellData.getMetadata().put(k, l);
          } else {
            List<Double> existing = (List<Double>)cellData.getMetadata().get(k);
            existing.add(Double.valueOf(v.toString()));
          }

        });
      }
      return hexagon;
    }

    return null;
  }
}
