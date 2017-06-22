'use strict';

var API_ENDPOINT = '/tomcat/irma_email_provider/api/';

function init() {
    $('#email-form').on('submit', addEmail);
}

function addEmail(e) {
    var address = $('#email-form [type=email]').val();
    $.post(API_ENDPOINT + 'send-email-token',
        {email: address})
        .done(function(e) {
            console.log('success', e);
        })
        .fail(function(e) {
            console.error('fail', e);
        });
}

init();