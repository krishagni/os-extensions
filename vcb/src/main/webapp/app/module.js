function initVcbPlugin() {
  osApp.providers.pluginReg.registerViews(
    'vcb',
    {
      'visit-addedit': {
        'page-body': {
          template: 'plugin-ui-resources/vcb/addedit-visit.html'
        }
      },

      'participant-overview': {
        'add-visit': {
          template: 'plugin-ui-resources/vcb/add-visit.html'
        }
      }
    }
  );
}

initVcbPlugin();
