(
~l = LaunchpadPro();
~n = LaunchpadNoteSeq(1, ~l);
~l.registerMode(~n);

~n.numSteps = 5;

~n.synthFunc = {|out, freq=200, amp=0.1, gate=1|
	var sig, env;
	env = EnvGen.ar(Env.adsr(0.02, 0.5, 0.2, 0.1), gate, doneAction: 2);
	sig = SinOscFB.ar(freq, env * 0.6, amp * env);

	Out.ar(out, sig ! 2)
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

Tdef(\test).stop;

a = ~n.synthFunc.play(args: [\freq, 600])
a.release;

~n.sequence[0][4] = true;
~n.sequence[0][3] = 2;
~n.sequence[3][2] = 2;
~n.sequence[0];

TempoClock.tempo = 100/120;

(
~n.synthFunc = {|out, freq=200, amp=0.1, gate=1|
	var sig, envA, envF;
	envA = EnvGen.ar(Env.adsr(0.001, 0.3, 0.7, 0.1), gate, doneAction: 2);
	envF = EnvGen.ar(Env.perc(0.01, 0.2, 1, -6)).linlin(0, 1, 500, 5000);
	sig = SawDPW.ar(freq * [1, 0.5001, 3], BrownNoise.ar(0.05));
	sig = (sig + PulseDPW.ar(freq*0.2498, BrownNoise.ar(0.1, 0.5), 0.7) );
	sig = sig + WhiteNoise.ar(0.1 * envA);
	sig = sig.tanh * envA * amp;
	sig = DFM1.ar(sig, envF, 0.4, 2);
	sig = Splay.ar(sig, 0.2);
	Out.ar(out, sig)
};
)

s.scope