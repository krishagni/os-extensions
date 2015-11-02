osApp.providers
  .controller('unswIdentifiedSprCtrl', function($scope, $sce, $http,
     Alerts, ApiUrls, ApiUtil, DeleteUtil) {
	  
    function init() {
      $scope.sprUploader = {};
      $scope.sprUploadUrl = $sce.trustAsResourceUrl(ApiUrls.getBaseUrl() + 'form-files');
      loadIdentifiedSpr();
      $scope.uploadMode = false;
    }

    function loadIdentifiedSpr() {
      var url = ApiUrls.getBaseUrl() + '/identified-spr/'+ $scope.visit.id;
      $http.get(url).then(function(result) {
        $scope.sprFormDetail = result.data;
        var formData = $scope.sprFormDetail.formData;
        $scope.identifiedSprName = formData ? formData.fileUpload.filename : undefined;
        $scope.identifiedSprUrl = getDownloadSprUrl($scope.sprFormDetail);
      });
    }

    function getDownloadSprUrl(sprFormDetail) {
      var url = ApiUrls.getBaseUrl() + 'form-files?';
      url = url + 'formId=' + sprFormDetail.formId + '&recordId=' + sprFormDetail.recordId + '&ctrlName=fileUpload';
      return $sce.trustAsResourceUrl(url);
    }

    $scope.showUploadMode = function() {
      $scope.uploadMode = true;
    }

    $scope.cancel = function() {
      $scope.uploadMode = false;
    }

    $scope.upload = function() {
      $scope.sprUploader.ctrl.submit().then(
        function(response) {
          var formData = {
            appData: {formCtxtId: $scope.sprFormDetail.formContextId, objectId: $scope.visit.id},
            fileUpload: {filename: response.filename, contentType: response.contentType, fileId: response.fileId},
            recordId: $scope.sprFormDetail.recordId || undefined
          };

          var saveDataUrl = ApiUrls.getBaseUrl() + '/forms/' + $scope.sprFormDetail.formId + '/data';
          $http.put(saveDataUrl, formData).then(function(result) {
            Alerts.success("visits.spr_uploaded", {file:response.filename});
            $scope.sprFormDetail.recordId = result.data.id;
            $scope.identifiedSprName = result.data.fileUpload.filename;
            $scope.identifiedSprUrl = getDownloadSprUrl($scope.sprFormDetail);
            $scope.uploadMode = false;
          });
        }
      )
    }

    $scope.confirmDeleteSpr = function() {
      DeleteUtil.confirmDelete({
        entity: {sprName: $scope.identifiedSprName},
        templateUrl: 'modules/biospecimen/participant/visit/confirm-delete-spr-file.html',
        delete: deleteSpr
      });
    }

    function deleteSpr() {
      var url = ApiUrls.getBaseUrl() + '/forms/' + $scope.visit.id + '/data/' + $scope.sprFormDetail.recordId;
      url = $sce.trustAsResourceUrl(url);
      $http.delete(url).then(
        function(result) {
          if (result) {
            $scope.identifiedSprName = undefined;
            $scope.sprFormDetail.recordId = undefined;
          }
        }
      );
    }

    init();
  });