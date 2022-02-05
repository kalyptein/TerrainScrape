package map;

public enum GraphicsFormatEnum
{
	PNG, JPEG, BMP, GIF;
	
	public String ext() { return "." + name().toLowerCase(); }
}
