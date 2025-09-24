#!/usr/bin/env python3

import mysql.connector
import logging
import sys
import argparse
import json

# Configure logging
logging.basicConfig(
    filename='truncate_insert.log',
    level=logging.INFO,
    format='%(asctime)s - %(levelname)s - %(message)s'
)

def parse_args():
    parser = argparse.ArgumentParser(description='Truncate and insert into visit_specimen_type_summary and visit_summary')
    parser.add_argument('config_file', help='Path to DB config JSON file')
    return parser.parse_args()

def load_db_config(config_path):
    try:
        with open(config_path) as f:
            return json.load(f)
    except Exception as e:
        logging.error(f"Failed to load config file: {e}")
        sys.exit(1)

def get_db_connection(config):
    try:
        return mysql.connector.connect(
            host=config['host'],
            user=config['user'],
            password=config['password'],
            database=config['database']
        )
    except mysql.connector.Error as err:
        logging.error(f"Error connecting to DB: {err}")
        sys.exit(1)

def truncate_and_insert(conn):
    truncate_visit_summary = "TRUNCATE TABLE visit_summary"
    truncate_visit_specimen_type_summary = "TRUNCATE TABLE visit_specimen_type_summary"
    truncate_sequencing_type_and_results_summary = "TRUNCATE TABLE sequencing_type_and_results_summary"
    insert_query1 = """
    INSERT INTO visit_specimen_type_summary (
        visit_id,
        specimen_type,
        count_of_aliquots
    )
    SELECT
        spmn.specimen_collection_group_id AS visit_id,
        spmn_type.value AS specimen_type,
        COUNT(*) AS count_of_aliquots
    FROM
        catissue_specimen spmn
        LEFT JOIN catissue_coll_event_param coll_event ON coll_event.specimen_id = spmn.identifier
        LEFT JOIN catissue_permissible_value spmn_type ON spmn_type.identifier = spmn.specimen_type_id
        LEFT JOIN (
            OS_SPMN_EXTN_RECS form_rec
            INNER JOIN OS_CHILD_SPMN_EVENTS child_events ON child_events.IDENTIFIER = form_rec.record_id
            AND form_rec.FORM_ID = 75
        ) ON form_rec.SPECIMEN_ID = spmn.IDENTIFIER
    WHERE
        spmn.activity_status != 'Disabled'
        AND spmn_type.value IS NOT NULL
        AND spmn.availability_status = 'Available'
        AND spmn.collection_status = 'Collected'
    AND (
         ( spmn.lineage = 'New' 
           AND child_events.IDENTIFIER IS NULL
         )
       )
    GROUP BY
      spmn.specimen_collection_group_id,
      spmn_type.value
    """

    insert_query2 = """
    INSERT INTO visit_specimen_type_summary (
        visit_id,
        specimen_type,
        count_of_aliquots
    )
    SELECT
    spmn.specimen_collection_group_id AS visit_id,
    spmn_type.value AS specimen_type,
    COUNT(*) AS count_of_aliquots
FROM
    catissue_specimen spmn
    LEFT JOIN catissue_coll_event_param coll_event ON coll_event.specimen_id = spmn.identifier
    LEFT JOIN catissue_permissible_value spmn_type ON spmn_type.identifier = spmn.specimen_type_id
WHERE
    spmn.activity_status != 'Disabled'
    AND spmn_type.value IS NOT NULL
    AND spmn.availability_status = 'Available'
    AND spmn.collection_status = 'Collected'
    AND spmn.lineage = 'Aliquot'
GROUP BY
    spmn.specimen_collection_group_id,
    spmn_type.value
    """

    update_query = """
    UPDATE visit_specimen_type_summary vsts
    JOIN CATISSUE_SPECIMEN_COLL_GROUP scg
      ON vsts.VISIT_ID = scg.IDENTIFIER
    JOIN OS_CPR_RECS cpr
      ON scg.COLLECTION_PROTOCOL_REG_ID = cpr.CPR_ID
    SET vsts.COLLECTION_AGE = TIMESTAMPDIFF(YEAR, cpr.DOB, scg.COLLECTION_TIMESTAMP)
    WHERE scg.ACTIVITY_STATUS != 'Disabled'
      AND cpr.ACTIVITY_STATUS != 'Disabled';
    """

    insert_query3 = """
    INSERT INTO visit_summary (
        SPEC_TYPE_SUMMARY_ID,
        VISIT_CUSTOM_CLINICAL_DIAGNOSIS
    )
    SELECT DISTINCT
        vsts.IDENTIFIER AS SPEC_TYPE_SUMMARY_ID,
        de.DE_A_3 AS VISIT_CUSTOM_CLINICAL_DIAGNOSIS
    FROM catissue_specimen_coll_group visit
    JOIN visit_specimen_type_summary vsts
      ON vsts.VISIT_ID = visit.IDENTIFIER
    LEFT JOIN os_visit_extn_recs visit_ext
      ON visit_ext.VISIT_ID = visit.IDENTIFIER
      AND visit_ext.FORM_ID = 928
    LEFT JOIN de_e_12200 de
      ON de.IDENTIFIER = visit_ext.RECORD_ID
    WHERE visit.activity_status = 'Active'
      AND de.DE_A_3 IS NOT NULL;
    """
    
    insert_query4="""
    INSERT INTO sequencing_type_and_results_summary (
    SPEC_TYPE_SUMMARY_ID,
    SEQUENCING_TYPE_AND_RESULTS
)
SELECT
    vsts.IDENTIFIER AS SPEC_TYPE_SUMMARY_ID,
    CONCAT(de.DE_A_2, ' (', de.DE_A_5, ')') AS SEQUENCING_TYPE_AND_RESULTS
FROM de_e_11151 de
JOIN os_spmn_extn_recs form
  ON form.RECORD_ID = de.IDENTIFIER
  AND form.FORM_ID = 226
JOIN catissue_specimen spmn
  ON spmn.IDENTIFIER = form.SPECIMEN_ID
LEFT JOIN visit_specimen_type_summary vsts
  ON vsts.VISIT_ID = spmn.SPECIMEN_COLLECTION_GROUP_ID
WHERE spmn.activity_status != 'Disabled'
  AND spmn.availability_status = 'Available'
  AND spmn.collection_status = 'Collected'
  AND (de.DE_A_2 IS NOT NULL OR de.DE_A_5 IS NOT NULL);
    """

    try:
        with conn.cursor() as cursor:
            logging.info("Disabling foreign key checks...")
            cursor.execute("SET FOREIGN_KEY_CHECKS = 0")

            logging.info("Executing TRUNCATE...")
            cursor.execute(truncate_sequencing_type_and_results_summary)
            cursor.execute(truncate_visit_summary)
            cursor.execute(truncate_visit_specimen_type_summary)
            logging.info("TRUNCATE complete.")

            logging.info("Re-enabling foreign key checks...")
            cursor.execute("SET FOREIGN_KEY_CHECKS = 1")

            logging.info("Executing INSERT into visit_specimen_type_summary...")
            cursor.execute(insert_query1)
            cursor.execute(insert_query2)
            cursor.execute(insert_query3)
            cursor.execute(insert_query4)
            logging.info("Updating COLLECTION_AGE...")
            cursor.execute(update_query)
            logging.info("Inserting into visit_summary...")
            cursor.execute(insert_query3)

            conn.commit()
            logging.info("All operations completed successfully.")
    except mysql.connector.Error as err:
        conn.rollback()
        logging.error(f"Query execution failed: {err}")
    finally:
        conn.close()

if __name__ == "__main__":
    args = parse_args()
    config = load_db_config(args.config_file)
    conn = get_db_connection(config)
    truncate_and_insert(conn)
