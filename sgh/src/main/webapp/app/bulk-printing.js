angular.module("os.plugins.sgh")
  .controller('sghCpBulkPrintingCtrl', function(
    $scope, $http, $state, ApiUrls, PvManager, Alerts) {

    function init() {
      $scope.regReq = {
        tridCount: 0,
        printLabels : false
      }
      loadPvs();
    }
    
    function loadPvs() {
      $scope.printers = PvManager.getPvs('printer-site');
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
  .controller('sghCpBulkRePrintingCtrl', function(
    $scope, $http, $state, ApiUrls, PvManager, Alerts) {

    function init() {
      $scope.trids = "";
      $scope.printer= {
    	printerName : ""
      }
      loadPvs();
    }
    
    function loadPvs() {
      $scope.printers = PvManager.getPvs('printer-site');
    };

    $scope.bulkPrint = function() {
      var trids =
        $scope.trids.split(/,|\t|\n/)
          .map(function(label) { return label.trim(); })
          .filter(function(label) { return label.length != 0; });
      if (trids.length == 0) {
        return;
      }

      $http.post(ApiUrls.getBaseUrl() + 'sgh/trids/print?printer='+$scope.printer.printerName, {trids: trids}).then(
        function(result) {
          Alerts.success("custom_sgh.trid_printed");
          $state.go('home');  
        }
      );
    }

    init();
  });
