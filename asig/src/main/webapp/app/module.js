function initSvhPlugin() {
  osApp.providers.pluginReg.registerViews(
    'svh',
    {
      'participant-list': {
        'header': {
          template: 'plugin-ui-resources/svh/participant-list-header.html'
        }
      },
      
      'participant-detail': {
        'summary': {
          template: 'plugin-ui-resources/svh/participant-summary.html'
        }
      },
      
      'participant-addedit': {
        'page-body': {
          template: 'plugin-ui-resources/svh/participant-edit.html'
        }
      }
    }
  );
}

initSvhPlugin()
