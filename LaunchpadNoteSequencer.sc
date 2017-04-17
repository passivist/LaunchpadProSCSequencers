LaunchpadNoteSequencer {
	var <>modeID, <>launchpad, <>isActive;

	var <>sequence;

	var <>position;
	
	var <internalState;
	
	*new { |modeID, launchpad, isActive=false|
		^super.new.init(modeID, launchpad, isActive);
	}

	init { |id, lPad, active|
		modeID = id;
		launchpad = lPad;
		isActive = active;

		// format: note, velocity, ccs...
		sequence = [0, 0] ! 64;

		// does it make sense to use a point here... Don't know yet
		position = [0, 0];
	}

	inputCallback { |button, val, type|
		[button, val, type].postln;

		/* INNER GRID */
		if(type == 'inner'){
			
		};

		/* OUTER EDGE */
		if(type == 'outer'){
			/* ARROW KEYS */
			if((button < 4) && (val > 0)){
				this.scroll(button);
			};

			/* MODIFIER KEYS */
			if(((button > 3) && (button < 12)) || ((button > 19) && (button < 28) )){
	
			};

			/* VALUE KEYS  */
			if(((button > 11) && (button < 20)) && (val > 0)){
				
			};
		};
	}

	scroll { |direction|
		switch(direction,
			// up
			0, { position = position + [0, 1] },
			// down
			1, { position = position - [0, 1] },
			// left
			2, { position = position - [1, 0] },
			// right
			3, { position = position + [1, 0] }
		);

		this.updateInternalState;
	}

	updateInternalState {
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

		var buttonLookup = [
			56, 57, 58, 59, 60, 61, 62, 63,
			48, 49, 50, 51, 52, 53, 54, 55,
			40, 41, 42, 43, 44, 45, 46, 47,
			32, 33, 34, 35, 36, 37, 38, 39,
			24, 25, 26, 27, 28, 29, 30, 31,
			16, 17, 18, 19, 20, 21, 22, 23,
			8,   9, 10, 11, 12, 13, 14, 15,
			0,   1,  2,  3,  4,  5,  6,  7
		];

		innerGrid = 8.collect{|i| sequence[i + position[0]] };
		
		innerGrid = innerGrid.collect{|item, i|
			// test if velocity is > 0
			if(item[1] > 0){
				// the note	is below the visible area
				if(item[0] < position[1]){
					led = i + 56;
					colour = 36;

					"below".postln;
				};
				
				// the note is above the visible area
				if(item[0] > (position[1] + 8)){
					led = i;
					colour = 36;

					"above".postln;
				};

				// the note is in the visible area
				if((item[0] >= position[1]) && (item[0] < (position[1] + 8))){
					led = buttonLookup[ ((item[0] - position[1]) * 8) + i];
					colour = 4;

					"in range".postln;
				};
			}{
				led = buttonLookup[ ((item[0] - position[1]) * 8) + i];
				colour = 0;
			};

			[led, colour].postln;
			
		};

		// light up modifiers
		outerGrid = [[4, 16], [5, 16], [7, 16], [22, 16]];
		
		state = [innerGrid, outerGrid].postln;

		launchpad.updateLeds(state);
	}
}