package com.vizzuality.he.demo.maps.common.projection;

/**
 * Defines the interface for dealing with conversations between WGS84 referenced coordinates and pixels within tiles
 * at various zoom levels.
 */
public interface TileProjection {

  /**
   * Converts the coordinate to the global pixel address at the given zoom.
   * @param latitude To convert
   * @param longitude To convert
   * @param zoom The zoom level
   * @return The pixel location addressed globally
   */
  Double2D toGlobalPixelXY(double latitude, double longitude, int zoom);

  boolean isPlottable(double latitude, double longitude);
}
