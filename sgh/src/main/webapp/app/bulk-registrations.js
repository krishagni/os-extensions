angular.module("os.plugins.sgh")
  .controller('sghCpBulkRegistrationsCtrl', function(
    $scope, $http, $state,
    cp, ApiUrls, PvManager, Alerts) {

    function init() {
      $scope.cp = cp;
      $scope.regReq = {
        cpId: cp.id,
        participantCount: 0,
        printLabels: false
      }
      
      loadPvs();
    }
    
    function loadPvs() {
      $scope.printers = PvManager.getPvs('printer-site');
    };

    $scope.bulkRegister = function() {
      $http.post(ApiUrls.getBaseUrl() + 'sgh/registrations', $scope.regReq).then(
        function(result) {
          Alerts.success("custom_sgh.participant_registered", result.data);
          $state.go('participant-list', {cpId: $scope.regReq.cpId});  
        }
      );
    }

    init();
  })

  .controller('sghBulkRegistrationButtonCtrl', function(
    $scope, CpConfigSvc) {

    function init() {
      CpConfigSvc.getWorkflowData($scope.cp.id, "bulkRegister").then(
        function(workflowData) {
          $scope.bulkRegEnabled = workflowData.enabled;
        }
      );
      }

    init();
  });
