
osApp.providers
  .factory('RcProject', function(osModel, $http, ApiUrls) {
    var Project = osModel('redcap-projects');

    var auditLogUrl = ApiUrls.getBaseUrl() + '/redcap-project-audit-logs';

    function getUrl(cp, project, resource) {
      var url;
      if (!!project && !!project.id) {
        url = Project.url() + '/' + project.id + '/' + resource;
      } else {
        url = Project.url() + '/collection-protocols/' + cp.id + '/' + resource;
      }

      return url;
    }

    Project.updateInstruments = function(cp, project) {
      return $http.post(getUrl(cp, project, 'instruments'), []).then(
        function(result) {
          return result.data;
        }
      );
    }

    Project.updateData = function(cp, project) {
      return $http.post(getUrl(cp, project, 'data'), {}).then(
        function(result) {
          return result.data;
        }
      );
    }

    Project.loadAuditLogs = function(cpId) {
      return $http.get(auditLogUrl, {params: {cpId: cpId}}).then(
        function(result) {
          return result.data.map(
            function(log) {
              if (!!log.failedEventsLog) {
                log.failedEventsLogUrl = auditLogUrl + '/' + log.id + '/failed-events-log';
              }

              return log;
            }
          );
        }
      );
    }

    return Project;
  });

