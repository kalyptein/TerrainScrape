package map;

import netscape.javascript.JSObject;

public class AreaBounds
{
	public double north, south, east, west, centerLat, centerLng;

	public AreaBounds(double n, double s, double e, double w)
	{
    	north = n;
    	south = s;
    	east = e;
    	west = w;
    	centerLat = (n + s) / 2.0;
    	centerLng = (e + w) / 2.0;
	}

	public AreaBounds(JSObject bounds)
	{
    	north = (double) bounds.getMember("north");
    	south = (double) bounds.getMember("south");
    	east = (double) bounds.getMember("east");
    	west = (double) bounds.getMember("west");
    	centerLat = (double) bounds.getMember("centerLat");
    	centerLng = (double) bounds.getMember("centerLng");
	}
}
