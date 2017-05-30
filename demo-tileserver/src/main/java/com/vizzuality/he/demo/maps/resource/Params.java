package com.vizzuality.he.demo.maps.resource;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

/**
 * Utilities for dealing with the parameters and mappings.
 */
class Params {
  // Patterns used in splitting strings
  static final Pattern COMMA = Pattern.compile(",");
  static final Pattern PIPE = Pattern.compile("[|]");

  // Parameters for dealing with hexagon binning
  static final String BIN_MODE_HEX = "hex";
  static final int HEX_TILE_SIZE = 4096;
  static final String DEFAULT_HEX_PER_TILE = "51";


  /**
   * Open the tiles to the world (especially your friendly localhost developer!)
   * @param response
   */
  static void enableCORS(HttpServletResponse response) {
    response.addHeader("Allow-Control-Allow-Methods", "GET,OPTIONS");
    response.addHeader("Access-Control-Allow-Origin", "*");
    response.addHeader("Cache-Control", "max-age=2592000"); // 1 week
  }

}
