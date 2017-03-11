MIDIClient.init;
~l = LaunchpadPro();

// *****************************************************************
~l.outPort
~l.resetLeds;

~l.drawLeds((0 .. 95), Array.fill(96, {128.rand}));
~l.drawLeds((0 .. 95), 0);

~l.drawColumn(3, Array.fill(10, {3.rand}))


(
fork{
	(25 * 3).do{
		~l.drawLeds((0 .. 100), Array.fill(6, {64.rand}));
		(1/12).wait;
	}
};
)


(
fork{
	101.do{ |item, i|
		~l.drawLeds((0 .. 100), i%12);
		(1/24).wait;
	}
};
)

(
fork{
	101.do{ |item, i|
		~l.drawLeds(i, i);
		(1/24).wait;
	}
};
)

(
fork{
	255.do{ |item, i|
		~l.drawLeds(16, i);
		(1/6).wait;
	}
};
)

~l.resetLeds
~l.drawLeds(92, 3);