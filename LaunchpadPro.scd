MIDIClient.init;
~l = LaunchpadPro();

// *****************************************************************
~l.outPort
~l.resetLeds;

~l.drawLed((0 .. 95), Array.fill(96, {128.rand}));
~l.drawLed((0 .. 95), 0);

~l.drawColumn(3, Array.fill(10, {3.rand}))
~l.drawRow(3, Array.fill(10, {3.rand}))

~l.drawAll(3)

~l.flashLed(15, 3);
~l.flashLed((0 .. 95), Array.fill(96, {128.rand}));

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

~l.resetLeds
~l.drawLed(92, 3);