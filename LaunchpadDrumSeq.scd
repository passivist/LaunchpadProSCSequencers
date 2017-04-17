(
//MIDIClient.init;
~l = LaunchpadPro();
~dr = LaunchpadDrumSeq(0, ~l);
~l.registerMode(~dr);
)

(
~dr.registerSlot(0, { |out|
	var sig, click, aEnv, pEnv;
	var dur = 0.6;

	aEnv = EnvGen.ar(Env.perc(0.001, dur), doneAction: 2);
	pEnv = EnvGen.ar(Env([100, 50], dur, -12));
	click = Blip.ar(0, 50) * aEnv * 0.7;
	sig = SinOsc.ar(pEnv, 0, aEnv) ! 2;
	sig = (sig + click).tanh;

	Out.ar(out, (sig + click) * 0.3);
});

~dr.registerSlot(1, { |out|
	var sig, noise, aEnv, pEnv, nEnv;
	var dur = 0.3;

	aEnv = EnvGen.ar(Env.perc(0.001, dur), doneAction: 2);
	pEnv = EnvGen.ar(Env([250, 200], dur, -12));
	nEnv = EnvGen.ar(Env.perc(0.001, 0.2));
	noise = WhiteNoise.ar(0.3) * nEnv;
	sig = SinOsc.ar(pEnv) * aEnv ! 2;
	sig = (sig + noise).tanh;

	Out.ar(out, sig * 0.2)
});

~dr.registerSlot(2, { |out|
	var sig, noise, aEnv, pEnv, nEnv;
	var dur = 0.3;

	aEnv = EnvGen.ar(Env.perc(0.001, dur), doneAction: 2);
	pEnv = EnvGen.ar(Env([150, 100], dur, -12));
	nEnv = EnvGen.ar(Env.perc(0.001, 0.2, 1, -6));
	noise = WhiteNoise.ar(WhiteNoise.ar(0.2)) * nEnv;
	sig = SinOsc.ar(pEnv) * aEnv ! 2;
	sig = (sig + noise).tanh;

	Out.ar(out, sig * 0.2)
});

~dr.registerSlot(4, {|out|
	var mod, sig, env;

	env = EnvGen.ar(Env.perc(0.001, 0.1), doneAction: 2);
	mod = SinOsc.ar([1730, 3420]) * env;
	sig = SinOsc.ar([1120, 1130], mod * 2 * env) * env;
	Out.ar(out, sig * 0.1)
});

~dr.registerSlot(6, { |out|
	var env, sig;
	var dur = 0.1;
	env = EnvGen.ar(Env.perc(0.001, dur), doneAction: 2);
	sig = WhiteNoise.ar(0.5);
	sig = Ringz.ar(sig, 5000, 0.01) * env;
	sig = Pan2.ar(sig, 0.3);

	Out.ar(out, sig * 0.2)
});
)

TempoClock.tempo = 110/120

(
Tdef(\testSeq, {
	loop{
		~dr.next;
		0.125.wait;
	}
}).play;
)

Tdef(\testSeq).stop;

(
~dr.slots[2].synthFunc = {}

);

(
~keepChanged = {|new, old|
	var newArr;
	"\nArray Diagnostics: ".postln;
	new.do{ |item, i|
		postf("New: % Old: %, HasChanged: % \n", item, old[i], item != old[i]);
		if(item.includes(nil).not){
			if(item != old[i] ){
				newArr = newArr ++ [item];
				postf("NewArr: % \n", newArr);
			};
		}
	};

	postf("isArrNil: % finalArr: % \n\n", newArr.isNil, newArr);

	if(newArr.notNil){ newArr }{ [] }
};
)

a = [[[0, 1], [1, 1], [12, 0]], [[55, 1], [56, 0]]];
b = [[[0, 1], [1, 0], [12, 1]], [[55, 1], [56, 1]]];
b = [[[0, 1], [1, 0], [12, 1]], [[55, 1], [56, nil]]];

c = b.collect{ |item, i| ~keepChanged.(item, a[i]) };
c[0].flop


