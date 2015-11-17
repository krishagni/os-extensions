
osApp.providers
  .state('cp-detail.rc-config', {
    url: '/rc-config',
    templateUrl: 'plugin-ui-resources/redcap/cp-config.html',
    controller: 'rcCpConfigCtrl',
    parent: 'cp-detail'
  });

  function initRedcapPlugin() {
    osApp.providers.pluginReg.registerViews(
      'redcap',
      {
        'cp-detail': {
          'side-menu': {
            'template': 'plugin-ui-resources/redcap/cp-config-menu-item.html'
          }
        }
      }
    );
  }

  initRedcapPlugin();
