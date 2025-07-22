Below are the steps and the code which are set up on WCM servers.

1. WCM Custom Plugin code: (plugin name: 'zos-wcmc-5.2.0.jar')

To see the custom plugin code use the URL https://github.com/krishagni/os-extensions/tree/master/wcmc
This plugin is used to rename the OpenSpecimen core fields.
Eg: Rename 'Collection Protocol' to 'Studies', Institute to 'Institution', etc.

2. Workflow Files(JSON)
The super admin or site admin can configure UI screen layout (known as 'workflow') at the system or at specific study level. The study specific workflow file is set on test, dev and prod environment.

For more details:
https://openspecimen.atlassian.net/wiki/x/DgDcBQ

Steps to download the study workflow JOSN.

Steps:
1. Login to the OpenSpecimen.
2. Click on Studies Card.
3. Click on 'eye'icon to view the study details for 'Center for Advanced Digestive Disorders'.
4. Click on 'More' ->'Export Workflows'
It downloads the workflow to set up on 'Center for Advanced Digestive Disorders' study.

Steps to import the study level workflow.
Steps:
1. Login to the OpenSpecimen.
2. Click on Studies Card.
3. Click on 'eye'icon to view the target study details
4. Click on 'More' ->'Import Workflows'

Steps to import/export the system level workflow
Steps:
1. Login to the OpenSpecimen with Super admin account.
2. Click on 'Settings' card.
3. Search for 'System Workflows' in the search bar.
4. Click on the 'Default-system-workflow.json' file to download it.
