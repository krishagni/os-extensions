osApp.providers
  .controller('participantCtrl', function($scope) {
    $scope.sites = [];
    
    $scope.cpr.getConsents().then(
      function(consents) {
        $scope.consentTierResponses = consents.consentTierResponses;
      }
    );
    
    angular.forEach($scope.cpr.participant.pmis, function(pmi) {
      $scope.sites.push(pmi.siteName);
    });
  });
