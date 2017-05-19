LaunchpadNoteSeq {
	var <>modeID, <>launchpad, <>isActive;
	var <>position;

	var modifier;
	var mode;
	var <internalState;

	var buttonLookup;

	var <>sequence;
	var <>stepCounter, <>numSteps, nextFlag, repetitionCounter, holdFlag;

	var <>synthFunc, <>synth;
	
	*new { |modeID, launchpad, isActive=false|
		^super.new.init(modeID, launchpad, isActive);
	}

	init { |id, lPad, active|
		modeID = id;
		launchpad = lPad;
		isActive = active;

		modifier = Set[];

		// start up the sequencer in note mode
		// mode IDs:
		// 0: 'note'
		// 1: 'velocity'
		// 2: 'octave'
		// 3: 'filter'
		// 4: 'repetition'
		mode = 'note';

		// does it make sense to use a point here... Don't know yet
		// best to keep it simple for now
		position = [0, 0];
		
		// format: [[note, velocity, octave, repetitions, hold], [...]] (in the future maybe ccs etc...)
		sequence = [0, 0, -1, 0, false] ! 16;

		// a counter for the playback position:
		stepCounter = 0;
		nextFlag = true;
		repetitionCounter = 0;
		holdFlag = false;
		
		this.numSteps = 8;

		synthFunc = {};
		
		//is there a more elegant and safe way than lookuptables?
		buttonLookup = [
			56, 57, 58, 59, 60, 61, 62, 63,
			48, 49, 50, 51, 52, 53, 54, 55,
			40, 41, 42, 43, 44, 45, 46, 47,
			32, 33, 34, 35, 36, 37, 38, 39,
			24, 25, 26, 27, 28, 29, 30, 31,
			16, 17, 18, 19, 20, 21, 22, 23,
			8 ,  9, 10, 11, 12, 13, 14, 15,
			0 ,  1,  2,  3,  4,  5,  6,  7,
		];
	}

	inputCallback { |button, val, type|
		// [button, val, type].postln;

		/* INNER GRID */
		if((type == 'inner') && (val > 0)){
			var x = (button % 8);
			var y = (button / 8).floor;

			// [x, y].postln;
			switch(mode,
				'note',       { this.noteSelection(x, y)   },
				'repetition', { this.repSelection(x, y)    },
				'octave',     { this.octaveSelection(x, y) },
				'length',     { this.lengthSelection(button) },
			);
		};

		/* OUTER EDGE */
		if(type == 'outer'){
			/* ARROW KEYS */
			if((button < 4) && (val > 0)){
				this.scroll(button);
			};

			/* MODIFIER KEYS */
			if(((button > 3) && (button < 12)) || ((button > 19) && (button < 28) )){
				// modifiers
				switch(button,
					4, { if(val > 0){ modifier.add('shift') }{ modifier.remove('shift') } },
				);

				// editing modes
				if(val > 0){
					switch(button,
						20, { mode = 'note'; this.updateInternalState(true);       },
						21, { mode = 'repetition'; this.updateInternalState(true); },
						22, { mode = 'octave'; this.updateInternalState(true);     },
						23, { mode = 'length'; this.updateInternalState(true);     },
					)
				};

				// mode.postln;
			};

			/* VALUE KEYS  */
			if(((button > 11) && (button < 20)) && (val > 0)){
				
			};
		};
	}

	/* SEQUENCER **/
	noteSelection { |x, y|
		// the rows are reversed in this mode so we need another lookup table
		// maybe we need to wrap all this behaviour in a function earlier in
		// the process so we don't have to deal with this so many times
		var noteLookup = (7 .. 0);

		// the position at which we want to input into the sequence
		var inputPos = position[0] + x;

		// the note position with offset
		var note = position[1] + noteLookup[y];

		var selectedSlot = sequence[inputPos];

		// [inputPos, note, selectedSlot].postln;
		
		if(selectedSlot[1] == 0){
			// "case 0".postln;
			// there is no note in the selected slot
			// --> create a note
			sequence[inputPos][0] = note;
			sequence[inputPos][1] = 1;
		}{
			// there is a note in the selected slot
			if(note == selectedSlot[0]){
				// "case 1".postln;
				// the selected note is equal to the note in the selected slot
				// --> delete the note
				sequence[inputPos][0] = 0;
				sequence[inputPos][1] = 0;
			}{
				// "case 2".postln;
				// the selected note is not equal to the note in the selected slot
				// --> change the existing note to the selected note
				sequence[inputPos][0] = note;
				sequence[inputPos][1] = 1;
			}
		};

		this.updateInternalState(true);
	}

	repSelection { |x, y|
		var repLookup = (6 .. 0);
		var inputPos = position[0] + x;
		var repetition;

		// last row handles hold paremeter
		if(y == 7){
			if(sequence[inputPos][4]){
				sequence[inputPos][4] = false;
			}{
				sequence[inputPos][4] = true;
			}
		}{
			repetition = repLookup[y];
			sequence[inputPos][3] = repetition;
		};
		
		this.updateInternalState(true);
	}

	octaveSelection{|x, y|
		var oktLookup = (7 .. 0) - 4;
		var inputPos = position[0] + x;
		var oktave;

		oktave = oktLookup[y];

		sequence[inputPos][2] = oktave;

		this.updateInternalState(true);
	}

	lengthSelection{|length|
		this.numSteps = length + 1;
		this.updateInternalState(true);
	}

	/* PLAYBACK **/
	next {
		var currentStep;
		
		currentStep = sequence[stepCounter];
		// postf("currentStep: %; stepCounter: %; nextFlag %;\n", currentStep, stepCounter, nextFlag);

		// nextFlag is true before evaluating next() this means that we have
		// just moved from the previous step --> initialize everything
		if(nextFlag){ repetitionCounter = currentStep[3] };

		// check if the velocity of the current step is > 0 if not release any
		// playing synth.
		if(currentStep[1] > 0){
			// if the hold flag of the step is false or nextFlag is true   
			if(currentStep[4].not || nextFlag){
				if(synth.isPlaying){ synth.release };
				this.play(currentStep[0], currentStep[1], currentStep[2]);
			}
		}{
			if(synth.isPlaying){ synth.release };
		};

		// if this is the first step set nextFlag to false
		if(nextFlag){ nextFlag = false };

		// if repetitionCounter reaches 0 we set nextFlag to true and
		// iterate to the next step. If not we decrement repetitionCounter
		if(repetitionCounter < 1){
			nextFlag = true;
			stepCounter = (stepCounter + 1) % this.numSteps;
		}{
			repetitionCounter = repetitionCounter - 1;
		};
	}

	play {|note, amp=1, octave=1|
		var freq = Scale.minor.degreeToFreq(note, 60.midicps, octave);
		this.synth = synthFunc.play(args: [\freq, freq, \amp, 0.1]);
		NodeWatcher.register(synth);
	}

	/* DRAWING **/
	scroll { |direction|
		var val;

		if( modifier.includes('shift') ){ val = 7; }{ val = 1; };

		switch(direction,
			// up
			0, { position = position + [0, val] },
			// down
			1, { position = position - [0, val] },
			// left
			2, { if((position[0] - val) >= 0){position = position - [val, 0] } },
			// right
			3, { if((position[0] + val) < (sequence.size - 7)){position = position + [val, 0] } }
		);

		this.updateInternalState(true);
	}

	updateInternalState {|reset = true|
		// fill an array with the appropriate horizontal slice of the sequence
		var innerGrid, outerGrid;
		var state;

		var led, colour;

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
				};
			};

			// postf("isArrNil: % finalArr: % \n", newArr.isNil, newArr);

			if(newArr.notNil){ newArr }{ [] }
		};

		// if the reset flag is set reset the inner Grid before continuing
		// this allows us to make scrolling work without complicated maths
		if(reset){
			launchpad.resetLeds;
		};

		innerGrid = 8.collect{|i| sequence[i + position[0]] };
		
		innerGrid = innerGrid.collect{|item, i|
			switch(mode,
				// note mode: draw the notes
				'note', {
					var note = item[0];
					var vel = item[1];
					// test if velocity is > 0
					if(vel > 0){
						// the note	is below the visible area:
						// --> draw a blue LED in the downmost row
						if(note < position[1]){
							led = i + 56;
							colour = 36;
						};

						// the note is above the visible area:
						// --> draw a blue LED in the upmost row
						if(note > (position[1] + 7)){
							led = i;
							colour = 36;
						};

						// the note is in the visible area:
						// --> draw the note at the appropriate position
						if((note >= position[1]) && (note < (position[1] + 8))){
							led = buttonLookup[ i + ((note - position[1]) * 8)];
							
							colour = 4;
						};
					}{
						// there is a rest / no note:
						// --> draw a black LED at the appropriate spot
						led = buttonLookup[i];
						colour = 0;
					};
				},
				// repetition mode: draw the states of the repetition and hold variables
				'repetition', {
					var rep = item[3];
					var hold = item[4];

					led = buttonLookup[ i + ((rep * 8) + 8).floor];
					if(hold){ colour = 16 }{ colour = 15 };
				},

				// octave mode: draw the octave states
				'octave', {
					var okt = item[2];

					led = buttonLookup[ i + ((okt + 4) * 8).floor];
					colour = 17;
				},
			);

			[led, colour]
		};

		if(mode == 'length'){
			// draw the two upper rows orange with the LED that has the same
			// number as the number of sequencer steps a deeper orange
			innerGrid = 16.collect{|length|
				var led, colour;
				led = length;
				if((length + 1) == numSteps){
					colour = 8;
				}{
					colour = 60;
				};
				
				[led, colour]
			}
		};

		// light up the modifiers
		outerGrid = [[4, 16], [5, 16], [7, 16], [20, 16], [21, 16], [22, 16], [23, 16]];

		state = [innerGrid, outerGrid];
		
		internalState = state;
		
		launchpad.updateLeds(state);
	}
}