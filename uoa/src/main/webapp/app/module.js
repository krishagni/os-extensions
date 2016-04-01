angular.module('os.plugins.vcb', ['openspecimen'])
  .run(function(PluginReg) {
  
    PluginReg.registerViews(
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
        }
      }
    );
  });

