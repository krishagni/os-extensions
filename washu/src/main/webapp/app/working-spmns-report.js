
angular.module('os.plugins.washu')
  .controller('wuWorkingSpmnsRptCtrl', function($scope, $stateParams, $http, ApiUrls, SettingUtil, Util) {
    function init() {
      $scope.ctx = {
        rptTmplConfigured: false
      }

      SettingUtil.getSetting('common', 'cart_specimens_rpt_query').then(
        function(setting) {
          $scope.ctx.rptTmplConfigured = !!setting.value;
        }
      );
    }

    $scope.downloadReport = function() {
      var generator = new function() {
        this.name = $scope.$parent.ctx && $scope.$parent.ctx.list.getDisplayName();

        this.generateReport = function() {
          var params = {listId: $stateParams.listId};
          return $http.get(ApiUrls.getBaseUrl() + 'washu-reports/working-specimens', {params: params}).then(
            function(resp) {
              return resp.data;
            }
          );
        }
      }

      Util.downloadReport(generator, 'washu.working_spmns', generator.name + '.xlsx');
    }

    init();
  });
