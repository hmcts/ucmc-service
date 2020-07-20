const {I} = inject();

const date = require('../../fragments/date');

module.exports = {

  fields: {
    extensionCounter: {
      id: '#defendantSolicitor1claimResponseExtensionCounter',
      options: {
        yes: 'Yes',
        no: 'No'
      }
    },
    extensionCounterDate: {
      id: 'defendantSolicitor1claimResponseExtensionCounterDate',
    }
  },

  async enterCounterDate() {
    await within(this.fields.extensionCounter.id, () => {
      I.click(this.fields.extensionCounter.options.yes);
    });

    await date.enterDate(this.fields.extensionCounterDate.id);
    await I.clickContinue();
  }
};

