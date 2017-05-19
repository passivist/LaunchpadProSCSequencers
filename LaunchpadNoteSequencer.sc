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
		// mode are:
		// -- 'note'
		// -- 'velocity'
		// -- 'octave'
		// -- 'filter'
		// -- 'repetition'
		mode = 'note';
		
		// format 'mode': [[valA_0, valB_0, ...], [...]]
		sequence = (
			'note':       ('arr': [0, false] ! 16, 'numSteps': 8, 'pos': [0, 0], 'step': 0),
			'velocity':   ('arr': 0 ! 16, 'numSteps': 8, 'pos': [0, 0], 'step': 0),
			'octave':     ('arr': -1 ! 16, 'numSteps': 8, 'pos': [0, 0], 'step': 0),
			'filter':     ('arr': -1 ! 16, 'numSteps': 8, 'pos': [0, 0], 'step': 0),
			'repetition': ('arr': [0, false] ! 16, 'numSteps': 8, 'pos': [0, 0], 'step': 0),
		);
		
		// a counter for the playback position:
		nextFlag = true;
		repetitionCounter = 0;
		holdFlag = false;

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
				'note',       { this.noteSelection(x, y)     },
				'velocity',   { this.velocitySelection(x, y) },
				'octave',     { this.octaveSelection(x, y)   },
				'filter',     { this.filterSelection(x, y)   },
				'repetition', { this.repSelection(x, y)      }
			);
		};

		/* OUTER EDGE */
		if(type == 'outer'){
			/* ARROW KEYS */
			if((button < 4) && (val > 0)){
				this.scroll(button);
			};

			/* MODIFIER KEYS */
			if((button > 3) && (button < 12)){
				// modifiers
				switch(button,
					4, { if(val > 0){ modifier.add('shift')  }{ modifier.remove('shift')  } },
					5, { if(val > 0){ modifier.add('length') }{ modifier.remove('length') } },
				);

				// for the modifiers we actually want to draw on note-off
				if(val == 0){ this.draw(true) };
			};
			
			if(((button > 19) && (button < 28) )){
				// editing modes
				if(val > 0){
					switch(button,
						20, { mode = 'note';       },
						21, { mode = 'velocity';   },
						22, { mode = 'octave';     },
						23, { mode = 'filter';     },
						24, { mode = 'repetition'; },
					)
				};
			};

			/* VALUE KEYS  */
			if(((button > 11) && (button < 20)) && (val > 0)){
				
			};
		};
		
		// usually we want to redraw only on note-on
		if(val > 0){ this.draw(true)};
	}

	/* SEQUENCER **/
	noteSelection { |x, y|
		// the rows are reversed in this mode so we need another lookup table
		// maybe we need to wrap all this behaviour in a function earlier in
		// the process so we don't have to deal with this so many times
		var noteLookup = (7 .. 0);

		// the position at which we want to input into the sequence
		var inputPos = sequence.note.pos[0] + x;

		// the note position with offset
		var note = sequence.note.pos[1] + noteLookup[y];

		var selectedSlot = sequence.note.arr[inputPos];

		if(modifier.includes('length').not){
			if(selectedSlot[1].not){
				// "case 0".postln;
				// there is no note in the selected slot
				// --> create a note
				sequence.note.arr[inputPos][0] = note;
				sequence.note.arr[inputPos][1] = true;
			}{
				// there is a note in the selected slot
				if(note == selectedSlot[0]){
					// "case 1".postln;
					// the selected note is equal to the note in the selected slot
					// --> delete the note
					sequence.note.arr[inputPos][0] = 0;
					sequence.note.arr[inputPos][1] = false;
				}{
					// "case 2".postln;
					// the selected note is not equal to the note in the selected slot
					// --> change the existing note to the selected note
					sequence.note.arr[inputPos][0] = note;
					sequence.note.arr[inputPos][1] = true;
				}
			};
		}{
			var length = (x+1) * (y+1);
			sequence.note.numSteps = length;
		};		
	}

	velocitySelection{ |x, y|
		if(modifier.includes('length').not){

		}{
			var length = (x+1) + (y*8);
			sequence.filter.numSteps = length;			
		}
	}

	octaveSelection{ |x, y|
		var oktLookup = (7 .. 0) - 4;
		var inputPos = sequence.octave.pos[0] + x;
		var oktave;

		if(modifier.includes('length').not){
			oktave = oktLookup[y];
			
			sequence.octave.arr[inputPos] = oktave;
		}{
			var length = (x+1) + (y*8);
			sequence.octave.numSteps = length;
		};
		
	}

	filterSelection{ |x, y|
		if(modifier.includes('length').not){

		}{
			var length = (x+1) + (y*8);
			sequence.filter.numSteps = length;			
		}
	}
	
	repSelection { |x, y|
		var repLookup = (6 .. 0);
		var inputPos = sequence.repetition.pos[0] + x;
		var repetition;

		// last row handles hold paremeter
		if(modifier.includes('length').not){
			if(y == 7){
				if(sequence.repetition.arr[inputPos][1]){
					sequence.repetition.arr[inputPos][1] = false;
				}{
					sequence.repetition.arr[inputPos][1] = true;
				}
			}{
				repetition = repLookup[y];
				sequence.repetition.arr[inputPos][0] = repetition;
			};	
		}{
			var length = (x+1) * (y+1);
			sequence.repetition.numSteps = length;
		};		
	}

	/* PLAYBACK **/
	next {
		var currentNote;
		var holdFlag = sequence.repetition.arr[sequence.repetition.step][1];
		currentNote = sequence.note.arr[sequence.note.step];
		// postf("currentNote: %; stepCounter: %; nextFlag %;\n", currentNote, stepCounter, nextFlag);

		// nextFlag is true before evaluating next() this means that we have
		// just moved from the previous step --> initialize everything
		if(nextFlag){ repetitionCounter =  sequence.repetition.arr[sequence.repetition.step][0] };

		// check if the gate of the current step is true; if not release any
		// playing synth.
		if(currentNote[1]){
			// if the hold flag of the step is false or nextFlag is true   
			if(holdFlag.not || nextFlag){
				if(synth.isPlaying){ synth.release };
				this.play(
					currentNote[0],
					sequence.velocity.arr[sequence.velocity.step],
					sequence.octave.arr[sequence.octave.step]
				);
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

			// increment all the steps of the various sequences
			sequence.do{|item, i|
				item.step = (item.step + 1) % item.numSteps;
			};
		}{
			repetitionCounter = repetitionCounter - 1;
		};
	}

	play {|note, velocity=1, octave=1|
		var freq = Scale.minor.degreeToFreq(note, 60.midicps, octave);
		this.synth = synthFunc.play(args: [\freq, freq, \amp, 0.1]);
		NodeWatcher.register(synth);
	}

	/* DRAWING **/
	scroll { |direction|
		var val;

		if( modifier.includes('shift') ){ val = 7; }{ val = 1; };

		sequence.keysValuesDo{|key, item|
			if(key == mode){
				switch(direction,
					// up
					0, { item.pos = item.pos + [0, val] },
					// down
					1, { item.pos = item.pos - [0, val] },
					// left
					2, { if((item.pos[0] - val) >= 0){ item.pos = item.pos - [val, 0] } },
					// right
					3, { if((item.pos[0] + val) < (item.arr.size - 7)){item.pos = item.pos + [val, 0] } }
				);
			}
		};
	}

	draw {|reset = true|
		// fill an array with the appropriate horizontal slice of the sequence
		var innerGrid, outerGrid;
		var state;

		var led, colour;
		
		var arrayCounter;
		// if the reset flag is set reset the inner Grid before continuing
		// this allows us to make scrolling work without complicated maths
		if(reset){
			launchpad.resetLeds;
		};

		switch(mode,
			// note mode: draw notes and gates
			'note', {
				if(modifier.includes('length').not){
					// this is probably always the same maybe we can define this beforehand???
					innerGrid = 8.collect{|i|
						var note = sequence.note.arr[i + sequence.note.pos[0] ];

						// check if the gate flag of the note is true:
						if(note[1]){
							// check if the position of the note is in range:
							// the note	is below the visible area:
							// --> draw a blue LED in the downmost row
							if(note[0] < sequence.note.pos[1]){
								led = i + 56;
								colour = 36;
							};

							// the note is above the visible area:
							// --> draw a blue LED in the upmost row
							if(note[0] > (sequence.note.pos[1] + 7)){
								led = i;
								colour = 36;
							};

							// the note is in the visible area:
							// --> draw the note at the appropriate position
							if((note[0] >= sequence.note.pos[1]) && (note[0] < (sequence.note.pos[1] + 8))){
								led = buttonLookup[ i + ((note[0] - sequence.note.pos[1]) * 8)];		
								colour = 4;
							};
						}{
							// there is a rest / no note:
							// --> draw a black LED at the appropriate spot
							led = buttonLookup[i];
							colour = 0;
						};
						// return value
						[led, colour]
					};
				}{
					innerGrid = 16.collect{|length|
						var led, colour;
						led = length;
						if((length + 1) == sequence.note.numSteps){
							colour = 8;
						}{
							colour = 60;
						};
						
						[led, colour]
					}
				}
			},
			'velocity', {
				if(modifier.includes('length').not){
					innerGrid = 8.collect{|i|
						led = 0;
						colour = 0;
						
						[led, colour]
					}
				}{
					innerGrid = 16.collect{|length|
						var led, colour;
						led = length;
						if((length + 1) == sequence.velocity.numSteps){
							colour = 8;
						}{
							colour = 60;
						};
						
						[led, colour]
					}					
				}
			},
			'octave', {
				if(modifier.includes('length').not){
					innerGrid = 8.collect{|i|
						var okt = sequence.octave.arr[i + sequence.octave.pos[0] ];
						
						led = buttonLookup[ i + ((okt + 4) * 8).floor];
						colour = 17;

						[led, colour]
					}
				}{
					innerGrid = 16.collect{|length|
						var led, colour;
						led = length;
						if((length + 1) == sequence.octave.numSteps){
							colour = 8;
						}{
							colour = 60;
						};
						
						[led, colour]
					}
				}
			},

			'filter', {
				if(modifier.includes('length').not){
					
					led = 0;
					colour = 0;
					
					innerGrid = [[led, colour]];
				}{
					innerGrid = 16.collect{|length|
						var led, colour;
						led = length;
						if((length + 1) == sequence.filter.numSteps){
							colour = 8;
						}{
							colour = 60;
						};
						
						[led, colour]
					}					
				};

			},
			
			'repetition', {
				if(modifier.includes('length').not){
					innerGrid = 8.collect{|i|
						var rep  = sequence.repetition.arr[i + sequence.repetition.pos[0] ][ 0 ];
						var hold = sequence.repetition.arr[i + sequence.repetition.pos[0] ][ 1 ];
						var arr;
						if(hold){
							arr = [
								[ buttonLookup[ i + ((rep * 8) + 8).floor], 15 ],
								[ buttonLookup[ i ], 16 ]
							];
						}{
							led = buttonLookup[ i + ((rep * 8) + 8).floor];
							colour = 15;
							arr = [led, colour];
						};
						arr;
					}
				}{
					innerGrid = 16.collect{|length|
						var led, colour;
						led = length;
						if((length + 1) == sequence.repetition.numSteps){
							colour = 8;
						}{
							colour = 60;
						};
						
						[led, colour]
					}					
				}
			},
		);

		// flatten any nested arrays:
		innerGrid = innerGrid.flatIf{|item| item[0].isArray};
		
		// light up the modifiers
		outerGrid = [[4, 16], [5, 16], [7, 16], [20, 16], [21, 16], [22, 16], [23, 16], [24, 16]];
		
		state = [innerGrid, outerGrid];
		
		internalState = state;
		
		launchpad.updateLeds(state);
	}
}