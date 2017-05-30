package com.vizzuality.he.demo.maps.common.filter;

/*
import com.vizzuality.he.demo.maps.common.projection2.Double2D;
import com.vizzuality.he.demo.maps.common.projection2.Int2D;
import com.vizzuality.he.demo.maps.common.projection2.TileProjection;
import com.vizzuality.he.demo.maps.common.projection2.TileSchema;
import com.vizzuality.he.demo.maps.common.projection2.Tiles;
*/
import com.google.common.collect.Lists;
import com.vizzuality.he.demo.maps.common.projection.Double2D;
import com.vizzuality.he.demo.maps.common.projection.Int2D;
import com.vizzuality.he.demo.maps.common.projection.TileProjection;
import com.vizzuality.he.demo.maps.common.projection.Tiles;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import no.ecc.vectortile.VectorTileEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vizzuality.he.demo.maps.io.Feature;

/**
 * Filters and converters for PointFeature based tiles.
 */
public class PointFeatureFilters {
  private static final Logger LOG = LoggerFactory.getLogger(PointFeatureFilters.class);
  private static final int NULL_INT_VALUE = 0; // as specified in the protobuf file
  private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory();

  public static final String META_BIODIVERSITY = "biodiversity";

  /**
   * Encoders the features that fall within the buffered tile space when projected.
   *
   * @param encoder To collect into
   * @param source To project and filter
   * @param projection To projectthe points
   * @param z The zoom level
   * @param x The target tile X address
   * @param y The target tile Y address
   * @param tileSize The tile size at which we are operating
   * @param buffer The buffer in pixels around the tile to support (typically same as that used in the encoder)
   */
  public static void collectInVectorTile(VectorTileEncoder encoder, List<Feature> source, TileProjection projection,
                                         int z, long x, long y, int tileSize, int buffer) {

    // Note:  This projects the coordinates twice: once for filtering to the tile and secondly when collecting the
    //        tile features.  This could be optimized by calculating the WGS84 lat,lng of the tile+buffer extent and
    //        filtering the incoming stream using that.  At the time of writing the TileProjection does not offer that
    //        inverse capability and performance is not considered worthwhile (yet).
    AtomicInteger count = new AtomicInteger();
    source.stream()
          .filter(filterFeatureByTile(projection, z, x, y, tileSize, buffer)) // filter to the tile
          .collect(
            Collectors.groupingBy(toTileLocalPixelXY(projection, z, x, y, tileSize, buffer))
          )
          .forEach((pixel, attributes) -> {
            count.incrementAndGet();

            Map<String, Integer> meta = mergeAttributes(attributes);
            Point point = GEOMETRY_FACTORY.createPoint(new Coordinate(pixel.getX(), pixel.getY()));
            encoder.addFeature(META_BIODIVERSITY, meta, point);

              // Zoom 0 is a special case, whereby we copy data across the dateline into buffers
              if (z==0 && pixel.getX()<buffer) {
                Double2D adjustedPixel = new Double2D(pixel.getX() + tileSize, pixel.getY());
                Point point2 = GEOMETRY_FACTORY.createPoint(new Coordinate(adjustedPixel.getX(), adjustedPixel.getY()));
                encoder.addFeature(META_BIODIVERSITY, meta, point2);

              } else if (z==0 && pixel.getX()>=tileSize-buffer) {
                Double2D adjustedPixel = new Double2D(pixel.getX() - tileSize, pixel.getY());
                Point point2 = GEOMETRY_FACTORY.createPoint(new Coordinate(adjustedPixel.getX(), adjustedPixel.getY()));
                encoder.addFeature(META_BIODIVERSITY, meta, point2);
              }
          });
    LOG.info("Collected {} features from total {}", count.get(), source.size());
  }

  // a quick hack implementation follows. TODO: review this and clean it up
  public static Map<String, Integer> mergeAttributes(List<Feature> source) {
    Map<String, List<Double>> collected = Maps.newHashMap();
    for (Feature f : source) {
      for (Map.Entry<String, Double> e : f.getMeta().entrySet()) {
        List<Double> running = collected.get(e.getKey()) == null ? Lists.newArrayList() : collected.get(e.getKey());
        running.add(e.getValue());
        if (collected.get(e.getKey())==null) {
          collected.put(e.getKey(), running);
        }
      }
    }
    Map<String, Integer> averaged = Maps.newHashMap();
    for (Map.Entry<String, List<Double>> e : collected.entrySet()) {
      double avg = e.getValue().stream().mapToDouble(d -> d).average().getAsDouble();
      int avg100 = (int) Math.floor(avg * 100);  // scale 0-100
      averaged.put(e.getKey(), avg100);
    }
    return averaged;
  }

  /**
   * Provides a function that can project a feature into a pixel in tile local addressing space at that zoom.
   * @param projection To use in conversion
   * @param z Zoom level
   * @param x Tile x
   * @param y Tile y
   * @param tileSize That we operate with
   * @return The function to convert
   */
  public static Function<Feature, Int2D> toTileLocalPixelXY(final TileProjection projection,
                                                              final int z,
                                                               final long x, final long y,
                                                               final int tileSize, final int bufferSize) {
    return new Function<Feature, Int2D>() {
      @Override
      public Int2D apply(Feature t) {
        Double2D pixelXY = projection.toGlobalPixelXY(t.getLatitude(),t.getLongitude(), z);

        // EXPERIMENTAL
        // we snap to a grid for global layers to reduce the tile sizes (note: must be on the global XY, not the tile
        // local XY to ensure alignment at tile boundaries
        int snap = z<5 ? 5-z : 1;
        double x1 = pixelXY.getX() - (pixelXY.getX() % snap);
        double y1 = pixelXY.getY() - (pixelXY.getY() % snap);

        //Double2D pixel = Tiles.toTileLocalXY(pixelXY, z, x, y, tileSize, bufferSize);
        Double2D pixel = Tiles.toTileLocalXY(new Double2D(x1, y1), z, x, y, tileSize, bufferSize);

        return new Int2D(Double.valueOf(pixel.getX()).intValue(), Double.valueOf(pixel.getY()).intValue());
      }
    };
  }

  /**
   * Provides a predicate which can be used to filter Features that fall within the tile bounds when projected at the
   * given zoom level.
   * @param projection To use in conversion
   * @param z Zoom level
   * @param x Tile x
   * @param y Tile y
   * @param tileSize That we operate with
   * @param buffer The allowable buffer in pixels around the tile
   * @return
   */
  public static Predicate<Feature> filterFeatureByTile(final TileProjection projection, final int z,
                                                       final long x, final long y, final int tileSize,
                                                       final int buffer) {
    return new Predicate<Feature>() {
      @Override
      public boolean test(Feature f) {
        if (projection.isPlottable(f.getLatitude(), f.getLongitude())) {
          Double2D pixelXY = projection.toGlobalPixelXY(f.getLatitude(), f.getLongitude(), z);
          return Tiles.tileContains(z, x, y, tileSize, pixelXY, buffer);
        } else {
          return false;
        }
      }
    };
  }

}
