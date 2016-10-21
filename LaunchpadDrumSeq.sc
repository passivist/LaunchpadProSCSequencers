LaunchpadDrumSeq {
	var launchpad;
	var <>currentStep, <>prevStep, <selectedStep, <>numSteps;

	var stepLookUp;

	*new { |inPort, outPort|
		^super.new.init(inPort, outPort)
	}

	init {|inPort, outPort|
		MIDIIn.connect(inPort);
		launchpad = MIDIOut(outPort);

		stepLookUp = [
			81, 82, 83, 84, 85, 86, 87, 88,
			71, 72, 73, 74, 75, 76, 77, 78,
			61, 62, 63, 64, 65, 66, 67, 68,
			51, 52, 53, 54, 55, 56, 57, 58,
			41, 42, 43, 44, 45, 46, 47, 48,
			31, 32, 33, 34, 35, 36, 37, 38,
			21, 22, 23, 24, 25, 26, 27, 28,
			11, 12, 13, 14, 15, 16, 17, 18
		];

		currentStep = 0;
		prevStep = 0;
		selectedStep = 0;
		numSteps = 16;
	}

	draw {
		var prevLed, selectedLed, activeLed, activeColour, selectedColour;
		prevLed = stepLookUp[prevStep];
		activeLed = stepLookUp[currentStep];
		selectedLed = stepLookUp[selectedStep];


		launchpad.sysex(Int8Array[ 240,0,32,41,2,16,10,
			prevLed, 0,
			activeLed, 16,
			selectedLed, 18,
			247 ]);
	}

	selectedStep_ { |step|
		var prevLed, led;
		prevLed = stepLookUp[selectedStep];
		led = stepLookUp[step];
		selectedStep = step;

		launchpad.sysex(Int8Array[ 240,0,32,41,2,16,10,
			prevLed, 0,
			led, 18,
			247]);
	}

	nextStep {
		prevStep = currentStep;
		currentStep = (currentStep + 1) % numSteps;
	}

	reset {

	}
} 