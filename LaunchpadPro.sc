/*
* LaunchpadPro a class for handling interfacing with the LaunchpadPro hardware.
*/

LaunchpadPro {
	*new {
		super.new.init()
	}

	init {
		MIDIClient.init
	}

	connect {

	}
}