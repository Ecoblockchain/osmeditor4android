package de.blau.android.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;
import de.blau.android.Logic;
import de.blau.android.osm.Node;
import de.blau.android.osm.OsmElement;
import de.blau.android.osm.Way;
import de.blau.android.util.mapbox.geojson.Feature;
import de.blau.android.util.mapbox.geojson.FeatureCollection;
import de.blau.android.util.mapbox.geojson.Geometry;
import de.blau.android.util.mapbox.geojson.Point;
import de.blau.android.util.mapbox.geojson.Polygon;
import de.blau.android.util.mapbox.models.Position;
import de.blau.android.util.mapbox.turf.TurfException;
import de.blau.android.util.mapbox.turf.TurfJoins;

/**
 * Tags that we want to remove before saving to server. List is in discarded.json from the iD repository
 * @author simon
 *
 */
public class GeoContext {

	private FeatureCollection imperialAreas;
	private FeatureCollection driveLeftAreas;

	/**
	 * Implicit assumption that the list will be short and that it is OK to read in synchronously
	 */
	public GeoContext(Context context) {	
		AssetManager assetManager = context.getAssets();
		
		imperialAreas = getGeoJsonFromAssets(assetManager, "imperial.json");
		driveLeftAreas = getGeoJsonFromAssets(assetManager, "drive-left.json");
	}
	
	FeatureCollection getGeoJsonFromAssets(AssetManager assetManager, String fileName) {
		InputStream is = null;
		try {
			is = assetManager.open(fileName);
		
			BufferedReader rd = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
        	StringBuilder sb = new StringBuilder();
        	int cp;
        	while ((cp = rd.read()) != -1) {
        		sb.append((char) cp);
        	}
        	is.close();
        	return FeatureCollection.fromJson(sb.toString());
		} catch (IOException e) {
			Log.d("GeoContext", "Unable to read file " + fileName + " exception " + e);
			return null;
		} finally {
			try {
				if (is != null) {
					is.close();
				}
			} catch (IOException ignored) {
			}
		}
	}
	
	public boolean imperial(double lon, double lat) {
		return  inside(lon, lat, imperialAreas);
	}
	
	public boolean imperial(OsmElement e) { 
		if (e instanceof Node) {
			return imperial((Node)e);
		} else if (e instanceof Way) {
			return imperial((Way)e);
		} else {
			return false; //FIXME handle relations
		}
	}
	
	public boolean imperial(Node n) { 
		return imperial(n.getLon(),n.getLat());
	}
	
	public boolean imperial(Way w) {
		double[]coords = Logic.centroidLonLat(w);
		return imperial(coords[0],coords[1]);
	}
	
	public boolean driveLeft(double lon, double lat) {
		return  inside(lon, lat, driveLeftAreas);
	}
	
	public boolean driveLeft(Node n) { 
		return driveLeft(n.getLon(),n.getLat());
	}
	
	boolean inside(double lon, double lat, FeatureCollection fc) {
		Point p = Point.fromCoordinates(Position.fromCoordinates(lon,lat));
		for (Feature f:fc.getFeatures()) {
			Geometry g = f.getGeometry();
			try {
				if (g instanceof Polygon && TurfJoins.inside(p, (Polygon)g)) {
					return true;
				}
			} catch (TurfException e) {
				return false;
			}
		}
		return false;
	}
}

