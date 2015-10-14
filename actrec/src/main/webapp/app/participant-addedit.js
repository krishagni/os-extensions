osApp.providers
  .controller('actrecAddEditCtrl', function($scope, PvManager) {
    function loadPvs() {
      $scope.genders = PvManager.getPvs('gender');
      $scope.vitalStatuses = PvManager.getPvs('vital-status');
      
      PvManager.loadPvs('gender').then(function(genders) {
        $scope.genders = [];
        angular.forEach(genders, function(gender) {
          if (gender == 'Male' || gender == 'Female') {
            $scope.genders.push(gender);
          }
        });
      });
      
      PvManager.loadPvs('vital-status').then(function(statuses) {
        $scope.vitalStatuses = [];
        angular.forEach(statuses, function(status) {
          if (status == 'Alive' || status == 'Dead') {
            $scope.vitalStatuses.push(status);
          }
        });
      });
      
      $scope.category = PvManager.getPvs('Animal_Category_PID');
      
      if($scope.cpr.participant.extensionDetail) {
        $scope.populateBreed($scope.cpr.participant.extensionDetail.attrsMap.category);
      }
    }
    
    $scope.populateBreed = function(category) {
      $scope.breeds = [];
      if (category == 'Mouse' || category == 'Nude mouse') {
        $scope.breeds = PvManager.getPvsByParent('Animal_Category_PID', 'Mouse');
      } else {
        $scope.breeds = PvManager.getPvsByParent('Animal_Category_PID', category);
      }
    }
    
    loadPvs();
  });
