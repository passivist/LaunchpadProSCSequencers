(
MIDIClient.init;
~l = LaunchpadDrumSeq(2, 2);
)

~l.reset;

(
Tdef(\seq, {
	i = 0;
	loop{
		var seq = ~l.slots[0].sequence;
		var numSteps = ~l.slots[0].numSteps;
		~l.nextStep;
		if(seq[i%numSteps] == 1){Synth(\test)};

		i = i+1;
		0.25.wait;
	}
}).play;
)

Tdef(\seq).stop;

(
SynthDef(\test, {|out|
	var envF, envA, sig;
	envF = EnvGen.kr(Env.perc(0.001, 0.1, 1, -12));
	envA = EnvGen.ar(Env.perc(0.001, 0.5, 1, -3), doneAction: 2);
	sig = SinOsc.ar(envF.linexp(0, 1, 50, 1000), 0, envA) ! 2;
	Out.ar(out, sig);
}).add;
)

Synth(\test)

TempoClock.default.tempo_(110/60)