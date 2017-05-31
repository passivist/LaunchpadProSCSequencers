LaunchpadNoteSeq {
	var <>modeID, <>launchpad, <>isActive;
	var <>position;

	var modifier, shift;
	var mode;
	var <internalState;

	var buttonLookup;

	var <>sequence;
	var <>globalCounter, <>clockDivision, <nextFlag, <repetitionCounter, <holdFlag;
	
	var scale;
	var <>synthFunc, <>synth, <>midiOut, <>midiChannel;
	var <>midiOutFlag;
	
	*new { |modeID, launchpad, isActive=false|
		^super.new.init(modeID, launchpad, isActive);
	}

	init { |id, lPad, active|
		modeID = id;
		launchpad = lPad;
		isActive = active;

		modifier = 'none';
		shift = false;

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
			'velocity':   ('arr': 5 ! 16, 'numSteps': 8, 'pos': [0, 0], 'step': 0),
			'octave':     ('arr': -1 ! 16, 'numSteps': 8, 'pos': [0, 0], 'step': 0),
			'filter':     ('arr': 5 ! 16, 'numSteps': 8, 'pos': [0, 0], 'step': 0),
			'repetition': ('arr': [0, false] ! 16, 'numSteps': 8, 'pos': [0, 0], 'step': 0),
		);
		
		// a counter for the playback position:
		// we'll keep that one global in the scope of the sequencer
		// and all the counters of the different modes will derive
		// from this counter so we can reset everything easily and
		// phasing is more controlled
		globalCounter = 0;
		clockDivision = 1;
		
		nextFlag = true;
		repetitionCounter = 0;
		holdFlag = false;

		synthFunc = {};
		midiOutFlag = false;
		midiChannel = 1;
		scale = Scale.minor;
		
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
					4, { if(val > 0){ shift = true  }{ shift = false  } },
					5, { if(val > 0){ modifier = 'length' }{ modifier = 'none' } },
					6, { if(val > 0){ modifier = 'divide' }{ modifier = 'none' } },
					7, { if(val > 0){ modifier = 'scale'  }{ modifier = 'none' } },
					11, { if(val > 0){ this.clear } },
				);

				// for the modifiers we actually want to redraw on note-off
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
			// implement pattern read and write here
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

		switch(modifier,
			'none', {
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
			},
			'length', {
				var length = (x+1) + (y * 8);
				sequence.note.numSteps = length;
			},
			'divide', {
				var divide = (x+1);
				if(y == 0){
					clockDivision = divide;
				}
			},
		);	
	}

	velocitySelection{ |x, y|
		var velLookup = (7 .. 0);
		var inputPos = sequence.velocity.pos[0] + x;
		var velocity;

		switch(modifier,
			'none', {
				velocity = velLookup[y];
				sequence.velocity.arr[inputPos] = velocity;
			},
			'length', {
				var length = (x+1) + (y * 8);
				sequence.velocity.numSteps = length;
			},
			'divide', {
				var divide = (x+1);
				if(y == 0){
					clockDivision = divide;
				}
			},
		);	
	}

	octaveSelection{ |x, y|
		var oktLookup = (7 .. 0) - 4;
		var inputPos = sequence.octave.pos[0] + x;
		var oktave;

		switch(modifier,
			'none', {
				oktave = oktLookup[y];
				sequence.octave.arr[inputPos] = oktave;
			},
			'length', {
				var length = (x+1) + (y * 8);
				sequence.octave.numSteps = length;
			},
			'divide', {
				var divide = (x+1);
				if(y == 0){
					clockDivision = divide;
				}
			},
		);
	}

	filterSelection{ |x, y|
		var filtLookup = (7 .. 0);
		var inputPos = sequence.filter.pos[0] + x;
		var filter;

		switch(modifier,
			'none', {
				filter = filtLookup[y];
				sequence.filter.arr[inputPos] = filter;
			},
			'length', {
				var length = (x+1) + (y * 8);
				sequence.filter.numSteps = length;
			},
			'divide', {
				var divide = (x+1);
				if(y == 0){
					clockDivision = divide;
				}
			},
		);
	}
	
	repSelection { |x, y|
		var repLookup = (6 .. 0);
		var inputPos = sequence.repetition.pos[0] + x;
		var repetition;
		
		switch(modifier,
			'none', {
				// last row handles hold paremeter
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
			},
			'length', {
				var length = (x+1) + (y * 8);
				sequence.repetition.numSteps = length;
			},
			'divide', {
				var divide = (x+1);
				if(y == 0){
					clockDivision = divide;
				}
			},
		);	
	}

	clear {
		sequence.note = ('arr': [0, false] ! 16, 'numSteps': 8, 'pos': [0, 0], 'step': 0); 
	}

	/* PLAYBACK **/
	next {
		var currentNote;
		var holdFlag = sequence.repetition.arr[sequence.repetition.step][1];
		currentNote = sequence.note.arr[sequence.note.step];
		// postf("currentNote: %; globalCounter: %; nextFlag %;\n", currentNote, globalCounter, nextFlag)

		// iterate the global counter before updating the step count of the sequences
		globalCounter = globalCounter + 1;

		// wait for the next step
		if((globalCounter/clockDivision).frac == 0){
			// nextFlag is true before evaluating next() this means that we have
			// just moved from the previous step --> initialize everything
			if(nextFlag){ repetitionCounter =  sequence.repetition.arr[sequence.repetition.step][0] };

			// check if the gate of the current step is true; if not release any
			// playing synth.
			if(currentNote[1]){
				// if the hold flag of the step is false or nextFlag is true   
				if(holdFlag.not || nextFlag){
					if(synth.isPlaying){ synth.release };
					if(midiOutFlag){this.releaseNote};
					this.play(
						currentNote[0],
						sequence.velocity.arr[sequence.velocity.step],
						sequence.octave.arr[sequence.octave.step],
						sequence.filter.arr[sequence.filter.step]
					);
				}
			}{
				if(synth.isPlaying){ synth.release };
				if(midiOutFlag){this.releaseNote};
			};

			// if this is the first step set nextFlag to false
			if(nextFlag){ nextFlag = false };
			
			// if repetitionCounter reaches 0 we set nextFlag to true and
			// iterate to the next step. If not we decrement repetitionCounter
			if(repetitionCounter < 1){
				nextFlag = true;

				// increment all the steps of the various sequences
				sequence.do{|item, i|
					item.step = (globalCounter/clockDivision).floor % (item.numSteps + repetitionCounter);
				};
			}{
				repetitionCounter = repetitionCounter - 1;
			};

		}
	}

	play {|note, velocity=1, octave=1, filter=1|
		var freq  = scale.degreeToFreq(note, 60.midicps, octave);
		var vel   = velocity / 8;
		var filt  = filter / 8;

		if(midiOutFlag){
			var midinote, octOffset;
			if(note >= 0){
				octOffset = ((note + 1) / 8).floor;
			}{
				octOffset = (note / 8).floor;				
			};

			midinote = 60 + scale.wrapAt(note) + ((octOffset+1)*12) + (octave*12);
			
			midiOut.noteOn(midiChannel, midinote, vel.linlin(0, 1, 0, 127).floor);
		}{
			this.synth = synthFunc.play(args: [\freq, freq, \amp, 0.1 * vel, \filter, filt ]);
			NodeWatcher.register(synth);			
		};
	}

	releaseNote {
		midiOut.allNotesOff(midiChannel);
	}

	/* DRAWING **/
	scroll { |direction|
		var val;

		if( shift ){ val = 7; }{ val = 1; };

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

	drawNotes {
		var led, colour;
		var arr;
		
		switch(modifier,
			'none', {
				arr = 8.collect{|i|
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
					
					[led, colour]
				};
			},
			'length', {
				arr = 16.collect{|length|

					led = length;
					if((length + 1) == sequence.note.numSteps){
						colour = 8;
					}{
						colour = 60;
					};
					
					[led, colour]
				}
			},
			'divide', {
				arr = 8.collect{|divide|
					
					led = divide;
					if((divide+1) == clockDivision){
						colour = 8;
					}{
						colour = 16;
					};
					
					[led, colour]
				};
				arr;
			},
		);

		^arr;
	}

	drawVelocity {
		var led, colour;
		var arr;
		switch(modifier,
			'none', {
				arr = 8.collect{|i|
					var vel = sequence.velocity.arr[i + sequence.velocity.pos[0] ];
					
					led = buttonLookup[ i + (vel * 8).floor];
					colour = 9;

					[led, colour]
				}
			},
			'length', {
				arr = 16.collect{|length|

					led = length;
					if((length + 1) == sequence.velocity.numSteps){
						colour = 8;
					}{
						colour = 60;
					};
					
					[led, colour]
				}
			},
			'divide', {
				arr = 8.collect{|divide|
					
					led = divide;
					if((divide+1) == clockDivision){
						colour = 8;
					}{
						colour = 16;
					};
					
					[led, colour]
				};
				arr;	
			},
		);

		^arr;
	}

	drawOctave {
		var led, colour;
		var arr;
		
		switch(modifier,
			'none', {
				arr = 8.collect{|i|
					var oct = sequence.octave.arr[i + sequence.octave.pos[0] ];
					
					led = buttonLookup[ i + ((oct + 4) * 8).floor];
					colour = 17;

					[led, colour]
				}	
			},
			'length', {
				arr = 16.collect{|length|
					
					led = length;
					if((length + 1) == sequence.octave.numSteps){
						colour = 8;
					}{
						colour = 60;
					};
					
					[led, colour]
				}		
			},
			'divide', {
				arr = 8.collect{|divide|
					
					led = divide;
					if((divide+1) == clockDivision){
						colour = 8;
					}{
						colour = 16;
					};
					
					[led, colour]
				};
				arr;
			}
		);
		
		^arr;
	}

	drawFilter {
		var led, colour;
		var arr;
		
		switch(modifier,
			'none', {
				arr = 8.collect{|i|
					var filt = sequence.filter.arr[i + sequence.filter.pos[0] ];
					
					led = buttonLookup[ i + (filt * 8).floor];
					colour = 41;

					[led, colour]
				}
			},
			'length', {
				arr = 16.collect{|length|
					var led, colour;
					led = length;
					if((length + 1) == sequence.filter.numSteps){
						colour = 8;
					}{
						colour = 60;
					};
					
					[led, colour]
				}	
			},
			'divide', {
				arr = 8.collect{|divide|
					
					led = divide;
					if((divide+1) == clockDivision){
						colour = 8;
					}{
						colour = 16;
					};
					
					[led, colour]
				};
				arr;
			},
		);
		
		^arr;
	}

	drawRepetition {
		var led, colour;
		var arr;
		
		switch(modifier,
			'none', {
				arr = 8.collect{|i|
					var rep  = sequence.repetition.arr[i + sequence.repetition.pos[0] ][ 0 ];
					var hold = sequence.repetition.arr[i + sequence.repetition.pos[0] ][ 1 ];
					var innerArr;
					if(hold){
						innerArr = [
							[ buttonLookup[ i + ((rep * 8) + 8).floor], 15 ],
							[ buttonLookup[ i ], 16 ]
						];
					}{
						led = buttonLookup[ i + ((rep * 8) + 8).floor];
						colour = 15;
						innerArr = [led, colour];
					};
					
					innerArr
				}	
			},
			'length', {
				arr = 16.collect{|length|
					var led, colour;
					led = length;
					if((length + 1) == sequence.repetition.numSteps){
						colour = 8;
					}{
						colour = 60;
					};
					
					[led, colour]
				}	
			},
			'divide', {
				arr = 8.collect{|divide|
					
					led = divide;
					if((divide+1) == clockDivision){
						colour = 8;
					}{
						colour = 16;
					};
					
					[led, colour]
				};
				arr;
			},
		);
		
		^arr;
	}

	draw {|reset = true|
		// fill an array with the appropriate horizontal slice of the sequence
		var innerGrid, outerGrid;
		var state;
		
		var arrayCounter;
		// if the reset flag is set reset the inner Grid before continuing
		// this allows us to make scrolling work without complicated maths
		if(reset){
			launchpad.resetLeds;
		};

		switch(mode,
			// note mode: draw notes and gates
			'note', {
				innerGrid = this.drawNotes;
			},
			'velocity', {
				innerGrid = this.drawVelocity;
			},
			'octave', {
				innerGrid = this.drawOctave;
			},

			'filter', {
				innerGrid = this.drawFilter;
			},
			
			'repetition', {
				innerGrid = this.drawRepetition;
			},
		);

		// flatten any nested arrays:
		innerGrid = innerGrid.flatIf{|item| item[0].isArray};
		
		// light up the modifiers
		outerGrid = [[4, 16], [5, 16], [6, 16], [7, 16], [20, 16], [21, 16], [22, 16], [23, 16], [24, 16]];
		
		state = [innerGrid, outerGrid];
		
		internalState = state;
		
		launchpad.updateLeds(state);
	}
}