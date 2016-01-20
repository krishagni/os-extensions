osApp.providers
  .controller('specimenAddEditCtrl', function($scope, Form) {
    function init() {
      $scope.currSpecimen.pathology = "Not Specified";
      $scope.currSpecimen.anatomicSite = "Not Specified";
      $scope.currSpecimen.laterality = "Not Specified";
    }
    
    init();
  });
