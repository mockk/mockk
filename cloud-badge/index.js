"use strict";
const google = require('googleapis');

const key = require('./credentials.json');
const viewId = '163520949';

function getUsers(key, viewId) {
    // https://github.com/google/google-api-nodejs-client#using-jwt-service-tokens
    const jwtClient = new google.auth.JWT(
        key.client_email,
        null,
        key.private_key,
        ['https://www.googleapis.com/auth/analytics.readonly'],
        null
    );

    return new Promise((resolve, reject) => {
        jwtClient.authorize(err => {
            if (err) {
                console.log(err);
                return;
            }

            // based on https://github.com/google/google-api-nodejs-client/issues/561
            const analytics = google.analyticsreporting('v4');
            analytics.reports.batchGet({
                auth: jwtClient,
                resource: {
                    reportRequests: [
                        {
                            viewId: viewId,
                            dateRanges: [
                                {
                                    startDate: '7daysAgo',
                                    endDate: 'today'
                                }
                            ],
                            metrics: [
                                {
                                    expression: 'ga:users'
                                }
                            ]
                        }
                    ]
                }
            }, (err, data) => {
                if (err) {
                    return reject(err);
                } else {
                    return resolve(data);
                }
            });
        });
    });
}

exports.function = function (req, res) {
    return getUsers(key, viewId)
        .then(data => {
            const value = data.reports[0].data.totals[0].values[0];
            return res.redirect(302, `https://img.shields.io/badge/users-${value}%2Fweek-yellow.svg`);
        })
        .catch(err => {
            console.error(err);
            return res.sendStatus(500);
        });
};
