
angular.module('os.plugins.washu', [])
  .run(function(PluginReg) {
    PluginReg.registerViews(
      'washu',
      {
        'specimen-list': {
          'page-header': {
            template: 'plugin-ui-resources/washu/working-spmns-report.html'
          }
        },
        'order-detail': {
          'more-menu': {
            template: 'plugin-ui-resources/washu/working-order-report.html'
          }
        },
        'tracker-request-specimens': {
          'page-header': {
            template: 'plugin-ui-resources/washu/working-request-report.html'
          }
        }
      }
    );
  });

