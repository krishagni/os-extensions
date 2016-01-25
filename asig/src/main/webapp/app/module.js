angular.module('os.plugins.svh', ['openspecimen'])
  .run(function(PluginReg) {
    PluginReg.registerViews(
      'svh',
      {
		    'participant-list': {
		      'header': {
		        template: 'plugin-ui-resources/svh/participant-list-header.html'
		      }
		    },
		    
		    'participant-detail': {
		      'summary': {
		        template: 'plugin-ui-resources/svh/participant-summary.html'
		      }
		    },
		    
		    'participant-addedit': {
		      'page-body': {
		        template: 'plugin-ui-resources/svh/participant-edit.html'
		      }
		    },
		    
		    'specimen-detail': {
		      'overview': {
		        template: 'plugin-ui-resources/svh/specimen/overview.html'
		      }
		    },
		    
		    'specimen-addedit': {
		      'page-body': {
		        template: 'plugin-ui-resources/svh/specimen/addedit.html'
		      }
		    },
		    
		    'specimen-create-derivative': {
		      'page-body': {
		        template: 'plugin-ui-resources/svh/specimen/add-derivative.html'
		      }
		    },
		    
		    'specimen-create-aliquots': {
		      'page-body': {
		        template: 'plugin-ui-resources/svh/specimen/add-aliquots.html'
		      }
		    }
		  }
		);
  });
