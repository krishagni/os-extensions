osApp.providers
  .controller('sghCpBulkRegistrationsCtrl', function(
    $scope, $http, $state,
    cp, ApiUrls, Alerts) {

    function init() {
      $scope.cp = cp;
      $scope.regReq = {
        cpId: cp.id,
        participantCount: 0,
        printLabels: false
      }
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

  .controller('sgh.BulkRegistrationButtonCtrl', function(
    $scope, CpConfigSvc) {

    function init() {
      CpConfigSvc.getBulkRegParticipantTmpl($scope.cp.id, -1).then(
        function(tmpl) {
          $scope.bulkRegEnabled = tmpl != undefined;
        }
      );
      }

    init();
  });
