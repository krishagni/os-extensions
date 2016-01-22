angular.module('os.plugins.vcb') 
  .controller('vcbAddEditVisitCtrl', function($scope, PvManager) {
    function init() {
      loadPvs();
    }

    function loadPvs() {
      $scope.anatomicSites = PvManager.getLeafPvs('anatomic-site');
    }

    init();
  });
