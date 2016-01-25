osApp.providers
  .controller('jhuSprDetailCtrl', function($state, $scope, $rootScope, sprDetail) {
    function init() {
      $scope.sprDetail = sprDetail;
      $rootScope.selectedSpr = {
        visitId: $scope.visit.id,
        specimenId: sprDetail.specimenId,
        mrn: sprDetail.mrn
      };
    }
    
    $scope.reloadSprList = function(){
      $rootScope.selectedSpr = undefined;
      $state.go('visit-detail.spr');
    }
    
    init();
  });
