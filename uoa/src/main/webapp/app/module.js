angular.module('os.plugins.uoa', ['openspecimen'])
  .run(function(PluginReg) {
  
    PluginReg.registerViews(
      'vcb',
      {
        'participant-addedit': {
          'page-body': {
            template: 'plugin-ui-resources/uoa/participant-addedit.html'
          }
        },
  
        'participant-detail': {
          'summary': {
            template: 'plugin-ui-resources/uoa/participant-summary.html'
          }
        }
      }
    );
  });

