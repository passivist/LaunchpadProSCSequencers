(
~l = LaunchpadPro();
~n = LaunchpadNoteSeq(1, ~l);
~l.registerMode(~n);

~n.synthFunc = {|out, freq=200, amp=0.1, filter=1, gate=1|
	var sig, env;
	env = EnvGen.ar(Env.adsr(0.02, 0.5, 0.7, 0.1), gate, doneAction: 2);
	sig = Saw.ar(freq*[1, 0.5] * BrownNoise.ar(0.01, 1)).sum + WhiteNoise.ar(0.2);
	sig = Pan2.ar(sig) * env;
	sig = MoogVCF.ar(sig, filter.linexp(0, 1, 50, 5000), 0.7, 4).tanh;
	Out.ar(out, sig*amp)
};
)

(
Tdef(\test, {
	loop{
		~n.next;
		0.125.wait;
	}
}).play;
)

s.boot

~n.globalCounter
~n.sequence.note.step
~n.nextFlag
~n.next


Tdef(\test).stop;

~n.sequence.note.numSteps = 5;
~n.synth.release

TempoClock.tempo = 100/120;

