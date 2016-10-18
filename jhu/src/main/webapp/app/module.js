
angular.module('os.plugins.jhu', ['openspecimen'])
  .config(function($stateProvider) {
    $stateProvider  
      .state('view-spr-content', {
        url: '/view-spr-content/:specimenId/:mrn',
        templateUrl: 'plugin-ui-resources/jhu/spr-detail.html',
        controller: 'jhuSprDetailCtrl',
        resolve: {
          sprDetail : function($stateParams, $http, ApiUrls){
            var url = ApiUrls.getBaseUrl() + 'jhu/sprs/';
            return $http.get(url + "/" + $stateParams.mrn + "?pathId=" + $stateParams.specimenId).then(
              function (result){
                return result.data;
              }
            );
            }
          },
      parent: 'visit-detail'  
    });
  })
  .run(function(PluginReg) {
  
    PluginReg.registerViews(
      'jhu',
      {
        'visit-spr': {
          'side-menu': {
            template: 'plugin-ui-resources/jhu/spr-list.html'
          }
        }
      }
    );
  });

  function reloadReportList(){
    
  }
