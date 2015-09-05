osApp.providers
  .controller('sghCpBulkPrintingCtrl', function(
    $scope, $http, $state, ApiUrls, Alerts) {

    function init() {
      $scope.regReq = {
        tridCount: 0
      }
    };

    $scope.bulkPrint = function() {
      $http.post(ApiUrls.getBaseUrl() + 'sgh/trids', $scope.regReq).then(
        function(result) {
          Alerts.success("custom_sgh.trid_printed");
          $state.go('home');  
        }
      );
    }

    init();
  })
  .controller('sgh.CpBulkRePrintingCtrl', function(
    $scope, $http, $state, ApiUrls, Alerts) {

    function init() {
      $scope.trids = "";
    };

    $scope.bulkPrint = function() {
      var trids =
        $scope.trids.split(/,|\t|\n/)
          .map(function(label) { return label.trim(); })
          .filter(function(label) { return label.length != 0; });
      if (trids.length == 0) {
        return;
      }

      $http.post(ApiUrls.getBaseUrl() + 'sgh/trids/print', {trids: trids}).then(
        function(result) {
          Alerts.success("custom_sgh.trid_printed");
          $state.go('home');  
        }
      );
    }

    init();
  });
