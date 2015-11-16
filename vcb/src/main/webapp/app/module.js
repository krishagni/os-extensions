function initVcbPlugin() {
  osApp.providers.pluginReg.registerViews(
    'vcb',
    {
      'participant-addedit': {
        'page-body': {
          template: 'plugin-ui-resources/vcb/participant-addedit.html'
        }
      },

      'participant-detail': {
        'summary': {
          template: 'plugin-ui-resources/vcb/participant-summary.html'
        }
      },

      'visit-addedit': {
        'page-body': {
          template: 'plugin-ui-resources/vcb/addedit-visit.html'
        }
      },

      'visit-detail': {
        'overview': {
          template: 'plugin-ui-resources/vcb/visit-overview.html'
        }
      }
    }
  );
}

initVcbPlugin();
