package com.vizzuality.he.demo.maps;

import javax.annotation.Nullable;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.Configuration;

/**
 * Application configuration with sensible defaults if applicable.
 */
public class TileServerConfiguration extends Configuration {
  @Valid
  @NotNull
  private String sourceCSV;

  @Valid
  @NotNull
  private int tileSize=512;

  @Valid
  @Nullable
  private Integer maxFeatures;

  @JsonProperty
  public String getSourceCSV() {
    return sourceCSV;
  }

  @JsonProperty
  public void getSourceCSV(String sourceCSV) {
    this.sourceCSV = sourceCSV;
  }

  @JsonProperty
  public Integer getMaxFeatures() {
    return maxFeatures;
  }

  @JsonProperty
  public void setMaxFeatures(Integer maxFeatures) {
    this.maxFeatures = maxFeatures;
  }

  @JsonProperty
  public int getTileSize() {
    return tileSize;
  }

  @JsonProperty
  public void setTileSize(int tileSize) {
    this.tileSize = tileSize;
  }
}
