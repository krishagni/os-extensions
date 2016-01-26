angular.module('os.plugins.actrec')
  .controller('actrecAddEditCtrl', function($scope, PvManager) {
    function loadPvs() {
      $scope.category = PvManager.getPvs('Animal_Category_PID');
      
      if($scope.cpr.participant.extensionDetail) {
        $scope.loadBreeds($scope.cpr.participant.extensionDetail.attrsMap.category);
      }
    }
    
    $scope.loadBreeds = function(category) {
      $scope.breeds = [];
      if (category == 'Mouse' || category == 'Nude mouse') {
        $scope.breeds = PvManager.getPvsByParent('Animal_Category_PID', 'Mouse');
      } else {
        $scope.breeds = PvManager.getPvsByParent('Animal_Category_PID', category);
      }
    }
    
    loadPvs();
  });
