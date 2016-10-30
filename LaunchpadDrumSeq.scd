(
TempoClock.default.tempo_(110/60);
MIDIClient.init;
MIDIIn.connectAll;
~l = LaunchpadDrumSeq(4, 5);
)

(
SynthDef(\sinDr, {|out, freqC, freqM, mod=0.5, att=0.001, rel=0.2 amp=0.2|
	var sig, env;
	env = EnvGen.ar(Env.perc(att, rel), doneAction: 2);
	sig = SinOsc.ar(freqC, SinOsc.ar(freqM, 0, mod * pi)) * amp ! 2;
	Out.ar(out, sig*env);
}).add;
)

~l.addSlot(0, 0, { Synth(\sinDr, [\freqC, 50, \freqM, 120, \mod, 2]) });
~l.addSlot(0, 1, { Synth(\sinDr, [\freqC, 30, \freqM, 12, \mod, 1.6]) });
~l.addSlot(1, 0, { Synth(\sinDr, [\freqC, 300, \freqM, 120, \mod, 1.6]) });
~l.addSlot(2, 0, { Synth(\sinDr, [\freqC, 3000, \freqM, 3210, \mod, 3.6, \rel, 0.01]) });

(
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

