## External Participants Migration Script

### Note: This script will reset all the current records in 'os_staged_participants' table, it is recommended to take a DB backup before running.

### Pre-requisite:
1. Make sure the `mssql-cli` utility is installed. (Run `mssql-cli --version` to verify)
2. Make sure the remote DB and target DB is accessible.
3. Make sure the user has write privilege to the current directory.
4. Update the "Configuration" section with appropriate credentials.

### How the migration script works?
1. The script will create a 'os_staged_participant_bkp' table which is the operating table until all the records are fetched.
2. The script fetches record, in a batch of 50k, from the remote DB.
3. For each batch these records are then stored to a temporary CSV (buffer) file.
4. This file is further processed to remove the table formatting and make this file suitable for importing into MySQL.
5. The target DB (MySQL) then loads the current batch from the CSV file into the 'os_staged_participant_bkp' table.
6. The CSV (buffer) files are cleared for the next batch.
7. After all the records are pulled and processed, the 'Gender_Id' column is updated to point the catissue_permissible_value identifier.
8. The 'os_staged_participants' table is truncated.
9. All the records are moved from 'os_staged_participants_bkp' to 'os_staged_participants' table.
