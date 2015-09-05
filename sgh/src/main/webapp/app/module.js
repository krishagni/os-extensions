
osApp.providers
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
    templateUrl: 'custom-modules/sgh/biospecimen/bulk-reprint.html',
    controller: 'sgh.CpBulkRePrintingCtrl',
    parent: 'signed-in'  
  });

  function initSghPlugin() {
    var injector = angular.injector(['ng', 'openspecimen']);
    var tmplCache = injector.get('$templateCache');
    var pluginReg = injector.get('PluginReg');

    tmplCache.put(
      'plugin-ui-resources/sgh/cp-bulk-reg-btn.html',
      '<span ng-controller="sgh.BulkRegistrationButtonCtrl">'+
      '  <button class="default" ui-sref="cp-bulk-part-regs" ng-if="bulkRegEnabled">' +
      '    <span class="fa fa-plus"></span>' +
      '    <span translate="custom_sgh.bulk_reg">' +
      '      Bulk Register' +
      '    </span>' +
      '  </button>' +
      '</span>'
    );

    tmplCache.put(
      'plugin-ui-resources/sgh/unplanned-bulk-pre-print-icon.html',
      '<li class="os-home-item"> ' +
        '<a ui-sref="unplanned-bulk-pre-printing">' +
          '<span class="os-home-item-icon">' +
            '<span class="fa fa-print"></span>' +
          '</span>' +
          '<span class="os-home-item-info">' +
            '<h3 translate="custom_sgh.bulk_pre_printing">Bulk Pre Print TRIDs</h3>' +
            '<span translate="custom_sgh.bpp_desc">Bulk pre print TRIDs for unplanned collection protocols</span>' +
          '</span>' +
        '</a>' +
      '</li> '
    );
      
    pluginReg.registerViews(
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
  }

  initSghPlugin();
