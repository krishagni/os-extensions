# BBMRI directory forms plugin

## Introduction
*[MIABIS](https://github.com/BBMRI-ERIC/miabis)* describes the Minimum Information About BIobank data Sharing, developed by *[BBMRI-ERIC](https://www.bbmri-eric.eu/)*.
This extension can be used to add the respective forms to Collection Protocol and Site entities. 


## Build Instructions:

INSTALL BUILD TOOLS 

wget https://downloads.gradle-dn.com/distributions/gradle-2.0-bin.zip -P /tmp \
    && apt-get update \
    && apt-get install -y unzip curl git \
    && unzip -d /opt/gradle /tmp/gradle-*.zip \
    && curl -sL https://deb.nodesource.com/setup_10.x | bash - \
    && apt-get install -y nodejs --force-yes \
    && curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.34.0/install.sh | bash \

cd <cloned repo> \
    && gradle build \
    && cp build/libs/* <openSpecimenPluginDir> \

Restart OpenSpecimen in order to see the attached forms
- default hängt form an jedem CP und jeder Site 


## Further Resources
The process of exporting data from OpenSpecimen and data integration into the BBMRI directory can be automated using scheduled jobs in OpenSpecimen. 
In order to export the information recorded in the forms as a BBMRI directory conform EMX2-file, please refer to the following *[resource](https://github.com/bibbox/app-openspecimen/tree/master/data/os-plugins)*.
