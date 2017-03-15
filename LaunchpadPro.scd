// This is the setup you have to do before using the class:

(
MIDIClient.init;
~l = LaunchpadPro();
~mode = LaunchpadDrumSeq(1, ~l);
~l.registerMode(~mode);
)


~mode = ~mode.drumInit(1, ~l)
~mode.isKindOf(LaunchpadProMode)

~m = LaunchpadProMode(1, ~l);
~m.modeID;


(
~l.modes.do{|item, i|
	item.isActive.postln;
};
)

a = Set[]
a.add('dict')

// ******************************EXAMPLES**************************************
~l.outPort
~l.resetLeds;

~l.drawLed((0 .. 95), Array.fill(96, {128.rand}));
~l.drawLed((0 .. 95), 0);

~l.drawColumn(3, Array.fill(10, {3.rand}))
~l.drawRow(3, Array.fill(10, {3.rand}))

~l.drawAll(3)

~l.flashLed(15, 3);
~l.flashLed((0 .. 95), Array.fill(96, {128.rand}));

~l.pulseLed(16, 3);

~l.drawLedRGB(16, 60, 7, 6)

~l.drawGridRGB(1, 60 ! 100, 7 ! 29, 6 ! 30)
~l.drawGridRGB(1, Array.fill(64, {63.rand}), Array.fill(64, {63.rand}), Array.fill(64, {63.rand}))


(
fork{
	(25 * 3).do{
		~l.drawLed((0 .. 95), Array.fill(6, {128.rand}));
		(1/12).wait;
	}
};
)


(
fork{
	101.do{ |item, i|
		~l.drawLed((0 .. 100), i%12);
		(1/24).wait;
	}
};
)

(
fork{
	101.do{ |item, i|
		~l.drawLed(i, i);
		(1/24).wait;
	}
};
)

(
fork{
	128.do{ |item, i|
		~l.drawLed(16, i);
		(1/6).wait;
	}
};
)

(
fork{
	128.do{ |item, i|
		~l.drawGridRGB(1, Array.fill(64, {63.rand}), Array.fill(64, {63.rand}), Array.fill(64, {63.rand}));
		(1/24).wait;
	}
};
)

~l.resetLeds