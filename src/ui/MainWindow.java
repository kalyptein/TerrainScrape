package ui;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Collectors;

import org.jdom2.Element;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;
import main.TerrainScrape;
import main.XMLUtil;
import map.MapGenerationThread;
import map.StaticMap;
import map.AreaBounds;
import map.GraphicsFormatEnum;

public class MainWindow extends BorderPane
{
	public static MainWindow instance;
	
	@FXML
	protected Spinner<Integer> xTileSpinner;
	
	@FXML
	protected Spinner<Integer> yTileSpinner;
	
	@FXML
	protected ChoiceBox<GraphicsFormatEnum> formatChoice;
	
	@FXML
	protected CheckBox subimageCheck;
	
	@FXML
	protected TextArea outputText;
	
	@FXML
	protected Menu recentFileMenu;
	
	FileChooser fc;
	

	public static void createMainWindow(Stage stage)
	{
        FXMLLoader fxmlLoader = new FXMLLoader(MainWindow.class.getResource("MainWindow.fxml"));
        fxmlLoader.setRoot(new MainWindow());

        try 
        {
            Parent parent = fxmlLoader.load();
            
    		stage.setTitle("TerrainScrape v" + TerrainScrape.version);
            Scene scene = new Scene(parent);
    		stage.setScene(scene);

    		stage.getIcons().add(new Image(MainWindow.class.getResourceAsStream("../images/earth-icon.png")));
    		
    		stage.setOnCloseRequest((event) -> instance.shutdown());

            stage.show();
        } 
        catch (IOException exception)
        {
            throw new RuntimeException(exception);
        }
	}

	public MainWindow()
	{
	}
	
	@FXML
	private void initialize()
	{
		fc = new FileChooser();
		fc.setInitialDirectory(new File("./maps"));
		fc.getExtensionFilters().add(new ExtensionFilter("XML", "*.xml", "*.XML"));
		
		formatChoice.getItems().addAll(GraphicsFormatEnum.values());
		formatChoice.getSelectionModel().select(0);
		
		xTileSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 1000, 4));
		yTileSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 1000, 4));
		
		instance = this;
		
		for (int i=0; i < 5; i++)
		{
			String val = TerrainScrape.props.getProperty("recent" + i);
			if (val != null)
			{
				MenuItem item = new MenuItem(val);
				item.setOnAction((event) -> openRecentFile(((MenuItem) event.getSource()).getText()));
				recentFileMenu.getItems().add(item);
			}
		}

		if (!recentFileMenu.getItems().isEmpty())
			recentFileMenu.setDisable(false);
	}
	
	public void doNewMapWindow()
	{
		TerrainScrape.instance.getHostServices().showDocument("http://localhost:" + TerrainScrape.port);
	}
	
	public void doOpenMapWindow()
	{
		File file = fc.showOpenDialog(outputText.getScene().getWindow());
		if (file != null)
		{
			TerrainScrape.instance.getHostServices().showDocument("http://localhost:" + TerrainScrape.port + "/MapPage.html?load=" + file.getAbsolutePath());
			updateRecentFiles(file);
			output(file.getName() + " opened.\n\n");
		}
	}
	
	public void saveMap(Element root)
	{
		// append capture parameters to map xml
		Element el = new Element("CaptureParameters");
		root.addContent(0, el);
		
		XMLUtil.xml(el, "xTiles", xTileSpinner.getValue());
		XMLUtil.xml(el, "yTiles", yTileSpinner.getValue());
		XMLUtil.xml(el, "Format", formatChoice.getValue().toString());
		XMLUtil.xml(el, "SaveSubimages", subimageCheck.isSelected());

		Platform.runLater(() ->
		{
			File file = fc.showSaveDialog(outputText.getScene().getWindow());
			if (file != null)
			{
				XMLUtil.writeXML(file, root);
				output(file.getName() + " saved.\n\n");
				updateRecentFiles(file);
			}
		});
	}

	public void captureArea(Element root)
	{
    	int xTiles = xTileSpinner.getValue();
    	int yTiles = yTileSpinner.getValue();
    	
    	GraphicsFormatEnum format = formatChoice.getValue();
    	boolean saveSubimages = subimageCheck.isSelected();

    	String name = root.getChildText("Name");
    	double north = Double.valueOf(root.getChildText("North"));
    	double south = Double.valueOf(root.getChildText("South"));
    	double east = Double.valueOf(root.getChildText("East"));
    	double west = Double.valueOf(root.getChildText("West"));
    	
    	final StaticMap overview = new StaticMap(1, new AreaBounds(north, south, east, west), true, true);
    	
    	Platform.runLater(() ->
    	{
        	Alert preview = FXDialogs.createAlert(AlertType.CONFIRMATION, outputText.getScene().getWindow(), "Preview", "", "", (ButtonType[]) null);
        	preview.getDialogPane().setGraphic(new ImageView(overview.getFXImage()));
        	((Button) preview.getDialogPane().lookupButton(ButtonType.OK)).setOnAction(event ->
        	{
        		// check if directory exists before overwriting
            	File mapDir = new File("./maps", name);
            	if (!mapDir.exists())
            	{
            		mapDir.mkdir();
            	}
            	else
            	{
            		Optional<ButtonType> b = FXDialogs.createAlert(AlertType.CONFIRMATION, outputText.getScene().getWindow(), "Overwrite", null,
            			"Directory '" + mapDir.getAbsolutePath() + "' already exists.  Overwrite?", (ButtonType[]) null).showAndWait();
            		
            		if (b.get().getButtonData() == ButtonData.OK_DONE)
            		{
            			Arrays.stream(mapDir.listFiles()).forEach(f -> f.delete());
            		}
            		else
            		{
            			return;
            		}
            	}
            	
            	new MapGenerationThread(mapDir, name, xTiles, yTiles, overview, format, saveSubimages).start();
        	});
        	
        	preview.show();
    	});
	}
	
	public void doSetPort()
	{
		FXDialogs.createTextInputDialog("Port", null, "Port: ", outputText.getScene().getWindow(), 
				Integer.toString(TerrainScrape.port)).showAndWait().ifPresent(port ->
		{
			if (port != null && !port.isEmpty())
			{
				TerrainScrape.port = Integer.valueOf(port);
				
				FXDialogs.createAlert(AlertType.CONFIRMATION, outputText.getScene().getWindow(), "Port Changed", "", 
						"Port change will take effect next time the application is launched.", (ButtonType[]) null);
			}
		});
	}
	
	public void doBuildMarkers()
	{
		StaticMap.buildMark();
	}
	
	public void doSetMarkerURL()
	{
		FXDialogs.createTextInputDialog("Marker URL", null, "Marker URL: ", outputText.getScene().getWindow(), 
				StaticMap.markerurl).showAndWait().ifPresent(url ->
		{
			if (url != null && !url.isEmpty())
			{
				StaticMap.markerurl = url;
			}
		});
	}
	
	public void doHelp()
	{
		try
		{
			TerrainScrape.instance.getHostServices().showDocument(MainWindow.class.getResource("help.html").toURI().toString());
		}
		catch (URISyntaxException e)
		{
			e.printStackTrace();
		}
	}
	
	public void output(String text)
	{
		outputText.appendText(text);
	}

	public void doClose()
	{
		shutdown();
		
		((Stage) outputText.getScene().getWindow()).close();
	}
	
	public void shutdown()
	{
		// save properties
		Properties props = new Properties(); 
		props.setProperty("port", Integer.toString(TerrainScrape.port));
		props.setProperty("markerURL", StaticMap.markerurl);
		
		List<String> recentList = recentFileMenu.getItems().stream().limit(5).map(m -> m.getText()).collect(Collectors.toList());
		
		for (int i=0; i < recentList.size(); i++)
		{
			props.setProperty("recent" + i, recentList.get(i));
		}
		
		try
		{
			props.store(new FileWriter(new File("TerrainScrape.ini")), "TerrainScrape Parameters");
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
	
	protected void updateRecentFiles(File file)
	{
		List<MenuItem> recentFiles = recentFileMenu.getItems().stream().filter(m -> !m.getText().equals(file.getAbsolutePath())).collect(Collectors.toList());
		
		MenuItem item = new MenuItem(file.getAbsolutePath());
		item.setOnAction((event) -> openRecentFile(((MenuItem) event.getSource()).getText()));
		recentFiles.add(0, item);
		
		recentFileMenu.setDisable(recentFiles.isEmpty());
		
		if (recentFiles.size() > 5)
			recentFiles = recentFiles.subList(0, 5);
		
		recentFileMenu.getItems().setAll(recentFiles);
	}

	protected void openRecentFile(String filename)
	{
		File file = new File(filename);
		if (file.exists())
		{
			TerrainScrape.instance.getHostServices().showDocument("http://localhost:" + TerrainScrape.port + "/MapPage.html?load=" + file.getAbsolutePath());
			updateRecentFiles(file);
			output(file.getName() + " opened.\n\n");
		}
		else
		{
			// file does not exist, delete recent entry
			FXDialogs.createAlert(AlertType.ERROR, outputText.getScene().getWindow(), "File Missing", "", "'" + filename + "' not found.", (ButtonType[]) null).show();

			List<MenuItem> recentFiles = recentFileMenu.getItems().stream().filter(m -> !m.getText().equals(filename)).collect(Collectors.toList());
			recentFileMenu.getItems().setAll(recentFiles);
			recentFileMenu.setDisable(recentFiles.isEmpty());
		}
	}
}
