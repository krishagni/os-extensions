
angular.module('os.plugins.washu', [])
  .run(function(PluginReg) {
    PluginReg.registerViews(
      'washu',
      {
        'specimen-list': {
          'page-header': {
            template: 'plugin-ui-resources/washu/working-spmns-report.html'
          }
        }
      }
    );
  });

