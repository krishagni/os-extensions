osApp.providers
  .controller('JhuSprListCtrl', function($scope, $rootScope, $state, $http, ApiUrls) {
    var url = ApiUrls.getBaseUrl() + 'jhu/';
    function init() {
      var spr = $rootScope.selectedSpr;
      if(spr != undefined && $scope.visit.id == spr.visitId) {
        $state.go('view-spr-content',{specimenId: spr.specimenId, mrn: spr.mrn});
      } else {
        $scope.sprList = [{},{}];
        loadSprList();
      }
    }
    
    function loadSprList() {
      var mrns = $scope.cpr.participant.pmis.map(function(pmi) {
        return pmi.mrn;
      });
      var reqParams = {"mrn":mrns}
      return $http.get(url + 'sprs?mrn=' + mrns).then(
        function (result){
          $scope.sprList = result.data;
        }
      );
    }
    
    $scope.viewSprContent = function(spr) {
      $state.go('view-spr-content', {'specimenId' : spr.key, 'mrn' : spr.epicMrn});
    }

    init();
  });
