(
TempoClock.default.tempo_(110/60);
MIDIClient.init;
~l = LaunchpadDrumSeq(4, 4);
~bd = Buffer.read(s, "/Volumes/SSD 500 GB/Samples/DRITTE/SAMPLES/VINTAGE MACHINES/[KB6]_Roland_CR8000/cr8k01.wav");
~sd = Buffer.read(s, "/Volumes/SSD 500 GB/Samples/DRITTE/SAMPLES/VINTAGE MACHINES/[KB6]_Roland_CR8000/cr8k07.wav");
~hh = Buffer.read(s, "/Volumes/SSD 500 GB/Samples/DRITTE/SAMPLES/VINTAGE MACHINES/[KB6]_Roland_CR8000/cr8k14.wav");
)

(
~l.addSlot(0, 0,  { PlayBuf.ar(1, ~bd, doneAction: 2) ! 2 * 0.5; });
~l.addSlot(0, 1, { PlayBuf.ar(1, ~sd, doneAction: 2) ! 2 * 0.3; });
~l.addSlot(6, 0, { PlayBuf.ar(1, ~hh, doneAction: 2) ! 2 * 0.3; });

Tdef(\seq, {
	i = 0;
	loop{
		~l.nextStep;

		i = i+1;
		0.25.wait;
	}
}).play;
)

Tdef(\seq).stop;

~l.reset;


~l.slots[0].synthFunc.play;

(
Tdef(\test, {
	var numSteps, offset, i;
	numSteps = -4;
	offset = 0;
	i = 0;
	loop{
		var step;
		i = (i + 1) % numSteps;
		if(numSteps.isNegative){
			step = (i + numSteps).abs + offset;
		}{
			step = (i + offset);
		};
		step.postln;
		0.25.wait;
	}
}).play;
)

