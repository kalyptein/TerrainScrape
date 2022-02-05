package main;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.servlet.ServletHandler;

import javafx.application.Application;
import javafx.stage.Stage;
import map.StaticMap;
import ui.MainWindow;
import ui.MapPageServlet;

/*
 * 
 * Google Account: 'terrainsim', '2568768316'
 * Marker Hosting: 'https://sites.google.com/site/terrainsim/home/files'
 * API Key: 'AIzaSyCRo7pxd6yoyd5gv_Gh7L_7Y-DhLeeYvKY'
 * 
 */

/*
 * TODO further development
 *
 * add "invert UTM zone" checkbox to declass stuff?
 * 
 * automate the final tile zoom level? (instead of manually choosing numbers of tiles)
 * 
 * allow features to be shown (roads, etc) in the final map?
 * 
 * When you click Save or Capture in MapPage.html, bring the app window and save/capture dialog to front/focus (do this within javascript or javafx?) 
 * 
 */

public class TerrainScrape extends Application
{
	public static final int version = 6;
	public static Server server;
	
	public static int port = 8080;
	
	public static Application instance;
	public static Properties props;
	
	public static void main(String[] args)
	{
		// configure properties
		props = new Properties();
		File propFile = new File("TerrainScrape.ini");
		if (propFile.exists())
		{
			try
			{
				props.load(new FileReader(propFile));
				
				String val = props.getProperty("port");
				if (val != null)
					TerrainScrape.port = Integer.valueOf(val);

				val = props.getProperty("markerURL");
				if (val != null)
					StaticMap.markerurl = val;
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}
		
		// launch server back-end
		server = new Server(port);
		server.setStopAtShutdown(true);
		
		ResourceHandler resHandler = new ResourceHandler();
		resHandler.setDirectoriesListed(true);
		resHandler.setWelcomeFiles(new String[] { "MapPage.html" });
		resHandler.setResourceBase("src/ui/");
		
		ServletHandler srvHandler = new ServletHandler();
		srvHandler.addServletWithMapping(MapPageServlet.class, "/srv/*");
		
		HandlerList handlerList = new HandlerList();
		handlerList.setHandlers(new Handler[] { resHandler, srvHandler, new DefaultHandler() });
		
        server.setHandler(handlerList);
	        
        try
        {
			server.start();
		        
		}
        catch (Exception e)
        {
			e.printStackTrace();
			System.exit(1);
		}
		
        // launch gui front-end
        Application.launch(args);
	}
	
	@Override
	public void start(Stage stage)
	{
		instance = this;
		
        // launch gui front-end
		MainWindow.createMainWindow(stage);
	}
	
	public void stop()
	{
		try
		{
			server.stop();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
}
