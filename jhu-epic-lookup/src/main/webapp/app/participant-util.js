angular.module('os.plugins.jhu-epic-lookup')
  .factory('jhuEpicParticipantUtil', function($q, Alerts) {

    function addMatchParticipantsFn(participant) {
      if (!!participant.$$epicMatchingFn) {
        return;
      }

      var matchingFn = participant.getMatchingParticipants;
      participant.getMatchingParticipants = function() {
        return matchingFn.apply(participant, [{returnThis: true}]).then(
          function(matches) {
            var hasEmpiOrMrn = !!participant.empi || hasMrn(participant);
            if (hasEmpiOrMrn && (!matches || matches.length == 0)) {
              // 
              // Prohibit users from registering participants with
              // non-existing eMPI/MRN in EPIC
              //
              Alerts.error('participant.no_matching_epic_participant');
              return $q.reject();
            }
            
            return matches.filter(function(match) {return !participant.id || participant.id != match.participant.id});
         });
      }

      participant.$$epicMatchingFn = true;
    }

    function hasMrn(participant) {
      return participant.pmis && (participant.pmis.filter(function(pmi) { return !!pmi.mrn; }).length > 0);
    }

    return {
      addMatchParticipantsFn: addMatchParticipantsFn
    }
  }); 
