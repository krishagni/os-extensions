angular.module("os.plugins.sgh", ['openspecimen'])
  .config(function($stateProvider) {
  $stateProvider
    .state('unplanned-bulk-pre-printing', {
      url: '/bulk-participant-regs',
      templateUrl: 'plugin-ui-resources/sgh/bulk-printing.html',
      controller: 'sghCpBulkPrintingCtrl',
      resolve: {
        cp: function() {
          return {};
        }
      },
      parent: 'signed-in'
    })
    .state('cp-bulk-part-regs', {
      url: '/bulk-participant-regs',
      templateUrl: 'plugin-ui-resources/sgh/bulk-registrations.html',
      controller: 'sghCpBulkRegistrationsCtrl',
      parent: 'cp-view'
    })
    .state('bulk-reprint-trids', {
      url: '/bulk-reprint-trids',
      templateUrl: 'plugin-ui-resources/sgh/bulk-reprint.html',
      controller: 'sghCpBulkRePrintingCtrl',
      parent: 'signed-in'  
    });
  })
  .run(function(PluginReg) {
    PluginReg.registerViews(
      'sgh',
      {
        'participant-list': {
    	  'page-header': {
            template: 'plugin-ui-resources/sgh/cp-bulk-reg-btn.html'
          }
        },

        'home': {
          'page-body': {
            template: 'plugin-ui-resources/sgh/unplanned-bulk-pre-print-icon.html'
          }
        }
      }
    );
  });
