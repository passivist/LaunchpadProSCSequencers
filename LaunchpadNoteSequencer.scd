(
s.waitForBoot{
	~l = LaunchpadPro();
	~n = LaunchpadNoteSeq(1, ~l);
	~l.registerMode(~n);

	s.sync;
	
	// setup for MIDIOut on Linux:
	/*
	~n.midiOut = MIDIOut(1);
	~n.midiOut.connect(1);
	~n.midiOut.latency = 0;
	~n.midiChannel = 2;
	~n.midiOutFlag = true;
	*/	
	/*
	~n.synthFunc = {|out, freq=200, amp=0.1, filter=1, gate=1, parA, parB, parC, parD|
		var sig, env;
		env = EnvGen.ar(Env.adsr(0.02, 0.5, 0.7, 0.1), gate, doneAction: 2);
		sig = Saw.ar(freq*[1, 0.5] * BrownNoise.ar(0.01, 1)).sum + WhiteNoise.ar(0.2);
		sig = Pan2.ar(sig) * env;
		sig = MoogVCF.ar(sig, filter.linexp(0, 1, 50, 5000), 0.7, 4).tanh;
		Out.ar(out, sig*amp)
		};
	*/
	~n.synthFunc = {|out, freq=200, amp=0.2, gate=1, filter=1, parA, parB, parC, parD|
		var att, dec, sus, noise, wobble, ffreq, drive, res, hipass, fifth, sub;
		var sig, env;
		att = parA.bilin(0.5, 0, 1, 0.2, 0.02, 0.1);
		dec = parA.lincurve(0, 1, 0.08, 0.4, 2);
		sus = parA.lincurve(0, 1, 0, 1, 3);
		ffreq = parB.lincurve(0, 1, 100, 9000, 4) * filter.linlin(0, 1, 0.25, 1);
		noise = parC;
		wobble = parC.linlin(0, 1, 0, 0.05);
		res = parC.linlin(0, 1, 0.5, 0.6);
		drive = parD.linlin(0, 1, 2.5, 6);
		hipass = parD.linlin(0, 1, 200, 25);
		fifth = parD.linlin(0, 1, 0.5, 1);
		sub = parD.linlin(0, 1, 0, 1.2);
		env = EnvGen.ar(Env.adsr(att, dec, sus, 0.1), gate, doneAction: 2);
		sig = Saw.ar(freq*[1, 3, 0.501] * BrownNoise.ar(wobble, 1), [1, fifth]).sum + WhiteNoise.ar(noise);
		sig = Pan2.ar(sig) * env;
		sig = MoogVCF.ar(sig, ffreq * env, res, drive).tanh * 1.2;
		sig = HPF.ar(sig, 200);
		Out.ar(out, sig*amp)
	};
	
	//Ndef(\synth, { SoundIn.ar(0) ! 2 }).play;

	SynthDef(\ping, {|out, freq=1000|
		var env, sig;
		env = EnvGen.ar(Env.perc(0.001, 0.03, 1, -4), doneAction: 2);
		sig = SinOsc.ar(freq) ! 2;

		Out.ar(out, sig * env * 0.3);
	}).add;

	Tdef(\ping, {
		loop{
			Synth(\ping, [\freq, 1500]);
			0.5.wait;	
			Synth(\ping, [\freq, 1000]);
			0.5.wait;	
			Synth(\ping, [\freq, 1000]);
			0.5.wait;
			Synth(\ping, [\freq, 1000]);
			0.5.wait;	
		}
	});

	Tdef(\test, {
		loop{
			~n.next;		
			0.125.wait;
		}
	});
}
)

(
Tdef(\test).play;
//Tdef(\ping).play;
)

~n.uncertainty = 0.01

(
Tdef(\test).stop;
Tdef(\ping).stop;
)
TempoClock.tempo = 100/120;

~n.midiOut.allNotesOff(2)

~n.globalCounter / ~n.clockDivision;
~n.next;
~n.sequence.note
~n.clockDivision = 3;

~n.next;




~n.parA = 1;
~n.parB = 0.4;
~n.parC = 0.1;
~n.parD = 0;

bilincurve

100.collect{|i| (i/100).bilin(0.5, 0, 1, 0.3, 0.05, 0.1)}.plot
100.collect{|i| (i/100).lincurve(0, 1, 0, 1, 3)}.plot
(0..1000).normalize(-10, 10).collect { |num| num.gaussCurve }.plot