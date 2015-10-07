osApp.providers
  .controller('demoSignupCtrl', function($scope, $http, ApiUrls) {
    $scope.signedUp = false;
    function init() {
      $scope.user.instituteName = "A1 - For Demo Users";
      $scope.user.deptName = "Pathology";
    }
    
    $scope.signup = function() {
      $scope.user.loginName = $scope.user.emailAddress;
      $http.post(ApiUrls.getBaseUrl() + 'demo/sign-up', $scope.user).then(
        function(resp) {
          if (resp.status == 200) {
            $scope.signedUp = true;
          }
        }
      );
    }
    
    init();
  });
