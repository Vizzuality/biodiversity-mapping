package com.vizzuality.he.demo.maps.resource;

import com.vizzuality.he.demo.maps.common.bin.HexBin;
import com.vizzuality.he.demo.maps.common.filter.PointFeatureFilters;
import com.vizzuality.he.demo.maps.common.projection.TileProjection;
import com.vizzuality.he.demo.maps.common.projection.Tiles;
import com.vizzuality.he.demo.maps.io.Feature;

import java.io.IOException;
import java.util.List;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;

import com.codahale.metrics.annotation.Timed;
import com.google.common.base.Preconditions;
import com.vividsolutions.jts.geom.GeometryFactory;
import no.ecc.vectortile.VectorTileDecoder;
import no.ecc.vectortile.VectorTileEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.vizzuality.he.demo.maps.resource.Params.BIN_MODE_HEX;
import static com.vizzuality.he.demo.maps.resource.Params.DEFAULT_HEX_PER_TILE;
import static com.vizzuality.he.demo.maps.resource.Params.HEX_TILE_SIZE;
import static com.vizzuality.he.demo.maps.resource.Params.enableCORS;

/**
 * The tile resource for the simple gbif data layers (i.e. HBase sourced, preprocessed).
 */
@Path("/spp/richness")
@Singleton
public final class TileResource {

  private static final Logger LOG = LoggerFactory.getLogger(TileResource.class);

  private static final String LAYER_PROTECTED = "protected";
  private static final String LAYER_UNPROTECTED = "unprotected";

  private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory();
  private static final VectorTileDecoder DECODER = new VectorTileDecoder();

  static {
    DECODER.setAutoScale(false); // important to avoid auto scaling to 256 tiles
  }

  private final List<Feature> features;
  private final int tileSize;
  private final int bufferSize;

  public TileResource(List<Feature> features, int tileSize) {
    this.features = features;
    this.tileSize = tileSize;
    this.bufferSize = tileSize / 4;
  }



  @GET
  @Path("/{z}/{x}/{y}.mvt")
  @Timed
  @Produces("application/x-protobuf")
  public byte[] all(
    @PathParam("z") int z,
    @PathParam("x") long x,
    @PathParam("y") long y,
    @DefaultValue("EPSG:3857") @QueryParam("srs") String srs,  // default as SphericalMercator
    @QueryParam("bin") String bin,
    @DefaultValue(DEFAULT_HEX_PER_TILE) @QueryParam("hexPerTile") int hexPerTile,
    @Context HttpServletResponse response,
    @Context HttpServletRequest request
    ) throws Exception {

    enableCORS(response);
    return getTile(z,x,y,srs,bin,hexPerTile);
  }


  byte[] getTile(
    int z,
    long x,
    long y,
    String srs,
    String bin,
    int hexPerTile
  ) throws Exception {
    Preconditions.checkArgument(bin == null || BIN_MODE_HEX.equalsIgnoreCase(bin), "Unsupported bin mode");

    byte[] vectorTile = filteredVectorTile(z, x, y, srs);

    // depending on the query, direct the request
    if (bin == null) {
      return vectorTile;

    } else if (BIN_MODE_HEX.equalsIgnoreCase(bin)) {
      LOG.info("Binning using Hex");
      HexBin binner = new HexBin(HEX_TILE_SIZE, hexPerTile);
      try {
        return binner.bin(vectorTile, z, x, y);
      } catch (IllegalArgumentException e) {
        e.printStackTrace();
        // happens on empty tiles
        return vectorTile;
      }

    } else {
      throw new IllegalArgumentException("Unsupported bin mode"); // cannot happen due to conditional check above
    }
  }

  private byte[] filteredVectorTile(int z, long x, long y, String srs)
    throws IOException {
    TileProjection projection = Tiles.fromEPSG(srs, tileSize);
    VectorTileEncoder encoder = new VectorTileEncoder(tileSize, bufferSize, false);
    PointFeatureFilters.collectInVectorTile(encoder, features, projection, z, x, y, tileSize, bufferSize);
    return encoder.encode();
  }
}
