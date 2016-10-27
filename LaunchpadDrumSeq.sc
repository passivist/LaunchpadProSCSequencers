LaunchpadDrumSeqSlot {
	var <>currentStep, <>prevStep, <>numSteps;
	var <>sequence, <>synthFunc;

	*new {
		^super.new.init()
	}

	init {
		currentStep = 0;
		prevStep = 0;
		numSteps = 32;
		sequence = 0 ! 32;
	}

	nextStep {
		prevStep = currentStep;
		currentStep = (currentStep + 1) % numSteps;
	}
}

LaunchpadDrumSeq {
	var launchpad;
	var slotSelectionResponder, slotSelectionResponder, noteSelectionResponder;
	var pressedKeys;
	var <slots, <selectedSlot;
	var drawArrHeader;

	var stepLookUp, slotLookUp, varLookUp;

	*new { |inPort, outPort|
		^super.new.init(inPort, outPort)
	}

	init {|inPort, outPort|
		var drawArr = Array(200);
		// connect to launchpad
		MIDIIn.connect(0, inPort);
		launchpad = MIDIOut(outPort);

		drawArrHeader = Int8Array[240,0,32,41,2,16,10];

		stepLookUp = [
			81, 82, 83, 84, 85, 86, 87, 88,
			71, 72, 73, 74, 75, 76, 77, 78,
			61, 62, 63, 64, 65, 66, 67, 68,
			51, 52, 53, 54, 55, 56, 57, 58,
		];

		slotLookUp = [
			11, 12, 13, 14,
			21, 22, 23, 24,
			31, 32, 33, 34,
			41, 42, 43, 44
		];

		varLookUp = [
			15, 16, 17, 18,
			25, 26, 27, 28,
			35, 36, 37, 38,
			45, 46, 47, 48
		];

		slots = Array.fill(16, { LaunchpadDrumSeqSlot.new() });
		selectedSlot = 0;

		slotSelectionResponder = MIDIFunc({|vel, note|
			slotLookUp.do{|item, i| if(note == item){ this.selectedSlot_(i);} };
		}, [
			41, 42, 43, 44,
			31, 32, 33, 34,
			21, 22, 23, 24,
			11, 12, 13, 14
		], nil, \noteOn);


		noteSelectionResponder = MIDIFunc({|vel, note|
			var sl;
			sl = slots[selectedSlot];
			stepLookUp.do{|item, i|
				if(note == item){
					switch(sl.sequence[i],
						0, {
							sl.sequence.put(i, 1);
							launchpad.sysex(Int8Array[ 240,0,32,41,2,16,10,
								stepLookUp[i], 12,
								247]);
						},
						1, {
							sl.sequence.put(i, 0);
							launchpad.sysex(Int8Array[ 240,0,32,41,2,16,10,
								stepLookUp[i], 0,
								247]);
						},

					)
				}
			}
		}, [
			81, 82, 83, 84, 85, 86, 87, 88,
			71, 72, 73, 74, 75, 76, 77, 78,
			61, 62, 63, 64, 65, 66, 67, 68,
			51, 52, 53, 54, 55, 56, 57, 58
		], nil, \noteOn);

		slotLookUp.do{|item|
			drawArr.add(item);
			drawArr.add(9);
		};
		this.draw(drawArr);
	}

	draw {|arr|
		launchpad.sysex(drawArrHeader);
		launchpad.sysex(Int8Array.newFrom(arr));
		launchpad.sysex(Int8Array[247]);
	}

	nextStep {
		var prevLed, selectedLed, activeLed, sl;
		var drawArr = Array(200);


		slots.do{|item| item.nextStep};

		sl = slots[selectedSlot];

		prevLed = stepLookUp[sl.prevStep];
		activeLed = stepLookUp[sl.currentStep];

		drawArr.add(prevLed);

		switch(sl.sequence[sl.prevStep],
			0, {
				drawArr.add(0);
			},
			1, {
				drawArr.add(12)
			}
		);
		drawArr.add(activeLed);
		drawArr.add(16);

		this.draw(drawArr);
	}


	selectedSlot_ { |slotIndex|
		var prevLed, led, activeLed, sl;
		var drawArr = Array(200);

		prevLed = slotLookUp[selectedSlot];
		led = slotLookUp[slotIndex];
		selectedSlot = slotIndex;

		sl = slots[slotIndex];

		activeLed = stepLookUp[sl.currentStep];

		sl.sequence.do{|item, i|
			drawArr.add(stepLookUp[i]);
			if(item > 0){
				drawArr.add(12);
			}{
				drawArr.add(0);
			}
		};

		drawArr.add(prevLed);
		drawArr.add(9);
		drawArr.add(led);
		drawArr.add(18);

		drawArr.add(activeLed);
		drawArr.add(12);

		this.draw(drawArr);
	}


	reset {
		var drawArr;
		slots.do{|sl|
			sl.currentStep_(0);
			sl.prevStep_(0);
			sl.numSteps_(32);
			sl.sequence_(0 ! 32);
		};

		launchpad.sysex(Int8Array[ 240,0,32,41,2,16,14,0,247]);

		slotLookUp.do{|item|
			drawArr.add(item);
			drawArr.add(9);
		};

	}
}