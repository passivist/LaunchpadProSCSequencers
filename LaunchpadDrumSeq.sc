LaunchpadDrumSeqSlot {
	var <>currentStep, <>prevStep;
	var <>numSteps, <>offset;
	var <>sequence, <>synthFunc, synth;
	var <>selectedVar;
	var <>mute;

	*new { |func|
		^super.new.init(func)
	}

	init { |func|
		currentStep = 0;
		prevStep = 0;
		numSteps = 32;
		offset = 0;
		sequence = 0 ! 32;
		selectedVar = 0;

		synthFunc = func;
		
		mute = false;
	}

	nextStep { |i|	
		i = i % numSteps;

		this.play(i);
		
		prevStep = currentStep;

		if(numSteps.isPositive){
			currentStep = i + offset;
		}{
			currentStep = (i + numSteps).abs + offset;
		};
	}

	play { |i|
		if(mute.not){
			if(sequence[i] > 0){
				if(synth.isPlaying){ synth.release };
				synth = synthFunc.play;
				NodeWatcher.register(synth);	
			};
		};
	}

	reset {
		this.init(synthFunc)
	}
}

LaunchpadDrumSeq {
	var <modeID, <launchpad, <>isActive;

	var <slots, <selectedSlot;
	var slotLookup;
	var modifier;

	var count;

	/*
		this is an array holding the state of all the values relevant for drawing
		so active steps, modifiers, slots etc. It is constructed at a sensible point
		and then passed to the LaunchpadPro object
	*/
	var <internalState;

	*new { |modeID, launchpad, isActive = false|
		^super.new.init(modeID, launchpad, isActive)
	}

	init {|id, lPad, active|
		modeID = id;
		launchpad = lPad;
		isActive = active;

		slotLookup =  [
			56, 57, 58, 59,
			48, 49, 50, 51,
			40, 41, 42, 43,
			32, 33, 34, 35
		];

		modifier = Set[];

		count = 0;

		// initialize slots
		slots = nil ! 16;
		
		// initialize a dummy slot
		slots[0] = LaunchpadDrumSeqSlot({});
		selectedSlot = slots[0];

		this.updateInternalState;
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

			slotLookup.do{|item, i|
				if(item == button){
					this.setActiveSlot(i)
				}
			};
		};

		if(type == 'outer'){
			switch(button,
				4, { if(val > 0){ modifier.add('length') }{ modifier.remove('length') } },
				7, { if(val > 0){ modifier.add('delete') }{ modifier.remove('delete') } },
				22, { if(val > 0){ modifier.add('mute'  ) }{ modifier.remove('mute'  ) } },
			);
			// modifier.postln;
		};
	}

	// handle slots
	registerSlot {|id, func|
		slots[id] = LaunchpadDrumSeqSlot(func);

		this.updateInternalState;
	}

	setActiveSlot{ |slot|
		// slot.postln;
		/*
			select the action to take with modifiers
		*/
		if(modifier.isEmpty){
			if(slots[slot].notNil){
				selectedSlot = slots[slot];
			};	
		}{
			if(modifier.includes('delete')){
				slots[slot].sequence = 0 ! 32;
			};
			
			if(modifier.includes('mute')) {
				if(slots[slot].mute){
					slots[slot].mute = false;
				}{
					slots[slot].mute = true;
				}
			};	
		};

		this.updateInternalState;
	}
	
	// sequencer logic
	noteSelection { |i|
		var step = selectedSlot.sequence[i];

		if(modifier.includes('length')){
			selectedSlot.numSteps = i + 1;
		}{
			if(step == 0){ selectedSlot.sequence[i] = 1 }{ selectedSlot.sequence[i] = 0 };	
		};
		
		this.updateInternalState;
	}

	next {
		slots.do{|item, i|	
			if(item.notNil){
				item.nextStep(count);
			};
		};
		
		count = (count + 1);
		
		this.updateInternalState;
	}

	// drawing / interfacing
	// TODO: wie kann ich hier nur das was sich verändert hat übertragen
	updateInternalState {
		var innerGrid = [];
		var outerGrid = [];

		var state, reducedState;
		var isEmpty;
		
		var led, colour;

		// function to only keep the changed things to draw
		var keepChanged = {|new, old|
			var newArr;
			// "\n \nArray Diagnostics: ".postln;
			new.do{ |item, i|
				// postf("New: % Old: %, HasChanged: % \n", item, old[i], item != old[i]);
				if(item.includes(nil).not){
					if(item != old[i] ){
						newArr = newArr ++ [item];
						// postf("NewArr: % \n", newArr);
					};
				}
			};

			// postf("isArrNil: % finalArr: % \n", newArr.isNil, newArr);

			if(newArr.notNil){ newArr }{ [] }
		};
		
		// construct the values for the step display. The values for colour are
		// taken from the LaunchpadPro Programmers Manual
		innerGrid = selectedSlot.sequence.collect {|item, i|
			
			led = i;

			if(item == 1){ colour = 32 }{ colour = 0 };
			if(selectedSlot.currentStep == i){ colour = 9};
			
			[led, colour]
		};

		//construct the values for the slots display
		innerGrid = innerGrid ++ slotLookup.collect{ |item, i|

			led = item;

			if(slots[i].notNil){
				if(slots[i] == selectedSlot){
					if(slots[i].mute){ colour = 3 }{ colour = 5 }
				}{
					if(slots[i].mute){ colour = 2 }{ colour = 4 }
				};
			}{
				colour = 0;
			};
			
			[led, colour]
		};

		// always light the modifiers
		outerGrid = [[4, 16], [7, 16], [22, 16]];

		// construct the complete array
		state = [innerGrid, outerGrid];

		if(internalState.notNil){
			
			reducedState = [];

			reducedState = state.collect{|item, i| keepChanged.(item, internalState[i])};
		
			// postf("\nreduced drawing array: %\n", reducedState );
		}{
			reducedState = state;
		};
		// check if the mode is active before calling the draw methods
		// NOTE: it is important to execute the rest of the function so
		// that when we recall the state of this mode when switching to
		// it the current state is recalled and not some old state
		
		if(this.isActive){ launchpad.updateLeds(reducedState) };

		internalState = state;
	}
}
