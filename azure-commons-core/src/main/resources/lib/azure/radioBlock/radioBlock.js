// prototype object to be duplicated for each radio button group

var radioBlockSupport = {
    buttons : null, // set of functions, one for updating one radio block each

    updateButtons : function() {
        for( var i=0; i<this.buttons.length; i++ )
            this.buttons[i]();
    },

    // update one block based on the status of the given radio button
    updateSingleButton : function(radio) {
        var show = radio.checked;

        if (radio.getAttribute("checked") == "true" &&
            (radio.getAttribute("radioblock-init") == undefined ||
            radio.getAttribute("radioblock-init") == "false")) {
            show = true;
            radio.setAttribute("radioblock-init", "true");
        }

        var blockStart = findAncestorClass(radio,"radio-block-start");
        blockStart.setAttribute("ref", radio.id);
        $(blockStart).next().style.display = show ? "" : "none";

        layoutUpdateCallback.call();
    }
};

Behaviour.specify("INPUT.radio-block-control", 'radioBlock', -100, function(r) {

        r.id = "radio-block-"+(iota++);
        var f = r.form;
        var radios = f.radios;
        if (radios == null)
            f.radios = radios = {};

        var g = radios[r.name];
        if (g == null) {
            radios[r.name] = g = object(radioBlockSupport);
            g.buttons = [];
        }

        var u = function() {
            g.updateSingleButton(r);
        };
        g.buttons.push(u);

        // apply the initial visibility
        u();

        // install event handlers to update visibility.
        // needs to use onclick and onchange for Safari compatibility
        r.onclick = r.onchange = function() { g.updateButtons(); };
});
