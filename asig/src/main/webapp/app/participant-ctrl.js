angular.module('os.plugins.svh')
  .controller('participantCtrl', function($scope) {
    $scope.sites = [];
    
    $scope.cpr.getConsents().then(
      function(consents) {
        $scope.consentTierResponses = consents.consentTierResponses;
      }
    );
  });
