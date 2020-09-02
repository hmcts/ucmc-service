const {I} = inject();

module.exports = {

  fields: {
    proceed: {
      id: '#applicant1ProceedWithClaim',
      options: {
        yes: 'Yes',
        no: 'No'
      }
    }
  },

  async proceedWithClaim() {
    I.waitForElement(this.fields.proceed.id);
    await within(this.fields.proceed.id, () => {
      I.click(this.fields.proceed.options.yes);
    });
    await I.clickContinue();
  }
};
