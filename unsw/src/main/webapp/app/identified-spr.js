osApp.providers
  .controller('unswIdentifiedSprCtrl', function($scope, $sce, $http,
     Alerts, ApiUrls, DeleteUtil) {
    function init() {
      $scope.sprUploader = {};
      $scope.identifiedSprUrl = $sce.trustAsResourceUrl(getIdentifiedSprUrl());
      $scope.uploadMode = false;
      $http.get(getIdentifiedSprFileNameUrl()).then(function(result) {
        $scope.identifiedSprName = result.data;
      });
    }

    $scope.upload = function() {
      $scope.sprUploader.ctrl.submit().then(
        function(result) {
          Alerts.success("visits.identified_spr_uploaded", {file:result.filename});
          $scope.identifiedSprName = result.filename;
          $scope.uploadMode = false;
        }
      )
    }

    $scope.confirmDeleteSpr = function() {
      DeleteUtil.confirmDelete({
        entity: {sprName: $scope.identifiedSprName},
        templateUrl: 'plugin-ui-resources/unsw/confirm-delete-identified-spr.html',
        delete: deleteSpr
      });
    }

    $scope.showUploadMode = function() {
      $scope.uploadMode = true;
    }

    $scope.cancel = function() {
      $scope.uploadMode = false;
    }

    function deleteSpr() {
      $http.delete($scope.identifiedSprUrl).then(
        function(result) {
          if (result) {
            $scope.identifiedSprName = undefined;
          }
        }
      );
    }

    function getIdentifiedSprUrl() {
      return ApiUrls.getBaseUrl() + '/identified-spr/'+ $scope.visit.id;
    }

    function getIdentifiedSprFileNameUrl() {
       return getIdentifiedSprUrl() + '/name';
    }

    init();
  });
