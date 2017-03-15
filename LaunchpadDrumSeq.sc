LaunchpadDrumSeqSlot {
	var <>currentStep, <>prevStep, i;
	var <>numSteps, <>offset;
	var <>sequence, <>synthFuncs, synth;
	var <>selectedVar;
	var <>mute;

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

		mute = false;

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

	play { |i|
		if(synth.isPlaying){ synth.free };

		if(mute.not){
			synth = synthFuncs[i].value;
			NodeWatcher.register(synth);
		};
	}
}

LaunchpadDrumSeq {
	var <modeID, <launchpad, <>isActive;

	var curStep;
	var <slots, <selectedSlot;
	var slotLookup;
	var modifier;
	var internalState;

	*new { |modeID, launchpad, isActive = false|
		^super.new.init(modeID, launchpad, isActive)
	}

	init {|id, lPad, active|
		modeID = id;
		launchpad = lPad;
		isActive = active;

		slotLookup = [
			32, 33, 34, 35,
			40, 41, 42, 43,
			48, 49, 50, 51,
			56, 57, 58, 59
		];

		modifier = Set[];
	}

	/** INPUT HANDLING */
	inputCallback { |button, val, type|
		// [button, val, type].postln;

		if(val > 0){
			if(type == 'inner'){
				if(button < 32){
					this.noteSelection(button)
				}
			};
		};

		if(type == 'outer'){
			switch(button,
				4, { if(val > 0){ modifier.add('length'); modifier.postln; }{ modifier.remove('length') } },
				5, { if(val > 0){ modifier.add('delete') }{ modifier.remove('delete') } },
				6, { if(val > 0){ modifier.add('mute') }{ modifier.remove('mute') } },
			);
			// modifier.postln;
		};


		launchpad.updateLeds(internalState);
	}

	noteSelection { |step|

	}

	updateInternalState {

	}
}

/*
LaunchpadDrumSeq {
	var launchpad;
	var slotSelectionResponder, varSelectionResponder, noteSelectionOnResponder, noteSelectionOffResponder;
	var modifierOnResponder, modifierOffResponder;
	var pressedKeys, modifierState;
	var <slots, <selectedSlot;

	var stepLookUp, slotLookUp, varLookUp, modLookUp;

	var drawArrHeader;
	var i;

	*new { |launchpad|
		^super.new.init(launchpad)
	}

	init {|lPad|

		// connect to launchpad
		launchpad = lPad;

		i = 0;

		slots = Array.fill(16, { LaunchpadDrumSeqSlot.new() });
		selectedSlot = 0;

		pressedKeys = OrderedIdentitySet.new(2);
		modifierState = 0;

		/** LOOKUP TABLES */
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
			80, 70, 50
		];

		/** MIDI INPUT RESPONDER FUNCTIONS */
		// TODO: move each to their own method. This way the launchpad doesn't have to
		// be connected and the code becomes much more readable
		slotSelectionResponder = MIDIFunc({|vel, note|
			var slotIndex;
			slotLookUp.do{|item, i|
				if(note == item){
					slotIndex = i;
				}
			};

			switch(modifierState,
				0, { this.selectedSlot_(slotIndex) },
				2, { this.muteSlot(slotIndex) },
				4, { this.clearSlot(slotIndex) },
			);
		}, slotLookUp, nil, \noteOn, launchpad.inUID);

		varSelectionResponder = MIDIFunc({|vel, note|
			varLookUp.do{|item, i| if(note == item){ this.selectVariation(i); } };
		}, varLookUp, nil, \noteOn, launchpad.inUID);

		/*
		modifier states:
		0: no modifier pressed
		1: shift modifier pressed -> select range
		2: click modifier pressed -> mute slot
		3: ?
		4: delete modifier pressed -> delete slot sequence
		*/
		// TODO: Make modifiers Symbols or strings for better readability
		modifierOnResponder = MIDIFunc({|val, cc|
			switch(cc,
				80, { if(val > 0){ modifierState = 1 }{ modifierState = 0 } },
				70, { if(val > 0){ modifierState = 2 }{ modifierState = 0 } },
				50, { if(val > 0){ modifierState = 4 }{ modifierState = 0 } },
			)
		}, modLookUp, nil, \control, launchpad.inUID);

		noteSelectionOnResponder = MIDIFunc({|vel, note|
			var sl;
			sl = slots[selectedSlot];

			switch(modifierState,
				0, { this.createNote(note) },
				1, { this.changeNumSteps(note) },
			);
		}, stepLookUp, nil, \noteOn, launchpad.inUID);

		noteSelectionOffResponder = MIDIFunc({|vel, note|
			stepLookUp.do{|item, i|
				if(note == item){
					pressedKeys.remove(i);
				};
			}
		}, stepLookUp, nil, \noteOff, launchpad.inUID);
	}

	selectSlot {

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
		var prevLed, wasMuted, led, activeLed, sl;
		var drawArr = Array(200);

		prevLed = slotLookUp[selectedSlot];
		wasMuted = slots[selectedSlot].mute;
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
		if(wasMuted){ drawArr.add(118) }{ drawArr.add(9) };

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

	addSlot {|index, variation, func|
		slots[index].synthFuncs[variation] = func;
	}

	clearSlot {|slotIndex|
		var prevLed, led, activeLed, sl;
		var drawArr = Array(200);

		led = slotLookUp[selectedSlot];

		sl = slots[selectedSlot];

		activeLed = stepLookUp[sl.currentStep];

		slots[slotIndex].sequence = 0 ! 32;

		sl.sequence.do{|item, i|
			drawArr.add(stepLookUp[i]);
			drawArr.add(0);
		};

		this.draw(drawArr);
	}

	muteSlot{ |slotIndex|
		var sl;
		var drawArr = Array(200);

		sl = slots[slotIndex];

		drawArr.add(slotLookUp[slotIndex]);

		if(sl.mute){
			sl.mute = false;
			if(slotIndex == selectedSlot){
				drawArr.add(18);
			}{
				drawArr.add(9);
			}
		}{
			sl.mute = true;
			drawArr.add(118);
		};


		this.draw(drawArr);
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
				item.play(item.sequence[step] - 1)
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
		var full, body, end;
		body = Int8Array.newFrom(arr);
		end = Int8Array[247];
		full = drawArrHeader ++ body ++ end;
		launchpad.sysex(full);
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
*/