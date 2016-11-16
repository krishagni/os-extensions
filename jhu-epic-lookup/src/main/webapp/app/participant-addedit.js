angular.module('os.plugins.jhu-epic-lookup')
  .controller('jhuEpicParticipantAddEditCtrl',
    function($scope, $interval, $document) {
      if ($scope.partCtx.twoStepReg) {
        $scope.partCtx.includeSiteTypes = ['EPIC'];
      } else {
        $scope.partCtx.excludeSiteTypes = ['EPIC'];
      }

      var hideFn = $interval(
        function() {
          console.log("Attempt to hide SSN...");
          var uidInputEl = $document.find('input[name="uid"]');
          uidInputEl.parent().parent().hide();
          if (uidInputEl.length > 0) {
            console.log ("Cancel...");
            $interval.cancel(hideFn);
          }
        }, 100, 0, false);
    }
  );
