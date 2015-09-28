osApp.providers
  .controller('vcbAddVisitCtrl', function($scope, PvManager) {
    function init() {
      loadPvs();
    }

    function loadPvs() {
      $scope.anatomicSites = PvManager.getLeafPvs('anatomic-site');
    }

    init();
  });
