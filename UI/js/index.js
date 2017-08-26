// Loader Module
var loader = {
	// Create a loader
	init: function(fullScreenFlag) {
		// Create loader elements
		var loader = document.createElement("div"),
			loaderBalls = [];
			
		for (var i = 0; i < 5; i++) {
			loaderBalls.push(document.createElement("div"));
		}
			
		// Check if loader must have fullscreen modifier or not
		fullScreenFlag ? loader.classList.add("loader", "loader-full-screen") : loader.classList.add("loader");
		// Append loader to appropriate parent: body if fullscreen / else #search-results
		fullScreenFlag ? document.body.appendChild(loader) : view.srcResults.appendChild(loader);
		// Append loader elements
		loaderBalls.forEach(function(el) {
			el.classList.add("loader-ball");
			loader.appendChild(el);
		});
		
		// Store a reference in model
		model.loader = loader;
	},
	// Remove the loader
	removeObj: function(loader) {
		if (loader && loader.parentNode) {
			loader.parentNode.removeChild(loader);
			model.loader = null;
		}
	}
};

// App components
var model = {
	// Search input
	srcInput: document.getElementById("search-input"),
	// Search string from user input
	srcString: "",
	// Continue parameter from query response
	lastContinue: null,
	// Loader reference
	loader: null
};

var view = {
	
	// Search mode view
	srcView: document.getElementById("search-view"),
	// Results mode view
	resultsView: document.getElementById("results-view"),
	// Search results section
	srcResults: document.getElementById("search-results"),
	// Search hint
	srcHint: document.getElementById("search-hint"),
	
	// Add results to the DOM
	addResults: function(page) {
		// Wrap each result in a div
		var wrapper = document.createElement("div");
		wrapper.classList.add("result");
		// Add content
		wrapper.innerHTML = "<a href='"+ page.weblink + "' target='_blank'>" + "<h1>" + page.title + "</h1><p>" + page.extract + "</p></a>";
		// Append the new div to results section
		view.srcResults.appendChild(wrapper);
	},
	
	// Toggle view modes
	toggleMode: function(mode) {
		var args = arguments;
		for (var i = 0; i < args.length; i++) {
			args[i].classList.toggle("hide");
		}
	},
	
	// Display messages in search and results mode
	displayMessage: function(messageTxt, parent) {
		// Create a message box
		var msg = document.createElement("div");
		msg.innerHTML = messageTxt;
		// Add style
		msg.classList.add("msg");
		// Append msg element to given parent (style depends on the parent, see CSS)
		parent.appendChild(msg);
	}
	
};

var controller = {
	
	// Prepare and send request to MediaWiki API
	doRequest: function(srcString, continueParam) {
		// Dynamically create a script
		var script = document.createElement("script");
		// Set its src property to API url - this technique would allow for cross-domain requests
		// Ask for JSON response (default is XML) and use a generator
		script.src = "http://localhost:7070/search/?query=";
		// Add search keywords to url through gsrsearch param
		script.src += formatSrcString(srcString);
		// Pass a reference to callback for data processing
		script.src += "&callback=controller.processData";
		// Append the script to body
		document.body.appendChild(script);
		// Remove the script after it executes
		script.onload = function() {
			document.body.removeChild(script);
		};
		
		// Helper function: percent encode spaces
		function formatSrcString(rawString) {
			return rawString.replace(/[\s]+/g, "%20");
		}
	},
	
	// Collect data from query response
	processData: function(response) {
		// Remove Loader
		loader.removeObj(model.loader);
		
		console.log(response);
		
		// If results found
		if (response) {
			// Print searched keywords in toolbar
			view.srcHint.innerHTML = model.srcString;
			
			// Store results in an array for easy access
			var results = [];
			for (var id in response) {
				results.push(response[id]);
			}
			
			// Append results in the HTML
			results.forEach(view.addResults);
			
			// Display results view
			if (view.resultsView.classList.contains("hide")) {
				view.toggleMode(view.resultsView);
			}
			
			// If response has continue parameter
			if (response.continue) {
				// Store it for subsequent request
				model.lastContinue = response.continue.continue + "&gsroffset=" + response.continue.gsroffset;
				// And add onscroll listener for infinite scroll functionality
				window.addEventListener("scroll", controller.loadMoreData);
			} 
			// Else
			else {
				// Remove eventual scroll listener
				window.removeEventListener("scroll", controller.loadMoreData);
				// Tell user that's all
				view.displayMessage("That's all, Folks!", view.srcResults);
			}
		} else {
			// Show message
			view.displayMessage("Sorry, no matches. Try with new keywords.", document.getElementById("search-msg"));
			// Bring back search view
			view.toggleMode(view.srcView);
			model.srcInput.focus();
		}
	},
	
	// Infinite scroll functionality
	loadMoreData: function(e) {
		// Load next 10 results when reaching bottom of page
		if (document.body.scrollHeight - window.pageYOffset <= window.innerHeight) {	/* Tested in Chrome, FF and IE11 - IE11 only works only the first time with = instead of <=  */		
			// Remove event listener to avoid multiple requests at one time
			window.removeEventListener("scroll", controller.loadMoreData);
			// Display loader
			if (!model.loader) {
				loader.init(false);
			}
			// Send next query
			controller.doRequest(model.srcString, model.lastContinue);
		}
	},
	
	// Reset all data for new search
	resetResults: function() {
		// Empty results section
		view.srcResults.innerHTML = "";
		// Reset continue parameter
		model.lastContinue = null;
	},
	
	initApp: function() {
		// Collect buttons and input references
		var srcBtn = document.getElementById("search-btn"),
			closeResults = document.getElementById("close-results"),
			srcInput = model.srcInput;
		// Fire search when hitting enter or search button
		srcBtn.addEventListener("click", srcHandler);
		srcInput.addEventListener("keydown", srcHandler);
		
		// Auto focus on input field
		srcInput.focus();
		
		// Handle close button in results view
		closeResults.addEventListener("click", newSrc);
		
		/** Event Handlers **/
		// Define search handler
		function srcHandler(e) {
			if( (e.type === "keydown" && e.which === 13) || e.type === "click" ) {
				// Get search string if not empty
				if (srcInput.value) {
					// Prevent IE11 from keeping focus on input field after firing search
					srcInput.blur();
					// Store for later use
					model.srcString = srcInput.value;
					
					// Empty src results
					//controller.resetResults(); **** DON'T NEED THIS HERE, RIGHT??
					
					// Toggle search View
					view.toggleMode(view.srcView);
					// Reset msg box 
					document.getElementById("search-msg").innerHTML = "";
					// Display fullscreen loader
					loader.init(true);
					// Send the request
					controller.doRequest(model.srcString, model.lastContinue);
				} else {
					srcInput.focus();
				}
			}
		}
		
		// Define close button handler: close results mode and prepare new search
		function newSrc() {
			// Remove scroll listener
			window.removeEventListener("scroll", controller.loadMoreData);
			// Hide results and display search view
			view.toggleMode(view.resultsView, view.srcView);
			// Reset data
			controller.resetResults();
			// Set focus on input field
			srcInput.focus();
		}
	}
	
};

controller.initApp();
