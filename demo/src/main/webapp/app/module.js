function initDemoPlugin() {
  osApp.providers.pluginReg.registerViews(
    'demo',
    {
      'sign-up': {
        'page-body': {
          template: 'plugin-ui-resources/demo/signup.html'
        }
      }
    }
  );
}

initDemoPlugin()
