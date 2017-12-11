/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for license information.
 */

function checkboxHandler(checkbox, hidden, currentSelection) {
    return function () {
        var value = hidden.getAttribute('data-value');
        if ($(checkbox).checked) {
            hidden.setValue(value);
            currentSelection[value] = true;
        } else {
            hidden.setValue('');
            delete currentSelection[value];
        }
    };
}

function updateCheckboxList(checkboxDiv, url, config) {
    config = config || {};
    config = object(config);
    checkboxDiv.value = checkboxDiv.value || {};
    var originalOnSuccess = config.onSuccess;
    config.onSuccess = function (rsp) {
        var div = $(checkboxDiv);
        var fieldName = div.getAttribute('data-name');
        var currentSelection = checkboxDiv.value;

        var opts = JSON.parse(rsp.responseText);
        if (opts) {
            opts = opts.values;
        }
        while (div.firstChild) {
            div.removeChild(div.firstChild);
        }
        if (opts && opts.length > 0) {
            for (var i = 0, ie = opts.length; i < ie; ++i) {
                var opt = opts[i];
                var selected = opt.selected || currentSelection.hasOwnProperty(opt.value);

                // a checkbox
                var checkbox = document.createElement('input');
                checkbox.setAttribute('type', 'checkbox');
                checkbox.setAttribute('name', fieldName + '_check');
                checkbox.setAttribute('value', opt.value);
                if (selected) {
                    checkbox.setAttribute('checked', 'checked');
                }

                // checkbox label
                var label = document.createElement('label');
                var labelText = document.createTextNode(opt.name);
                label.appendChild(checkbox);
                label.appendChild(labelText);

                // a hidden input
                var input = document.createElement('input');
                input.setAttribute('type', 'hidden');
                input.setAttribute('name', fieldName);
                input.setAttribute('value', selected ? opt.value : '');
                input.setAttribute('data-value', opt.value);

                var group = document.createElement('div');
                group.appendChild(label);
                group.appendChild(input);

                div.appendChild(group);

                $(checkbox).observe('change', checkboxHandler(checkbox, input, currentSelection));
            }
        } else if (typeof opts === 'undefined' || opts === null) {
            div.innerText = "*** No items loaded ***";
        } else {
            div.innerText = "--- No items available ---";
        }

        if (originalOnSuccess) {
            originalOnSuccess(rsp);
        }
    };
    config.onFailure = function (rsp) {
        $(checkboxDiv).innerText = "ERROR: " + rsp;
    };

    new Ajax.Request(url, config);
}

Behaviour.specify("div.azure-checkbox-list", 'azure-checkbox-list', 1000, function (e) {
    var dataValue = e.getAttribute('data-value');
    if (dataValue !== null && dataValue !== '') {
        var values = JSON.parse(dataValue);
        var valuesMap = {};
        if (values !== null) {
            for (var i = 0, ie = values.length; i < ie; ++i) {
                valuesMap[values[i]] = true;
            }
        }
        e.value = valuesMap;
    } else {
        e.value = {};
    }

    var checkboxes = $(e).select('input[type="checkbox"]');
    var checkbox = checkboxes && checkboxes[0];
    if (checkbox) {
        var hidden = $(e).select('input[type="hidden"]');
        if (hidden) {
            hidden = hidden[0];
        }
        if (!hidden) {
            return;
        }
        $(checkbox).observe('change', checkboxHandler(checkbox, hidden, e.value));
    }

    refillOnChange(e, function (params) {
        updateCheckboxList(e, e.getAttribute('fillUrl'), {
            parameters: params,
            onSuccess: function () {
                fireEvent(e, 'filled');
            }
        })
    })
});
