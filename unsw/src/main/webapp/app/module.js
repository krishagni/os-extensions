function initUnswPlugin() {
  osApp.providers.pluginReg.registerViews(
    'unsw',
    {
      'spr': {
        'page-body': {
          template: 'plugin-ui-resources/unsw/identified-spr.html',
          controller: 'unswSpr',
	  parent: 'visit-detail'
        }
      }
    }
  );
}

initUnswPlugin();
