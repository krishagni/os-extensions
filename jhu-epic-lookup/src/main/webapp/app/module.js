
angular.module('os.plugins.jhu-epic-lookup', ['openspecimen'])
  .run(function(PluginReg) {
    PluginReg.registerViews('jhu-epic-lookup', {
      'participant-addedit': {
        'page-body': {
          template: 'plugin-ui-resources/jhu-epic-lookup/participant-addedit.html'
        }
      },

      'participant-detail': {
        'page-commands': {
          template: 'plugin-ui-resources/jhu-epic-lookup/participant-refresh.html'
        }
      }
    });
  });
