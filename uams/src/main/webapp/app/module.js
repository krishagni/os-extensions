angular.module('os.plugins.uams', [])
  .config(function($stateProvider) {
    $stateProvider
      .state('uams-gen-case-forms', {
        url: '/gen-case-forms',
        templateUrl: 'plugin-ui-resources/uams/generate-case-forms.html',
        controller: 'uamsGenCaseFormsCtrl',
        parent: 'signed-in'
      })
  })
  .controller('uamsGenCaseFormsCtrl', function($scope, $http, $state, Util, Alerts, ApiUrls) {
    var ctx;

    function init() {
      ctx = $scope.ctx = { count: null, subjectIds: null }
    }

    $scope.generate = function() {
      var payload = {};
      if (ctx.count > 0) {
        payload.count = +ctx.count;
      } else if (ctx.subjectIds) {
        payload.subjectIds = Util.splitStr(ctx.subjectIds, /,|\t|\n/);
      }

      var url = ApiUrls.getBaseUrl() + 'uams/case-forms';
      $http.post(url, payload).then(
        function(resp) {
          var fileId = resp.data.fileId;
          if (fileId) {
            Util.downloadFile(ApiUrls.getBaseUrl() + 'uams/case-forms/files?fileId=' + fileId);
          } else {
            Alerts.add('Link to download the case forms will be sent to you by email.', 'info');
          }

          $state.go('cp-list');
        }
      );
    }

    init();
  })
  .run(function(PluginReg) {
    PluginReg.registerViews(
      'uams',
      {
        'cp-list': {
          'more-menu': {
            template: 'plugin-ui-resources/uams/generate-case-forms-link.html'
          }
        }
      }
    );

  });

