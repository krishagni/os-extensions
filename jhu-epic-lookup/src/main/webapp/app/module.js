
angular.module('os.plugins.jhu-epic-lookup', ['openspecimen'])
  .config(function($stateProvider) {
    $stateProvider
      .state('jhu-epic-lookup-participant-addedit', {
        url: '/jhu-epic-lookup-participant-addedit',
        templateUrl: 'plugin-ui-resources/jhu-epic-lookup/participant-addedit.html',
        controller: 'jhuEpicParticipantLookupCtrl',
        resolve: {
          extensionCtxt: function(cp, Participant) {
            return Participant.getExtensionCtxt({cpId: cp.id});
          },
          addPatientOnLookupFail: function(SettingUtil) {
            return SettingUtil.getSetting('biospecimen', 'add_patient_on_lookup_fail').then(
              function(setting) {
                return setting.value == 'true';
              }
            );
          },
          lockedFields: function(cpr, CpConfigSvc) {
            var participant = cpr.participant || {};
            return CpConfigSvc.getLockedParticipantFields(participant.source || 'OpenSpecimen');
          },
          firstCpEvent: function(cp, cpr, CollectionProtocolEvent) {
            if (!!cpr.id) {
              return null;
            }

            return CollectionProtocolEvent.listFor(cp.id).then(
              function(events) {
                return events.length > 0 ? events[0] : null;
              }
            );
          },
          lookupFieldsCfg: function() {
            return {configured: true, fields: []};
          },
          cpEvents: function(cp, cpr, CollectionProtocolEvent) {
            if (!!cpr.id) {
              return null;
            }

            return CollectionProtocolEvent.listFor(cp.id).then(
              function(events) {
                return events.filter(function(event) { return event.activityStatus == 'Active'; });
              }
            );
          }
        },
        parent: 'participant-root'
      }
    )
  }).run(function(PluginReg) {
    PluginReg.registerViews('jhu-epic-lookup', {
      'participant-addedit': {
        'page-body': {
          template: 'plugin-ui-resources/jhu-epic-lookup/participant-addedit.html'
        }
      },

      'participant-detail': {
        'page-commands': {
          template: 'plugin-ui-resources/jhu-epic-lookup/participant-refresh.html'
        }
      },

      'participant-list': {
        'page-header': {
          template: 'plugin-ui-resources/jhu-epic-lookup/participant-lookup-button.html'
        }
      }
    });
  });
