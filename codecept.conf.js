/* global process */

exports.config = {
  tests: './e2e/tests/*_test.js',
  output: './output',
  helpers: {
    Puppeteer: {
      restart: false,
      keepCookies: true,
      show: process.env.SHOW_BROWSER_WINDOW || false,
      windowSize: '1200x900',
      waitForTimeout: process.env.WAIT_FOR_TIMEOUT_MS || 40000,
      waitForNavigation: [ "domcontentloaded", "networkidle0" ],
      chrome: {
        ignoreHTTPSErrors: true
      },
    },
    PuppeteerHelpers: {
      require: './e2e/helpers/puppeteer_helper.js',
    },
  },
  include: {
    I: './e2e/steps_file.js',
    api: './e2e/api/steps.js'
  },
  plugins: {
    autoDelay: {
      enabled: true,
      methods: [
        'click',
        'fillField',
        'checkOption',
        'selectOption',
        'attachFile',
      ],
    },
    retryFailedStep: {
      enabled: true,
    },
    screenshotOnFail: {
      enabled: true,
      fullPageScreenshots: true,
    },
  },
  mocha: {
    bail: true,
    reporterOptions: {
      'codeceptjs-cli-reporter': {
        stdout: '-',
        options: {
          steps: false,
        },
      },
      'mocha-junit-reporter': {
        stdout: '-',
        options: {
          mochaFile: 'test-results/result.xml',
        },
      },
      'mochawesome': {
        stdout: '-',
        options: {
          reportDir: './output',
          inlineAssets: true,
          json: false,
        },
      },
    }
  }
};
