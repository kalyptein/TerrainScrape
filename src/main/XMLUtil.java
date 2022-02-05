package main;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

public class XMLUtil
{
	public static Element xml(Element parent, String childName, String childText)
	{
		Element e = new Element(childName);
		
		if (childText != null)
			e.setText(childText);
		
		if (parent != null)
			parent.addContent(e);
		
		return e;
	}
	
	public static Element xml(Element parent, String childName, double childValue)
	{
		return xml(parent, childName, Double.toString(childValue));
	}

	public static Element xml(Element parent, String childName, int childValue)
	{
		return xml(parent, childName, Integer.toString(childValue));
	}

	public static Element xml(Element parent, String childName, boolean childValue)
	{
		return xml(parent, childName, Boolean.toString(childValue));
	}

	public static Element xmlSet(Element root, String name, String zoomLevel)
	{
		Element element = new Element("OverlaySet"); 
		element.setAttribute("name", name);
		element.setAttribute("zoomLevel", zoomLevel);
		root.addContent(element);
		return element;
	}
	
	public static void writeXML(File file, Element root)
	{
		try (FileOutputStream ostream = new FileOutputStream(file)) 
		{
			XMLOutputter xmlOutputter = new XMLOutputter(Format.getPrettyFormat());
			
			Document doc = (root.getDocument() != null) ? root.getDocument() : new Document(root);
			
			xmlOutputter.output(doc, ostream);
			ostream.flush();
			ostream.close();
		}
		catch (IOException ex) { ex.printStackTrace(); }
	}
	
	public static Element parseXML(File file)
	{
		Element root = null;
		
		try (FileInputStream istream = new FileInputStream(file))
		{
			SAXBuilder builder = new SAXBuilder();
	        Document doc = builder.build(istream);
	        root = doc.getRootElement();
		}
		catch (FileNotFoundException e) { e.printStackTrace(); }
		catch (IOException e) { e.printStackTrace(); }
		catch (JDOMException e) { e.printStackTrace(); }
		
		return root;
	}
	
	public static Element parseXML(String xmlString)
	{
		Element root = null;
		
		try (ByteArrayInputStream istream = new ByteArrayInputStream(xmlString.getBytes("UTF-8")))
		{
			
			SAXBuilder builder = new SAXBuilder();
	        Document doc = builder.build(istream);
	        root = doc.getRootElement();
		}
		catch (FileNotFoundException e) { e.printStackTrace(); }
		catch (IOException e) { e.printStackTrace(); }
		catch (JDOMException e) { e.printStackTrace(); }
		
		return root;
	}
}
