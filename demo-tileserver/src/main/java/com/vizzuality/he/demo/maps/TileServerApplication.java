package com.vizzuality.he.demo.maps;

import com.vizzuality.he.demo.maps.io.Feature;
import com.vizzuality.he.demo.maps.resource.NoContentResponseFilter;
import com.vizzuality.he.demo.maps.resource.TileResource;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Pattern;

import com.google.common.collect.Lists;
import io.dropwizard.Application;
import io.dropwizard.assets.AssetsBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

/**
 * The main entry point for running the app.
 */
public class TileServerApplication extends Application<TileServerConfiguration> {

  private static final String APPLICATION_NAME = "GBIF Tile Server";
  private static final Pattern COMMA = Pattern.compile(",");

  public static void main(String[] args) throws Exception {
    new TileServerApplication().run(args);
  }

  @Override
  public String getName() {
    return APPLICATION_NAME;
  }

  @Override
  public final void initialize(Bootstrap<TileServerConfiguration> bootstrap) {
    // We expect the assets bundle to be mounted on / in the config (applicationContextPath: "/")
    // Here we intercept the /map/debug/* URLs and serve up the content from /assets folder instead
    bootstrap.addBundle(new AssetsBundle("/assets", "/", "index.html"));
  }

  @Override
  public final void run(TileServerConfiguration configuration, Environment environment) throws IOException {

    List<Feature> data = Lists.newArrayListWithExpectedSize(6500000); // based on real data
    try (Scanner s = new Scanner(new File(configuration.getSourceCSV())).useDelimiter("\n")) {
      int runningCount = 0;
      int max = configuration.getMaxFeatures() == null ? Integer.MAX_VALUE : configuration.getMaxFeatures();

      // load a CSV which should be x,y,field1,field2 etc (fields being double in value)
      String[] schema = COMMA.split(s.next());
      while (s.hasNext() && runningCount++<max) {
        String[] fields = COMMA.split(s.next());

        Feature f = new Feature (Double.valueOf(fields[1]),Double.valueOf(fields[0]));
        for (int i=2; i<schema.length; i++) {
          try {
            f.addMeta(schema[i], Double.valueOf(fields[i]));
          } catch (Exception e) {
            f.addMeta(schema[i], 0d);
          }

        }
        data.add(f);
      }
    }

    TileResource tiles = new TileResource(data, configuration.getTileSize());
    environment.jersey().register(tiles);

    environment.jersey().register(NoContentResponseFilter.class);

  }
}
