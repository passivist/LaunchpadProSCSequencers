MIDIClient.init;
~l = LaunchpadDrumSeq(4, 4);
~l.nextStep;
~l.draw;

SynthDef(\a, { EnvGen.ar(Env.linen(0, 0, 0.00001), doneAction: 2)}).add;

(
Pdef(\test, Pbind(
	\instrument, \a,
	\a, Pfunc({ ~l.nextStep; ~l.draw }),
	\dur, 1/8
)).play
)

Pdef(\test).stop;
