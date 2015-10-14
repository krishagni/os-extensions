function initActrecPlugin() {
  osApp.providers.pluginReg.registerViews(
    'actrec',
    {
      'participant-addedit': {
        'page-body': {
          template: 'plugin-ui-resources/actrec/participant-addedit.html'
        }
      },
      
      'participant-detail': {
        'overview': {
          template: 'plugin-ui-resources/actrec/participant-overview.html'
        }
      }
    }
  );
}

initActrecPlugin()
