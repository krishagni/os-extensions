angular.module('os.plugins.jhu-epic-lookup')
  .controller('jhuEpicParticipantLookupCtrl',
    function($scope, $controller, cp, cpr, extensionCtxt, hasDict, cpDict, twoStepReg,
      mrnAccessRestriction, addPatientOnLookupFail, lookupFieldsCfg,
      lockedFields, firstCpEvent, layout, onValueChangeCb, cpEvents, visitsTab, userRole,
      hasConsentRules, tmWorkflowId) {

      var dependencies = {
        $scope: $scope, cp: cp, cpr: cpr, extensionCtxt: extensionCtxt,
        hasDict: hasDict, cpDict: cpDict, twoStepReg: twoStepReg,
        mrnAccessRestriction: mrnAccessRestriction,
        addPatientOnLookupFail: addPatientOnLookupFail,
        lookupFieldsCfg: lookupFieldsCfg, lockedFields: lockedFields, 
        firstCpEvent: firstCpEvent, layout: layout, onValueChangeCb: onValueChangeCb,
        cpEvents: cpEvents, visitsTab: visitsTab, userRole: userRole,
        hasConsentRules: hasConsentRules, tmWorkflowId: tmWorkflowId
      }

      var participantAddEditCtrl = $controller('ParticipantAddEditCtrl', dependencies);
      $scope.fieldsCtx.lookupFields = [];
    }
  );
