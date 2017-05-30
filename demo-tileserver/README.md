## Demo tile server

This is a very hastily prepared server to serve vector tiles suitable for a demonstration only.
 
The application reads a CSV file of `x,y,param1,param2` and turns those into vector tiles whereby each parameter is 
available in the feature metadata.  Parameters are expected to be 0.0 to 1.0 and they will be averaged and turned into 
a range value between 0 and 100.

To build (requires Java 8 and Maven): `mvn clean compile`

Edit the `server.conf` to locate the source data.

To run: `java -Xmx4G -jar target/demo-tileserver-1.1-SNAPSHOT.jar server server.conf`

The demo tiles can be seen on http://localhost:7001/index.html

The tile server should support:

  - `srs` values of `EPSG:4326`, `EPSG:3857`, `EPSG:3575`, `EPSG:3031`
  - `bin` omitted for points, or `hex` for hexagons
  - `hexPerTile` to control the size of hexagons.  This is an approximate value, and will be adjusted automatically to 
 tesselate tiles

An example tile URL might be http://localhost:7001/map/spp/richness/{z}/{x}/{y}.mvt?bin=hex&hexPerTile=91&srs=EPSG:4326

The Vector Tiles will contain a single layer called `biodiversity`.  See the `index.html` for a very simple example of 
use and styling.
 
This work is forked from an early version of the work undertaken by [GBIF](https://github.com/gbif/maps).  This work is
not expected to be used for anything other than a demo.
