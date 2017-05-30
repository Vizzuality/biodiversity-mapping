package com.vizzuality.he.demo.maps.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.List;
import java.util.Scanner;
import java.util.Timer;
import java.util.concurrent.TimeUnit;

import com.google.common.base.Stopwatch;
import com.google.common.collect.Lists;

public class MemoryTest {

  public static void main(String[] args) throws FileNotFoundException {
    Scanner s = new Scanner(new File("/tmp/data/he_data_v1.csv")).useDelimiter("\n");
    Stopwatch timer = Stopwatch.createStarted();
    List<String> lines = Lists.newArrayListWithExpectedSize(6500000);
    while (s.hasNext()) {
      lines.add(s.next());
    }
    System.out.println(timer.elapsed(TimeUnit.MILLISECONDS));
    s.close();

    timer = Stopwatch.createStarted();
    int count = 0;
    for (String line : lines) {
      count++;
      if (count%100000 == 0) {
        //System.out.println(line);
      }
    }
    System.out.println(count + ": " + timer.elapsed(TimeUnit.MILLISECONDS));

    timer = Stopwatch.createStarted();
    count = 0;
    for (String line : lines) {
      count++;
      if (count%100000 == 0) {
        //System.out.println(line);
      }
    }
    System.out.println(count + ": " + timer.elapsed(TimeUnit.MILLISECONDS));


  }
}
