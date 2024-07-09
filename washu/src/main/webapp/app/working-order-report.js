
angular.module('os.plugins.washu')
  .controller('wuWorkingOrderRptCtrl', function($scope, $http, ApiUrls, Util) {
    $scope.downloadReport = function(order) {
      var generator = new function() {
        this.name = order.name;

        this.generateReport = function() {
          var params = {orderId: order.id};
          return $http.get(ApiUrls.getBaseUrl() + 'washu-reports/order-report', {params: params}).then(
            function(resp) {
              return resp.data;
            }
          );
        }
      }

      Util.downloadReport(generator, 'washu.working_order', generator.name + '.xlsx');
    }
  });
