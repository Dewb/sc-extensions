RailDriverControl {

	classvar <buttonNames = #[
		\rockerup, \rockerdown, \up, \right, \down, \left,
		\toggleLup, \toggleLdown, \toggleRup, \toggleRdown,
		\cab1, \cab2, \cab3, \cab4,
		\hornup, \horndown
	];

	classvar <stickNames = #[
		\reverser, \throttle, \autobrake, \brake
	];

	classvar rdDevice = nil;

	var <buttonArrayFunc;
	var <controlFuncs;
	var <>verbose = false;

	*new {
		^super.new.init();
	}

	init {
		controlFuncs = Array.newClear(buttonNames.size + stickNames.size);
		this.registerHandlers();
		this.monitorConnection();
	}

	monitorConnection {
		Tdef(\tryOpenRD, {
			var keepLooking = true;
			while ( { keepLooking } ){
				if ( rdDevice.notNil ){
					if ( rdDevice.isOpen ){
						keepLooking = false;
					}
				};
				if ( keepLooking ){
					HID.findAvailable;
					if ( HID.findBy(0x05f3, 0x00d2).size > 0 ){
						rdDevice = HID.open(0x05f3, 0x00d2);
						if ( rdDevice.notNil ){
							rdDevice.closeAction = {
								"RailDriver device disconnected".postln;
								Tdef( \tryOpenRD ).reset.play;
							};
							"RailDriver device connected".postln;
							keepLooking = false;
						}{
							3.0.wait;
						};
					}{
						3.0.wait;
					}
				}
			}
		}).play;
	}

	registerHandlers {

		// Function buttons
		HIDFunc.usageID(
			{
				arg value, rawValue, usage;

				if (this.verbose, {
					var action = (value == 1.0).if("pressed", "released");
					("function button " ++ usage ++ " " ++ action).postln;
				});

				if (this.buttonArrayFunc.notNil, { this.buttonArrayFunc(usage, value); });
			},
			elUsageID: (1..28),
			elPageID: 9,
			deviceInfo: IdentityDictionary.newFrom([\vendorID, 0x05f3, \productID, 0x00d2])
		);

		// All other buttons
		HIDFunc.usageID(
			{
				arg value, rawValue, usage;
				var index = usage - 29;
				var buttonName = (buttonNames[index]);

				if (this.verbose, {
					var action = (value == 1.0).if("pressed", "released");
					(buttonName ++ " " ++ action).postln;
				});

				if (this.controlFuncs[index].notNil, { this.controlFuncs[index].(value); });
			},
			elUsageID: (29..44),
			elPageID: 9,
			deviceInfo: IdentityDictionary.newFrom([\vendorID, 0x05f3, \productID, 0x00d2])
		);

		// Analog sticks
		HIDFunc.usageID(
			{
				arg value, rawValue, usage;

				var reverser = (rawValue & 0xFF).linlin(60, 219, 1.0, 0.0);
				var throttle = (rawValue & (0xFF << 8) >> 8 & 0xFF).linlin(40, 222, 1.0, 0.0);
				var autobrake = (rawValue & (0xFF << 16) >> 16 & 0xFF).linlin(65, 213, 1.0, 0.0);
				var brake = (rawValue & (0xFF << 24) >> 24 & 0xFF).linlin(33, 205, 1.0, 0.0);
				var index = buttonNames.size;

				if (this.verbose, {
					("reverser: " ++ reverser).postln;
					("throttle: " ++ throttle).postln;
					("autobrake: " ++ autobrake).postln;
					("independent brake: " ++ brake).postln
				});

				if(this.controlFuncs[index + 0].notNil, { this.controlFuncs[index + 0].(reverser); });
				if(this.controlFuncs[index + 1].notNil, { this.controlFuncs[index + 1].(throttle); });
				if(this.controlFuncs[index + 2].notNil, { this.controlFuncs[index + 2].(autobrake); });
				if(this.controlFuncs[index + 3].notNil, { this.controlFuncs[index + 3].(brake) });
			},
			elUsageID: 1,
			elPageID: 65280,
			deviceInfo: IdentityDictionary.newFrom([\vendorID, 0x05f3, \productID, 0x00d2])
		);

		"RailDriver handlers registered.".postln;
	}

	buttonArray {
		arg func;
		this.buttonArrayFunc = func;
	}

	button {
		arg buttonName, func;
		var index = buttonNames.indexOf(buttonName);
		if (index.notNil, { this.controlFuncs[index] = func; });
	}

	stick {
		arg stickName, func;
		var index = stickNames.indexOf(stickName) + buttonNames.size;
		if (index.notNil, { this.controlFuncs[index] = func; });
	}

}