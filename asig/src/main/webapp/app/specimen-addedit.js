angular.module('os.plugins.svh')
  .controller('SvhSpecimenAddEditCtrl', function($scope) {
    $scope.currSpecimen.anatomicSite = "Not Specified";
    $scope.currSpecimen.laterality = "Not Specified";
    $scope.currSpecimen.pathology = "Not Specified";
  });
