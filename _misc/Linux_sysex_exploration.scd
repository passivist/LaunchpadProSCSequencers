MIDIClient.init;
MIDIIn.connectAll;
MIDIClient.destinations[4]
m = MIDIOut(0);
// appearently you have to explicitly connect on linux...
m.connect(2);
m.latency = 0;

m.noteOn(0, 17, 35);
MIDIFunc.trace(true);