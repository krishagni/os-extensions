angular.module('os.plugins.jhu-epic-lookup')
  .controller('jhuEpicParticipantRefreshCtrl',
    function($scope, jhuEpicParticipantUtil, CollectionProtocolRegistration, Alerts) {
      jhuEpicParticipantUtil.addMatchParticipantsFn($scope.cpr.participant);

      $scope.jhuEpicRefresh = function() {
        $scope.cpr.participant.getMatchingParticipants().then(
          function() {
            CollectionProtocolRegistration.getById($scope.cpr.id).then(
              function(cpr) {
                angular.extend($scope.cpr, cpr);
                Alerts.success('participant.epic_refreshed');
              }
            );
          }
        );
      }
    }
  );
