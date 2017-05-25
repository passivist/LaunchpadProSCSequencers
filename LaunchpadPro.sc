/*
	LaunchpadPro a class for handling interfacing with the LaunchpadPro hardware.
*/

LaunchpadPro {
	var <inUID, <outPort;
	var <>modes;
	var innerLookup, outerLookup, modeLookup;
	var innerButtonResponder, outerButtonOnResponder;

	*new {
		^super.new.init()
	}

	init {
		var launchpadExists = false;

		innerLookup = [
			// left to right top to bottom
			81, 82, 83, 84, 85, 86, 87, 88,
			71, 72, 73, 74, 75, 76, 77, 78,
			61, 62, 63, 64, 65, 66, 67, 68,
			51, 52, 53, 54, 55, 56, 57, 58,
			41, 42, 43, 44, 45, 46, 47, 48,
			31, 32, 33, 34, 35, 36, 37, 38,
			21, 22, 23, 24, 25, 26, 27, 28,
			11, 12, 13, 14, 15, 16, 17, 18
		];

		outerLookup = [
			91, 92, 93, 94, 				// top row left to right (the last 4 buttons here are reserved for modes)
			80, 70, 60, 50, 40, 30, 20, 10,	// left column top down
			89, 79, 69, 59, 49, 39, 29, 19, // right column top down
			1,   2,  3,  4,  5,  6,  7,  8, // bottom row left to right
		];

		modeLookup = [
			95, 96, 97, 98
		];

		/** STARTUP */
		// It's important that this comes before MIDIOut.connect because it will delete all prior connections
		MIDIIn.connectAll();

		// search for the LaunchpadPro Hardware which has a different name on each platform
		MIDIClient.sources.do{ |item|
			Platform.case(
				\osx,     {
					if((item.device == "Launchpad Pro") && (item.name == "Standalone Port")){ inUID = item.uid; launchpadExists = true; };
				},
				\linux,   {
					if(item.name == "Launchpad Pro MIDI 2"){ inUID = item.uid; launchpadExists = true; };
				},
				\windows, {
					if(item.name == "Win Name"){ inUID = item.uid; launchpadExists = true; };
				}
			)
		};

		MIDIClient.destinations.do{ |item, i=0|
			Platform.case(
				\osx,     {
					if(item.device == "Launchpad Pro" && item.name == "Standalone Port"){ outPort = MIDIOut(i) };
				},
				\linux,   {
					if(item.name == "Launchpad Pro MIDI 2"){ outPort = MIDIOut(0); outPort.connect(i); };
				},
				\windows, {
					if(item.name == "Win Name"){ outPort = MIDIOut(i) };
				}
			);
		};

		if(launchpadExists.not){
			Error("No LaunchpadPro connected! Connect your hardware before running creating the LaunchpadPro Object").throw;
		};

		// create initial empty modes
		modes = Array.fill(4, {|i| LaunchpadProMode.new(i, this, false) });

		/** MIDI INPUT HANDLING */
		/* inner grid: */
		MIDIFunc.noteOn({ |vel, note|
			var button;

			innerLookup.do{|item, i|
				if(note == item){button = i};
			};

			modes.do{|mode|
				if(mode.isActive){
					mode.inputCallback(button, vel, 'inner');
				}
			}
		}, innerLookup, nil, inUID);

		MIDIFunc.noteOff({ |vel, note|
			var button;

			innerLookup.do{|item, i|
				if(note == item){button = i}
			};

			modes.do{|mode|
				if(mode.isActive){
					mode.inputCallback(button, vel, 'inner')
				}
			}
		}, innerLookup, nil, inUID);

		/* outer buttons */
		MIDIFunc.cc({|val, cc|
			var button;

			outerLookup.do{|item, i|
				if(cc == item){button = i}
			};

			modes.do{|mode|
				if(mode.isActive){
					mode.inputCallback(button, val, 'outer')
				}
			}
		}, outerLookup, nil, inUID);

		/* mode switching */
		MIDIFunc.cc({|val, cc|
			var modeToActivate;
			var stateArr;

			if(val > 0){ // only respond to "note-on" even though these are cc values
				this.resetLeds;
				
				modeLookup.do{|item, i|
					if(cc == item){ modeToActivate = i }
				};

				modes.do{|mode, i|
					if(i == modeToActivate){
						mode.isActive_(true);
						stateArr = mode.internalState;
						mode.draw;
					} {
						mode.isActive_(false)
					}
				};
				
				this.updateLeds(stateArr);
			}
		}, modeLookup, nil, inUID);

		// finally reset all the leds on the launchpad
		this.resetLeds;

	}

	/** MODE HANDLING */
	registerMode { |mode|
		/* Errors */
		if(mode.modeID.isNil){ Error("modeID is nil! Don't know where to put mode").throw };
		if((mode.modeID > 3) || (mode.modeID < 0 )){ Error("modeID out of bounds error").throw};

		modes[mode.modeID] = mode;

		this.updateLeds;
	}

	/** DRAWING INTERNAL STATE */
	/*
		I recommend calling this whenever changing the internal state of the LaunchpadPro Object or one of the modes 
		I opted to format the stateArr thus for usability:
		[[innnerGrid [LED, Colour] pairs], [outerGrid [LED, Colour] Pairs] ]
		
		We rely on the modes to reset LEDs so we don't have to redraw everything everytime
	*/
	updateLeds {|stateArr|
		
		var innerLeds, outerLeds;

		innerLeds = [];
		outerLeds = [];
		
		/*
			Draw the mode leds
		*/
		modes.do{|mode, i|
			if(mode.isActive){
				this.drawLed(modeLookup[i], 13)
			}{
				this.drawLed(modeLookup[i], 0)
			}
		};

		/*
			Draw the current state of the selected mode, the modes will
			only call this when their isActive flag is true
		*/
		// postf("\nFinal Array before drawing: %\n", stateArr );

		// maybe wrap this in a method or function
		if(stateArr.notNil){
			// prepare innerLeds for drawing
			if(stateArr[0][0].notNil){
				innerLeds = stateArr[0].flop;
				
				innerLeds[0] = innerLeds[0].collect{ |item, i|
					innerLookup[item];
				};

				this.drawLed(innerLeds[0], innerLeds[1]);
			};

			if(stateArr[1][0].notNil){
				outerLeds = stateArr[1].flop;

				outerLeds[0] = outerLeds[0].collect { |item, i|
					outerLookup[item];
				};

				this.drawLed(outerLeds[0], outerLeds[1]);
			};
		}
	}

	/** DRAWING METHODS */
	// LED syntax: (240,0,32,41,2,16,10,<LED>,<Colour>,247)
	drawLed { |leds, colours|
		var header = Int8Array[240,0,32,41,2,16,10];
		var end = Int8Array[247];
		var body;

		leds = leds.asArray;
		colours = colours.asArray;

		if((leds.size > 96) || (colours.size > 96) ){
			"LED Colour Pairs can't exeed 97: everything more than that will not be processed by the LaunchpadPro".warn
		};

		// make colours the size of leds by repeating
		if(colours.size < leds.size){
			colours = colours.wrapExtend(leds.size)
		};

		body = Int8Array.newFrom( lace([leds, colours]) );

		outPort.sysex(header ++ body ++ end);
	}

	// Column syntax: (240,0,32,41,2,16,12, <Column>, <Colour>, 247)
	drawColumn {|column, colours|
		var header = Int8Array[240,0,32,41,2,16,12];
		var end = Int8Array[247];
		var body;

		column = column.asArray;
		colours = colours.asArray;

		if(colours.size > 10){
			"Colours can't exeed 10 values: everything more than that will not be processed by the LaunchpadPro".warn
		};

		body = Int8Array.newFrom( column ++ colours );

		outPort.sysex(header ++ body ++ end);
	}

	// Row syntax: (240,0,32,41,2,16,13,<Row>,<Colour>,247)
	drawRow {|row, colours|
		var header = Int8Array[240,0,32,41,2,16,13];
		var end = Int8Array[247];
		var body;

		row = row.asArray;
		colours = colours.asArray;

		if(colours.size > 10){
			"Colours can't exeed 10 values: everything more than that will not be processed by the LaunchpadPro".warn
		};

		body = Int8Array.newFrom( row ++ colours );

		outPort.sysex(header ++ body ++ end);
	}

	// All syntax: (240,0,32,41,2,16,14,<Colour>,247)
	drawAll { |colour|
		var header = Int8Array[240,0,32,41,2,16,14];
		var end = Int8Array[247];
		var body;

		body = Int8Array.newFrom( colour.asArray );

		outPort.sysex(header ++ body ++ end);
	}
	// Flash LEDs: (240,0,32,41,2,16,35, <LED>, <Colour>, 247)
	flashLed { |leds, colours|
		var header = Int8Array[240,0,32,41,2,16,35];
		var end = Int8Array[247];
		var body;

		leds = leds.asArray;
		colours = colours.asArray;

		if((leds.size > 96) || (colours.size > 96) ){
			"LED Colour Pairs can't exeed 97: everything more than that will not be processed by the LaunchpadPro".warn
		};

		// make colours the size of leds by repeating
		if(colours.size < leds.size){
			colours = colours.wrapExtend(leds.size)
		};

		body = Int8Array.newFrom( lace([leds, colours]) );

		outPort.sysex(header ++ body ++ end);
	}

	// Pulse LEDs: (240, 0,32,41,2,16,40,<LED>, <Colour>, 247)
	pulseLed { |leds, colours|
		var header = Int8Array[240,0,32,41,2,16,40];
		var end = Int8Array[247];
		var body;

		leds = leds.asArray;
		colours = colours.asArray;

		if((leds.size > 96) || (colours.size > 96) ){
			"LED Colour Pairs can't exeed 97: everything more than that will not be processed by the LaunchpadPro".warn
		};

		// make colours the size of leds by repeating
		if(colours.size < leds.size){
			colours = colours.wrapExtend(leds.size)
		};

		body = Int8Array.newFrom( lace([leds, colours]) );

		outPort.sysex(header ++ body ++ end);
	}

	// LED RGB: (240, 0, 32, 41, 2, 16, 11, <LED>, <Red>, <Green>, <Blue>, 247)
	drawLedRGB { |leds, r, g, b|
		var header = Int8Array[240,0,32,41,2,16,11];
		var end = Int8Array[247];
		var body;

		leds = leds.asArray;
		r = r.asArray; g = g.asArray; b = b.asArray;

		if((leds.size > 77) ){
			"LED RGB tuples can't exeed 78: everything more than that will not be processed by the LaunchpadPro".warn
		};

		// make colours the size of leds by repeating
		r = r.wrapExtend(leds.size);
		g = g.wrapExtend(leds.size);
		b = b.wrapExtend(leds.size);


		body = Int8Array.newFrom( lace([leds, r, g, b]) );

		outPort.sysex(header ++ body ++ end);
	}

	// Grid RGB: (240, 0, 32, 41, 2, 16, 15, <Grid Type>, <Red>, <Green>, <Blue>, 247)
	// Grid Type is 0 for 10x10 Grid or 1 for 8x8 inner grid
	drawGridRGB { |type, r, g, b|
		var header = Int8Array[240,0,32,41,2,16,15];
		var end = Int8Array[247];
		var body;

		var rgbSizes = [r.size, g.size, b.size];
		var rgb = [];

		rgbSizes = rgbSizes.sort.reverse.postln;

		type = type.asArray;
		r = r.asArray; g = g.asArray; b = b.asArray;

		r = r.wrapExtend(rgbSizes[0]);
		g = g.wrapExtend(rgbSizes[0]);
		b = b.wrapExtend(rgbSizes[0]);

		body = Int8Array.newFrom( type ++ lace([r, g, b]) );

		outPort.sysex(header ++ body ++ end);
	}

	resetLeds {
		outPort.sysex(Int8Array[ 240,0,32,41,2,16,14,0,247]);
	}
}


LaunchpadProMode {
	var <modeID, <launchpad, <>isActive;
	var <internalState;
	
	*new { |modeID, launchpad, isActive = false|
		^super.new.init(modeID, launchpad, isActive)
	}

	init { |id, lPad, active|
		modeID = id;
		launchpad = lPad;
		isActive = active;
		internalState = nil;
	}

	draw {
		launchpad.resetLeds;
	}
	
	// called from LaunchpadPro and should be implemented in all inheriting modes to
	// access the MIDI
	inputCallback {|button, val, type|

	}
}

