
osApp.providers
  .controller('rcCpConfigCtrl', function($scope, cp, RcProject, Alerts) {
    function init() {
      $scope.ctx = {
        view: 'no_projects',
        projects: [],
        project: undefined,
        auditLogs: []
      };

      RcProject.query({cpId: cp.id}).then(
        function(projects) {
          $scope.ctx.projects = projects
          listProjects();
        }
      );
    }

    function listProjects() {
      $scope.ctx.project = undefined;

      var projects = $scope.ctx.projects;
      if (!projects || projects.length == 0) {
        $scope.ctx.view = 'no_projects';
      } else if (projects.length == 1) {
        showProject(projects[0]);
        $scope.ctx.view = 'show_project';
      } else {
        $scope.ctx.view = 'list_projects';
      }
    }

    function showProject(project) {
      $scope.ctx.project = project;
      initProjectFieldsMap(project);
      $scope.ctx.view = 'show_project';
    }

    function initProjectFieldsMap(project) {
      if (!!project.uiSubjectFields || !!project.uiVisitFields) {
        return;
      }

      project.uiSubjectFields = createFieldsMap(project.subjectFields);
      project.uiVisitFields = createFieldsMap(project.visitFields);
    }

    function createFieldsMap(fields) {
      var result = [];

      if (!fields) {
        return result;
      }

      fields = fields.split(/[,|\n|\r|\t]+/);
      angular.forEach(fields,
        function(field) {
          var kv = field.split(/=/);
          result.push({rcField: kv[0], opsmnField: kv[1]});
        }
      );

      return result;
    }

    $scope.addProject = function() {
      $scope.ctx.project = new RcProject({cpId: cp.id});
      $scope.ctx.view = 'addedit_project';
    }

    $scope.editProject = function(project) {
      $scope.ctx.project = angular.copy(project);
      $scope.ctx.view = 'addedit_project';
    }

    $scope.showProject = showProject;

    $scope.saveProject = function() {
      var isNewProj = !$scope.ctx.project.id;
      delete $scope.ctx.project.uiSubjectFields;
      delete $scope.ctx.project.uiVisitFields;

      $scope.ctx.project.$saveOrUpdate().then(
        function(savedProj) {
          if (isNewProj) {
            $scope.ctx.projects.push(savedProj);
          } else {
            var projs = $scope.ctx.projects;
            for (var i = 0; i < projs.length; ++i) {
              if (projs[i].id == savedProj.id) {
                angular.extend(projs[i], savedProj);
                break;
              }
            }
          }

          listProjects();
        }
      );
    }

    $scope.listProjects = listProjects;

    $scope.synchronizeMetadata = function(project) {
      RcProject.updateInstruments(cp, project).then(
        function() {
          Alerts.add('REDCap plugin background job queued to update data collection instruments', 'success');
        }
      );
    }

    $scope.synchronizeData = function(project) {
      RcProject.updateData(cp, project).then(
        function() {
          Alerts.add('REDCap plugin background job queued to update project records', 'success');
        }
      );
    }

    $scope.loadAuditLogs = function() {
      RcProject.loadAuditLogs(cp.id).then(
        function(auditLogs) {
          $scope.ctx.auditLogs = auditLogs;
        }
      );
    }
          
    init();
  });
