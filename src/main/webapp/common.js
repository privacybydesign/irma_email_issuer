'use strict';

var API_ENDPOINT = '/irma_email_provider/api/';

var MESSAGES = {
    'error:email-address-malformed': 'E-mailadres is onjuist',
    'sending-verification-email': 'Verificatie e-mail wordt verzonden aan %address%…',
    'sent-verification-email': 'Verificatiemail is verzonden aan %address%. Controleer uw inbox voor een verificatie e-mail. Het kan een paar minuten duren voordat de e-mail arriveert.',
    'verifying-email-token': 'E-mail verifiëren...',
    'unknown-problem': 'Onbekend probleem: ',
};

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
    $('[id^=window-').addClass('hidden');
    $('#window-'+window).removeClass('hidden');
}

function addEmail(e) {
    var address = $('#email-form [type=email]').val();
    setStatus('info', MESSAGES['sending-verification-email'].replace('%address%', address));
    $('#email-form input').prop('disabled', true);
    $.post(API_ENDPOINT + 'send-email-token', {email: address})
        .done(function(e) {
            // Mail was sent - but we don't know whether it'll be received
            // (e.g. address may not exist).
            console.log('success', e);
            setStatus('success', MESSAGES['sent-verification-email'].replace('%address%', address));
        })
        .fail(function(e) {
            // Address format problem?
            setStatus('danger', MESSAGES[e.responseText] || MESSAGES['unknown-problem'] + e.responseText);
            console.error('fail', e.responseText);
        });
}

function verifyEmail(token) {
    console.log('verify:', token);
    setStatus('info', MESSAGES['verifying-email-token']);
}

function setStatus(alertType, message) {
    $('#status')
        .removeClass('alert-success')
        .removeClass('alert-info')
        .removeClass('alert-warning')
        .removeClass('alert-danger')
        .addClass('alert-'+alertType)
        .text(message)
        .removeClass('hidden');
}

init();