(
~l = LaunchpadPro();
~n = LaunchpadNoteSequencer(1, ~l);
~l.registerMode(~n);

~n.numSteps = 5;

~n.synthFunc = {|out, freq=200, amp=0.1, gate=1|
	var sig, env;
	env = EnvGen.ar(Env.adsr(0.02, 0.5, 0.2, 0.1), gate, doneAction: 2);
	sig = SinOscFB.ar(freq, env * 0.6, amp * env);

	Out.ar(out, sig ! 2)
};
)

~n.sequence[0][3] = true;
~n.sequence[0][2] = 1;

~n.sequence[0]
(
Tdef(\test, {
	loop{
		~n.next;
		0.25.wait		
	}
}).play;
)

Tdef(\test).stop;
a = ~n.synthFunc.play(args: [\freq, 600])
a.release;
