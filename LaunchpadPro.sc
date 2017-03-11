/*
* LaunchpadPro a class for handling interfacing with the LaunchpadPro hardware.
*/

LaunchpadPro {
	var <inPort, <outPort;

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
					if(item.name == "Mac Name"){ inPort = item; launchpadExists = true; };
				},
				\linux,   {
					if(item.name == "Launchpad Pro MIDI 2"){ inPort = item; launchpadExists = true; };
				},
				\windows, {
					if(item.name == "Win Name"){ inPort = item; launchpadExists = true; };
				}
			)
		};

		MIDIClient.destinations.do{ |item|
						Platform.case(
				\osx,     {
					if(item.name == "Mac Name"){ outPort = item; };
				},
				\linux,   {
					if(item.name == "Launchpad Pro MIDI 2"){ outPort = item; };
				},
				\windows, {
					if(item.name == "Win Name"){ outPort = item; };
				}
			)
		};

		if(launchpadExists.not){
			Error("No LaunchpadPro connected! Connect your hardware before running creating the LaunchpadPro Object").throw;
		};

		MIDIIn.connectAll(false);
	}

}