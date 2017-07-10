LaunchpadTriggerPlayer {
	var <>modeID, <>launchpad, <>isActive;
	var <internalState;

	var buttonLookup;
	
	var <>synthFuncs;
	var <>playingSynths;

	*new { |modeID, launchpad, isActive=false|
		^super.new.init(modeID, launchpad, isActive);
	}

	init { |id, lPad, active|
		modeID = id;
		launchpad = lPad;
		isActive = active;

		synthFuncs = Array.newClear(64);
		playingSynths = Array.newClear(16);

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
		if(type == 'inner'){
			if(val > 0){
				var index;
				index = buttonLookup[button];
				this.triggerFunction(index);
			};
		};

		if(type == 'outer'){
			
		};
	}

	registerSlot {|index, chokeGroup, function|
		synthFuncs[index] = (func: function, group: chokeGroup);
		this.draw;
	}
	
	triggerFunction {|i|

		// check if the slot we're trying to play exists
		if(synthFuncs[i].notNil){
			var group;
			var synth;
			
			group = synthFuncs[i].at('group');
			group.postln;

			playingSynths[group].release;

			synth = synthFuncs[i].at('func').play;
			
			NodeWatcher.register(synth);
			
			playingSynths[group] = (synth);
		};
	}


	
	draw {|reset = true|
		// fill an array with the appropriate horizontal slice of the sequence
		var innerGrid, outerGrid;
		var state;
		
		if(this.isActive.not){ ^nil };
		// if the reset flag is set reset the inner Grid before continuing
		// this allows us to make scrolling work without complicated maths
		if(reset){
			launchpad.resetLeds;
		};

		innerGrid = [[0, 0]];
		
		// light up the modifiers
		outerGrid = [[11, 6]];
		
		state = [innerGrid, outerGrid];
		
		internalState = state;
		
		if(this.isActive){ launchpad.updateLeds(state) };
	}
	
}