const {I} = inject();

const CASE_HEADER = 'ccd-case-header > h1';
const CONFIRMATION_HEADER = '#confirmation-header';

module.exports = {

  async submit(buttonText, expectedMessage) {
    I.waitForText(buttonText);
    await I.retryUntilExists(() => I.click(buttonText), CONFIRMATION_HEADER);
    await within(CONFIRMATION_HEADER, () => {
      I.see(expectedMessage);
    });
  },

  async returnToCaseDetails() {
    await I.retryUntilExists(() => I.click('Close and Return to case details'), CASE_HEADER);
  }
};
