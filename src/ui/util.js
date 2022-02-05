
// parse string into xml structure
function parseXML(xmlString)
{
	return (new DOMParser()).parseFromString(xmlString, "text/xml");
}

// create a new xml element of name with value text inside xmldoc
function element(xmldoc, name, text)
{
	var el = xmldoc.createElement(name);
	el.appendChild(xmldoc.createTextNode(text))
	return el;
}

// extract value for given key from querystring
function getQueryParameter(name, url)
{
    if (!url)
    	url = window.location.search;
    
    name = name.replace(/[\[\]]/g, "\\$&");
    
    var regex = new RegExp("[?&]" + name + "(=([^&#]*)|&|#|$)"),
        results = regex.exec(url);
    
    if (!results) return null;
    if (!results[2]) return '';
    
    return decodeURIComponent(results[2].replace(/\+/g, " "));
}
