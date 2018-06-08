
angular.module('os.plugins.msk', [])
  .run(function(PluginReg) {
    PluginReg.registerViews(
      'msk',
      {
        'specimen-addedit': {
          'info': {
            template: 'plugin-ui-resources/msk/visit-info.html'
          }
        }
      }
    );
  })

  .controller('mskSpecimenInfoCtrl', function($scope, $parse, CpConfigSvc) {
    function init() {
      $scope.info = {desc: '', parts: []};

      CpConfigSvc.getCommonCfg($scope.cp.id, 'msk').then(
        function(config) {
          var desc = $scope.info.desc = $parse((config && config.accessionDesc) || 'visit.comments')($scope.opts);
          if (!desc) {
            return;
          }

          var partInfo = desc;
          var segments = desc.split('-----\n');
          if (segments.length > 1) {
            partInfo = segments[1];
          }

          angular.forEach(partInfo.split('\n'),
            function(observation) {
              if (!observation) {
                return;
              }

              var partItems = observation.split(':', 2);
              if (partItems.length > 1) {
                $scope.info.parts.push({num: partItems[0], desc: partItems[1]});
              } else if (partItems.length > 0) {
                $scope.info.parts.push({desc: partItems[0]});
              }
            }
          );
        }
      );
    }

    init();
  });

