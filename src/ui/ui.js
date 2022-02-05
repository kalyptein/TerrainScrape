
var selected = null;

var drawName = null;
var drawing = false;

// select the specified area (display its coordinates, make it draggable and editable, and select the corresponding radio button) 
function select(name)
{
	if (selected == name)
		return;
	
	// lock all rects except this one (if it exists)
	
	var list = Object.keys(rects);
	for (var i=0; i < list.length; i++)
	{
		freezeRect(list[i], true);
		document.getElementById(list[i]).children[0].checked = false;
		var r2 = getRect(list[i])
		if (r2)
			r2.set('strokeColor', '#000000');
	}

	updateCoords(name);
	
	var r = getRect(name);
	if (r)
	{
		selected = name;
		document.getElementById(name).children[0].checked = true;

		if (!r.locked)
		{
			freezeRect(name, false);
			r.set('strokeColor', '#FFFFFF');
		}
	}
	else
	{
		selected = null;
	}
}

// if an area radio button is selected, select the corresponding area on the map
function radioSelect(event)
{
	select(event.target.value);
}

// get name for area to be added
function doDraw()
{
	if (drawing)
	{
		draw("");
	}
	else
	{
		var name = window.prompt('Name', 'NewArea');
		
		if (rects[name])
		{
			window.alert("'" + name + "' is already in use.");
		}
		else
		{
			draw(name);
		}
	}
}

// toggle drawing mode
function draw(name)
{
	drawing = !drawing;

	if (!drawing || name == null || name == "")
	{
		drawName = null;
		document.getElementById("addButton").className='';
		document.map.set('draggableCursor', 'auto');
	}
	else
	{
		drawName = name;
		document.getElementById("addButton").className='buttonPressed';		// button is indented
		document.map.set('draggableCursor', 'crosshair');
	}
}

// set map pannable or not
function lockMap(state)
{
	if (state)
		document.map.set("gestureHandling", "none");
	else
		document.map.set("gestureHandling", "auto");
}

// sets map type (SATELLITE, TERRAIN, ROADMAP, or HYBRID)
function setMapType(state)
{
	if (state)
		document.map.setMapTypeId(state);
	else
		document.map.setMapTypeId(google.maps.MapTypeId.SATELLITE);
}


// set up the map on page load
function initialize()
{
	// show whole world
	var mapOptions = {
		//zoom : 14,
		zoom : 1,
		minZoom : 1,
		center : new google.maps.LatLng(0.0, 0.0),
		mapTypeId : google.maps.MapTypeId.SATELLITE,
		disableDefaultUI: true,
		zoomControl: true,
		mapTypeControl: true,
		scaleControl: true,
		backgroundColor : "#666970",
		draggable: true
	};
	
	document.map = new google.maps.Map(document.getElementById("map_canvas"), mapOptions);
	
	rectList = document.getElementById("rect_list");
	
	// listen for map clicks, if one is detected and drawing is on, place a new area rectangle on the map and exit drawing.
	document.map.addListener('click', function(event)
	{
		// add default rectangle
		if (drawing)
		{
			var center = event.latLng;
			var bounds = document.map.getBounds();
			var dimensions = bounds.toSpan();
			
			if (drawName != null && drawName != "")
			{
				var r = rect(drawName, center.lat() + (dimensions.lat()/10), center.lat() - (dimensions.lat()/10), 
					center.lng() + (dimensions.lng()/10), center.lng() - (dimensions.lng()/10));

				select(drawName);
			}
			
			drawing = false;
			document.getElementById("addButton").className='';
			document.map.set('draggableCursor', 'auto');
		}
	});
	
	// if there is a querystring with load=filename, request load from server
	var filename = getQueryParameter('load');
	if (filename)
	{
		openMap(filename);
	}
}

// request xml map file from server, restore map parameters and areas
function openMap(filename)
{
	var xhttp = new XMLHttpRequest();
	
    xhttp.onreadystatechange = function()
    {
        if (this.readyState == 4 && this.status == 200)
        {
        	var xmlDoc = this.responseXML;

    		var latVal = xmlDoc.getElementsByTagName("CenterLat")[0].textContent; 
    		var lngVal = xmlDoc.getElementsByTagName("CenterLng")[0].textContent;
    		var zoom = Number(xmlDoc.getElementsByTagName("Zoom")[0].textContent);

        	document.map.setCenter(new google.maps.LatLng(latVal, lngVal));
        	document.map.setZoom(zoom);
        	
        	// clear all rects
        	select(null);
       		var list = Object.keys(rects);
       		for (var i=0; i < list.length; i++)
       		{
       			removeRect(list[i]);
       		}

        	// make rects
        	var rectXMLSets = xmlDoc.getElementsByTagName("MapArea");
        	for (var i=0; i < rectXMLSets.length; i++)
        	{
        		var rectXML = rectXMLSets[i];
        		
        		var name = rectXML.getElementsByTagName("Name")[0].textContent;
        		var north = Number(rectXML.getElementsByTagName("North")[0].textContent);
        		var south = Number(rectXML.getElementsByTagName("South")[0].textContent);
        		var east = Number(rectXML.getElementsByTagName("East")[0].textContent);
        		var west = Number(rectXML.getElementsByTagName("West")[0].textContent);
        		
        		var r = rect(name, north, south, east, west);
        		
        		r.locked = (rectXML.getElementsByTagName("Lock")[0].textContent === 'true');
        		document.getElementById(name).className = (r.locked) ? 'itemLocked' : '';
        		r.set('clickable', !r.locked);
        	}
       }
    };
    
    xhttp.open("GET", "http://localhost:8080/srv/openMap?load=" + filename, true);
    xhttp.send(); 
}

// package specified area coords as xml and send to server to perform static map capture 
function capture(name)
{
	if (!name)
		name = selected;
	
	var r = getRect(name)
	
	if (r)
	{
		var doc = document.implementation.createDocument(null, "", null);
		var root = doc.createElement("Capture");
		doc.appendChild(root);
		root.appendChild(element(doc, "Name", name));
		
		var bounds = getBounds(name);
		root.appendChild(element(doc, "North", bounds.north));
		root.appendChild(element(doc, "South", bounds.south));
		root.appendChild(element(doc, "East", bounds.east));
		root.appendChild(element(doc, "West", bounds.west));

		var msg = (new XMLSerializer()).serializeToString(doc);
		
		var xhttp = new XMLHttpRequest();
		xhttp.open("POST", "http://localhost:8080/srv/capture", true);
		xhttp.setRequestHeader("Content-type", "text/xml");
		xhttp.send(msg);
	}
}

// package map and area parameters as xml and send to server to save 
function saveMap()
{
	var doc = document.implementation.createDocument(null, "", null);
	
	var root = doc.createElement("MapView");
	doc.appendChild(root);

	var center = document.map.getCenter();
	root.appendChild(element(doc, "CenterLat", center.lat()));
	root.appendChild(element(doc, "CenterLng", center.lng()));
	root.appendChild(element(doc, "Zoom", document.map.getZoom()));

	var list = Object.keys(rects);
	for (var i=0; i < list.length; i++)
	{
		var el = doc.createElement("MapArea");
		root.appendChild(el);
		
		el.appendChild(element(doc, "Name", list[i]));
		el.appendChild(element(doc, "Lock", getRect(list[i]).locked));
		
		var bounds = getBounds(list[i]);
		el.appendChild(element(doc, "North", bounds.north));
		el.appendChild(element(doc, "South", bounds.south));
		el.appendChild(element(doc, "East", bounds.east));
		el.appendChild(element(doc, "West", bounds.west));
	}

	var msg = (new XMLSerializer()).serializeToString(doc);
	
	var xhttp = new XMLHttpRequest();
	xhttp.open("POST", "http://localhost:8080/srv/saveMap", true);
	xhttp.setRequestHeader("Content-type", "text/xml");
	xhttp.send(msg);
}

function enter(event, func, arg)
{
	// only trigger on Enter key press in textfield, or onchange
	if (event && event.keyCode == 13)
		func(arg);
}

