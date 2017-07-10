(
s.waitForBoot{
	~l = LaunchpadPro();
	~p = LaunchpadTriggerPlayer(3, ~l);
	~l.registerMode(~p);
}
)

(
~p.registerSlot(0, 0, {|out, gate=1|
	var env, sig;
	env = EnvGen.ar(Env.adsr(0.01, 0.7, 0.2, 0.2), gate, doneAction: 2);
	sig = SinOsc.ar(1000, 0, env) ! 2;
	Out.ar(out, sig*0.1);
});

~p.registerSlot(1, 0, {|out, gate=1|
	var env, sig;
	env = EnvGen.ar(Env.adsr(0.01, 0.7, 0.2, 0.2), gate, doneAction: 2);
	sig = SinOsc.ar(1300, 0, env) ! 2;
	Out.ar(out, sig*0.1);
});

~p.registerSlot(2, 0, {|out, gate=1|
	var env, sig;
	env = EnvGen.ar(Env.adsr(0.01, 0.7, 0.2, 0.2), gate, doneAction: 2);
	sig = SinOscFB.ar(300, 1, env) ! 2;
	Out.ar(out, sig*0.1);
});

~p.registerSlot(3, 1, {|out, gate=1|
	var env, sig;
	env = EnvGen.ar(Env.adsr(0.01, 0.7, 0.2, 0.2), gate, doneAction: 2);
	sig = SinOscFB.ar(600, 1, env) ! 2;
	Out.ar(out, sig*0.1);
});


~p.registerSlot(8, 1, {|out, gate=1|
	var env, sig;
	env = EnvGen.ar(Env.adsr(0.01, 0.7, 0.2, 0.2), gate, doneAction: 2);
	sig = SinOscFB.ar(910, 1.2, env) ! 2;
	Out.ar(out, sig*0.1);
});
)


nil.isPlaying

a = [] ! 128

a[0].add(2)
a

~p.playingSynths