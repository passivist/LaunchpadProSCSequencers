MIDIClient.init;
~launchpad = LaunchpadPro();

~launchpad.outPort;
// *****************************************************************
MIDIClient.initialized
MIDIClient.sources;

MIDIIn.findPort("Launchpad Pro", "Launchpad Pro MIDI 2").name
MIDIIn.connectAll()
MIDIEndPoint
MIDIOut.connectByUID
(
var launchpadExists = false;
MIDIClient.sources.do{ |item|
	if(item.name == "Launchpad Pro MIDI 2"){ launchpadExists = true };
};

launchpadExists.postln;
)