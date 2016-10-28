LaunchpadDrumSeqSlot {
	var <>currentStep, <>prevStep, i;
	var <>numSteps, <>offset;
	var <>sequence, <>synthFuncs;
	var <>selectedVar;

	*new {
		^super.new.init()
	}

	init {
		i = 0;
		currentStep = 0;
		prevStep = 0;
		numSteps = 32;
		offset = 0;
		sequence = 0 ! 32;
		selectedVar = 0;

		synthFuncs = Array.newClear(32);
	}

	nextStep { |i|
		i = i % numSteps;
		prevStep = currentStep;
		if(numSteps.isPositive){
			currentStep = i + offset;
		}{
			currentStep = (i + numSteps).abs + offset;
		}

	}
}

LaunchpadDrumSeq {
	var launchpad;
	var slotSelectionResponder, varSelectionResponder, noteSelectionResponder, noteOffSelectionResponder;
	var modifierOnResponder, modifierOffResponder;
	var pressedKeys, modifiers;
	var <slots, <selectedSlot;

	var stepLookUp, slotLookUp, varLookUp, modLookUp;

	var drawArrHeader;
	var i;

	*new { |inPort, outPort|
		^super.new.init(inPort, outPort)
	}

	init {|inPort, outPort|
		var drawArr = Array(200);
		// connect to launchpad
		MIDIIn.connect(0, inPort);
		launchpad = MIDIOut(outPort);

		drawArrHeader = Int8Array[240,0,32,41,2,16,10];

		i = 0;

		slots = Array.fill(16, { LaunchpadDrumSeqSlot.new() });
		selectedSlot = 0;

		pressedKeys = OrderedIdentitySet.new(2);
		modifiers = Set(32);

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

		modLookUp = [
			80, 50
		];

		slotSelectionResponder = MIDIFunc({|vel, note|
			if(modifiers.findMatch(\delete).notNil){
				slotLookUp.do{|item, i| if(note == item){ this.clearSlot(i);} };
			}{
				slotLookUp.do{|item, i| if(note == item){ this.selectedSlot_(i);} };
			};
		}, slotLookUp, nil, \noteOn);

		varSelectionResponder = MIDIFunc({|vel, note|
			varLookUp.do{|item, i| if(note == item){ this.selectVariation(i); } };
		}, varLookUp, nil, \noteOn);

		modifierOnResponder = MIDIFunc({|val, cc|
			switch(cc,
				80, {if(val > 0){ modifiers.add(\switch)}{ modifiers.remove(\switch) }; },
				50, {if(val > 0){ modifiers.add(\delete)}{ modifiers.remove(\delete) }; },
			)
		}, modLookUp, nil, \control);

		noteSelectionResponder = MIDIFunc({|vel, note|
			var sl;
			sl = slots[selectedSlot];

			if(modifiers.findMatch(\switch).notNil){
				this.changeNumSteps(note);
			} {
				this.createNote(note);
			}
		}, stepLookUp, nil, \noteOn);

		noteOffSelectionResponder = MIDIFunc({|vel, note|
			stepLookUp.do{|item, i|
				if(note == item){
					pressedKeys.remove(i);
				};
			}
		}, stepLookUp, nil, \noteOff);


		slotLookUp.do{|item|
			drawArr.add(item);
			drawArr.add(9);
		};

		varLookUp.do{|item|
			drawArr.add(item);
			drawArr.add(item);
		};

		this.draw(drawArr);
	}

	changeNumSteps {|note|
		var sl = slots[selectedSlot];

		stepLookUp.do{|item, i|
			if(note == item){
				pressedKeys.add(i);

				if(pressedKeys.size > 1){
					sl.numSteps_((pressedKeys.asArray[1] - pressedKeys.asArray[0]));

					if(sl.numSteps.isPositive){
						sl.numSteps = sl.numSteps + 1;
						sl.offset_(pressedKeys.asArray[0]);
					} {
						sl.numSteps = sl.numSteps - 1;
						sl.offset_(pressedKeys.asArray[1] - 1);
					}
				}
			}
		}
	}

	createNote { |note|
		var sl = slots[selectedSlot];

		stepLookUp.do{|item, i|
			if(note == item){
				if(sl.sequence[i] > 0){
					sl.sequence.put(i, 0);

					launchpad.sysex(Int8Array[ 240,0,32,41,2,16,10,
						stepLookUp[i], 0,
						247]);
				}{
					sl.sequence.put(i, sl.selectedVar + 1);
					sl.sequence;

					launchpad.sysex(Int8Array[ 240,0,32,41,2,16,10,
						stepLookUp[i], varLookUp[sl.selectedVar],
						247]);
				}
			}
		}
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
				drawArr.add(varLookUp[item - 1]);
			}{
				drawArr.add(0);
			};
		};

		varLookUp.do{|item, i|
			drawArr.add(item);
			if(i == sl.selectedVar){
				drawArr.add(5);
			}{
				drawArr.add(item);
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

	selectVariation { |variation|
		var sl, prevVar;
		var drawArr = Array(200);

		sl = slots[selectedSlot];
		prevVar = sl.selectedVar;
		sl.selectedVar_(variation);

		drawArr.add(varLookUp[prevVar]);
		drawArr.add(varLookUp[prevVar]);
		drawArr.add(varLookUp[variation]);
		drawArr.add(5);

		this.draw(drawArr);
	}

	clearSlot {|slot|
		var prevLed, led, activeLed, sl;
		var drawArr = Array(200);

		led = slotLookUp[selectedSlot];

		sl = slots[selectedSlot];

		activeLed = stepLookUp[sl.currentStep];

		slots[slot].sequence = 0 ! 32;

		sl.sequence.do{|item, i|
			drawArr.add(stepLookUp[i]);
			drawArr.add(0);
		};

		this.draw(drawArr);
	}

	addSlot {|index, variation, func|
		slots[index].synthFuncs[variation] = func;
	}

	nextStep {
		var prevLed, selectedLed, activeLed, sl;
		var prevStep;
		var drawArr = Array(200);

		slots.do{|item|
			var step, variation;
			item.nextStep(i);
			step = item.currentStep;
			if(item.sequence[step] > 0){
				item.synthFuncs[item.sequence[step] - 1].play

			}
		};

		sl = slots[selectedSlot];

		prevLed = stepLookUp[sl.prevStep];
		activeLed = stepLookUp[sl.currentStep];

		drawArr.add(prevLed);

		prevStep = sl.sequence[sl.prevStep];

		if(prevStep > 0){

			drawArr.add(varLookUp[prevStep - 1]);
		}{
			drawArr.add(0);
		};

		drawArr.add(activeLed);
		drawArr.add(16);

		this.draw(drawArr);

		i = i + 1;
	}

	draw {|arr|
		launchpad.sysex(drawArrHeader);
		launchpad.sysex(Int8Array.newFrom(arr));
		launchpad.sysex(Int8Array[247]);
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