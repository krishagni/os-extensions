#!/bin/bash

################################# Configuration #################################
startAt=0
startTime=$(date +%s)
# Output CSV filepath
outputCsv=`pwd`"/output.csv"
cleanedOutputCsv=`pwd`"/processed_output.csv"
# Remote Db credentials (Loading from...)
remoteDbServerName="<remoteDbServerName>"
remoteDbName="<remoteDbName>"
remoteDbUsername="<remoteDbUsername>"
remoteDbPassword="<remoteDbPassword>"
# Target DB (MySql) credentials (Loading into...)
targetDbUsername="<targetDbUsername>"
targetDbHostname="<targetDbHostname>"
targetDbPassword="<targetDbPassword>"
targetDbName="<targetDbName>"
# Total number of records in the remote DB and batchSize
batchSize=50000
################################# Configuration #################################

# Getting the total rows from the remote DB
totalRows=`mssql-cli -S "$remoteDbServerName" -d "$remoteDbName" -U "$remoteDbUsername" -P "$remoteDbPassword" -Q 'select count(*) from os.EPIC_CLARITY_OS_STAGING' | tail -3 | head -1 | tr -d -c 0-9`

# Create os_staged_participants_bkp if not exist
echo "Creating 'os_staged_participants_bkp' table if not exist.."
mysql -u"$targetDbUsername" -h"$targetDbHostname" -p"$targetDbPassword" "$targetDbName" -e "CREATE TABLE IF NOT EXISTS os_staged_participants_bkp LIKE os_staged_participants;"

# Truncate old data from os_staged_participants_bkp
echo "Cleaning up 'os_staged_participants_bkp' table..."
mysql -u"$targetDbUsername" -h"$targetDbHostname" -p"$targetDbPassword" "$targetDbName" -e "TRUNCATE os_staged_participants_bkp;"

# Pulling data from remote DB into CSV and loading into 'os_staged_participants_bkp' table
while [ "$startAt" -lt $totalRows ]
do
  echo "Export the data in the file.."
  mssql-cli -S "$remoteDbServerName" -d "$remoteDbName" -U "$remoteDbUsername" -P "$remoteDbPassword" -Q 'select PAT_FIRST_NAME, PAT_LAST_NAME, PAT_MIDDLE_NAME, Sex, BIRTH_DATE, MRN from os.EPIC_CLARITY_OS_STAGING ORDER BY PAT_ID OFFSET '$startAt' ROWS FETCH NEXT '$batchSize' ROWS ONLY;' -o $outputCsv

  echo "Cleaning up the file.."
  sed -e 's/\s\+|/|/g' $outputCsv | sed -e 's/|\s\+/|/g' | sed -e 's/|//' | sed '$d' | sed '$d' > $cleanedOutputCsv

  echo "Import the csv file in MySQL table"
  mysql -u"$targetDbUsername" -h"$targetDbHostname" -p"$targetDbPassword" "$targetDbName" -e "
  LOAD DATA LOCAL INFILE '$cleanedOutputCsv' 
  INTO TABLE os_staged_participants_bkp  
  FIELDS TERMINATED BY '|' 
  LINES TERMINATED BY '\n'
  IGNORE 3 LINES
  (@PAT_FIRST_NAME, @PAT_LAST_NAME, @PAT_MIDDLE_NAME, @Sex, @BIRTH_DATE, @MRN)
  set LAST_NAME = if (@PAT_LAST_NAME = '', NULL, @PAT_LAST_NAME),
  FIRST_NAME = if (@PAT_FIRST_NAME = '', NULL, @PAT_FIRST_NAME),
  MIDDLE_NAME = if (@PAT_MIDDLE_NAME = '', NULL, @PAT_MIDDLE_NAME),
  BIRTH_DATE = if (@BIRTH_DATE = NULL, NULL, STR_TO_DATE(date(@BIRTH_DATE),'%Y-%c-%d')),
  GENDER = if (@Sex = '', NULL, @Sex),
  GENOTYPE = NULL,
  NUID = NULL,
  ACTIVITY_STATUS = 'Active',
  DEATH_DATE = NULL,
  VITAL_STATUS = NULL,
  EMPI_ID = @MRN,
  UPDATED_TIME = (SELECT NOW()),
  SOURCE = 'wcmc',
  GENDER_ID = NULL,
  VITAL_STATUS_ID = NULL;"

  echo "Remove the CSV files"
  rm $outputCsv
  rm $cleanedOutputCsv

  mysql -u"$targetDbUsername" -h"$targetDbHostname" -p"$targetDbPassword" "$targetDbName" -e "ALTER TABLE os_staged_participants_bkp AUTO_INCREMENT = 1;"

  #Incrementing the count by batchSize.
  startAt=`expr $startAt + $batchSize`
done

# Updating the 'Gender_Id' column to point the catissue_permissible_value identifier
echo "Updating the Gender_ID"
echo "Updating gender_id for unknown gender..."
mysql -u"$targetDbUsername" -h"$targetDbHostname" -p"$targetDbPassword" "$targetDbName" -e "update os_staged_participants_bkp set GENDER_ID=(SELECT identifier FROM catissue_permissible_value WHERE public_id='Gender' AND value='Unknown') where GENDER='Unknown';"

echo "Updating gender_id for Nonbinary gender..."
mysql -u"$targetDbUsername" -h"$targetDbHostname" -p"$targetDbPassword" "$targetDbName" -e "update os_staged_participants_bkp set GENDER_ID=(SELECT identifier FROM catissue_permissible_value WHERE public_id='Gender' AND value='Nonbinary') where GENDER='Nonbinary';"

echo "Updating gender_id for X gender..."
mysql -u"$targetDbUsername" -h"$targetDbHostname" -p"$targetDbPassword" "$targetDbName" -e "update os_staged_participants_bkp set GENDER_ID=(SELECT identifier FROM catissue_permissible_value WHERE public_id='Gender' AND value='X') where GENDER='X';"

echo "Updating gender_id for Male gender..."
mysql -u"$targetDbUsername" -h"$targetDbHostname" -p"$targetDbPassword" "$targetDbName" -e "update os_staged_participants_bkp set GENDER_ID=(SELECT identifier FROM catissue_permissible_value WHERE public_id='Gender' AND value='Male') where GENDER='Male';"

echo "Updating gender_id for Female gender..."
mysql -u"$targetDbUsername" -h"$targetDbHostname" -p"$targetDbPassword" "$targetDbName" -e "update os_staged_participants_bkp set GENDER_ID=(SELECT identifier FROM catissue_permissible_value WHERE public_id='Gender' AND value='Female') where GENDER='Female';"

echo "Updating gender_id for Intersex gender..."
mysql -u"$targetDbUsername" -h"$targetDbHostname" -p"$targetDbPassword" "$targetDbName" -e "update os_staged_participants_bkp set GENDER_ID=(SELECT identifier FROM catissue_permissible_value WHERE public_id='Gender' AND value='Intersex') where GENDER='Intersex';"

# Moving everything from 'os_staged_participants_bkp' to 'os_staged_participants' table
echo "Truncating orignal table"
mysql -u $targetDbUsername -h $targetDbHostname -p $targetDbPassword $targetDbName -e  "SET FOREIGN_KEY_CHECKS=0; truncate table os_staged_participants;"

echo "Moving the data from backup table to orignal table"
mysql -u $targetDbUsername -h $targetDbHostname -p $targetDbPassword $targetDbName -e  "INSERT INTO os_staged_participants SELECT * FROM os_staged_participants_bkp;"

endTime=$(date +%s)

echo "It took $(($endTime - $startTime)) seconds to complete the import."
