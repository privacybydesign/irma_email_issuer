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

function setWindow(window) {
    $('[id^=window-]').addClass('hidden');
    $('#window-'+window).removeClass('hidden');
}

function addEmail(e) {
    var address = $('#email-form [type=email]').val();
    setStatus('info', MESSAGES['sending-verification-email'].replace('%address%', address));
    $('#email-form input').prop('disabled', true);
    $.post(config.EMAILSERVER + '/send-email-token', {email: address, language: MESSAGES['lang']})
        .done(function(e) {
            // Mail was sent - but we don't know whether it'll be received
            // (e.g. address may not exist).
            console.log('success', e);
            setStatus('success', MESSAGES['sent-verification-email'].replace('%address%', address));
        })
        .fail(function(e) {
            // Address format problem?
            setStatus('danger', MESSAGES[e.responseText] || MESSAGES['unknown-problem']);
            console.error('fail', e.responseText);
        });
}

function verifyEmail(token, url) {
    console.log('verify token:', token, url);
    $.post(config.EMAILSERVER + '/verify-email-token', {token: token})
        .done(function(jwt) {
            setStatus('info', MESSAGES['email-add-verified'])
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
                        setStatus('success', MESSAGES['email-add-success'] + MESSAGES['return-to-issue-page']);
                })
                .catch(function(err) {
                    console.error('error:', err);
                    if (err === irma.SessionStatus.Cancelled)
                        setStatus('info', MESSAGES['email-add-cancel'] + MESSAGES['return-to-issue-page']);
                    else
                        setStatus('danger', MESSAGES['email-add-error'] + MESSAGES['return-to-issue-page']);
                });
        })
        .fail(function(e) {
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
        });
    setStatus('info', MESSAGES['verifying-email-token']);
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
