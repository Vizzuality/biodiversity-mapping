package com.vizzuality.he.demo.maps.common.projection;

/**
 * Factories and utilities for dealing with coordinates and projecting those onto tiles.
 */
public class Tiles {

  /**
   * Factory of TileProjection for the given ESPG code.
   * @param epsg That defines the projection
   * @param size Of the tile in use
   * @return The Tileprojection for the EPSG
   * @throws IllegalArgumentException If the EPSG is not supported
   */
  public static TileProjection fromEPSG(String epsg, int size) throws IllegalArgumentException {
    if (SphericalMercator.EPSG_CODE.equals(epsg)) {
      return new SphericalMercator(size);

    } else if (NorthPoleLAEAEurope.EPSG_CODE.equals(epsg)) {
      return new NorthPoleLAEAEurope(size);

    } else if (WGS84.EPSG_CODE.equalsIgnoreCase(epsg)) {
      return new WGS84(size);

    } else if (WGS84AntarcticPolarStereographic.EPSG_CODE.equalsIgnoreCase(epsg)) {
      return new WGS84AntarcticPolarStereographic(size);
    }
    throw new IllegalArgumentException("Unsupported EPSG supplied: " + epsg);
  }

  /**
   * Converts the pixel from global addressing to an address local to the tile identified.
   * @param globalPixelXY To convert
   * @param z The zoom address of the tile we wish local addressing for
   * @param x The x address of the tile we wish local addressing for
   * @param y The y address of the tile we wish local addressing for
   * @param tileSize The tile size
   * @return The pixel XY local to the tile in question
   */
  public static Double2D toTileLocalXY(Double2D globalPixelXY, int z, long x, long y, int tileSize, int bufferSize) {
    long numTilesAtZoom = 1<<z;
    long maxGlobalPixelAddress = numTilesAtZoom * tileSize;

    double dateLineAdjustedX = globalPixelXY.getX();
    if (z>0 && x==0 && globalPixelXY.getX()>=maxGlobalPixelAddress-bufferSize) {
      dateLineAdjustedX -= maxGlobalPixelAddress;

    } else if (z>0 && x==numTilesAtZoom-1 && globalPixelXY.getX()<bufferSize) {
      dateLineAdjustedX += maxGlobalPixelAddress;
    }

    return new Double2D(dateLineAdjustedX - x * tileSize, globalPixelXY.getY() - y * tileSize);
  }

  /**
   * Provides the tile address for the tile that will contain the pixel at the given zoom and tile size.
   *
   * @param globalPixelXY The pixel address in global space
   * @param z             the zoom level
   * @param tileSize      the tile size
   *
   * @return The tile XY address
   */
  public static Long2D toTileXY(Double2D globalPixelXY, int z, int tileSize) {
    long x = (long) Math.min(Math.max(globalPixelXY.getX() / tileSize, 0), Math.pow(2, z) - 1);
    long y = (long) Math.min(Math.max(globalPixelXY.getY() / tileSize, 0), Math.pow(2, z) - 1);
    return new Long2D(x, y);
  }

  /**
   * Returns true if the global pixel falls on the tile located at x,y or within the given buffer.
   * A common buffer might be 16 pixels when using 4096 tiles for example.
   *
   * @param z             The zoom level
   * @param x             The tile X address
   * @param y             The tile Y address
   * @param globalPixelXY The pixel address to test which should be at the corresponding zoom level of X,Y
   * @param bufferPixels  The buffer area to consider (negative values will not throw error but should be used with
   *                      extreme caution and are unexpected)
   *
   * @return true if the the pixel falls on the tile or within the buffer zone
   */
  public static boolean tileContains(int z, long x, long y, int tileSize, Double2D globalPixelXY, int bufferPixels) {

    boolean verticallyContained = globalPixelXY.getY() >= y * tileSize - bufferPixels &&
                                  globalPixelXY.getY() <= y * tileSize + bufferPixels + tileSize;


    boolean contained = globalPixelXY.getX() >= x * tileSize - bufferPixels &&
           globalPixelXY.getX() <= x * tileSize + bufferPixels + tileSize &&
           verticallyContained;

    // handle dateline wrapping if it is a match vertically
    if (!contained && verticallyContained) {
      long maxTileAddress = (1 << z) - 1;

      // if the tile adjoins the dateline and it is a match vertically
      if (x == 0) {
        contained |= globalPixelXY.getX() >= tileSize - bufferPixels;
      } else if (x == maxTileAddress) {
        contained |= globalPixelXY.getX() < bufferPixels;
      }
    }
    return contained;
  }
}
