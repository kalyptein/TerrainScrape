package map;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Calendar;
import org.jdom2.Element;

import main.XMLUtil;
import ui.MainWindow;

public class MapGenerationThread extends Thread
{
	File dir;
	
	String areaName;
	StaticMap overview;
	
	int xTiles, yTiles;
	GraphicsFormatEnum format;
	boolean saveSubimages;

	long startTime;
	int requests;
	
	public MapGenerationThread(File d, String area, int x, int y, StaticMap ov, GraphicsFormatEnum frmt, boolean sub)
	{
		areaName = area;
		dir = d;
		
		xTiles = x;
		yTiles = y;
		
		overview = ov;
		
		format = frmt;
		saveSubimages = sub;
		
		startTime = 0;
		requests = 0;
	}
	
	public void run()
	{
		// save coordinates/parameters to text file
		saveParameters();
		
		// capture start time to throttle image requests to <50 per minute so Google doesn't cut you off
		startTime = Calendar.getInstance().getTimeInMillis();
		
		// save overview image
		overview.saveImage(new File(dir, areaName + "_overview" + format.ext()), format);

		MainWindow.instance.output("Creating '" + areaName + "_overview" + format.ext() + "'...\n");

		// grab tile images
	    StaticMap[][] tiles = new StaticMap[xTiles][yTiles];
	    double xstep = (overview.bounds.east - overview.bounds.west) / xTiles;
    	double ystep = (overview.bounds.north - overview.bounds.south) / yTiles; 

		for (int i=0; i < yTiles; i++)
		{
			for (int j=0; j < xTiles; j++)
			{
				double mw = overview.bounds.west + (j * xstep);
				double ms = overview.bounds.south + (i * ystep);
				double me = overview.bounds.west + ((j+1) * xstep);
				double mn = overview.bounds.south + ((i+1) * ystep);
				
			    throttle();
				tiles[j][i] = new StaticMap(1, new AreaBounds(mn, ms, me, mw), true, true);

				MainWindow.instance.output("Creating tile " + j + "x" + i + "...\n");

				if (saveSubimages)
				{
					tiles[j][i].saveImage(new File(dir, areaName + "_tile_" + j + "x" + i + format.ext()), format);
				}
			}
		}
		
		int totalWidth = 0, totalHeight = 0;
		for (int i=0; i < xTiles; i++) { totalWidth += tiles[i][0].pxWidth; }
		for (int i=0; i < yTiles; i++) { totalHeight += tiles[0][i].pxHeight; }

		// stitch together tiles to make total image
		StaticMap total = new StaticMap(1, overview.bounds, true, false);
		total.setImage(new BufferedImage(totalWidth, totalHeight, BufferedImage.TYPE_INT_ARGB));
		Graphics g = total.img.getGraphics();

		for (int i=yTiles-1, y=0; i >= 0; i--)
		{
			for (int j=0, x=0; j < xTiles; j++)
			{				
				g.drawImage(tiles[j][i].img, x, y, null);
				x += tiles[j][i].pxWidth;
			}
			y += tiles[0][i].pxHeight;
		}
		
		total.saveImage(new File(dir, areaName + "_total" + format.ext()), format);
		MainWindow.instance.output("Creating '" + areaName + "_total" + format.ext() + "'...\n");

		// save terrain overlay file
		Element root = new Element("ImageOverlays");
		
		Element setElement = XMLUtil.xmlSet(root, areaName, "0.00001");
		setElement.addContent(total.xml(areaName + "_total", format));
		
		setElement = XMLUtil.xmlSet(root, areaName + "_tiles", "1.00000");
		
		for (int i=0; i < yTiles; i++)
		{
			for (int j=0; j < xTiles; j++)
			{
				setElement.addContent(tiles[j][i].xml(areaName + "_tile_" + j + "x" + i, format));
			}
		}

		XMLUtil.writeXML(new File(dir, areaName + ".xml"), root);
		MainWindow.instance.output("Done.\n\n");
	}

	/** Prevents you from exceeding the free map limit and getting frozen out. */
	private void throttle()
	{
		requests++;
		long seconds = (Calendar.getInstance().getTimeInMillis()-startTime) / 1000;
		int expected_seconds = 60 * (1 + (requests / 50));
		
		if (seconds > expected_seconds)
		{
			MainWindow.instance.output("Throttling to <50 images/min...\n");
			while (seconds > expected_seconds)
			{
				// spin your wheels until enough time has passed
				seconds = (Calendar.getInstance().getTimeInMillis()-startTime) / 1000;
			}
		}
	}
	
	public void saveParameters()
	{		
		// save coordinates/parameters to text file
		try
		{
			FileOutputStream ostream = new FileOutputStream(new File(dir, areaName + "_coords.txt"));
			OutputStreamWriter out = new OutputStreamWriter(ostream);
			BufferedWriter write = new BufferedWriter(out);
		
			write.write("Map Name: " + areaName);
			write.newLine();			
			write.write("Center: " + overview.bounds.centerLat + "," + overview.bounds.centerLng);
			write.newLine();
			write.write("Map Width/Height: " + overview.width + " / " + overview.height + " degrees");
			write.newLine();
			write.write("SW corner: " + overview.bounds.south + "," + overview.bounds.west);
			write.newLine();
			write.write("NE corner: " + overview.bounds.north + "," + overview.bounds.east);
			write.newLine();
			write.write("X-Axis Tiles: " + xTiles);
			write.newLine();
			write.write("Y-Axis Tiles: " + yTiles);
			write.newLine();
			write.write("Image Format: " + format);
			write.newLine();
			write.write("Subimages Saved: " + saveSubimages);
			write.newLine();
			write.flush();
			write.close();
		}		
		catch (FileNotFoundException ex) { ex.printStackTrace(); }
		catch (IOException ex) { ex.printStackTrace(); }	    
	}
}
