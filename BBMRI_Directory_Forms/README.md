## BBMRI directory forms plugin

Source code for adding extension forms towards the Collection Protocoll and Site entities.

Build instructions:

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

This is the *[Python Script and job description](https://github.com/bibbox/app-openspecimen/tree/master/data/os-plugins)*.

In order to export the filled forms towards an BBMRI directory conform EMX2-File please see:

