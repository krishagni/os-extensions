angular.module('os.plugins.jhu-epic-lookup')
  .controller('jhuEpicParticipantAddEditCtrl',
    function($scope, $q, $interval, $document, Participant, Alerts) {
      $scope.partCtx.includeSiteTypes = ['EPIC'];

      var participant = $scope.cpr.participant;
      var matchingFn = participant.getMatchingParticipants;

      participant.getMatchingParticipants = function() {
        var copy = new Participant(participant);
        angular.extend(copy, {getMatchingParticipants: matchingFn});
        var matchPromise = $q.defer();
        return copy.getMatchingParticipants({returnThis: true}).then(
          function(matches) {
           if ((!matches || matches.length == 0) && (!!participant.empi || !!participant.mrn)) {
             Alerts.error("i18n key for : No matching participant {empi | mrn}");
             $q.reject("i18n key for : No matching participant {empi | mrn}");
           }

           return matches.filter(function(match) {return !participant.id || participant.id != match.participant.id});
         });
      }

      var hideFn = $interval(
        function() {
          //
          // UID field is not used
          //
          var uidInputEl = $document.find('input[name="uid"]');
          uidInputEl.parent().parent().hide();

          if (!$scope.partCtx.twoStepReg) {
            //
            // Hide MPI field in regular workflow for participants
            // not sourced from EPIC
            //
            if ($scope.cpr.participant.source != 'EPIC' && !$scope.cpr.participant.id) {
              var empiInputEl = $document.find('input[name="empi"]');
              empiInputEl.parent().parent().hide();
            }
          }
          //
          // No MRNs for non-EPIC participants
          //
          if ($scope.cpr.participant.source != 'EPIC' && !$scope.cpr.participant.id) {
            var pmiInputEl = $document.find('div[id="pmiForm"]');
            pmiInputEl.hide();
          }

          if (uidInputEl.length > 0) {
            //
            // Cancel only when we are sure all required fields are hidden
            //
            $interval.cancel(hideFn);
          }
        }, 100, 0, false);
    }
  );
