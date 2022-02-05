package map;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import javax.imageio.ImageIO;

import org.jdom2.Element;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import main.XMLUtil;

public class StaticMap
{
	public static final String endl = System.getProperty("line.separator");

	private static final Color c1 = new Color(255, 255, 255, 255);
	private static final Color c2 = new Color(0, 0, 0, 255);
	private static final Color[] mark = { c1,c1,c2,c2,c2,c1,c1,c1,c2,c2,c1,c2,c1 };
	public static String markerurl = "sites.google.com/site/terrainsim/home/files";

	/** If the API_KEY is changed here, it should also be changed in the api-loading script in MapPage.html */
	public static final String API_KEY = "AIzaSyCRo7pxd6yoyd5gv_Gh7L_7Y-DhLeeYvKY";

	private static String createParams(int scale, double n, double s, double e, double w)
	{
		String[] params = 
		{
			"key=" + API_KEY,
			"size=640x640",
			"maptype=satellite",
			"format=png32",
			"sensor=false",
			"scale=" + scale,
			"visible=" + s + "," + w + "|" + n + "," + e,
			"markers=icon:http://" + markerurl + "/markSW.png|" + s + "," + w,
			"markers=icon:http://" + markerurl + "/markNE.png|" + n + "," + e
		};
		
		return "http://maps.googleapis.com/maps/api/staticmap?" + String.join("&", params);
	}
	
	public AreaBounds bounds;
	public double width, height;
	public int pxWidth, pxHeight;
	
	public double pixelPerDegreeX, pixelPerDegreeY;
	public double degreesPerPixelX, degreesPerPixelY;
	
	BufferedImage img;
	
	
	/**
	 * Requests static map image with markers from google
	 */
	public static BufferedImage getMap(int scale, double n, double s, double e, double w)
	{
		BufferedImage img = null;
		try
		{
			img = ImageIO.read(new URL(createParams(scale, n, s, e, w)));
		}
		catch (MalformedURLException ex) { ex.printStackTrace(); }
		catch (IOException ex) { ex.printStackTrace(); }
		
		return img;
	}

	private static Point findMark(BufferedImage img, int ystart, int scale)
	{
		for (int i=ystart; i < img.getHeight(); i += scale)
		{
			for (int j=0; j < img.getWidth(); j += scale)
			{
				Color pixel = new Color(img.getRGB(j,i), true);

				if (pixel.equals(mark[0]))
				{
					boolean match = true;
					for (int cnt=0; cnt < mark.length; cnt++)
					{						
						pixel = new Color(img.getRGB(j+(scale*cnt),i), true);
						
						if (!pixel.equals(mark[cnt]) || j+cnt >= img.getWidth())
						{
							match = false;
							break;
						}
					}
					
					if (match) return new Point(j, i);							
				}
			}
		}
		
		return null;
	}
	
	private static BufferedImage cropToMarkers(BufferedImage img, int scale)
	{
		Point p1 = findMark(img, 0, scale);
		if (p1 == null)
			return null;
		
		Point p2 = findMark(img, p1.y + scale, scale);
		if (p2 == null)
			return null;
		
		p2.translate(scale * mark.length, 0);
		
		return img.getSubimage(p2.x, p1.y, p1.x-p2.x, p2.y-p1.y);
	}
	
	public StaticMap(int scale, AreaBounds b, boolean crop, boolean load)
	{
		bounds = b;
		width = Math.abs(bounds.west - bounds.east);
		height = Math.abs(bounds.north - bounds.south);
		
		img = null;
		if (load)
		{
			BufferedImage i = getMap(scale, bounds.north, bounds.south, bounds.east, bounds.west);
			if (crop)
				setImage(cropToMarkers(i, scale));
		}
	}

	public Image getFXImage()
	{
		return SwingFXUtils.toFXImage(img, new WritableImage(img.getWidth(), img.getHeight()));
	}

	public void setImage(Image fximg)
	{
		setImage(SwingFXUtils.fromFXImage(fximg, null));
	}

	public void setImage(BufferedImage i)
	{
		img = i;
		
		pxWidth = img.getWidth();
		pxHeight = img.getHeight();
		
		pixelPerDegreeX = pxWidth / width;
		pixelPerDegreeY = pxHeight / height;
		degreesPerPixelX = width / (double) pxWidth;
		degreesPerPixelY = height / (double) pxHeight;
	}
	
	public void saveImage(File file, GraphicsFormatEnum format)
	{
		try
		{
			ImageIO.write(img, format.name(), file);
		}
		catch (IOException ex) { ex.printStackTrace(); }
	}
	
	public Element xml(String mapName, GraphicsFormatEnum format)
	{
		Element element = new Element("ImageOverlay");
		
		File base = new File(".");
		File path = new File(mapName + format.ext());
		String relative = "./" + base.toURI().relativize(path.toURI()).getPath();

		XMLUtil.xml(element, "m_sFilePath", relative);
		
		XMLUtil.xml(element, "m_dLowerLeftLatitude", bounds.south);
		XMLUtil.xml(element, "m_dLowerLeftLongitude", bounds.west);
		XMLUtil.xml(element, "m_dLowerRightLatitude", bounds.south);
		XMLUtil.xml(element, "m_dLowerRightLongitude", bounds.east);
		XMLUtil.xml(element, "m_dUpperRightLatitude", bounds.north);
		XMLUtil.xml(element, "m_dUpperRightLongitude", bounds.east);
		XMLUtil.xml(element, "m_dUpperLeftLatitude", bounds.north);
		XMLUtil.xml(element, "m_dUpperLeftLongitude", bounds.west);
		
		return element;
	}

	public static void buildMark()
	{
		BufferedImage img1 = new BufferedImage(1 + 2*mark.length, 1, BufferedImage.TYPE_INT_ARGB);
		Graphics g1 = img1.getGraphics();
		g1.setColor(new Color(0,0,0,0));
		g1.fillRect(0, 0, 2*mark.length, 0);
		
		BufferedImage img2 = new BufferedImage(1 + 2*mark.length, 1, BufferedImage.TYPE_INT_ARGB);
		Graphics g2 = img2.getGraphics();
		g2.setColor(new Color(0,0,0,0));
		g2.fillRect(0, 0, 2*mark.length, 0);
		
		for (int i=0; i < mark.length; i++)
		{
			g1.setColor(mark[i]);
			g1.drawLine(i, 0, i, 0);
		
			g2.setColor(mark[i]);
			g2.drawLine(mark.length + i, 0, mark.length + i, 0);
		}
		
		try
		{
			ImageIO.write(img1, "png", new File("markSW.png"));
			ImageIO.write(img2, "png", new File("markNE.png"));
		}
		catch (IOException ex) { ex.printStackTrace(); }
	}
}
