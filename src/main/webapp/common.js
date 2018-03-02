'use strict';

var API_ENDPOINT = '/tomcat/irma_email_issuer/api/';

function init() {
    $('#email-form').on('submit', addEmail);

    // Route to the correct window - e.g. when clicking on a verification link.
    var hash = location.hash.substring(1);
    var parts = hash.split('/');
    var key = parts[0];
    if (key === 'verify-email') {
        // When clicking a verification link
        setWindow('email-verify');
        verifyEmail(parts[1]);
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
    $.post(API_ENDPOINT + 'send-email-token', {email: address, language: MESSAGES['lang']})
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

function verifyEmail(token) {
    console.log('verify token:', token);
    $.post(API_ENDPOINT + 'verify-email-token', {token: token})
        .done(function(jwt) {
            setStatus('info', MESSAGES['email-add-verified'])
            console.log('success: ', jwt);
            IRMA.issue(jwt, function(e) {
                setStatus('success', MESSAGES['email-add-success'] + MESSAGES['return-to-issue-page'])
                console.log('email issued:', e);
            }, function(e) {
                console.warn('cancelled:', e);
                // TODO: don't interpret these strings, use error codes instead.
                if (e === 'Session timeout, please try again') {
                    setStatus('info', MESSAGES['email-add-timeout'] + MESSAGES['return-to-issue-page'])
                } else { // e === 'User cancelled authentication'
                    setStatus('info', MESSAGES['email-add-cancel'] + MESSAGES['return-to-issue-page'])
                }
            }, function(e) {
                setStatus('danger', MESSAGES['email-add-error'] + MESSAGES['return-to-issue-page'])
                console.error('error:', e);
            })
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
        })
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
