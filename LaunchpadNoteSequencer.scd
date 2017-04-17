(
~l = LaunchpadPro();
~n = LaunchpadNoteSequencer(1, ~l);
~l.registerMode(~n);
)

~n.position;
~n.sequence.postcs;
~n.sequence = ~n.sequence.collect{ |item, i| [i, 128.rand].postln};
~n.updateInternalState;
a[0]

~l.resetLeds
~l.updateLeds([a]);
