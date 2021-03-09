const events = require('./events.js');

module.exports = {
  solicitor: {
    AWAITING_CASE_NOTIFICATION: [
      events.NOTIFY_DEFENDANT_OF_CLAIM,
      events.ADD_DEFENDANT_LITIGATION_FRIEND,
      events.ADD_OR_AMEND_CLAIM_DOCUMENTS
    ],
    AWAITING_CASE_DETAILS_NOTIFICATION: [
      events.NOTIFY_DEFENDANT_OF_CLAIM_DETAILS,
      events.ADD_DEFENDANT_LITIGATION_FRIEND,
      events.ADD_OR_AMEND_CLAIM_DOCUMENTS
    ],
    CREATED: [
      events.ACKNOWLEDGE_SERVICE,
      events.ADD_DEFENDANT_LITIGATION_FRIEND,
      events.DEFENDANT_RESPONSE,
      events.INFORM_AGREED_EXTENSION_DATE
    ],
    PROCEEDS_WITH_OFFLINE_JOURNEY: [],
    AWAITING_CLAIMANT_INTENTION: [
      events.ADD_DEFENDANT_LITIGATION_FRIEND,
      events.CLAIMANT_RESPONSE,
    ],
    PENDING_CASE_ISSUED: [
      events.RESUBMIT_CLAIM,
      events.ADD_DEFENDANT_LITIGATION_FRIEND,
      events.NOTIFY_DEFENDANT_OF_CLAIM
    ]
  },
  admin: {
    AWAITING_CASE_NOTIFICATION: [
      events.CASE_PROCEEDS_IN_CASEMAN
    ],
    AWAITING_CASE_DETAILS_NOTIFICATION: [
      events.CASE_PROCEEDS_IN_CASEMAN
    ],
    CREATED: [
      events.CASE_PROCEEDS_IN_CASEMAN
    ],
    PROCEEDS_WITH_OFFLINE_JOURNEY: [],
    AWAITING_CLAIMANT_INTENTION: [
      events.CASE_PROCEEDS_IN_CASEMAN
    ],
    PENDING_CASE_ISSUED: []
  }
};
