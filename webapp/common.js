'use strict';

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
    }
}

function setWindow(window, back) {
    $('[id^=window-]').addClass('hidden');
    $('#window-'+window).removeClass('hidden');

    const backButton = $('#back-button');
    backButton.off();
    if (back) {
        backButton
          .click(() => {setWindow(back); return false;})
          .removeClass('button-hidden');
    } else if (location.href.includes('?inapp=true')) {
        backButton.addClass('button-hidden');
    }

    const submitButtonText = MESSAGES['submit-' + window];
    const submitButton = $('#submit-button');
    if (submitButtonText) {
        submitButton.text(submitButtonText);
        submitButton.removeClass('hidden');
    }
    else
        submitButton.addClass('hidden');
}

function addEmail(e) {
    if ($('#window-email-confirm').hasClass('hidden')) {
        setWindow('email-confirm', 'email-add');
        return;
    }

    const address = $('#email-form [id=email]').val();
    const addressConfirmed = $('#email-form [id=email-confirm]').val();

    console.log(address, addressConfirmed);
    if (address !== addressConfirmed) {
        setStatus('warning', MESSAGES['email-confirm-differs']);
        setWindow('email-add');
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
            setStatus('info', MESSAGES['sent-verification-email'].replace('%address%', address));
        })
        .fail(function(e) {
            // Address format problem?
            setStatus('danger', MESSAGES[e.responseText] || MESSAGES['unknown-problem']);
            setWindow('email-add');
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

    irma.startSession(config.IRMASERVER, jwt, 'publickey')
        .then(function(pkg) {
            console.log('session started');
            return irma.handleSession(pkg.sessionPtr,
                {method: 'popup', language: language}
            );
        })
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
            if (err === irma.SessionStatus.Cancelled)
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
    $('#status')
        .removeClass('alert-success')
        .removeClass('alert-info')
        .removeClass('alert-warning')
        .removeClass('alert-danger')
        .addClass('alert-'+alertType)
        .html(message)
        .removeClass('hidden');
}

init();