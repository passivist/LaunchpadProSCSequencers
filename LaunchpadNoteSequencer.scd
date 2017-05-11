(
~l = LaunchpadPro();
~n = LaunchpadNoteSequencer(1, ~l);
~l.registerMode(~n);
)

~n.position;
~n.sequence.postcs;
~n.sequence = ~n.sequence.collect{ |item, i| [8.rand, 128.rand].postln};

~n.sequence[0].postln;
~n.numSteps = 7
~n.numSteps

(
SynthDef(\sin, {|out, freq, amp|
	var sig, env;
	env = EnvGen.ar(Env.perc(0.02, 0.5), doneAction: 2);
	sig = SinOscFB.ar(freq, env * 1.2, amp * env);

	Out.ar(out, sig ! 2)
}).add;
)

(
Tdef(\test, {
	loop{
		var freq, amp;
		var step = ~n.sequence[~n.stepCounter];

		freq = Scale.melodicMinor.degreeToFreq(step[0], 60.midicps, 1);
		
		amp = step[1] * 0.6;

		Synth(\sin, [\freq, freq, \amp, amp]);

		~n.next;

		0.125.wait		
	}
}).play;
)

Tdef(\test).stop;