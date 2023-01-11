'use strict';

const isInApp = location.href.includes('?inapp=true');

function init() {
    $('#email-form').on('submit', addEmail);

    // Route to the correct window - e.g. when clicking on a verification link.
    var hash = location.hash.substring(1);
    var parts = hash.split('/');
    var key = parts[0];
    if (key === 'verify-email') {
        // When clicking a verification link
        setWindow('email-verify');
        verifyEmail(parts[1], parts[2]);
    } else {
        // Default window
        setWindow('email-add');
        $('#email-form input').prop('disabled', false);
    }
}

function setWindow(window, back) {
    $('[id^=window-]').addClass('hidden');
    $('#window-'+window).removeClass('hidden');

    // Put h1 in header when not being on mobile
    const h1 = $('#window-'+window + ' h1');
    if (isInApp) {
        $('header').hide();
        h1.show();
    } else {
        $('header').html(h1.clone().show()).show();
        h1.hide();
    }
    const backButton = $('#back-button');
    backButton.off();
    if (back) {
        backButton
          .click(() => {clearStatus(); setWindow(back); return false;})
          .removeClass('button-hidden');
    } else if (history.length > 1) {
        backButton
          .click(() => {clearStatus(); history.back(); return false;})
          .removeClass('button-hidden');
    } else {
        backButton.addClass('button-hidden');
    }

    const submitButtonText = MESSAGES['submit-' + window];
    const submitButton = $('#submit-button');
    if (submitButtonText) {
        submitButton.text(submitButtonText);
        submitButton.removeClass('hidden');
    } else {
        submitButton.addClass('hidden');
    }
}

function addEmail(e) {
    const address = $('#email-form [id=email]').val().toLowerCase();

    if ($('#window-email-confirm').hasClass('hidden')) {
        $('#email-confirm').text(address);
        setWindow('email-confirm', 'email-add');
        return;
    }

    setStatus('info', MESSAGES['sending-verification-email'].replace('%address%', address));
    setWindow('email-sent', 'email-add');
    $('#email-form input').prop('disabled', true);
    $.post(config.EMAILSERVER + '/send-email-token', {email: address, language: MESSAGES['lang']})
        .done(function(e) {
            // Mail was sent - but we don't know whether it'll be received
            // (e.g. address may not exist).
            console.log('success', e);
            setStatus('success', MESSAGES['sent-verification-email'].replace('%address%', address));

            // Make empty and editable again
            $('#email-form input').prop('disabled', false).val('');
        })
        .fail(function(e) {
            // Address format problem?
            setStatus('danger', MESSAGES[e.responseText] || MESSAGES['unknown-problem']);
            setWindow('email-add');

            // Make editable again
            $('#email-form input').prop('disabled', false);

            console.error('fail', e.responseText);
        });
}

function verifyEmail(token, url) {
    console.log('verify token:', token, url);
    setStatus('info', MESSAGES['verifying-email-token']);
    $.post(config.EMAILSERVER + '/verify-email-token', {token: token})
        .done(function(jwt) { issue(jwt, url); })
        .fail(handleIssuanceError);
}

function issue(jwt, url) {
    setStatus('info', MESSAGES['email-add-verified']);
    console.log('success: ', jwt);

    irma.newPopup({
        url: config.IRMASERVER,
        language: language,
        session: {
            start: {
                method: 'POST',
                headers: {
                    'Content-Type': 'text/plain',
                },
                body: jwt,
            },
            result: false,
        },
    })
        .start()
        .then(function() {
            console.log('session done');
            if (url) {
                window.location.replace(decodeURIComponent(url));
                setStatus('success', MESSAGES['email-add-success']);
            } else
                setStatus('success', MESSAGES['email-add-success']);
        })
        .catch(function(err) {
            console.error('error:', err);
            if (err === 'Aborted')
                setStatus('info', MESSAGES['email-add-cancel']);
            else
                setStatus('danger', MESSAGES['email-add-error']);
        });
}

function handleIssuanceError(e) {
    console.error('email token not accepted:', e.responseText);
    var errormsg = e.responseText;
    if (errormsg.substr(0, 6) === 'error:' && errormsg in MESSAGES) {
        // Probably the token didn't verify.
        setStatus('danger', MESSAGES[errormsg])
    } else {
        // Problem outside the API (e.g. misconfiguration, network
        // error, etc.)
        setStatus('danger', MESSAGES['email-failed-to-verify'])
    }
}

function setStatus(alertType, message) {
    $('#status').html(message);
    $('#status-bar')
        .removeClass('alert-success')
        .removeClass('alert-info')
        .removeClass('alert-warning')
        .removeClass('alert-danger')
        .addClass('alert-'+alertType)
        .removeClass('hidden');
    window.scrollTo(0,0);
}

function clearStatus() {
    $('#status-bar').addClass('hidden');
}

init();
