	
var emptyBounds = { north: "", south: "", east: "", west: "", centerLat: "", centerLng: "" };

// stores map areas as properties (area name = property name, google.maps.Rectangle = property value)
var rects = { };

// reference to the <ul> in MapPage containing area names
var rectList = null;

// creates a new area rectangle with the given name and bounds, places it on the map, and adds it to the radio button list 
function rect(name, n, s, e, w)
{
	if (rects[name])
	{
		window.alert(newName + " is already in use.");
	}

	rects[name] = new google.maps.Rectangle(
	{
		strokeColor : '#000000',
		strokeOpacity : 0.8,
		strokeWeight : 2,
		fillColor : '#000000',
		fillOpacity : 0.1,
		map : document.map,
		bounds :
		{
			north : n,
			south : s,
			east : e,
			west : w
		},
		draggable : false,
		editable : false
	});
	
	rects[name].name = name;
	rects[name].locked = false;
	
	// if clicked, select this rectangle
	rects[name].addListener('click', function()
	{
		select(this.name);
	});
	
	// if this rectangle is resized or moved, update its coordinate display
	rects[name].addListener('bounds_changed', function()
	{
		updateCoords(this.name);
	});
	
	var item = document.createElement("li");
	item.id = name;
	item.style.verticalAlign = "middle";
	
	// area select radio button
	var input = document.createElement("input");
	input.type = "radio";
	input.name = "area";
	input.value = name;
	input.onclick = radioSelect;
	item.appendChild(input);
	
	// area name text
	var text = document.createTextNode(" " + name + " ");
	item.appendChild(text);
	
	rectList.appendChild(item);
	document.getElementById("removeButton").disabled = false;
	document.getElementById("renameButton").disabled = false;
	document.getElementById("lockButton").disabled = false;
	document.getElementById("captureButton").disabled = false;
	
	return rects[name];
}

// rename specified area
function renameRect(oldName)
{
	var r = getRect(oldName);
	if (r)
	{
		var newName = window.prompt('New Name', oldName)
	
		if (newName == null || newName == "" || newName == oldName)
			return;
	
		// name already in use
		if (rects[newName])
		{
			window.alert(newName + " is already in use.");
			return;
		}
	
		var item = document.getElementById(oldName);
		
		item.id = newName;
		item.childNodes[0].value = newName;
		item.childNodes[1].nodeValue = " " + newName + " ";

		rects[newName] = r;
		r.name = newName;
		delete rects[oldName];
		select(newName);
	}
}

// removes the specified area from the map and radio button list
function removeRect(name)
{
	var r = getRect(name);
	if (r && confirm("Delete " + r.name + "?"))
	{
		google.maps.event.clearListeners(r, 'click');
		google.maps.event.clearListeners(r, 'bounds_changed');
		updateCoords(null);
		r.setMap(null);
		
		var item = document.getElementById(name);
		if (item)
			rectList.removeChild(item);
		
		delete rects[name];
		selected = null;
	}
	
	if (rectList.children.length < 1)
	{
		document.getElementById("removeButton").disabled = true;
		document.getElementById("renameButton").disabled = true;
		document.getElementById("lockButton").disabled = true;
		document.getElementById("captureButton").disabled = true;
	}
}

// set whether the specified area can be dragged and edited
function freezeRect(name, state)
{
	var r = getRect(name);
	if (r)
	{
		r.setDraggable(!state);
		r.setEditable(!state);
	}
}

//set whether the specified area unfreezes when selected
function lockRect(name, state)
{
	var r = getRect(name);
	if (r)
	{
		if (!state)
			state = !r.locked;
		
		r.locked = state;
		document.getElementById(name).className = (r.locked) ? 'itemLocked' : '';
		r.set('clickable', !r.locked);
		
		select(null);
		select(name);
	}
}

// returns the specified area
function getRect(name)
{
	if (rects[name])
		return rects[name];
	else
		return null;
}

// package the coordinates of the given area in a custom object
function getBounds(name)
{
	var r = getRect(name);
	if (r)
	{
		var bounds = r.getBounds();
		return {
			north: bounds.getNorthEast().lat(), 
			south: bounds.getSouthWest().lat(), 
			east: bounds.getNorthEast().lng(), 
			west: bounds.getSouthWest().lng(),
			centerLat: bounds.getCenter().lat(),
			centerLng: bounds.getCenter().lng()
		};
	}
	else
	{
		return emptyBounds;
	}
}

// display the coordinates of the specified area in the text fields
function updateCoords(name)
{
	if (rects[name])
	{
		var bounds = getBounds(name);
		document.getElementById("centerLat").value = bounds.centerLat;
		document.getElementById("centerLng").value = bounds.centerLng;
		document.getElementById("northLat").value = bounds.north;
		document.getElementById("southLat").value = bounds.south;
		document.getElementById("eastLng").value = bounds.east;
		document.getElementById("westLng").value = bounds.west;
	}
}

function moveRect(name)
{
	if (!name)
	{
		if (selected)
			name = selected;
		else
			return;
	}

	var r = getRect(name);
	
	if (r)
	{
		var bounds = r.getBounds();
		var center = bounds.getCenter();
		var dx = document.getElementById("centerLng").value - center.lng();
		var dy = document.getElementById("centerLat").value - center.lat();

		r.setBounds({
			north: bounds.getNorthEast().lat() + dy,
			south: bounds.getSouthWest().lat() + dy,
			east: bounds.getNorthEast().lng() + dx,
			west: bounds.getSouthWest().lng() + dx
		});
		
		updateCoords(name);
	}
}

function setRectBounds(name)
{
	if (!name)
	{
		if (selected)
			name = selected;
		else
			return;
	}

	var r = getRect(name);
	
	if (r)
	{
		r.setBounds({
			north: Number(document.getElementById("northLat").value),
			south: Number(document.getElementById("southLat").value),
			east: Number(document.getElementById("eastLng").value),
			west: Number(document.getElementById("westLng").value)
		});
		
		updateCoords(name);
	}
}
