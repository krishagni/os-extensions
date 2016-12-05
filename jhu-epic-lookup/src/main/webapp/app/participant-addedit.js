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
          if (!$scope.partCtx.twoStepReg) {
            //Hiding MPI field
            if ($scope.cpr.participant.source != 'EPIC') {
              var empiInputEl = $document.find('input[name="empi"]');
              empiInputEl.parent().parent().hide();
            }
          }
          var uidInputEl = $document.find('input[name="uid"]');
          uidInputEl.parent().parent().hide();

        //Hiding MRN Site
          if ($scope.cpr.participant.source != 'EPIC') {
            var pmiInputEl = $document.find('div[id="pmiForm"]');
            pmiInputEl.hide();
          }

          $interval.cancel(hideFn);
        }, 100, 0, false);
      }
  );
