package ui;

import java.io.File;
import java.io.IOException;
import java.util.stream.Collectors;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jdom2.Element;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

import main.XMLUtil;
import map.GraphicsFormatEnum;

@SuppressWarnings("serial")
public class MapPageServlet extends HttpServlet
{
	public MapPageServlet()
    {
		super();
    }

	@Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        response.setContentType("text/html");
        response.setStatus(HttpServletResponse.SC_OK);
        
        if (request.getContentType().equalsIgnoreCase("text/xml"))
        {
        	String xmlString = request.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
        	Element root = XMLUtil.parseXML(xmlString);
        	
        	if (root != null)
        	{
        		if (root.getName().equalsIgnoreCase("Capture"))
        		{
        			MainWindow.instance.captureArea(root);
        		}
        		else if (root.getName().equalsIgnoreCase("MapView"))
        		{
        			MainWindow.instance.saveMap(root);
        		}
        	}
        }
    }

	@Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
		File file = new File(request.getParameter("load"));
		
        response.setContentType("text/xml");
        response.setStatus(HttpServletResponse.SC_OK);
        
		XMLOutputter xmlOutputter = new XMLOutputter(Format.getPrettyFormat());
		if (file.exists())
		{
			Element root = XMLUtil.parseXML(file);
			String xmlString = xmlOutputter.outputString(root);
			
	        response.getWriter().print(xmlString);
	        
	        // set capture parameters
			Element params = root.getChild("CaptureParameters");
			if (params != null)
			{
				MainWindow.instance.xTileSpinner.getValueFactory().setValue(Integer.valueOf(params.getChildText("xTiles")));
				MainWindow.instance.yTileSpinner.getValueFactory().setValue(Integer.valueOf(params.getChildText("yTiles")));
				MainWindow.instance.formatChoice.setValue(GraphicsFormatEnum.valueOf(params.getChildText("Format")));
				MainWindow.instance.subimageCheck.setSelected(Boolean.valueOf(params.getChildText("SaveSubimages")));
			}
		}
    }
}
