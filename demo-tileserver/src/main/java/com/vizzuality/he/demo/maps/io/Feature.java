package com.vizzuality.he.demo.maps.io;

import java.util.Map;

import com.google.common.base.Objects;
import com.google.common.collect.Maps;

public class Feature {
  private double latitude;
  private double longitude;
  private Map<String,Double> meta = Maps.newHashMap();

  public Feature() {
  }

  public Feature(double latitude, double longitude) {
    this.latitude = latitude;
    this.longitude = longitude;
  }

  public double getLatitude() {
    return latitude;
  }

  public void setLatitude(double latitude) {
    this.latitude = latitude;
  }

  public double getLongitude() {
    return longitude;
  }

  public void setLongitude(double longitude) {
    this.longitude = longitude;
  }

  public void addMeta(String k, Double v) {
    meta.put(k,v);
  }

  public Map<String, Double> getMeta() {
    return meta;
  }
}
