/*
* LaunchpadPro a class for handling interfacing with the LaunchpadPro hardware.
*/

LaunchpadPro {
	var <inUID, <outPort;

	*new {
		^super.new.init()
	}

	init {
		var launchpadExists = false;

		if(MIDIClient.initialized.not){
			Error("MIDIClient not initialized. Must call MIDIClient.init before creating a LaunchpadPro Object").throw
		};

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
					if(item.name == "Launchpad Pro MIDI 2"){ outPort = MIDIOut(i) };
				},
				\windows, {
					if(item.name == "Win Name"){ outPort = MIDIOut(i) };
				}
			);
		};

		if(launchpadExists.not){
			Error("No LaunchpadPro connected! Connect your hardware before running creating the LaunchpadPro Object").throw;
		};

		MIDIIn.connectAll(false);
	}

	// LED syntax: (240,0,32,41,2,16,10,<LED>,<Colour>,247)
	drawLeds { |leds, colours|
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

	// All syntax: (240,0,32,41,2,16,14,<Colour>,247)

	// Flash LEDs: (240,0,32,41,2,16,35, <LED>, <Colour>, 247)

	// Pulse LEDs: (240, 0,32,41,2,16,40,<LED>, <Colour>, 247)

	// LED RGB: (240, 0, 32, 41, 2, 16, 11, <LED>, <Red>, <Green>, <Blue>, 247)

	// Grid RGB: (240, 0, 32, 41, 2, 16, 15, <Grid Type>, <Red>, <Green>, <Blue>, 247)
	// Grid Type is 0 for 10x10 Grid or 1 for 8x8 inner grid




	resetLeds {
		outPort.sysex(Int8Array[ 240,0,32,41,2,16,14,0,247]);
	}
}
