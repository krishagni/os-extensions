
angular.module('os.plugins.washu')
  .controller('wuWorkingRequestRptCtrl', function($scope, $stateParams, $http, ApiUrls, SettingUtil, Util) {
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
        this.name = "request_" + ($scope.$parent.ctx && $scope.$parent.ctx.request.id);

        this.generateReport = function() {
          var params = {requestId: $stateParams.requestId};
          var selectedSpmns = $scope.$parent.lctx && $scope.$parent.lctx.checkList.getSelectedItems();
          if (selectedSpmns && selectedSpmns.length > 0) {
            params.specimenId = selectedSpmns.map(function(spmn) { return spmn.id; });
          }

          return $http.get(ApiUrls.getBaseUrl() + 'washu-reports/request-report', {params: params}).then(
            function(resp) {
              return resp.data;
            }
          );
        }
      }

      Util.downloadReport(generator, 'washu.working_req', generator.name + '.xlsx');
    }

    init();
  });
