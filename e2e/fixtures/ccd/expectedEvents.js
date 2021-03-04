const events = require('./events.js');

module.exports = {
  AWAITING_CASE_NOTIFICATION: [
    events.NOTIFY_DEFENDANT_OF_CLAIM,
    events.ADD_DEFENDANT_LITIGATION_FRIEND,
    events.CASE_PROCEEDS_IN_CASEMAN,
    events.ADD_OR_AMEND_CLAIM_DOCUMENTS,
    events.AMEND_PARTY_DETAILS
  ],
  AWAITING_CASE_DETAILS_NOTIFICATION: [
    events.NOTIFY_DEFENDANT_OF_CLAIM_DETAILS,
    events.ADD_DEFENDANT_LITIGATION_FRIEND,
    events.CASE_PROCEEDS_IN_CASEMAN,
    events.ADD_OR_AMEND_CLAIM_DOCUMENTS,
    events.AMEND_PARTY_DETAILS
  ],
  CREATED: [
    events.ACKNOWLEDGE_SERVICE,
    events.ADD_DEFENDANT_LITIGATION_FRIEND,
    events.DEFENDANT_RESPONSE,
    events.CASE_PROCEEDS_IN_CASEMAN,
    events.INFORM_AGREED_EXTENSION_DATE,
    events.AMEND_PARTY_DETAILS
  ],
  PROCEEDS_WITH_OFFLINE_JOURNEY: [],
  AWAITING_CLAIMANT_INTENTION: [
    events.ADD_DEFENDANT_LITIGATION_FRIEND,
    events.CLAIMANT_RESPONSE,
    events.CASE_PROCEEDS_IN_CASEMAN,
    events.AMEND_PARTY_DETAILS
  ],
  PENDING_CASE_ISSUED: [
    events.RESUBMIT_CLAIM,
    events.ADD_DEFENDANT_LITIGATION_FRIEND,
    events.NOTIFY_DEFENDANT_OF_CLAIM,
    events.AMEND_PARTY_DETAILS
  ]
};
